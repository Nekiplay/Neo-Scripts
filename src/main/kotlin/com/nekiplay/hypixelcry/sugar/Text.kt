package com.nekiplay.hypixelcry.sugar

import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting


fun Text.getFormattedString(): String {
    val sb = StringBuilder()
    val ordered: OrderedText = this.asOrderedText()
    ordered.accept { _: Int, style: Style?, codePoint: Int ->
        // Применяем цвет
        if (style!!.getColor() != null) {
            val rgb = style.getColor()!!.rgb
            // Пытаемся подобрать ближайший ванильный цвет
            val nearest: Formatting? = nearestVanillaColor(rgb)
            if (nearest != null) {
                sb.append('§').append(nearest.code)
            } else {
                // Либо используем hex-коды MiniMessage-стиля — но ванильный клиент §-hex не понимает.
                // Тогда можно просто сбросить к белому или игнорировать.
            }
        }
        // Применяем стилевые флаги
        if (style.isBold) sb.append('§').append(Formatting.BOLD.code)
        if (style.isItalic) sb.append('§').append(Formatting.ITALIC.code)
        if (style.isUnderlined) sb.append('§').append(Formatting.UNDERLINE.code)
        if (style.isStrikethrough) sb.append('§').append(Formatting.STRIKETHROUGH.code)
        if (style.isObfuscated) sb.append('§').append(Formatting.OBFUSCATED.code)

        // Добавляем символ
        sb.append(Character.toChars(codePoint))
        true
    }
    return sb.toString()
}

private fun nearestVanillaColor(rgb: Int): Formatting? {
    var best: Formatting? = null
    var bestDist = Long.MAX_VALUE
    for (f in Formatting.entries) {
        if (!f.isColor) continue
        val frgb: Int = f.colorValue!! // в 1.21+ доступен для цветовых форматирований
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