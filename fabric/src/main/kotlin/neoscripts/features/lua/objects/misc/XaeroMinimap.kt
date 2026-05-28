package com.nekiplay.neoscripts.features.lua.objects.misc

import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction

class XaeroMinimap : LuaValue() {
    override fun typename(): String = "xaero-minimap"
    override fun tojstring(): String = "XeeroMinimap"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "createWaypoint" -> CreateWaypoint()
            else -> super.get(key)
        }
    }

    inner class CreateWaypoint : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            //val minimapSession: MinimapSession? = BuiltInHudModules.MINIMAP.getCurrentSession()
            //if (minimapSession == null) return


            return NIL
        }
    }
}