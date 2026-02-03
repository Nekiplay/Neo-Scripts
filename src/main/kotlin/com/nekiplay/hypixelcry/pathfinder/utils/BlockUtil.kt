package com.nekiplay.hypixelcry.pathfinder.utils

import com.nekiplay.hypixelcry.pathfinder.movement.CalculationContext
import com.nekiplay.hypixelcry.pathfinder.movement.MovementHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

object BlockUtil {
    fun bresenham(ctx: CalculationContext, start: BlockPos, end: BlockPos): Boolean {
        return bresenham(
            ctx,
            Vec3.atCenterOf(start),
            Vec3.atCenterOf(end)
        )
    }

    fun bresenham(ctx: CalculationContext, start: Vec3, end: Vec3): Boolean {
        var currentPos = start

        val x1 = Mth.floor(end.x)
        val y1 = Mth.floor(end.y)
        val z1 = Mth.floor(end.z)
        var x0 = Mth.floor(currentPos.x)
        var y0 = Mth.floor(currentPos.y)
        var z0 = Mth.floor(currentPos.z)

        var lastState: BlockState? = world?.getBlockState(BlockPos(x0, y0, z0))
        var lastPos = BlockPos(x0, y0, z0)

        var iterations = 200
        while (iterations-- >= 0) {
            if (x0 == x1 && y0 == y1 && z0 == z1) {
                return true
            }

            var hasNewX = true
            var hasNewY = true
            var hasNewZ = true
            var newX = 999.0
            var newY = 999.0
            var newZ = 999.0

            if (x1 > x0) {
                newX = x0 + 1.0
            } else if (x1 < x0) {
                newX = x0 + 0.0
            } else {
                hasNewX = false
            }

            if (y1 > y0) {
                newY = y0 + 1.0
            } else if (y1 < y0) {
                newY = y0 + 0.0
            } else {
                hasNewY = false
            }

            if (z1 > z0) {
                newZ = z0 + 1.0
            } else if (z1 < z0) {
                newZ = z0 + 0.0
            } else {
                hasNewZ = false
            }

            var stepX = 999.0
            var stepY = 999.0
            var stepZ = 999.0

            val dx = end.x - currentPos.x
            val dy = end.y - currentPos.y
            val dz = end.z - currentPos.z

            if (hasNewX) stepX = (newX - currentPos.x) / dx
            if (hasNewY) stepY = (newY - currentPos.y) / dy
            if (hasNewZ) stepZ = (newZ - currentPos.z) / dz

            if (stepX == -0.0) stepX = -1.0E-4
            if (stepY == -0.0) stepY = -1.0E-4
            if (stepZ == -0.0) stepZ = -1.0E-4

            val direction: Direction?
            if (stepX < stepY && stepX < stepZ) {
                direction = if (x1 > x0) Direction.WEST else Direction.EAST
                currentPos = Vec3(newX, currentPos.y + dy * stepX, currentPos.z + dz * stepX)
            } else if (stepY < stepZ) {
                direction = if (y1 > y0) Direction.DOWN else Direction.UP
                currentPos = Vec3(currentPos.x + dx * stepY, newY, currentPos.z + dz * stepY)
            } else {
                direction = if (z1 > z0) Direction.NORTH else Direction.SOUTH
                currentPos = Vec3(currentPos.x + dx * stepZ, currentPos.y + dy * stepZ, newZ)
            }

            x0 = Mth.floor(currentPos.x) - (if (direction == Direction.EAST) 1 else 0)
            y0 = Mth.floor(currentPos.y) - (if (direction == Direction.UP) 1 else 0)
            z0 = Mth.floor(currentPos.z) - (if (direction == Direction.SOUTH) 1 else 0)

            var currState: BlockState? = world?.getBlockState(BlockPos(x0, y0, z0))
            var i = 0

            if (!MovementHelper.canStandOn(x0, y0, z0, ctx, currState) || !MovementHelper.canWalkThrough(
                    ctx,
                    x0,
                    y0 + 1,
                    z0
                ) || !MovementHelper.canWalkThrough(ctx, x0, y0 + 2, z0)
            ) {
                i = -3
                var foundValidBlock = false
                while (++i <= 3) {
                    if (i == 0) continue
                    currState = world?.getBlockState(BlockPos(x0, y0 + i, z0))
                    if (!MovementHelper.canStandOn(x0, y0 + i, z0, ctx, currState)) {
                        continue
                    }
                    if (!MovementHelper.canWalkThrough(ctx, x0, y0 + i + 1, z0)) {
                        continue
                    }
                    if (!MovementHelper.canWalkThrough(ctx, x0, y0 + i + 2, z0)) {
                        continue
                    }
                    foundValidBlock = true
                    break
                }
                if (!foundValidBlock) {
                    return false
                }
            }

            val delta = (y0 + i) - lastPos.getY()
            if (delta > 0) {
                if (delta > 1) {
                    return false
                }

                var sourceHeight = -1.0
                var destHeight = -1.0
                var snow = false

                if (lastState?.getBlock() is SnowLayerBlock) {
                    sourceHeight = (lastState.getValue(SnowLayerBlock.LAYERS) - 1) * 0.125
                    snow = true
                }

                if (currState?.getBlock() is SnowLayerBlock) {
                    destHeight = (currState.getValue(SnowLayerBlock.LAYERS) - 1) * 0.125
                    snow = true
                }

                if (!snow) {
                    val srcSmall: Boolean = MovementHelper.isBottomSlab(lastState)
                    val destSmall: Boolean = MovementHelper.isBottomSlab(currState)
                    val destSmallStair: Boolean =
                        MovementHelper.isValidStair(currState, x0 - lastPos.getX(), z0 - lastPos.getZ())

                    if (srcSmall != (destSmall || destSmallStair)) {
                        return false
                    } else if (srcSmall) {
                        return false
                    }
                } else {
                    if (sourceHeight == -1.0) {
                        val level = world
                        sourceHeight = if (level != null) {
                            lastState?.getCollisionShape(level, lastPos)?.max(Direction.Axis.Y) ?: 0.0
                        } else {
                            0.0
                        }
                    }
                    if (destHeight == -1.0) {
                        val level = world
                        destHeight = if (level != null) {
                            currState?.getCollisionShape(level, BlockPos(x0, y0 + i, z0))?.max(Direction.Axis.Y)
                                ?: 0.0
                        } else {
                            0.0
                        }
                    }
                    if (destHeight - sourceHeight > -0.5) {
                        return false
                    }
                }
            }

            lastState = currState
            lastPos = BlockPos(x0, y0 + i, z0)
        }
        return false
    }
}