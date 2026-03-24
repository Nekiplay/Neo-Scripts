package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import party.iroiro.luajava.Lua

class LuaAxisDirection(val l: Lua, val axis: Direction.AxisDirection): SimpleLuaWrapper(l) {
    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "opposite" -> LuaAxisDirection(l, axis.opposite())
            "name" -> axis.name
            "step" -> axis.step
            else -> null
        }
    }
}