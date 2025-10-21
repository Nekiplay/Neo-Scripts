package com.nekiplay.hypixelcry.sugar

import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting

fun Text.getFormattedString(): String {
    val sb = StringBuilder()
    val ordered: OrderedText = this.asOrderedText()

    var currentColor: Formatting? = null
    var currentStyles = mutableSetOf<Formatting>()

    ordered.accept { _: Int, style: Style?, codePoint: Int ->
        val char = Character.toChars(codePoint)[0]

        // Определяем новый цвет
        val newColor = if (style?.getColor() != null) {
            nearestVanillaColor(style.getColor()!!.rgb)
        } else {
            null
        }

        // Определяем новые стили
        val newStyles = mutableSetOf<Formatting>()
        if (style?.isBold == true) newStyles.add(Formatting.BOLD)
        if (style?.isItalic == true) newStyles.add(Formatting.ITALIC)
        if (style?.isUnderlined == true) newStyles.add(Formatting.UNDERLINE)
        if (style?.isStrikethrough == true) newStyles.add(Formatting.STRIKETHROUGH)
        if (style?.isObfuscated == true) newStyles.add(Formatting.OBFUSCATED)

        // Если цвет изменился или это начало строки
        if (newColor != currentColor || sb.isEmpty()) {
            if (newColor != null) {
                sb.append('§').append(newColor.code)
            } else if (currentColor != null) {
                // Сбрасываем цвет, если он был установлен, но теперь null
                sb.append('§').append(Formatting.RESET.code)
            }
            currentColor = newColor
        }

        // Добавляем стили, которые появились
        for (styleFlag in newStyles) {
            if (!currentStyles.contains(styleFlag)) {
                sb.append('§').append(styleFlag.code)
            }
        }

        // Убираем стили, которые пропали (кроме RESET, который сбрасывает всё)
        for (styleFlag in currentStyles) {
            if (!newStyles.contains(styleFlag)) {
                // Вместо удаления отдельных стилей используем RESET и применяем заново
                // Это проще, чем отслеживать каждый стиль отдельно
                if (newStyles.isNotEmpty() || newColor != null) {
                    sb.append('§').append(Formatting.RESET.code)
                    if (newColor != null) {
                        sb.append('§').append(newColor.code)
                    }
                    for (s in newStyles) {
                        sb.append('§').append(s.code)
                    }
                }
                break
            }
        }

        currentStyles = newStyles

        // Добавляем символ
        sb.append(char)
        true
    }
    return sb.toString()
}

private fun nearestVanillaColor(rgb: Int): Formatting? {
    var best: Formatting? = null
    var bestDist = Long.MAX_VALUE
    for (f in Formatting.entries) {
        if (!f.isColor) continue
        val frgb: Int = f.colorValue ?: continue
        if (frgb == -1) continue
        val dr = (((rgb shr 16) and 0xFF) - ((frgb shr 16) and 0xFF)).toLong()
        val dg = (((rgb shr 8) and 0xFF) - ((frgb shr 8) and 0xFF)).toLong()
        val db = ((rgb and 0xFF) - (frgb and 0xFF)).toLong()
        val dist = dr * dr + dg * dg + db * db
        if (dist < bestDist) {
            bestDist = dist
            best = f
        }
    }
    return best
}