package com.nekiplay.hypixelcry.features.lua.objects.modules

import org.luaj.vm2.LuaValue

class ModulesObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "pathFinder" -> PathFinderRendererObject()
            else -> NIL
        } as LuaValue
    }

    override fun typename(): String = "modules"
    override fun tojstring(): String = "ModulesObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}