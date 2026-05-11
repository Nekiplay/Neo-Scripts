package com.nekiplay.neoscripts.sugar

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import kotlin.jvm.optionals.getOrDefault

fun Component.getJsonString(): String {
    val flatList = this.toFlatList()

    if (flatList.isEmpty()) {
        return "{\"text\":\"\"}"
    }

    fun buildSegmentJson(comp: Component): JsonObject {
        val obj = JsonObject()
        obj.addProperty("text", comp.string)

        val style = comp.style

        // Цвет
        style.color?.let { color ->
            val vanillaColor = ChatFormatting.entries
                .firstOrNull { it.isColor && it.color != null && it.color == color.value }
            obj.addProperty(
                "color",
                vanillaColor?.name?.lowercase() ?: String.format("#%06X", color.value)
            )
        }

        // Форматирование
        if (style.isBold) obj.addProperty("bold", true)
        if (style.isItalic) obj.addProperty("italic", true)
        if (style.isUnderlined) obj.addProperty("underlined", true)
        if (style.isStrikethrough) obj.addProperty("strikethrough", true)
        if (style.isObfuscated) obj.addProperty("obfuscated", true)

        // ClickEvent
        style.clickEvent?.let { click ->
            val clickObj = JsonObject()
            when (click) {
                is ClickEvent.OpenUrl -> {
                    clickObj.addProperty("action", "open_url")
                    clickObj.addProperty("value", click.uri.rawQuery.toString())
                }
                is ClickEvent.OpenFile -> {
                    clickObj.addProperty("action", "open_file")
                    clickObj.addProperty("value", click.path)
                }
                is ClickEvent.RunCommand -> {
                    clickObj.addProperty("action", "run_command")
                    clickObj.addProperty("value", click.command)
                }
                is ClickEvent.SuggestCommand -> {
                    clickObj.addProperty("action", "suggest_command")
                    clickObj.addProperty("value", click.command)
                }
                is ClickEvent.ChangePage -> {
                    clickObj.addProperty("action", "change_page")
                    clickObj.addProperty("value", click.page.toString())
                }
                is ClickEvent.CopyToClipboard -> {
                    clickObj.addProperty("action", "copy_to_clipboard")
                    clickObj.addProperty("value", click.value)
                }
            }
            obj.add("clickEvent", clickObj)
        }

        // HoverEvent
        style.hoverEvent?.let { hover ->
            val hoverObj = JsonObject()
            when (hover) {
                is HoverEvent.ShowText -> {
                    hoverObj.addProperty("action", "show_text")
                    // Рекурсивно сериализуем вложенный Component
                    hoverObj.add(
                        "contents",
                        JsonParser.parseString(hover.value.getJsonString())
                    )
                }
                is HoverEvent.ShowItem -> {
                    hoverObj.addProperty("action", "show_item")
                    val itemObj = JsonObject()
                    itemObj.addProperty("id", hover.item.item.toString())
                    itemObj.addProperty("count", hover.item.count)
                    hoverObj.add("contents", itemObj)
                }
                is HoverEvent.ShowEntity -> {
                    hoverObj.addProperty("action", "show_entity")
                    val entityObj = JsonObject()
                    entityObj.addProperty("type", hover.entity.type.toString())
                    entityObj.addProperty("uuid", hover.entity.uuid.toString())
                    entityObj.addProperty("name", hover.entity.name.toString())
                    hover.entity.name?.let { name ->
                        entityObj.add(
                            "name",
                            JsonParser.parseString(name.getOrDefault(Component.empty()).getJsonString())
                        )
                    }
                    hoverObj.add("contents", entityObj)
                }
            }
            obj.add("hoverEvent", hoverObj)
        }

        // Insertion
        style.insertion?.let { obj.addProperty("insertion", it) }

        return obj
    }

    // Первый сегмент — корень
    val root = buildSegmentJson(flatList[0])

    // Остальные — в extra
    if (flatList.size > 1) {
        val extra = JsonArray()
        for (i in 1 until flatList.size) {
            extra.add(buildSegmentJson(flatList[i]))
        }
        root.add("extra", extra)
    }

    return Gson().toJson(root)
}

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