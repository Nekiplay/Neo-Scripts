package neoscripts.features.lua.objects.datatypes.core

import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.level.ChunkPos
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.JavaInstance

class LuaChunkPos(val pos: ChunkPos): LuaUserdata(pos) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "javaClass", "class" -> JavaInstance(pos)
            "x" -> valueOf(pos.x)
            "z" -> valueOf(pos.z)

            "worldPosition" -> LuaBlockPos(pos.worldPosition)
            "min" -> {
                val t = tableOf()
                t.set("x", pos.minBlockX)
                t.set("z", pos.minBlockZ)
                t
            }
            "max" -> {
                val t = tableOf()
                t.set("x", pos.maxBlockX)
                t.set("z", pos.maxBlockZ)
                t
            }
            "contains" -> contains()
            else -> super.get(key)
        }
    }


    private inner class contains : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val arg1 = args?.arg1() ?: return FALSE
            if (arg1.isint() && args.arg(2)?.isint() == true && args.arg(3)?.isint() == true) {
                valueOf(pos.contains(BlockPos(arg1.toint(), args.arg(2).toint(), args.arg(3).toint())))
            }
            else if (arg1 is LuaBlockPos) {
                valueOf(pos.contains(arg1.pos))
            }
            return FALSE
        }
    }


    override fun eq(other: LuaValue?): LuaValue {
        val user = other?.touserdata()
        if (other is LuaChunkPos) {
            if (other.pos == pos) {
                return TRUE
            }
        }
        if (user is ChunkPos) {
            if (user == pos) {
                return TRUE
            }
            return TRUE
        }
        return FALSE
    }

    override fun typename(): String = "chunkpos"
}