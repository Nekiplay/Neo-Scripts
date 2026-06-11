package com.nekiplay.neoscripts.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaAxisDirection(val axis: Direction.AxisDirection): LuaUserdata(axis) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "opposite" -> LuaAxisDirection(axis.opposite())
            "name" -> valueOf(axis.name.lowercase())
            "step" -> valueOf(axis.step)
            else -> NIL
        }
    }

    override fun eq(other: LuaValue?): LuaValue {
        return when (other) {
            is LuaAxisDirection if axis.name == other.axis.name -> {
                LuaValue.TRUE
            }
            else -> {
                LuaValue.FALSE
            }
        }
    }

    override fun typename(): String = "axis_direction"
}