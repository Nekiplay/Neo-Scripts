package com.nekiplay.hypixelcry.features.lua.objects.modules

import com.nekiplay.hypixelcry.HypixelCry.LUA_MANAGER
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction

class ModulesObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getLoadedScripts" -> GetLoadedScriptsFunction()
            "pathFinder" -> PathFinderRendererObject()
            else -> NIL
        } as LuaValue
    }

    private inner class GetLoadedScriptsFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = tableOf()
            var index = 1
            for (script in LUA_MANAGER.getLoadedScripts()) {
                table.set(index, script.name)
                index++
            }
            return table
        }
    }

    override fun typename(): String = "modules"
    override fun tojstring(): String = "ModulesObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}