package com.nekiplay.neoscripts.features.lua.objects.player

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.sugar.silentUse
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

class WindowObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // Objects
            "isMinimized" -> valueOf(mc.window.isMinimized)
            "isFullscreen" -> valueOf(mc.window.isFullscreen)
            "refreshRate" -> valueOf(mc.window.refreshRate)

            "y" -> valueOf(mc.window.y)
            "x" -> valueOf(mc.window.x)

            "width" -> valueOf(mc.window.width)
            "height" -> valueOf(mc.window.height)
            "screenWidth" -> valueOf(mc.window.screenWidth)
            "screenHeight" -> valueOf(mc.window.screenHeight)
            "setTitle" -> SetTitleFunction()
            else -> super.get(key)
        }
    }

    private inner class SetTitleFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isstring()) {
                mc.window.setTitle(arg.tojstring())
                TRUE
            } else {
                NIL
            }
        }
    }


    override fun typename(): String = "window"
    override fun tojstring(): String = "WindowObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}