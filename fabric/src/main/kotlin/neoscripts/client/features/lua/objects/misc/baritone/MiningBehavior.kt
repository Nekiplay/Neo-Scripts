package neoscripts.features.lua.objects.misc.baritone;

import baritone.api.BaritoneAPI
import org.luaj.vm2.LuaValue;

class MiningBehavior : LuaValue() {
    override fun typename(): String = "baritone-pathing-behavior"
    override fun tojstring(): String = "Baritone-Pathing-Behavior"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "isActive" -> valueOf(BaritoneAPI.getProvider().primaryBaritone.mineProcess.isActive)
            "isTemporary" -> valueOf(BaritoneAPI.getProvider().primaryBaritone.mineProcess.isTemporary)
            else -> super.get(key)
        }
    }
}