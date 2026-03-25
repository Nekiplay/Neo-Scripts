package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.ComparatorBlock
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.RedstoneTorchBlock
import net.minecraft.world.level.block.RedstoneWallTorchBlock
import net.minecraft.world.level.block.RepeaterBlock
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.WallTorchBlock
import net.minecraft.world.level.block.piston.PistonBaseBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.AttachFace
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaBlockState(L: Lua, val blockState: BlockState) : SimpleLuaWrapper(L) {
    override fun push(): LuaValue {
        val luaValue = super.push()

        if (L.getMetatable(-1) != 0) {
            L.push(JFunction { l ->
                l.push(blockState.block.descriptionId)
                1
            })
            L.setField(-2, "__tostring")
            L.pop(1)
        }

        return luaValue
    }
    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "id" -> Block.getId(blockState)
            "name" -> blockState.block.descriptionId
            "type" ->blockState.toString()
            "hardness" -> blockState.block.friction.toDouble()
            "blast_resistance" -> blockState.block.explosionResistance.toDouble()
            "is_solid" -> blockState.isSolid
            "is_liquid" -> blockState.liquid()
            "is_air" -> blockState.isAir
            "age" -> {
                val age = blockState.getOptionalValue(CropBlock.AGE)
                if (age.isPresent) {
                    age.get()
                } else {
                    null
                }
            }
            "delay" -> {
                val delay = blockState.getOptionalValue(RepeaterBlock.DELAY)
                if (delay.isPresent) {
                    delay.get()
                } else {
                    null
                }
            }
            "locked" -> {
                val locked = blockState.getOptionalValue(RepeaterBlock.LOCKED)
                if (locked.isPresent) {
                    locked.get()
                } else {
                    null
                }
            }
            "power" -> {
                val power = blockState.getOptionalValue(RedStoneWireBlock.POWER)
                if (power.isPresent) {
                    power.get()
                } else {
                    null
                }
            }
            "facing" -> {
                val facing = blockState.getOptionalValue(DoorBlock.FACING)
                if (facing.isPresent) {
                    LuaDirection(l, facing.get())
                }
                else {
                    val facing = blockState.getOptionalValue(DirectionalBlock.FACING)
                    if (facing.isPresent) {
                        LuaDirection(l, facing.get())
                    }
                    else {
                        null
                    }
                }
            }
            "face" -> {
                val face = blockState.getOptionalValue(FaceAttachedHorizontalDirectionalBlock.FACE)
                if (face.isPresent) {
                    face.get().serializedName
                }
                else {
                    null
                }
            }
            "lit" -> {
                val lit = blockState.getOptionalValue(RedstoneTorchBlock.LIT)
                if (lit.isPresent) {
                    lit.get()
                }
                else {
                    null
                }
            }
            "mode" -> {
                val mode = blockState.getOptionalValue(ComparatorBlock.MODE)
                if (mode.isPresent) {
                    mode.get().serializedName
                }
                else {
                    null
                }
            }
            "is_walled" -> {
                val is_walled = blockState.getOptionalValue(FaceAttachedHorizontalDirectionalBlock.FACE)
                if (is_walled.isPresent && is_walled.get() == AttachFace.WALL) {
                    true
                }
                else if (blockState.block is RedstoneWallTorchBlock || blockState.block is WallTorchBlock) {
                    true
                }
                false
            }
            "extended" -> {
                val extended = blockState.getOptionalValue(PistonBaseBlock.EXTENDED)
                if (extended.isPresent) {
                    extended.get()
                } else {
                    null
                }
            }
            "layers" -> {
                val layers = blockState.getOptionalValue(SnowLayerBlock.LAYERS)
                if (layers.isPresent) {
                    layers.get()
                } else {
                    null
                }
            }
            "is_still" -> {
                if (blockState.fluidState != null) {
                    blockState.fluidState.isSource
                } else {
                    null
                }
            }
            else -> null
        }
    }
}