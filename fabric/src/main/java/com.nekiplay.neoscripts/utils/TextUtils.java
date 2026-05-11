package com.nekiplay.neoscripts.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class TextUtils {
    public static String textToFormattingString(Component text) {
        StringBuilder result = new StringBuilder();

        // Рекурсивно обрабатываем все части текста
        for (Component sibling : text.getSiblings()) {
            // Получаем стиль текущего текста
            Style style = sibling.getStyle();

            // Добавляем коды форматирования
            if (style.getColor() != null) {
                ChatFormatting formatting = ChatFormatting.getByName(style.getColor().serialize());
                if (formatting != null) {
                    result.append("§").append(formatting.getChar());
                }
            }

            result.append( style.isBold() ? "§l" : "" );
            result.append( style.isItalic() ? "§o" : "" );
            result.append( style.isUnderlined() ? "§n" : "" );
            result.append( style.isStrikethrough() ? "§m" : "" );
            result.append( style.isObfuscated() ? "§k" : "" );
        }

        return result.toString();
    }

}
