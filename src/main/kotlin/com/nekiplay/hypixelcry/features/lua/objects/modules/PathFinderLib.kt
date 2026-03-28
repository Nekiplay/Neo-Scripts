package com.nekiplay.hypixelcry.features.lua.objects.modules

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.pathfinder.LuaPath
import com.nekiplay.hypixelcry.pathfinder.calculate.path.AStarPathFinder
import com.nekiplay.hypixelcry.pathfinder.goal.Goal
import com.nekiplay.hypixelcry.pathfinder.movement.CalculationContext
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction

class PathFinderLib: LuaValue() {
    override fun typename(): String = "pathfinder"
    override fun tojstring(): String = "PathFinderObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "find" -> Find()
            else -> NIL
        } as LuaValue
    }

    private inner class Find : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val start_x = args.checkint(1)
            val start_y = args.checkint(2)
            val start_z = args.checkint(3)

            val end_x = args.checkint(4)
            val end_y = args.checkint(5)
            val end_z = args.checkint(6)

            val ctx = CalculationContext()

            val finder = AStarPathFinder(
                start_x, start_y, start_z,
                Goal(end_x, end_y, end_z, ctx),
                ctx
            )
            val path = finder.calculatePath()
            if (path != null) {
                return LuaPath(path)
            }
            return NIL;
        }
    }
}