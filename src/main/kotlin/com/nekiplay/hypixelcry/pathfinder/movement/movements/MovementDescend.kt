package com.nekiplay.hypixelcry.pathfinder.movement.movements

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.pathfinder.movement.CalculationContext
import com.nekiplay.hypixelcry.pathfinder.movement.Movement
import com.nekiplay.hypixelcry.pathfinder.movement.MovementHelper
import com.nekiplay.hypixelcry.pathfinder.movement.MovementResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

class MovementDescend(mm: HypixelCry, from: BlockPos, to: BlockPos) : Movement(mm, from, to) {
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
            res.set(destX, y - 1, destZ)
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
            val destUpState = ctx.world?.getBlockState(BlockPos(destX, y, destZ))
            if (!MovementHelper.canWalkThrough(ctx, destX, y + 2, destZ)
                || !MovementHelper.canWalkThrough(ctx, destX, y + 1, destZ)
                || !MovementHelper.canWalkThrough(ctx, destX, y, destZ, destUpState)
            ) {
                return
            }
            val sourceState = ctx.world?.getBlockState(BlockPos(x, y, z))
            if (MovementHelper.isLadder(sourceState) || MovementHelper.isLadder(destUpState)) {
                return
            }
            val destState = ctx.world?.getBlockState(BlockPos(destX, y - 1, destZ))
            if (!MovementHelper.canStandOn(
                    destX,
                    y - 1,
                    destZ,
                    ctx,
                    destState
                ) || MovementHelper.isLadder(destState)
            ) {
                freeFallCost(ctx, y, destX, destZ, destState, res)
                return
            }

            // Safe way to get sourceHeight
            val sourceHeight = try {
                sourceState?.getCollisionShape(ctx.world, BlockPos(x, y, z))?.bounds()?.maxY ?: y.toDouble()
            } catch (e: UnsupportedOperationException) {
                y.toDouble()
            }

            // Safe way to get destHeight
            val destHeight = try {
                destState?.getCollisionShape(ctx.world, BlockPos(destX, y - 1, destZ))?.bounds()?.maxY ?: (y - 1.0)
            } catch (e: UnsupportedOperationException) {
                (y - 1.0)
            }

            val diff = sourceHeight - destHeight
            res.cost = when {
                diff <= 0.5 -> ctx.cost.ONE_BLOCK_WALK_COST
                diff <= 1.125 -> ctx.cost.WALK_OFF_ONE_BLOCK_COST * ctx.cost.SPRINT_MULTIPLIER + ctx.cost.N_BLOCK_FALL_COST[1]
                else -> ctx.cost.INF_COST
            }
        }

        fun freeFallCost(
            ctx: CalculationContext,
            y: Int,
            destX: Int,
            destZ: Int,
            destState: BlockState?,
            res: MovementResult
        ) {
            // im starting from 2 because I work with the blocks itself. x, y, z aren't for sourceBlock.up() like its in baritone its sourceBlock
            if (!MovementHelper.canWalkThrough(ctx, destX, y - 1, destZ, destState)) {
                return
            }

            var effStartHeight = y // for ladder
            var cost = 0.0
            for (fellSoFar in 2..Int.MAX_VALUE) {
                val newY = y - fellSoFar
                if (newY < 0) return

                val blockOnto = ctx.world?.getBlockState(BlockPos(destX, newY, destZ))
                val unprotectedFallHeight = fellSoFar - (y - effStartHeight) // basic math
                val costUpUntilThisBlock =
                    ctx.cost.WALK_OFF_ONE_BLOCK_COST + ctx.cost.N_BLOCK_FALL_COST[unprotectedFallHeight] + cost

                // This is probably a massive monkeypatch. Can't wait to suffer
                if (!MovementHelper.canStandOn(destX, newY, destZ, ctx, blockOnto)) {
                    if (MovementHelper.isWater(blockOnto)) {
                        if (MovementHelper.canStandOn(destX, newY - 1, destZ, ctx)) {
                            res.y = newY - 1
                            res.cost = costUpUntilThisBlock
                            return
                        }
                        return
                    }

                    if (!MovementHelper.canWalkThrough(ctx, destX, newY, destZ, blockOnto)) {
                        return
                    }
                    continue
                }
                if (unprotectedFallHeight <= 11 && MovementHelper.isLadder(blockOnto)) {
                    // very cool logic baritone is built by smart ppl ong
                    cost += ctx.cost.N_BLOCK_FALL_COST[unprotectedFallHeight - 1] + ctx.cost.ONE_DOWN_LADDER_COST
                    effStartHeight = newY
                    continue
                }
                if (fellSoFar <= ctx.maxFallHeight) {
                    res.y = newY
                    res.cost = costUpUntilThisBlock
                    return
                }
                return
            }
        }
    }
}