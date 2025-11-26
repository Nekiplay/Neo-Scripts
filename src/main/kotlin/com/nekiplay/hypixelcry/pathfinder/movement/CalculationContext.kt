package com.nekiplay.hypixelcry.pathfinder.movement


import com.nekiplay.hypixelcry.pathfinder.costs.ActionCosts
import com.nekiplay.hypixelcry.pathfinder.utils.player
import net.minecraft.client.Minecraft
import net.minecraft.world.effect.MobEffects

class CalculationContext(sprintFactor: Double = 0.13, walkFactor: Double = 0.1, sneakFactor: Double = 0.03) {
    val jumpBoostAmplifier = player?.activeEffectsMap[MobEffects.JUMP_BOOST]?.amplifier ?: 0
    val cost = ActionCosts(sprintFactor, walkFactor, sneakFactor, jumpBoostAmplifier)
    val maxFallHeight = 20
    val world = Minecraft.getInstance().level;
    var stepSize = 1
    var maxIterations = 15000
}