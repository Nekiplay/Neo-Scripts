package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.LuaDirection
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.RepeaterBlock
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.piston.PistonBaseBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.redstone.Redstone
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaBlockState(val blockState: BlockState) : LuaUserdata(blockState) {
    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "id" -> valueOf(Block.getId(blockState))
            "name" -> valueOf(blockState.block.descriptionId)
            "type" -> valueOf(blockState.toString())
            "hardness" -> valueOf(blockState.block.friction.toDouble())
            "blast_resistance" -> valueOf(blockState.block.explosionResistance.toDouble())
            "is_solid" -> valueOf(blockState.isSolid)
            "is_liquid" -> valueOf(blockState.liquid())
            "is_air" -> valueOf(blockState.isAir)
            "delay" -> {
                val delay = blockState.getOptionalValue(RepeaterBlock.DELAY)
                if (delay.isPresent) {
                    valueOf(delay.get())
                } else {
                    LuaValue.NIL
                }
            }
            "power" -> {
                val power = blockState.getOptionalValue(RedStoneWireBlock.POWER)
                if (power.isPresent) {
                    valueOf(power.get())
                } else {
                    LuaValue.NIL
                }
            }
            "facing" -> {
                val facing = blockState.getOptionalValue(DoorBlock.FACING)
                if (facing.isPresent) {
                    LuaDirection(facing.get())
                } else {
                    LuaValue.NIL
                }
            }
            "extended" -> {
                val extended = blockState.getOptionalValue(PistonBaseBlock.EXTENDED)
                if (extended.isPresent) {
                    valueOf(extended.get())
                } else {
                    LuaValue.NIL
                }
            }
            "layers" -> {
                val layers = blockState.getOptionalValue(SnowLayerBlock.LAYERS)
                if (layers.isPresent) {
                    valueOf(layers.get())
                } else {
                    LuaValue.NIL
                }
            }
            "is_still" -> {
                if (blockState.fluidState != null) {
                    valueOf(blockState.fluidState.isSource)
                } else {
                    LuaValue.NIL
                }
            }

            else -> super.get(key)
        }
    }
}