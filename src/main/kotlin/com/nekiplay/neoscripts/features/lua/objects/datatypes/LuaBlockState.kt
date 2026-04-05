package com.nekiplay.neoscripts.features.lua.objects.datatypes

import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaDirection
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.ComparatorBlock
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.RedstoneTorchBlock
import net.minecraft.world.level.block.RepeaterBlock
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.piston.PistonBaseBlock
import net.minecraft.world.level.block.state.BlockState
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
            "age" -> {
                val age = blockState.getOptionalValue(CropBlock.AGE)
                if (age.isPresent) {
                    valueOf(age.get())
                } else {
                    NIL
                }
            }
            "delay" -> {
                val delay = blockState.getOptionalValue(RepeaterBlock.DELAY)
                if (delay.isPresent) {
                    valueOf(delay.get())
                } else {
                    NIL
                }
            }
            "locked" -> {
                val locked = blockState.getOptionalValue(RepeaterBlock.LOCKED)
                if (locked.isPresent) {
                    valueOf(locked.get())
                } else {
                    NIL
                }
            }
            "power" -> {
                val power = blockState.getOptionalValue(RedStoneWireBlock.POWER)
                if (power.isPresent) {
                    valueOf(power.get())
                } else {
                    NIL
                }
            }
            "facing" -> {
                val facing = blockState.getOptionalValue(DoorBlock.FACING)
                if (facing.isPresent) {
                    LuaDirection(facing.get());
                }
                else {
                    val facing = blockState.getOptionalValue(DirectionalBlock.FACING)
                    if (facing.isPresent) {
                        LuaDirection(facing.get());
                    }
                    else {
                        NIL
                    }
                }
            }
            "face" -> {
                val face = blockState.getOptionalValue(FaceAttachedHorizontalDirectionalBlock.FACE)
                if (face.isPresent) {
                    valueOf(face.get().serializedName)
                }
                else {
                    NIL
                }
            }
            "lit" -> {
                val lit = blockState.getOptionalValue(RedstoneTorchBlock.LIT)
                if (lit.isPresent) {
                    valueOf(lit.get())
                }
                else {
                    NIL
                }
            }
            "mode" -> {
                val mode = blockState.getOptionalValue(ComparatorBlock.MODE)
                if (mode.isPresent) {
                    valueOf(mode.get().serializedName)
                }
                else {
                    NIL
                }
            }
            "extended" -> {
                val extended = blockState.getOptionalValue(PistonBaseBlock.EXTENDED)
                if (extended.isPresent) {
                    valueOf(extended.get())
                } else {
                    NIL
                }
            }
            "layers" -> {
                val layers = blockState.getOptionalValue(SnowLayerBlock.LAYERS)
                if (layers.isPresent) {
                    valueOf(layers.get())
                } else {
                    NIL
                }
            }
            "is_still" -> {
                if (blockState.fluidState != null) {
                    valueOf(blockState.fluidState.isSource)
                } else {
                    NIL
                }
            }

            else -> super.get(key)
        }
    }

    override fun typename(): String = "blockstate"
}