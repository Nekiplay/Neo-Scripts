package com.nekiplay.hypixelcry.pathfinder.movement.movements

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.pathfinder.movement.CalculationContext
import com.nekiplay.hypixelcry.pathfinder.movement.Movement
import com.nekiplay.hypixelcry.pathfinder.movement.MovementHelper
import com.nekiplay.hypixelcry.pathfinder.movement.MovementResult
import net.minecraft.core.BlockPos

class MovementAscend(mm: HypixelCry, from: BlockPos, to: BlockPos) : Movement(mm, from, to) {
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
            res.set(destX, y + 1, destZ)
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
            val destPos = BlockPos(destX, y + 1, destZ)
            val destState = ctx.world?.getBlockState(destPos)

            if (!MovementHelper.canStandOn(destX, y + 1, destZ, ctx, destState)) return
            if (!MovementHelper.canWalkThrough(ctx, destX, y + 3, destZ)) return
            if (!MovementHelper.canWalkThrough(ctx, destX, y + 2, destZ)) return
            if (!MovementHelper.canWalkThrough(ctx, x, y + 3, z)) return

            val sourcePos = BlockPos(x, y, z)
            val sourceState = ctx.world?.getBlockState(sourcePos)

            if (MovementHelper.isLadder(sourceState)) return
            if (MovementHelper.isLadder(destState) && !MovementHelper.canWalkIntoLadder(
                    destState,
                    destX - x,
                    destZ - z
                )
            ) return

            // Safe way to get sourceHeight
            val sourceHeight = try {
                sourceState?.getCollisionShape(ctx.world, sourcePos)?.bounds()?.maxY ?: y.toDouble()
            } catch (e: UnsupportedOperationException) {
                y.toDouble()
            }

            // Safe way to get destHeight
            val destHeight = try {
                destState?.getCollisionShape(ctx.world, destPos)?.bounds()?.maxY ?: (y + 1.0)
            } catch (e: UnsupportedOperationException) {
                (y + 1.0)
            }

            val diff = destHeight - sourceHeight
            res.cost = when {
                diff <= 0.5 -> ctx.cost.ONE_BLOCK_SPRINT_COST
                diff <= 1.125 -> ctx.cost.JUMP_ONE_BLOCK_COST
                else -> ctx.cost.INF_COST
            }
        }
    }
}