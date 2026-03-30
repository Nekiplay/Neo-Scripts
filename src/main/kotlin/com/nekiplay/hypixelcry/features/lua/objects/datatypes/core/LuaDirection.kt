package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import java.util.Locale
import java.util.Locale.getDefault

class LuaDirection(val direction: Direction): LuaUserdata(direction) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
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
            else -> NIL
        }
    }

    override fun tojstring(): String {
        return direction.name.lowercase();
    }

    override fun tostring(): LuaValue? {
        return valueOf(direction.name.lowercase())
    }
}