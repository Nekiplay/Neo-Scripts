package com.nekiplay.hypixelcry.pathfinder.movement

import com.nekiplay.hypixelcry.HypixelCry
import net.minecraft.core.BlockPos

interface IMovement {
    val mm: HypixelCry
    val source: BlockPos
    val dest: BlockPos
    val costs: Double // plural cuz kotlin gae

    fun getCost(): Double
    fun calculateCost(ctx: CalculationContext, res: MovementResult)
}