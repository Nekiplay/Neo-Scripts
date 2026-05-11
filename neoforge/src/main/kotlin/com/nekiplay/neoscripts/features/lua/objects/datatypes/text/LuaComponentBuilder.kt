package com.nekiplay.neoscripts.features.lua.objects.datatypes.text

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.net.URI

class LuaComponentBuilder(private var text: String = "") : LuaUserdata(LuaComponentBuilder) {

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

    init {
        setmetatable(MT)
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

    companion object {

        fun createLibrary(): LuaTable = LuaTable().apply {
            set("new", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue =
                    LuaComponentBuilder(if (arg.isnil()) "" else arg.checkjstring())
            })
            set("empty", object : ZeroArgFunction() {
                override fun call(): LuaValue = LuaComponentBuilder()
            })
        }

        private val MT: LuaTable = LuaTable().apply {
            val idx = LuaTable()
            set("__index", idx)

            set("__tostring", object : OneArgFunction() {
                override fun call(self: LuaValue): LuaValue =
                    valueOf((self as LuaComponentBuilder).buildComponent().getString())
            })

            // ═══════════════════ Text ═══════════════════

            idx.set("text", object : TwoArgFunction() {
                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    (self as LuaComponentBuilder).text = arg.checkjstring()
                    return self
                }
            })

            // ═══════════════════ Formatting ═══════════════════

            idx.set("color", object : TwoArgFunction() {
                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    (self as LuaComponentBuilder).color = arg.checkjstring()
                    return self
                }
            })

            for ((name, setter) in listOf<Pair<String, (LuaComponentBuilder, Boolean) -> Unit>>(
                "bold" to { b, v -> b.bold = v },
                "italic" to { b, v -> b.italic = v },
                "underlined" to { b, v -> b.underlined = v },
                "strikethrough" to { b, v -> b.strikethrough = v },
                "obfuscated" to { b, v -> b.obfuscated = v },
            )) {
                idx.set(name, object : TwoArgFunction() {
                    override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                        setter(self as LuaComponentBuilder, arg.checkboolean())
                        return self
                    }
                })
            }

            idx.set("insertion", object : TwoArgFunction() {
                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    (self as LuaComponentBuilder).insertion = arg.checkjstring()
                    return self
                }
            })

            // ═══════════════════ Click Events ═══════════════════

            idx.set("clickRunCommand", object : TwoArgFunction() {
                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    (self as LuaComponentBuilder).clickEvent =
                        ClickEvent.RunCommand(arg.checkjstring())
                    return self
                }
            })

            idx.set("clickSuggestCommand", object : TwoArgFunction() {
                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    (self as LuaComponentBuilder).clickEvent =
                        ClickEvent.SuggestCommand(arg.checkjstring())
                    return self
                }
            })

            idx.set("clickOpenUrl", object : TwoArgFunction() {
                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    (self as LuaComponentBuilder).clickEvent =
                        ClickEvent.OpenUrl(URI.create(arg.checkjstring()))
                    return self
                }
            })

            idx.set("clickCopyToClipboard", object : TwoArgFunction() {
                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    (self as LuaComponentBuilder).clickEvent =
                        ClickEvent.CopyToClipboard(arg.checkjstring())
                    return self
                }
            })

            idx.set("clickChangePage", object : TwoArgFunction() {
                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    (self as LuaComponentBuilder).clickEvent =
                        ClickEvent.ChangePage(arg.checkint())
                    return self
                }
            })

            // ═══════════════════ Hover Events ═══════════════════

            idx.set("hoverText", object : TwoArgFunction() {
                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    val builder = self as LuaComponentBuilder
                    val hoverComp: Component = when (arg) {
                        is LuaComponentBuilder -> arg.buildComponent()
                        is LuaComponent -> arg.component.copy()
                        else -> Component.literal(arg.checkjstring())
                    }
                    builder.hoverEvent = HoverEvent.ShowText(hoverComp)
                    return self
                }
            })

            // ═══════════════════ Structure ═══════════════════

            idx.set("append", object : TwoArgFunction() {
                override fun call(self: LuaValue, arg: LuaValue): LuaValue {
                    val builder = self as LuaComponentBuilder
                    when (arg) {
                        is LuaComponentBuilder -> builder.children.add(arg)
                        is LuaString -> builder.children.add(LuaComponentBuilder(arg.tojstring()))
                        else -> throw LuaError("Expected ComponentBuilder or string, got ${arg.typename()}")
                    }
                    return self
                }
            })

            // ═══════════════════ Build ═══════════════════

            idx.set("build", object : OneArgFunction() {
                override fun call(self: LuaValue): LuaValue =
                    LuaComponent((self as LuaComponentBuilder).buildComponent())
            })
        }
    }
}