package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

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
}