package com.nekiplay.neoscripts.features.lua.objects.datatypes.core

import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class LuaMutableBlockPos(val pos: BlockPos.MutableBlockPos): LuaUserdata(pos) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "x" -> valueOf(pos.x)
            "y" -> valueOf(pos.y)
            "z" -> valueOf(pos.z)

            "set" -> set()

            "above" -> LuaBlockPos(pos.above())
            "below" -> LuaBlockPos(pos.below())
            "east" -> LuaBlockPos(pos.east())
            "north" -> LuaBlockPos(pos.north())
            "west" -> LuaBlockPos(pos.west())
            "south" -> LuaBlockPos(pos.south())

            "distSqr", "distanceSqr" -> distSqr()
            "distToCenterSqr", "distanceToCenterSqr" -> distToCenterSqr()

            "toImmutable" -> object : ZeroArgFunction() {
                override fun call(): LuaValue = LuaBlockPos(pos.immutable())
            }

            else -> NIL
        } as LuaValue
    }

    override fun eq(other: LuaValue?): LuaValue {
        val user = other?.touserdata()
        if (other is LuaMutableBlockPos) {
            if (other.pos == pos) {
                return TRUE
            }
        }
        if (user is BlockPos.MutableBlockPos) {
            if (user == pos) {
                return TRUE
            }
            return TRUE
        }
        return FALSE
    }

    private inner class set : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            if (args?.arg(1)?.isint() == true && args.arg(2)?.isint() == true && args.arg(3)?.isint() == true) {
                pos.set(arg(1).toint(), arg(2).toint(), arg(3).toint())
            }
            return NIL
        }
    }

    private inner class distSqr : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            if (args?.arg(1)?.isint() == true && args.arg(2)?.isint() == true && args.arg(3)?.isint() == true) {
                valueOf(pos.distSqr(Vec3i(args.arg(1).toint(), args.arg(2).toint(), args.arg(3).toint())))
            }
            return NIL
        }
    }

    private inner class distToCenterSqr : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            if (args?.arg(1)?.isnumber() == true && args.arg(2)?.isnumber() == true && args.arg(3)?.isnumber() == true) {
                valueOf(pos.distToCenterSqr(args.arg(1).todouble(), args.arg(2).todouble(), args.arg(3).todouble()))
            }
            return NIL
        }
    }

    override fun typename(): String = "blockpos"
}