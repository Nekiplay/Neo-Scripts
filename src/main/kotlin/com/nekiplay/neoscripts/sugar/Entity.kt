package com.nekiplay.neoscripts.sugar

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.utils.aiming.RotationManager
import net.minecraft.world.entity.Entity

fun Entity.getRotation(): Pair<Float, Float> {
    return if (this == mc.player && !RotationManager.getCurrentYaw().isNaN()) {
        Pair(RotationManager.getCurrentYaw(), RotationManager.getCurrentPitch())
    }
    else {
        Pair(this.yRot, this.xRotO)
    }
}