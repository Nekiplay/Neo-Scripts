package com.nekiplay.hypixelcry.pathfinder.movement

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CarpetBlock
import net.minecraft.world.level.block.CauldronBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.FlowerBlock
import net.minecraft.world.level.block.HalfTransparentBlock
import net.minecraft.world.level.block.LadderBlock
import net.minecraft.world.level.block.SkullBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.StainedGlassBlock
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraft.world.level.material.FlowingFluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.material.WaterFluid
import kotlin.math.abs

object MovementHelper {

    fun canWalkThrough(
        ctx: CalculationContext,
        x: Int,
        y: Int,
        z: Int,
        state: BlockState? = ctx.world?.getBlockState(BlockPos(x, y, z))
    ): Boolean {
        val canWalk = canWalkThroughBlockState(state)
        if (canWalk != null) {
            return canWalk
        }
        return canWalkThroughPosition(x, y, z, state, ctx)
    }

    fun canWalkThroughBlockState(state: BlockState?): Boolean? {
        val block = state?.block
        return when (block) {
            Blocks.AIR -> true
            is FlowerBlock -> true
            Blocks.FIRE, Blocks.TRIPWIRE, Blocks.COBWEB, Blocks.END_PORTAL, Blocks.COCOA, is SkullBlock, is TrapDoorBlock -> false
            is DoorBlock, is FenceGateBlock -> {
                // TODO this assumes that all doors in all mods are openable
                if (block == Blocks.IRON_DOOR) {
                    false
                } else {
                    state.getValue(DoorBlock.OPEN) // Check if the door is open
                }
            }
            is CarpetBlock -> null
            is SnowLayerBlock -> null
            is FlowingFluid -> { // Changed from BlockLiquid to FluidBlock
                if (state.getValue(FlowingFluid.LEVEL) != 0) { // Changed property access
                    false
                } else {
                    null
                }
            }
            is CauldronBlock -> false
            Blocks.LADDER -> false
            else -> {
                try {
                    block == Blocks.AIR
                } catch (exception: Throwable) {
                    println("The block ${state?.block?.descriptionId} requires a special case due to the exception ${exception.message}")
                    null
                }
            }
        }
    }

    fun canWalkThroughPosition(
        x: Int,
        y: Int,
        z: Int,
        state: BlockState?,
        ctx: CalculationContext
    ): Boolean {
        val block = state?.block

        if (block is CarpetBlock) {
            return canStandOn(x, y - 1, z, ctx)
        }

        if (block is SnowLayerBlock) {
            ctx.world?.isLoaded(BlockPos(x shr 4, 1, z shr 4))?.let {
                if (!it) {  // Updated chunk check
                    return true
                }
            }
            if (state.getValue(SnowLayerBlock.LAYERS) >= 1) {  // Updated property access
                return false
            }
            return canStandOn(x, y - 1, z, ctx)
        }

        if (block is FlowingFluid) {
            if (isFlowing(x, y, z, state, ctx)) {
                return false
            }

            val up = ctx.world?.getBlockState(BlockPos(x, y + 1, z))
            if (up?.block is FlowingFluid || up?.block == Blocks.LILY_PAD) {  // Updated block names
                return false
            }
            return state.fluidState.type == Fluids.WATER  // Updated fluid check
        }

        return state?.isAir == true
    }

    fun canStandOn(x: Int, y: Int, z: Int, ctx: CalculationContext, state: BlockState? = ctx.world?.getBlockState(BlockPos(x, y, z))): Boolean {
        val block = state?.block ?: return false
        return when {
            block.defaultBlockState().isSolid() -> true  // Replaces isNormalCube
            block == Blocks.LADDER -> true
            block == Blocks.FARMLAND || block == Blocks.GRASS_BLOCK || block == Blocks.DIRT_PATH -> true
            block == Blocks.ENDER_CHEST || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST -> true
            block == Blocks.GLASS || block is StainedGlassBlock -> true
            block is StairBlock -> true  // Changed from BlockStairs to StairsBlock
            block == Blocks.SEA_LANTERN -> true
            isWater(state) -> {
                val up = ctx.world?.getBlockState(BlockPos(x, y + 1, z))?.block
                up == Blocks.LILY_PAD || up is CarpetBlock  // Changed from waterlily to LILY_PAD
            }
            isLava(state) -> false
            block is SlabBlock -> true  // Changed from BlockSlab to SlabBlock
            block is SnowLayerBlock -> true  // Changed from BlockSnow to SnowBlock
            else -> false
        }
    }
    fun possiblyFlowing(state: BlockState?): Boolean {
        return state?.block is FlowingFluid && state.getValue(FlowingFluid.LEVEL) != 0
    }

    fun isFlowing(x: Int, y: Int, z: Int, state: BlockState, ctx: CalculationContext): Boolean {
        if (state.block !is FlowingFluid) {
            return false
        }
        if (state.getValue(FlowingFluid.LEVEL) != 0) {
            return true
        }
        return possiblyFlowing(ctx.world?.getBlockState(BlockPos(x + 1, y, z))) ||
                possiblyFlowing(ctx.world?.getBlockState(BlockPos(x - 1, y, z))) ||
                possiblyFlowing(ctx.world?.getBlockState(BlockPos(x, y, z + 1))) ||
                possiblyFlowing(ctx.world?.getBlockState(BlockPos(x, y, z - 1)))
    }

    fun isWater(state: BlockState?): Boolean {
        return state?.fluidState?.type == Fluids.WATER
    }

    fun isLava(state: BlockState): Boolean {
        return state.fluidState.type == Fluids.LAVA
    }

    fun isBottomSlab(state: BlockState?): Boolean {
        return state?.block is SlabBlock &&
                state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
    }

    fun isValidStair(state: BlockState?, dx: Int, dz: Int): Boolean {
        if (dx == dz) return false
        if (state?.block !is StairBlock) return false
        val stairFacing = state.getValue(StairBlock.FACING)

        return when {
            dz == -1 -> stairFacing == Direction.NORTH
            dz == 1 -> stairFacing == Direction.SOUTH
            dx == -1 -> stairFacing == Direction.WEST
            dx == 1 -> stairFacing == Direction.EAST
            else -> false
        }
    }
    
    fun getFacing(dx: Int, dz: Int): Direction {
        return if (dx == 0 && dz == 0) {
            Direction.UP
        } else {
            // Calculate index based on dx/dz values
            val index = abs(dx) * (2 + dx) + abs(dz) * (1 - dz)
            // Get horizontal directions
            val horizontals = Direction.entries.filter { it.axis.isHorizontal }
            horizontals.getOrNull(index) ?: Direction.NORTH
        }
    }

    fun isLadder(state: BlockState?): Boolean {
        return state?.block == Blocks.LADDER
    }

    fun canWalkIntoLadder(ladderState: BlockState?, dx: Int, dz: Int): Boolean {
        return isLadder(ladderState) && ladderState?.getValue(LadderBlock.FACING) != getFacing(
            dx,
            dz
        )
    }
}