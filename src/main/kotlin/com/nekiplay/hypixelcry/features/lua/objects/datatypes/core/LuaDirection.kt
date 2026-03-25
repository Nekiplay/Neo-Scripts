package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

import net.minecraft.core.Direction
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaDirection(private val lua: Lua?, val direction: Direction): SimpleLuaWrapper(lua) {
    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "opposite" -> LuaDirection(null, direction.opposite)
            "name" -> direction.name
            "axisDirection" -> LuaAxisDirection(null, direction.axisDirection)
            "axis" -> LuaAxis(null, direction.axis)
            "clockWise" -> LuaDirection(null, direction.clockWise)
            "step" -> {
                l.newTable()

                l.push(direction.stepX.toDouble()); l.setField(-2, "x")
                l.push(direction.stepY.toDouble()); l.setField(-2, "y")
                l.push(direction.stepZ.toDouble()); l.setField(-2, "z")

                l.get()
            }
            else -> null
        }
    }

    override fun push(targetLua: Lua?) {
        super.push(targetLua)

        val luaInstance = targetLua ?: L ?: return
        if (luaInstance.getMetatable(-1) != 0) {
            luaInstance.push(JFunction { l ->
                l.push(direction.name)
                1
            })
            luaInstance.setField(-2, "__tostring")
            luaInstance.pop(1)
        }
    }

    override fun pushValue(targetLua: Lua?): LuaValue {
        push(targetLua)
        return (targetLua ?: L)!!.get()
    }
}