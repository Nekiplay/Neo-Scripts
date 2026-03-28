package com.nekiplay.hypixelcry.features.lua.objects.datatypes.pathfinder

import com.nekiplay.hypixelcry.pathfinder.calculate.Path
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction

class LuaPath(val path: Path): LuaUserdata(path) {
    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "getSmoothed" -> getSmoothedPath()
            "getNormal" -> getNormalPath()
            else -> super.get(key)
        }
    }

    private inner class getNormalPath : ZeroArgFunction() {
        override fun call(): LuaValue {
            val smoothed = path.path
            val table = LuaTable()

            smoothed.forEachIndexed { index, pos ->
                val position = LuaTable()
                position.set("x", pos.x)
                position.set("y", pos.y)
                position.set("z", pos.z)
                table.set(index+1, position)
            }
            return table
        }
    }

    private inner class getSmoothedPath : ZeroArgFunction() {
        override fun call(): LuaValue {
            val smoothed = path.getSmoothedPath()
            val table = LuaTable()

            smoothed.forEachIndexed { index, pos ->
                val position = LuaTable()
                position.set("x", pos.x)
                position.set("y", pos.y)
                position.set("z", pos.z)
                table.set(index+1, position)
            }
            return table
        }
    }
}