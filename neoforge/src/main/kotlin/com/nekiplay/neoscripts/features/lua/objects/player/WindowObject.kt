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
            "isMinimized" -> valueOf(mc?.window?.isMinimized ?: false)
            "isFullscreen" -> valueOf(mc?.window?.isFullscreen ?: false)
            "refreshRate" -> valueOf(mc?.window?.refreshRate ?: 0)

            "y" -> valueOf(mc?.window?.y ?: 0)
            "x" -> valueOf(mc?.window?.x ?: 0)

            "width" -> valueOf(mc?.window?.width ?: 0)
            "height" -> valueOf(mc?.window?.height ?: 0)
            "screenWidth" -> valueOf(mc?.window?.screenWidth ?: 0)
            "screenHeight" -> valueOf(mc?.window?.screenHeight ?: 0)
            "setTitle" -> SetTitleFunction()
            else -> super.get(key)
        }
    }

    private inner class SetTitleFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isstring()) {
                mc?.window?.setTitle(arg.tojstring())
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