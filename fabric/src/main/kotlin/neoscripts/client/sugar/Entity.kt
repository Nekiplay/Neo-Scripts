package com.nekiplay.neoscripts.client.sugar

import com.nekiplay.neoscripts.ClientMain.mc
import com.nekiplay.neoscripts.client.utils.aiming.RotationManager
import net.minecraft.world.entity.Entity

fun Entity.getRotation(): Pair<Float, Float> {
    return if (this == mc.player && !RotationManager.getCurrentYaw().isNaN()) {
        Pair(RotationManager.getCurrentYaw(), RotationManager.getCurrentPitch())
    }
    else {
        Pair(this.yRot, this.xRotO)
    }
}