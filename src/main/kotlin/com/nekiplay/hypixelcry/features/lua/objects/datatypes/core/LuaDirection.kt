package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import party.iroiro.luajava.Lua

class LuaDirection(private val lua: Lua, val direction: Direction): SimpleLuaWrapper(lua) {
    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "opposite" -> LuaDirection(l, direction.opposite)
            "name" -> direction.name
            "axisDirection" -> LuaAxisDirection(l, direction.axisDirection)
            "axis" -> LuaAxis(l, direction.axis)
            "clockWise" -> LuaDirection(L, direction.clockWise)
            "step" -> {
                L.newTable()

                l.push(direction.stepX.toDouble()); l.setField(-2, "x")
                l.push(direction.stepY.toDouble()); l.setField(-2, "y")
                l.push(direction.stepZ.toDouble()); l.setField(-2, "z")

                l.get()
            }
            else -> null
        }
    }

    override fun push() {
        val luaValue = super.push()

        if (L.getMetatable(-1) != 0) {
            L.push(party.iroiro.luajava.JFunction { l ->
                l.push(direction.name)
                1
            })
            L.setField(-2, "__tostring")
            L.pop(1)
        }

        return luaValue
    }
}