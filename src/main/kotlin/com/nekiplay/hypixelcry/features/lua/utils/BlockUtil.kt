package com.nekiplay.hypixelcry.features.lua.utils

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.FacingBlock
import net.minecraft.block.PistonBlock
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
            // Main information
            table.set("id", LuaValue.valueOf(Block.getRawIdFromState(state)))
            table.set("name", LuaValue.valueOf(block.translationKey))
            table.set("type", LuaValue.valueOf(state.toString()))

            // Block property
            table.set("hardness", LuaValue.valueOf(block.hardness.toDouble()))
            table.set("blast_resistance", LuaValue.valueOf(block.blastResistance.toDouble()))

            // Material info
            table.set("is_solid", LuaValue.valueOf(state.isSolid))
            table.set("is_liquid", LuaValue.valueOf(state.isLiquid))

            // Additional property
            table.set("is_air", LuaValue.valueOf(state.isAir))

            // Additional stated
            if (state.block is FacingBlock && state.getOrEmpty(FacingBlock.FACING).isPresent) {
                table.set("facing", LuaValue.valueOf(state.get(FacingBlock.FACING).name))
            }
            if (state.getOrEmpty(PistonBlock.EXTENDED).isPresent) {
                table.set("extended", LuaValue.valueOf(state.get(PistonBlock.EXTENDED)))
            }
            if (state.fluidState != null) {
                table.set("is_still", LuaValue.valueOf(state.fluidState.isStill))
            }

            return table
        } else {
            return LuaValue.NIL
        }
    }
}