package com.nekiplay.hypixelcry.sugar

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

fun Component.getFormattedString(): String {
    val sb = StringBuilder()
    val ordered = this.visualOrderText

    var currentColor: ChatFormatting? = null
    var currentStyles = mutableSetOf<ChatFormatting>()

    ordered.accept { _: Int, style: Style?, codePoint: Int ->
        val char = Character.toChars(codePoint)[0]

        // Определяем новый цвет
        val newColor = if (style?.getColor() != null) {
            nearestVanillaColor(style.getColor()!!.value)
        } else {
            null
        }

        // Определяем новые стили
        val newStyles = mutableSetOf<ChatFormatting>()
        if (style?.isBold == true) newStyles.add(ChatFormatting.BOLD)
        if (style?.isItalic == true) newStyles.add(ChatFormatting.ITALIC)
        if (style?.isUnderlined == true) newStyles.add(ChatFormatting.UNDERLINE)
        if (style?.isStrikethrough == true) newStyles.add(ChatFormatting.STRIKETHROUGH)
        if (style?.isObfuscated == true) newStyles.add(ChatFormatting.OBFUSCATED)

        // Если цвет изменился или это начало строки
        if (newColor != currentColor || sb.isEmpty()) {
            if (newColor != null) {
                sb.append('§').append(newColor.char)
            } else if (currentColor != null) {
                // Сбрасываем цвет, если он был установлен, но теперь null
                sb.append('§').append(ChatFormatting.RESET.char)
            }
            currentColor = newColor
        }

        // Добавляем стили, которые появились
        for (styleFlag in newStyles) {
            if (!currentStyles.contains(styleFlag)) {
                sb.append('§').append(styleFlag.char)
            }
        }

        // Убираем стили, которые пропали (кроме RESET, который сбрасывает всё)
        for (styleFlag in currentStyles) {
            if (!newStyles.contains(styleFlag)) {
                // Вместо удаления отдельных стилей используем RESET и применяем заново
                // Это проще, чем отслеживать каждый стиль отдельно
                if (newStyles.isNotEmpty() || newColor != null) {
                    sb.append('§').append(ChatFormatting.RESET.char)
                    if (newColor != null) {
                        sb.append('§').append(newColor.char)
                    }
                    for (s in newStyles) {
                        sb.append('§').append(s.char)
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

private fun nearestVanillaColor(rgb: Int): ChatFormatting? {
    var best: ChatFormatting? = null
    var bestDist = Long.MAX_VALUE
    for (f in ChatFormatting.entries) {
        if (!f.isColor) continue
        val frgb: Int = f.color ?: continue
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