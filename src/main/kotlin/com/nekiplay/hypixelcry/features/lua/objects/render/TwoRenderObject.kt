package com.nekiplay.hypixelcry.features.lua.objects.render

import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.awt.Color

class TwoRenderObject(private val context: DrawContext?): LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getWindowScale" -> GetWindowScaleFunction()

            "renderText" -> RenderTextFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class GetWindowScaleFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = tableOf()
            val width: Int = mc.window.scaledWidth
            val height: Int = mc.window.scaledHeight

            table.set("width", width)
            table.set("height", height)
            return table
        }
    }

    private inner class RenderTextFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val text = if (table.get("text").isstring()) table.get("text").tojstring() else "Empty"

                val x: Int = if (table.get("x").isnumber()) table.get("x").toint() else 0
                val y: Int = if (table.get("y").isnumber()) table.get("y").toint() else 0

                val red = if (table.get("red").isnumber()) table.get("red").toint() else -0x1
                val green = if (table.get("green").isnumber()) table.get("green").toint() else -0x1
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else -0x1
                val (hue, sat, bri) = Color.RGBtoHSB(red, green, blue, null)
                val color = Color.HSBtoRGB(hue, sat, bri)

                val textRenderer: TextRenderer? = mc.textRenderer
                context.drawTextWithShadow(textRenderer, Text.literal(text), x, y, color);
            }
            return NIL
        }
    }


    override fun typename(): String = "2d_renderer"
    override fun tojstring(): String = "2DRenderObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return LuaValue.TUSERDATA
    }
}