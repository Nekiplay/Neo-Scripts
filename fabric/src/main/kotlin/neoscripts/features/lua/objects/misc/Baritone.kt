package com.nekiplay.neoscripts.features.lua.objects.misc

import baritone.api.BaritoneAPI
import com.nekiplay.neoscripts.features.lua.objects.misc.baritone.BaritoneSettings
import neoscripts.features.lua.objects.misc.baritone.MiningBehavior
import neoscripts.features.lua.objects.misc.baritone.PathingBehavior
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
import java.util.Locale
import java.util.Locale.getDefault


class Baritone : LuaValue() {
    override fun typename(): String = "baritone"
    override fun tojstring(): String = "Baritone"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring().lowercase(getDefault())) {
            "settings" -> BaritoneSettings()
            "goal" -> Goal()
            "goto" -> Goto()
            "mine" -> Mine()
            "elytra" -> Elytra()
            "pause" -> Pause()
            "resume" -> Resume()
            "stop" -> Stop()
            "pathingbehavior", "pathing_behavior" -> PathingBehavior()
            "miningbehavior", "mining_behavior" -> MiningBehavior()
            else -> super.get(key)
        }
    }

    inner class Mine : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val ore = args.arg(1).optstring(valueOf("diamond_ore"))
            BaritoneAPI.getProvider().primaryBaritone.commandManager.execute("mine " + ore.tojstring());
            return TRUE
        }
    }

    inner class Goal : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.arg(1).optint(0)
            val y = args.arg(2).optint(0)
            val z = args.arg(3).optint(0)
            BaritoneAPI.getProvider().primaryBaritone.commandManager.execute("goal " + x.toDouble() + " " + y.toDouble() + " " + z.toDouble());
            return TRUE
        }
    }

    inner class Goto : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.arg(1).optint(0)
            val y = args.arg(2).optint(0)
            val z = args.arg(3).optint(0)
            BaritoneAPI.getProvider().primaryBaritone.commandManager.execute("goto " + x.toDouble() + " " + y.toDouble() + " " + z.toDouble());
            return TRUE
        }
    }

    inner class Pause : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            BaritoneAPI.getProvider().primaryBaritone.commandManager.execute("pause");
            return TRUE
        }
    }

    inner class Resume : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            BaritoneAPI.getProvider().primaryBaritone.commandManager.execute("resume");
            return TRUE
        }
    }

    inner class Elytra : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            BaritoneAPI.getProvider().primaryBaritone.commandManager.execute("elytra");
            return TRUE
        }
    }

    inner class Stop : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            BaritoneAPI.getProvider().primaryBaritone.commandManager.execute("stop");

            BaritoneAPI.getProvider().primaryBaritone.pathingBehavior.isPathing
            return TRUE
        }
    }
}