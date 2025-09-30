package com.nekiplay.hypixelcry.sugar

import com.mojang.brigadier.LiteralMessage
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.text.TranslatableTextContent

fun Text.getFormattedString(): String {
    val result = StringBuilder()

    // Рекурсивная функция для обработки текста и его компонентов
    fun processText(currentText: Text, inheritedStyle: Style = Style.EMPTY) {
        val style = inheritedStyle.withParent(currentText.style)

        // Обрабатываем цвет
        style.color?.let { textColor ->
            val colorCode = getMinecraftColorCode(textColor)
            if (colorCode.isNotEmpty()) {
                result.append(colorCode)
            }
        }

        // Обрабатываем форматирование
        if (style.isBold) result.append("§l")
        if (style.isItalic) result.append("§o")
        if (style.isUnderlined) result.append("§n")
        if (style.isStrikethrough) result.append("§m")
        if (style.isObfuscated) result.append("§k")

        // Добавляем содержимое текста, если это литеральный текст
        if (currentText is LiteralMessage) {
            result.append(currentText.string)
        } else if (currentText is TranslatableTextContent) {
            // Для переводимого текста можно добавить специальную обработку
            result.append(currentText.key)
        }

        // Обрабатываем дочерние компоненты
        for (sibling in currentText.siblings) {
            processText(sibling, style)
        }
    }

    processText(this)
    return result.toString()
}

private fun getMinecraftColorCode(textColor: TextColor): String {
    // Используем маппинг стандартных цветов Minecraft
    return when (textColor.rgb) {
        0x000000 -> "§0" // BLACK
        0x0000AA -> "§1" // DARK_BLUE
        0x00AA00 -> "§2" // DARK_GREEN
        0x00AAAA -> "§3" // DARK_AQUA
        0xAA0000 -> "§4" // DARK_RED
        0xAA00AA -> "§5" // DARK_PURPLE
        0xFFAA00 -> "§6" // GOLD
        0xAAAAAA -> "§7" // GRAY
        0x555555 -> "§8" // DARK_GRAY
        0x5555FF -> "§9" // BLUE
        0x55FF55 -> "§a" // GREEN
        0x55FFFF -> "§b" // AQUA
        0xFF5555 -> "§c" // RED
        0xFF55FF -> "§d" // LIGHT_PURPLE
        0xFFFF55 -> "§e" // YELLOW
        0xFFFFFF -> "§f" // WHITE
        else -> "" // Для неизвестных цветов
    }
}