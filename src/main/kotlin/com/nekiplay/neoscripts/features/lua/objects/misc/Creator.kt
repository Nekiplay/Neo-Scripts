package com.nekiplay.neoscripts.features.lua.objects.misc

import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.utils.itemlist.ItemRepository
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction

class Creator : LuaValue() {
    override fun typename(): String = "creator"
    override fun tojstring(): String = "CreatorObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "createAABB", "createBox" -> CreateBox()
            "createDirection" -> CreateDirection()
            "createBlockPos" -> CreateBlockPos()
            "createBlockState" -> CreateBlockState()
            "createVector3", "createVector3d" -> CreateVector3()
            "createItemStackFromHypixelSkyblockId", "createItemFromHypixelSkyblockId" -> CreateStackFromHypixelSkyblockID()
            "createItemStackFromIdentifier" -> CreateStackFromIdentifier()
            "createItemStackFromId" -> CreateStackFromId()
            else -> super.get(key)
        }
    }

    inner class CreateStackFromId : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isnumber()) {
                error("create item expects a number as 1st argument")
            }
            val id = args.arg(1).checkint()

            val stack = BuiltInRegistries.ITEM.get(id)
            return if (stack.isPresent) {
                LuaItemStack(stack.get().value().defaultInstance)
            } else {
                NIL
            }
        }
    }

    inner class CreateStackFromIdentifier : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) {
                error("create item expects a string as 1st argument (minecraft:stone)")
            }
            val idString = args.arg(1).checkjstring()

            val identifier = Identifier.parse(idString)

            val stack = BuiltInRegistries.ITEM.get(identifier)
            return if (stack.isPresent) {
                LuaItemStack(stack.get().value().defaultInstance)
            } else {
                NIL
            }
        }
    }

    inner class CreateVector3 : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isnumber() && args.arg(2).isnumber() && args.arg(3).isnumber()) {
                return LuaVector3d(Vec3(args.arg(1).todouble(), args.arg(2).todouble(), args.arg(3).todouble()))
            }
            return NIL
        }
    }

    inner class CreateBlockState : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isint()) {
                return LuaBlockState(Block.stateById(args.arg(1).toint()))
            }
            return NIL
        }
    }

    inner class CreateBlockPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isint() && args.arg(2).isint() && args.arg(3).isint()) {
                return LuaBlockPos(BlockPos(args.arg(1).toint(), args.arg(2).toint(), args.arg(3).toint()))
            }
            return NIL
        }
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

    inner class CreateStackFromHypixelSkyblockID : VarArgFunction() {
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