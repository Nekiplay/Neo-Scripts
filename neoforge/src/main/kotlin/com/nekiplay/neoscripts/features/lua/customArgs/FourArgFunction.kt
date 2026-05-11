package com.nekiplay.neoscripts.features.lua.customArgs

import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction

abstract class FourArgFunction : LibFunction() {
    abstract fun invoke(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?, arg4: LuaValue?): LuaValue?

    override fun call(): LuaValue? = invoke(NIL, NIL, NIL, NIL)
    override fun call(arg: LuaValue?): LuaValue? = invoke(arg, NIL, NIL, NIL)
    override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue? = invoke(arg1, arg2, NIL, NIL)
    override fun call(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue? = invoke(arg1, arg2, arg3, NIL)
    override fun call(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?, arg4: LuaValue?): LuaValue? {
        return invoke(arg1, arg2, arg3, arg4)
    }
}