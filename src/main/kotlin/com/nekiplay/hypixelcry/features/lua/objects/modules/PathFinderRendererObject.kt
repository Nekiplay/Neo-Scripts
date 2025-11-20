package com.nekiplay.hypixelcry.features.lua.objects.modules

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.esp.pathfinder.PathFinderWorker
import net.minecraft.util.math.BlockPos
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class PathFinderRendererObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "isHasPath" -> IsHasPathFunction()
            "removePath" -> RemovePathFunction()
            "addOrUpdatePath" -> AddOrUpdatePathFunction()
            "getPathBlocks" -> GetPathBlocksFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class IsHasPathFunction : OneArgFunction() {
        override fun call(string: LuaValue): LuaValue {
            if (string.isstring()) {
                return valueOf(PathFinderWorker.hasPath(string.tojstring()));
            }
            return NIL;
        }
    }

    private inner class RemovePathFunction : OneArgFunction() {
        override fun call(string: LuaValue): LuaValue {
            if (string.isstring()) {
                if (PathFinderWorker.hasPath(string.tojstring())) {
                    PathFinderWorker.removePath(string.tojstring())
                    return valueOf(true);
                }
                return valueOf(false);
            }
            return NIL;
        }
    }

    private inner class AddOrUpdatePathFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable()) {
                val id = if (table.get("id").isstring()) table.get("id").tojstring() else "empty"

                val x = if (table.get("x").isnumber()) table.get("x").toint() else 0
                val y = if (table.get("y").isnumber()) table.get("y").toint() else 0
                val z = if (table.get("z").isnumber()) table.get("z").toint() else 0

                val red = if (table.get("red").isnumber()) table.get("red").toint() else 0
                val green = if (table.get("green").isnumber()) table.get("green").toint() else 0
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 0
                val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 255

                val smooth = if (table.get("smooth").isboolean()) table.get("smooth").toboolean() else false
                val updater = if (table.get("updater").isboolean()) table.get("updater").toboolean() else true

                val colorComponents = floatArrayOf(
                    red.toFloat() / 255.0f,
                    green.toFloat() / 255.0f,
                    blue.toFloat() / 255.0f,
                    alpha.toFloat() / 255.0f
                )

                val endText = if (table.get("end_text").isstring()) table.get("end_text").tojstring() else "empty"

                PathFinderWorker.addOrUpdatePath(
                    id, BlockPos(x, y, z),
                    colorComponents,
                    endText,
                    smooth,
                    updater
                )
                return valueOf(true);
            }
            return NIL;
        }
    }

    private inner class GetPathBlocksFunction : OneArgFunction() {
        override fun call(string: LuaValue): LuaValue {
            if (string.isstring()) {
                val list = PathFinderWorker.getPathBlocks(string.tojstring())
                var index = 0;
                val table = tableOf();
                for (item in list) {
                    val table_item = tableOf();
                    table_item.set("x", item.x)
                    table_item.set("y", item.y)
                    table_item.set("z", item.z)

                    table.set(index, table_item)
                    index++
                }
                return table
            }
            return NIL;
        }
    }

    override fun typename(): String = "path_finder_renderer"
    override fun tojstring(): String = "PathFinderRendererObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}