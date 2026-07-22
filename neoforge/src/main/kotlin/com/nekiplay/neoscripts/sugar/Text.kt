package com.nekiplay.neoscripts.sugar

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import kotlin.jvm.optionals.getOrDefault

private val VANILLA_COLORS_JSON = mapOf(
    0x000000 to "black",
    0x0000AA to "dark_blue",
    0x00AA00 to "dark_green",
    0x00AAAA to "dark_aqua",
    0xAA0000 to "dark_red",
    0xAA00AA to "dark_purple",
    0xFFAA00 to "gold",
    0xAAAAAA to "gray",
    0x555555 to "dark_gray",
    0x5555FF to "blue",
    0x55FF55 to "green",
    0x55FFFF to "aqua",
    0xFF5555 to "red",
    0xFF55FF to "light_purple",
    0xFFFF55 to "yellow",
    0xFFFFFF to "white"
)

private val VANILLA_COLORS_CHAR = mapOf(
    0x000000 to '0',
    0x0000AA to '1',
    0x00AA00 to '2',
    0x00AAAA to '3',
    0xAA0000 to '4',
    0xAA00AA to '5',
    0xFFAA00 to '6',
    0xAAAAAA to '7',
    0x555555 to '8',
    0x5555FF to '9',
    0x55FF55 to 'a',
    0x55FFFF to 'b',
    0xFF5555 to 'c',
    0xFF55FF to 'd',
    0xFFFF55 to 'e',
    0xFFFFFF to 'f'
)

enum class LegacyFormat(val char: Char) {
    BOLD('l'),
    ITALIC('o'),
    UNDERLINE('n'),
    STRIKETHROUGH('m'),
    OBFUSCATED('k')
}

fun Component.getJsonString(): String {
    val flatList = this.toFlatList()

    if (flatList.isEmpty()) {
        return "{\"text\":\"\"}"
    }

    fun buildSegmentJson(comp: Component): JsonObject {
        val obj = JsonObject()
        obj.addProperty("text", comp.string)

        val style = comp.style

        style.color?.let { color ->
            val colorVal = color.value
            val vanillaName = VANILLA_COLORS_JSON[colorVal]
            obj.addProperty(
                "color",
                vanillaName ?: String.format("#%06X", colorVal)
            )
        }

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

    val root = buildSegmentJson(flatList[0])

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

    var currentColorChar: Char? = null
    var currentStyles = mutableSetOf<LegacyFormat>()

    ordered.accept { _: Int, style: Style?, codePoint: Int ->
        val char = Character.toChars(codePoint)[0]

        // Определяем новый цвет (через ближайший char)
        val newColorChar = style?.color?.let { nearestVanillaColorChar(it.value) }

        // Определяем новые стили
        val newStyles = mutableSetOf<LegacyFormat>()
        if (style?.isBold == true) newStyles.add(LegacyFormat.BOLD)
        if (style?.isItalic == true) newStyles.add(LegacyFormat.ITALIC)
        if (style?.isUnderlined == true) newStyles.add(LegacyFormat.UNDERLINE)
        if (style?.isStrikethrough == true) newStyles.add(LegacyFormat.STRIKETHROUGH)
        if (style?.isObfuscated == true) newStyles.add(LegacyFormat.OBFUSCATED)

        // Если цвет изменился или это начало строки
        if (newColorChar != currentColorChar || sb.isEmpty()) {
            if (newColorChar != null) {
                sb.append('§').append(newColorChar)
            } else if (currentColorChar != null) {
                // Сбрасываем цвет
                sb.append('§').append('r')
            }
            currentColorChar = newColorChar
        }

        // Добавляем стили, которые появились
        for (styleFlag in newStyles) {
            if (!currentStyles.contains(styleFlag)) {
                sb.append('§').append(styleFlag.char)
            }
        }

        // Убираем стили, которые пропали
        for (styleFlag in currentStyles) {
            if (!newStyles.contains(styleFlag)) {
                if (newStyles.isNotEmpty() || newColorChar != null) {
                    sb.append('§').append('r')
                    if (newColorChar != null) {
                        sb.append('§').append(newColorChar)
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

// Поиск ближайшего цвета и возврат его символа ('a', 'b', 'c' и т.д.)
private fun nearestVanillaColorChar(rgb: Int): Char {
    var bestChar = 'f'
    var bestDist = Long.MAX_VALUE
    for ((frgb, char) in VANILLA_COLORS_CHAR) {
        val dr = (((rgb shr 16) and 0xFF) - ((frgb shr 16) and 0xFF)).toLong()
        val dg = (((rgb shr 8)  and 0xFF) - ((frgb shr 8)  and 0xFF)).toLong()
        val db = ((rgb and 0xFF)           - (frgb and 0xFF)          ).toLong()
        val dist = dr * dr + dg * dg + db * db
        if (dist < bestDist) {
            bestDist = dist
            bestChar = char
        }
    }
    return bestChar
}