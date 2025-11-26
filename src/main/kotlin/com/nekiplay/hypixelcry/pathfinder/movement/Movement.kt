package com.nekiplay.hypixelcry.pathfinder.movement

import com.nekiplay.hypixelcry.HypixelCry
import net.minecraft.core.BlockPos

abstract class Movement(override val mm: HypixelCry, override val source: BlockPos, override val dest: BlockPos) :
    IMovement {

    override var costs: Double = 1e6
    override fun getCost() = costs
}