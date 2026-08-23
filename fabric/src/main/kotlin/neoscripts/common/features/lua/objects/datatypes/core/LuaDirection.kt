package com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JavaInstance

class LuaDirection(val direction: Direction): LuaUserdata(direction) {
    override fun eq(other: LuaValue?): LuaValue {
        return when (other) {
            is LuaDirection if direction.name == other.direction.name -> {
                LuaValue.TRUE
            }
            else -> {
                LuaValue.FALSE
            }
        }
    }


    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "javaClass", "class" -> JavaInstance(direction)
            "opposite" -> LuaDirection(direction.opposite)
            "name" -> valueOf(direction.name.lowercase())
            "axisDirection" -> LuaAxisDirection(direction.axisDirection)
            "axis" -> LuaAxis(direction.axis)
            "clockWise" -> LuaDirection(direction.clockWise)
            "step" -> {
                val t = tableOf()
                t.set("y", valueOf(direction.stepY))
                t.set("x", valueOf(direction.stepX))
                t.set("z", valueOf(direction.stepZ))
                t
            }
            else -> super.get(key)
        }
    }

    override fun tojstring(): String {
        return direction.name.lowercase();
    }

    override fun tostring(): LuaValue? {
        return valueOf(direction.name.lowercase())
    }

    override fun typename(): String = "direction"
}