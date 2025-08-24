package com.nekiplay.hypixelcry.features.lua.objects.world

import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.block.Block
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShapes
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction

class WorldObject : LuaValue() {

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // Functions
            "getBlock" -> GetBlockFunction()
            else -> LuaValue.NIL
        } as LuaValue
    }

    private inner class GetBlockFunction : ThreeArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?
        ): LuaValue? {
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val state = mc.world?.getBlockState(blockPos)

                if (state != null) {
                    val block = state.block
                    val table = LuaValue.tableOf()

                    // Основная информация о блоке
                    table.set("id", LuaValue.valueOf(Block.getRawIdFromState(state)))
                    table.set("name", LuaValue.valueOf(block.translationKey))
                    table.set("type", LuaValue.valueOf(state.toString()))

                    // Позиция блока
                    table.set("x", LuaValue.valueOf(blockPos.x))
                    table.set("y", LuaValue.valueOf(blockPos.y))
                    table.set("z", LuaValue.valueOf(blockPos.z))

                    // Свойства блока
                    table.set("hardness", LuaValue.valueOf(block.hardness.toDouble()))
                    table.set("blast_resistance", LuaValue.valueOf(block.blastResistance.toDouble()))

                    // Информация о материале
                    table.set("is_solid", LuaValue.valueOf(state.isSolid))
                    table.set("is_liquid", LuaValue.valueOf(state.isLiquid))

                    // Дополнительные свойства
                    table.set("has_collision", LuaValue.valueOf(block.defaultState.getCollisionShape(mc.world, blockPos) != VoxelShapes.empty()))
                    table.set("is_air", LuaValue.valueOf(state.isAir))
                    return table
                } else {
                    return LuaValue.NIL
                }
            }

            return LuaValue.NIL
        }
    }

    override fun typename(): String = "world"
    override fun tojstring(): String = "WorldObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return LuaValue.TUSERDATA
    }
}