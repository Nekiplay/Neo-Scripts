package com.nekiplay.hypixelcry.features.lua.utils

import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShapes
import org.luaj.vm2.LuaValue

object BlockUtil {

    fun ToLua(state: BlockState?): LuaValue? {
        if (state != null) {
            val table = LuaValue.tableOf()
            return ToLua(table, state)
        } else {
            return LuaValue.NIL
        }
    }

    fun ToLua(table: LuaValue, state: BlockState?): LuaValue? {
        if (state != null) {
            val block = state.block
            // Основная информация о блоке
            table.set("id", LuaValue.valueOf(Block.getRawIdFromState(state)))
            table.set("name", LuaValue.valueOf(block.translationKey))
            table.set("type", LuaValue.valueOf(state.toString()))

            // Свойства блока
            table.set("hardness", LuaValue.valueOf(block.hardness.toDouble()))
            table.set("blast_resistance", LuaValue.valueOf(block.blastResistance.toDouble()))

            // Информация о материале
            table.set("is_solid", LuaValue.valueOf(state.isSolid))
            table.set("is_liquid", LuaValue.valueOf(state.isLiquid))

            // Дополнительные свойства
            table.set("is_air", LuaValue.valueOf(state.isAir))
            return table
        } else {
            return LuaValue.NIL
        }
    }
}