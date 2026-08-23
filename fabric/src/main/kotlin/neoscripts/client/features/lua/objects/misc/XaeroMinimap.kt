package com.nekiplay.neoscripts.client.features.lua.objects.misc

import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import xaero.common.minimap.waypoints.Waypoint
import xaero.hud.minimap.BuiltInHudModules
import xaero.hud.minimap.world.MinimapWorld


class XaeroMinimap : LuaValue() {
    override fun typename(): String = "xaero-minimap"
    override fun tojstring(): String = "XeeroMinimap"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "createWaypoint" -> CreateWaypoint()
            "removeWaypoint" -> RemoveWaypoint()
            "getWaypoints" -> GetWaypoints()
            else -> super.get(key)
        }
    }

    fun getMinimapWorld(dim: ResourceKey<Level>): MinimapWorld? {
        val minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession() ?: return null
        val currentWorld = minimapSession.getWorldManager().currentWorld ?: return null
        if (currentWorld.dimId === dim) {
            return currentWorld
        }
        val rootContainer = minimapSession.getWorldManager().currentRootContainer
        for (world in rootContainer.getWorlds()) {
            if (world.dimId === dim) {
                return world
            }
        }
        val dimensionDirectoryName = minimapSession.dimensionHelper.getDimensionDirectoryName(dim)
        val worldNode = minimapSession.worldStateUpdater.getPotentialWorldNode(dim, true)
        val containerPath = minimapSession.worldState
            .autoRootContainerPath
            .resolve(dimensionDirectoryName)
            .resolve(worldNode)
        return minimapSession.getWorldManager().getWorld(containerPath)
    }

    inner class CreateWaypoint : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val world = args.arg(1).optjstring("minecraft:overworld")
            val dim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(world));
            val minimapworld = getMinimapWorld(dim) ?: return error("Minimap world not found")

            val x = args.arg(2).optint(0)
            val y = args.arg(3).optint(0)
            val z = args.arg(4).optint(0)
            val text = args.arg(5).optjstring("text")
            val initials = args.arg(6).optjstring("T")
            val color = args.arg(7).optint(0)

            minimapworld.currentWaypointSet.add(Waypoint(x, y, z, text, initials, color))

            return NIL
        }
    }
    inner class RemoveWaypoint : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val world = args.arg(1).optjstring("minecraft:overworld")
            val dim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(world));
            val minimapworld = getMinimapWorld(dim) ?: return error("Minimap world not found")

            val index = args.arg(2).optint(0)
            minimapworld.currentWaypointSet.remove(index - 1)

            return NIL
        }
    }
    inner class GetWaypoints : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val world = args.arg(1).optjstring("minecraft:overworld")
            val dim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(world));
            val minimapworld = getMinimapWorld(dim) ?: return error("Minimap world not found")

            val table = tableOf()
            minimapworld.currentWaypointSet.waypoints.forEachIndexed { index, waypoint ->
                val wp = tableOf()
                wp.set("name", valueOf(waypoint.name))
                wp.set("initials", valueOf(waypoint.initials))
                wp.set("isDisabled", valueOf(waypoint.isDisabled))
                wp.set("isDisabled", valueOf(waypoint.isDisabled))
                wp.set("isGlobal", valueOf(waypoint.isGlobal))
                wp.set("color", valueOf(waypoint.waypointColor.hex))
                wp.set("x", valueOf(waypoint.x))
                wp.set("y", valueOf(waypoint.y))
                wp.set("z", valueOf(waypoint.z))
                table.set(index + 1, wp)
            }
            return table
        }
    }

}