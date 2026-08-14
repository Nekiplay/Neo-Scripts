package com.nekiplay.neoscripts.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JavaInstance

class LuaAxisDirection(val axis: Direction.AxisDirection): LuaUserdata(axis) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "javaClass", "class" -> JavaInstance(axis);
            "opposite" -> LuaAxisDirection(axis.opposite())
            "name" -> valueOf(axis.name.lowercase())
            "step" -> valueOf(axis.step)
            else -> super.get(key)
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