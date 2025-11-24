package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.FacingBlock
import net.minecraft.block.PistonBlock
import net.minecraft.block.PowderSnowBlock
import net.minecraft.block.SnowyBlock
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaBlockState(val blockState: BlockState) : LuaUserdata(blockState) {

    override fun get(key: LuaValue): LuaValue {

        return when (val field = key.tojstring()) {
            "id" -> valueOf(Block.getRawIdFromState(blockState))
            "name" -> valueOf(blockState.block.translationKey)
            "type" -> valueOf(blockState.toString())
            "hardness" -> valueOf(blockState.block.hardness.toDouble())
            "blast_resistance" -> valueOf(blockState.block.blastResistance.toDouble())
            "is_solid" -> valueOf(blockState.isSolid)
            "is_liquid" -> valueOf(blockState.isLiquid)
            "is_air" -> valueOf(blockState.isAir)
            "facing" -> {
                val facing = blockState.getOrEmpty(FacingBlock.FACING)
                if (facing.isPresent) {
                    valueOf(facing.get().name)
                } else {
                    LuaValue.NIL
                }
            }
            "extended" -> {
                val extended = blockState.getOrEmpty(PistonBlock.EXTENDED)
                if (extended.isPresent) {
                    valueOf(extended.get())
                } else {
                    LuaValue.NIL
                }
            }
            "is_still" -> {
                if (blockState.fluidState != null) {
                    valueOf(blockState.fluidState.isStill)
                } else {
                    LuaValue.NIL
                }
            }
            else -> super.get(key)
        }
    }

    fun getState(): BlockState = blockState

}