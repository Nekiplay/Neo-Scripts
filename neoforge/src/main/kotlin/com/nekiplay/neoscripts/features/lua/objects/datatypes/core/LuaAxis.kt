package com.nekiplay.neoscripts.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaAxis(val axis: Direction.Axis): LuaUserdata(axis) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "negative" -> LuaDirection(axis.negative)
            "positive" -> LuaDirection(axis.positive)
            "isHorizontal" -> valueOf(axis.isHorizontal)
            "isVertical" -> valueOf(axis.isVertical)
            else -> NIL
        }
    }

    override fun eq(other: LuaValue?): LuaValue {
        return when (other) {
            is LuaAxis if axis.name == other.axis.name -> {
                TRUE
            }
            is Direction.Axis if axis.name == other.name -> {
                TRUE
            }
            else -> {
                FALSE
            }
        }
    }

    override fun typename(): String = "axis"
}