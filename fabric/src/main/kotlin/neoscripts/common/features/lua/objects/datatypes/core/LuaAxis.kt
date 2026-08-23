package com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JavaInstance

class LuaAxis(val axis: Direction.Axis): LuaUserdata(axis) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "javaClass", "class" -> JavaInstance(axis);
            "negative" -> LuaDirection(axis.negative)
            "positive" -> LuaDirection(axis.positive)
            "isHorizontal" -> valueOf(axis.isHorizontal)
            "isVertical" -> valueOf(axis.isVertical)
            else -> super.get(key)
        }
    }

    override fun eq(other: LuaValue?): LuaValue {
        return when (other) {
            is LuaAxis if axis.name == other.axis.name -> {
                LuaValue.TRUE
            }
            else -> {
                LuaValue.FALSE
            }
        }
    }

    override fun typename(): String = "axis"
}