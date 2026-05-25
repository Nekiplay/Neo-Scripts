package com.nekiplay.neoscripts.features.lua.objects.misc

import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaBlockState
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks.AIR
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction

class Blocks : LuaValue() {
    override fun typename(): String = "blocks"
    override fun tojstring(): String = "BlocksObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getAll", "getBlocks", "getBlockStates" -> GetBlocks()
            "getFromId" -> GetFromId()
            "getFromIdentifier" -> GetFromIdentifier()
            else -> super.get(key)
        }
    }

    inner class GetFromIdentifier : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isstring()) {
                val optional = BuiltInRegistries.BLOCK.get(Identifier.parse(args.arg(1).tojstring()))
                if (optional.isPresent) {
                    return LuaBlockState(optional.get().value().defaultBlockState())
                }
            }
            return NIL
        }
    }

    inner class GetFromId : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isint()) {
                return LuaBlockState(Block.stateById(args.arg(1).toint()))
            }
            return NIL
        }
    }

    inner class GetBlocks : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val blocksList = LuaValue.tableOf()
            var index = 1

            for (block in BuiltInRegistries.BLOCK) {
                blocksList.set(index++, LuaBlockState(block.defaultBlockState()))
            }

            return blocksList
        }
    }
}