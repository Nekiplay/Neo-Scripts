package com.nekiplay.hypixelcry.pathfinder.movement.movements

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.pathfinder.movement.CalculationContext
import com.nekiplay.hypixelcry.pathfinder.movement.Movement
import com.nekiplay.hypixelcry.pathfinder.movement.MovementHelper
import com.nekiplay.hypixelcry.pathfinder.movement.MovementResult
import net.minecraft.core.BlockPos

class MovementTraverse(mm: HypixelCry, from: BlockPos, to: BlockPos) : Movement(mm, from, to) {

    override fun calculateCost(ctx: CalculationContext, res: MovementResult) {
        calculateCost(ctx, source.x, source.y, source.z, dest.x, dest.z, res)
        costs = res.cost
    }

    companion object {
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

        private fun cost(
            ctx: CalculationContext,
            x: Int,
            y: Int,
            z: Int,
            destX: Int,
            destZ: Int,
            res: MovementResult
        ) {
            if (!MovementHelper.canStandOn(destX, y, destZ, ctx)) return

            val destUpState = ctx.world?.getBlockState(BlockPos(destX, y + 1, destZ))
            if (!MovementHelper.canWalkThrough(
                    ctx,
                    destX,
                    y + 1,
                    destZ,
                    destUpState
                ) || !MovementHelper.canWalkThrough(ctx, destX, y + 2, destZ)
            ) return

            val srcUpState = ctx.world?.getBlockState(BlockPos(x, y + 1, z))

            val isSourceTopWalkableLadder = MovementHelper.canWalkIntoLadder(srcUpState, x - destX, z - destZ)
            val isDestTopWalkableLadder = MovementHelper.canWalkIntoLadder(destUpState, destX - x, destZ - z)
            if (MovementHelper.isLadder(destUpState) && !isDestTopWalkableLadder) {
                res.cost = ctx.cost.INF_COST
                return
            }
            if (MovementHelper.isLadder(srcUpState) && !isSourceTopWalkableLadder) {
                res.cost = ctx.cost.INF_COST
                return
            }

            val sourceState = ctx.world?.getBlockState(BlockPos(x, y, z))
            val destState = ctx.world?.getBlockState(BlockPos(destX, y, destZ))

            // Safe way to get sourceHeight
            val sourceHeight = try {
                sourceState?.getCollisionShape(ctx.world, BlockPos(x, y, z))?.bounds()?.maxY ?: y.toDouble()
            } catch (e: UnsupportedOperationException) {
                y.toDouble()
            }

            // Safe way to get destHeight
            val destHeight = try {
                destState?.getCollisionShape(ctx.world, BlockPos(destX, y, destZ))?.bounds()?.maxY ?: y.toDouble()
            } catch (e: UnsupportedOperationException) {
                y.toDouble()
            }

            val diff = destHeight - sourceHeight
            res.cost = when {
                diff <= 0.5 -> ctx.cost.ONE_BLOCK_SPRINT_COST
                diff <= 1 -> ctx.cost.JUMP_ONE_BLOCK_COST
                else -> ctx.cost.INF_COST
            }
        }
    }
}