package com.nekiplay.hypixelcry.features.lua.objects.datatypes.text

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import java.net.URI

class LuaComponentBuilder(L: Lua, private var text: String = "") : SimpleLuaWrapper(L) {

    private var color: String? = null
    private var bold: Boolean? = null
    private var italic: Boolean? = null
    private var underlined: Boolean? = null
    private var strikethrough: Boolean? = null
    private var obfuscated: Boolean? = null
    private var clickEvent: ClickEvent? = null
    private var hoverEvent: HoverEvent? = null
    private var insertion: String? = null
    private val children = mutableListOf<LuaComponentBuilder>()

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "text" -> JFunction {
                this.text = it.toString(2) ?: ""
                it.pushValue(1) // Возвращаем self
                1
            }
            "color" -> JFunction {
                this.color = it.toString(2)
                it.pushValue(1)
                1
            }
            "bold" -> JFunction { this.bold = it.toBoolean(2); it.pushValue(1); 1 }
            "italic" -> JFunction { this.italic = it.toBoolean(2); it.pushValue(1); 1 }
            "underlined" -> JFunction { this.underlined = it.toBoolean(2); it.pushValue(1); 1 }
            "strikethrough" -> JFunction { this.strikethrough = it.toBoolean(2); it.pushValue(1); 1 }
            "obfuscated" -> JFunction { this.obfuscated = it.toBoolean(2); it.pushValue(1); 1 }
            "insertion" -> JFunction { this.insertion = it.toString(2); it.pushValue(1); 1 }

            // Click Events
            "clickRunCommand" -> JFunction {
                val value = it.toString(2) ?: ""
                this.clickEvent = ClickEvent.RunCommand(value)
                it.pushValue(1)
                1
            }
            "clickSuggestCommand" -> JFunction {
                val value = it.toString(2) ?: ""
                this.clickEvent = ClickEvent.SuggestCommand(value)
                it.pushValue(1)
                1
            }
            "clickOpenUrl" -> JFunction {
                val value = it.toString(2) ?: ""
                this.clickEvent = ClickEvent.OpenUrl(URI(value))
                it.pushValue(1)
                1
            }
            "clickCopyToClipboard" -> JFunction {
                val value = it.toString(2) ?: ""
                this.clickEvent = ClickEvent.CopyToClipboard(value)
                it.pushValue(1)
                1
            }
            "clickChangePage" -> JFunction {
                val page = it.toNumber(2).toInt()
                this.clickEvent = ClickEvent.ChangePage(page)
                it.pushValue(1)
                1
            }

            // Hover Events
            "hoverText" -> JFunction { lInner ->
                val arg = lInner.toJavaObject(2)
                val hoverComp: Component = when (arg) {
                    is LuaComponentBuilder -> arg.buildComponent()
                    is LuaComponent -> arg.component.copy()
                    else -> Component.literal(lInner.toString(2) ?: "")
                }
                this.hoverEvent = HoverEvent.ShowText(hoverComp)
                lInner.pushValue(1)
                1
            }

            // Structure
            "append" -> JFunction { lInner ->
                val arg = lInner.toJavaObject(2)
                when (arg) {
                    is LuaComponentBuilder -> this.children.add(arg)
                    is String -> this.children.add(LuaComponentBuilder(lInner, arg))
                    else -> {
                        val str = lInner.toString(2)
                        if (str != null) this.children.add(LuaComponentBuilder(lInner, str))
                    }
                }
                lInner.pushValue(1)
                1
            }

            // Build
            "build" -> JFunction { lInner ->
                lInner.push(LuaComponent(lInner, this.buildComponent()).push())
                1
            }
            else -> null
        }
    }

    fun buildComponent(): MutableComponent {
        var style = Style.EMPTY

        color?.let { c ->
            val fmt = ChatFormatting.getByName(c)
            if (fmt != null && fmt.isColor) {
                style = style.withColor(fmt)
            } else if (c.startsWith("#")) {
                try {
                    style = style.withColor(TextColor.fromRgb(Integer.parseInt(c.removePrefix("#"), 16)))
                } catch (_: NumberFormatException) {}
            }
        }

        bold?.let { style = style.withBold(it) }
        italic?.let { style = style.withItalic(it) }
        underlined?.let { style = style.withUnderlined(it) }
        strikethrough?.let { style = style.withStrikethrough(it) }
        obfuscated?.let { style = style.withObfuscated(it) }
        clickEvent?.let { style = style.withClickEvent(it) }
        hoverEvent?.let { style = style.withHoverEvent(it) }
        insertion?.let { style = style.withInsertion(it) }

        val component = Component.literal(text)
        component.setStyle(style)

        for (child in children) {
            component.append(child.buildComponent())
        }

        return component
    }

    override fun push(): LuaValue {
        val res = super.push()
        if (L.getMetatable(-1) != 0) {
            L.push(JFunction { l ->
                l.push(buildComponent().string)
                1
            })
            L.setField(-2, "__tostring")
            L.pop(1)
        }
        return res
    }

    companion object {
        fun register(L: Lua) {
            L.newTable()
            L.push(JFunction { l ->
                val text = if (l.isString(1)) l.toString(1)!! else ""
                l.push(LuaComponentBuilder(l, text).push())
                1
            })
            L.setField(-2, "new")

            L.push(JFunction { l ->
                l.push(LuaComponentBuilder(l, "").push())
                1
            })
            L.setField(-2, "empty")

            L.setGlobal("ComponentBuilder")
        }
    }
}