package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import party.iroiro.luajava.Lua

class LuaAxisDirection(private val lua: Lua, val axis: Direction.AxisDirection): SimpleLuaWrapper(lua) {
    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "opposite" -> LuaAxisDirection(l, axis.opposite())
            "name" -> axis.name
            "step" -> axis.step
            else -> null
        }
    }
}