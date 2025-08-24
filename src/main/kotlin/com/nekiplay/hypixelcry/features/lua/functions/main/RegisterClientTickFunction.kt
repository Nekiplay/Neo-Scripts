package com.nekiplay.hypixelcry.features.lua.functions.main

import com.nekiplay.hypixelcry.features.lua.LuaManager
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

class RegisterClientTickFunction(private val luaManager: LuaManager) : OneArgFunction() {
    override fun call(callback: LuaValue): LuaValue {
        return LuaValue.valueOf(luaManager.addClientTickCallback(callback))
    }
}