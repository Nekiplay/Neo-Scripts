package com.nekiplay.neoscripts.features.lua.objects.datatypes

import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.sugar.getFormattedString
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
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
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.level.block.state.properties.IntegerProperty
import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaLong
import org.luaj.vm2.LuaNumber
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaBlockState(var blockState: BlockState) : LuaUserdata(blockState) {
    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "id" -> valueOf(Block.getId(blockState))
            "identifier" -> valueOf(BuiltInRegistries.BLOCK.wrapAsHolder(blockState.block).registeredName)
            "traslation_id" -> valueOf(blockState.block.descriptionId)
            "name" -> valueOf(blockState.block.name.getFormattedString())
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

    override fun eq(other: LuaValue?): LuaValue {
        return when (other) {
            is LuaBlockState if blockState == other.blockState -> {
                LuaValue.TRUE
            }
            is BlockState if blockState == other -> {
                LuaValue.TRUE
            }
            is LuaInteger if Block.getId(blockState) == other.toint() -> {
                LuaValue.TRUE
            }
            is LuaNumber if Block.getId(blockState) == other.toint() -> {
                LuaValue.TRUE
            }
            is LuaLong if Block.getId(blockState) == other.toint() -> {
                LuaValue.TRUE
            }
            is LuaDouble if Block.getId(blockState) == other.toint() -> {
                LuaValue.TRUE
            }
            else -> {
                LuaValue.FALSE
            }
        }
    }

    override fun set(key: LuaValue, value: LuaValue) {
        if (blockState.`is`(Blocks.AIR)) return

        when (val field = key.tojstring()) {
            "extended" -> {
                if (value.isboolean() && blockState.hasProperty(PistonBaseBlock.EXTENDED)) {
                    blockState = blockState.setValue(PistonBaseBlock.EXTENDED, value.toboolean())
                }
            }
            "layers" -> {
                if (value.isnumber() && blockState.hasProperty(SnowLayerBlock.LAYERS)) {
                    blockState = blockState.setValue(SnowLayerBlock.LAYERS, value.toint())
                }
            }
            "lit" -> {
                if (value.isboolean() && blockState.hasProperty(RedstoneTorchBlock.LIT)) {
                    blockState = blockState.setValue(RedstoneTorchBlock.LIT, value.toboolean())
                }
            }
            "power" -> {
                if (value.isnumber() && blockState.hasProperty(RedStoneWireBlock.POWER)) {
                    blockState = blockState.setValue(RedStoneWireBlock.POWER, value.toint())
                }
            }
            "locked" -> {
                if (value.isboolean() && blockState.hasProperty(RepeaterBlock.LOCKED)) {
                    blockState = blockState.setValue(RepeaterBlock.LOCKED, value.toboolean())
                }
            }
            "delay" -> {
                if (value.isnumber() && blockState.hasProperty(RepeaterBlock.DELAY)) {
                    blockState = blockState.setValue(RepeaterBlock.DELAY, value.toint())
                }
            }
            "age" -> {
                if (value.isnumber() && blockState.hasProperty(CropBlock.AGE)) {
                    blockState = blockState.setValue(CropBlock.AGE, value.toint())
                }
            }
            "facing" -> {
                if (blockState.hasProperty(DoorBlock.FACING)) {
                    if (value.touserdata() is Direction) {
                        val dir = value.touserdata() as Direction
                        blockState = blockState.setValue(DoorBlock.FACING, dir)
                    }
                    else if (value.touserdata() is LuaDirection) {
                        val dir = value.touserdata() as LuaDirection
                        blockState = blockState.setValue(DoorBlock.FACING, dir.direction)
                    }
                    else if (value.isstring()) {
                        blockState = blockState.setValue(DoorBlock.FACING, Direction.valueOf(value.tojstring().uppercase()))
                    }
                }
            }
            "face" -> {
                if (blockState.hasProperty(FaceAttachedHorizontalDirectionalBlock.FACE)) {
                    if (value.isstring()) {
                        blockState = blockState.setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.valueOf(value.tojstring().uppercase()))
                    }
                }
            }
            else -> super.set(key, value)
        }
    }

    override fun typename(): String = "blockstate"
}
