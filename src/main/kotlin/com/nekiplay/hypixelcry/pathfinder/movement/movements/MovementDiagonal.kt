package com.nekiplay.hypixelcry.pathfinder.movement.movements

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.pathfinder.movement.CalculationContext
import com.nekiplay.hypixelcry.pathfinder.movement.Movement
import com.nekiplay.hypixelcry.pathfinder.movement.MovementHelper
import com.nekiplay.hypixelcry.pathfinder.movement.MovementResult
import net.minecraft.core.BlockPos
import kotlin.math.sqrt

class MovementDiagonal(mm: HypixelCry, from: BlockPos, to: BlockPos) : Movement(mm, from, to) {

    override fun calculateCost(ctx: CalculationContext, res: MovementResult) {
        calculateCost(ctx, source.x, source.y, source.z, dest.x, dest.z, res)
        costs = res.cost
    }

    companion object {
        private val SQRT_2 = sqrt(2.0)
        fun calculateCost(
            ctx: CalculationContext,
            x: Int,
            y: Int,
            z: Int,
            destX: Int,
            destZ: Int,
            res: MovementResult
        ) {
            res.set(destX, y, destZ)
            cost(ctx, x, y, z, destX, destZ, res)
        }

        // Todo: IS PROBABLY FUCKED verify that it isnt fucking cancer
        private fun cost(ctx: CalculationContext, x: Int, y: Int, z: Int, destX: Int, destZ: Int, res: MovementResult) {
            if (!MovementHelper.canWalkThrough(ctx, destX, y + 2, destZ)) return

            var ascend = false
            var descend = false
            val sourceState = ctx.world?.getBlockState(BlockPos(x, y, z))
            var destState = ctx.world?.getBlockState(BlockPos(destX, y, destZ))
            if (!MovementHelper.canWalkThrough(ctx, destX, y + 1, destZ)) {
                ascend = true
                if (!MovementHelper.canWalkThrough(ctx, x, y + 3, z) || !MovementHelper.canStandOn(
                        destX,
                        y + 1,
                        destZ,
                        ctx
                    ) || !MovementHelper.canWalkThrough(ctx, destX, y + 2, destZ)
                ) {
                    return
                }
                destState = ctx.world?.getBlockState(BlockPos(destX, y + 1, destZ))
                res.y = y + 1
            } else {
                if (!MovementHelper.canStandOn(destX, y, destZ, ctx, destState)) {
                    descend = true
                    if (!MovementHelper.canStandOn(
                            destX,
                            y - 1,
                            destZ,
                            ctx
                        ) || !MovementHelper.canWalkThrough(ctx, destX, y, destZ)
                    ) {
                        return
                    }
                    destState = ctx.world?.getBlockState(BlockPos(destX, y - 1, destZ))
                    res.y = y - 1
                }
            }

            var cost = ctx.cost.ONE_BLOCK_WALK_COST

            if (MovementHelper.isLadder(sourceState)) {
                return
            }

            if (MovementHelper.isWater(ctx.world?.getBlockState(BlockPos(x, y + 1, z)))) {
                if (ascend) return
                cost = ctx.cost.ONE_BLOCK_WALK_IN_WATER_COST * SQRT_2
            } else {
                cost *= ctx.cost.SPRINT_MULTIPLIER
            }

            val ALOWState = ctx.world?.getBlockState(BlockPos(x, y + 1, destZ))
            val BLOWState = ctx.world?.getBlockState(BlockPos(destX, y + 1, z))

            val ATOP = MovementHelper.canWalkThrough(ctx, x, y + 3, destZ)
            val AMID = MovementHelper.canWalkThrough(ctx, x, y + 2, destZ)
            val ALOW = MovementHelper.canWalkThrough(ctx, x, y + 1, destZ, ALOWState)
            val BTOP = MovementHelper.canWalkThrough(ctx, destX, y + 3, z)
            val BMID = MovementHelper.canWalkThrough(ctx, destX, y + 2, z)
            val BLOW = MovementHelper.canWalkThrough(ctx, destX, y + 1, z, BLOWState)

            if (!(ATOP && AMID && ALOW && BTOP && BMID && BLOW)) {
                return
            }
            if (!(ascend || descend)) {
                res.cost = cost * SQRT_2
                return
            }

            // Safe way to get sourceMaxY
            val sourceMaxY = try {
                sourceState?.getCollisionShape(ctx.world, BlockPos(x, y, z))?.bounds()?.maxY ?: y.toDouble()
            } catch (e: UnsupportedOperationException) {
                y.toDouble()
            }

            if (ascend) {
                // Safe way to get destMaxY
                val destMaxY = try {
                    destState?.getCollisionShape(ctx.world, BlockPos(destX, y + 1, destZ))?.bounds()?.maxY ?: (y + 1.0)
                } catch (e: UnsupportedOperationException) {
                    (y + 1.0)
                }
                when {
                    destMaxY - sourceMaxY <= 0.5 -> res.cost = cost * SQRT_2
                    destMaxY - sourceMaxY <= 1.125 -> res.cost = cost * SQRT_2 + ctx.cost.JUMP_ONE_BLOCK_COST
                    else -> res.cost = ctx.cost.INF_COST
                }
                return
            }

            if (descend) {
                // Safe way to get destMaxY
                val destMaxY = try {
                    destState?.getCollisionShape(ctx.world, BlockPos(destX, y - 1, destZ))?.bounds()?.maxY ?: (y - 1.0)
                } catch (e: UnsupportedOperationException) {
                    (y - 1.0)
                }
                when {
                    sourceMaxY - destMaxY <= 0.5 -> res.cost = cost * SQRT_2
                    sourceMaxY - destMaxY <= 1.0 -> res.cost = ctx.cost.N_BLOCK_FALL_COST[1] + cost * SQRT_2
                    else -> res.cost = ctx.cost.INF_COST
                }
            }
        }
    }
}