package com.nekiplay.hypixelcry.features.lua.objects.misc

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.hypixelcry.utils.itemlist.ItemRepository
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

class Creator : TwoArgFunction() {
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val library = LuaTable()
        library.set("createAABB", CreateBox())
        library.set("createBox", CreateBox())
        library.set("createDirection", CreateDirection())
        library.set("createItemStackFromId", CreateStackFromID())
        //env.set("creator", library)
        return library
    }

    inner class CreateDirection : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isstring()) {
                return LuaDirection(Direction.valueOf(args.arg1().tojstring().uppercase()))
            }
            return NIL
        }
    }

    inner class CreateBox : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isnumber() && args.arg(2).isnumber() && args.arg(3).isnumber() && args.arg(4).isnumber() && args.arg(5).isnumber() && args.arg(6).isnumber()) {
                return LuaBox(AABB(args.arg(1).todouble(), args.arg(2).todouble(), args.arg(3).todouble(), args.arg(4).todouble(), args.arg(5).todouble(), args.arg(6).todouble()))
            }
            return NIL
        }
    }

    inner class CreateStackFromID : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) {
                error("create item expects a string as 1st argument (item neu id)")
            }
            val idString = args.arg(1).checkjstring()

            val stack = ItemRepository.getItemStack(idString)
            return if (stack != null) {
                LuaItemStack(stack)
            } else {
                NIL
            }
        }
    }
}