package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import party.iroiro.luajava.Lua

class LuaAxis(val l: Lua, val axis: Direction.Axis): SimpleLuaWrapper(l) {
    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "negative" -> LuaDirection(l, axis.negative)
            "positive" -> LuaDirection(l, axis.positive)
            "isHorizontal" -> axis.isHorizontal
            "isVertical" -> axis.isVertical
            else -> null
        }
    }
}