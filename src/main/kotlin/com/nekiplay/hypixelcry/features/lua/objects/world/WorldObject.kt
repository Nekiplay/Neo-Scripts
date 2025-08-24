package com.nekiplay.hypixelcry.features.lua.objects.world

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.customArgs.FourArgFunction
import com.nekiplay.hypixelcry.features.lua.utils.block.BlockUtil
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.utils.Rotations
import net.minecraft.block.Block
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction

class WorldObject : LuaValue() {

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // Functions
            "getRotation" -> GetRotationFunction()
            "getBlock" -> GetBlockFunction()
            "setBlock" -> SetBlockFunction()
            else -> LuaValue.NIL
        } as LuaValue
    }

    private inner class GetRotationFunction : ThreeArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?
        ): LuaValue? {
            return if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {

                val yaw = Rotations.getYaw(Vec3d(arg1.todouble(), arg2.todouble(), arg3.todouble()))
                val pitch = Rotations.getPitch(Vec3d(arg1.todouble(), arg2.todouble(), arg3.todouble()))

                val table = LuaValue.tableOf()
                table.set("yaw", LuaValue.valueOf(yaw))
                table.set("pitch", LuaValue.valueOf(pitch))
                table
            }
            else {
                NIL
            }
        }


    }

    private inner class SetBlockFunction : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isnumber() == true) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val blockId = arg4.toint()
                val blockState = Block.getStateFromRawId(blockId)

                mc.world?.setBlockState(blockPos, blockState)

                mc.worldRenderer.updateBlock(
                    HypixelCry.mc.world,
                    blockPos,
                    HypixelCry.mc.world?.getBlockState(blockPos),
                    blockState,
                    0
                )
                mc.world?.updateNeighbors(blockPos, blockState.block)
            }
            return LuaValue.NIL
        }
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
                return BlockUtil.ToLua(blockPos, state);
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