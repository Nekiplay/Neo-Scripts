package com.nekiplay.neoscripts.client.features.lua.objects.misc

import baritone.api.BaritoneAPI
import com.nekiplay.neoscripts.client.features.lua.objects.misc.baritone.BaritoneSettings
import neoscripts.features.lua.objects.misc.baritone.MiningBehavior
import neoscripts.features.lua.objects.misc.baritone.PathingBehavior
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
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
            "execute", "executeCommand" -> Execute()
            "pathingbehavior", "pathing_behavior" -> PathingBehavior()
            "miningbehavior", "mining_behavior" -> MiningBehavior()
            else -> super.get(key)
        }
    }

    inner class Execute : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val command = args.arg(1).optstring(valueOf("stop"))
            BaritoneAPI.getProvider().primaryBaritone.commandManager.execute(command.tojstring());
            return TRUE
        }
    }
}