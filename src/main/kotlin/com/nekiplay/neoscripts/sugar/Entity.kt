package com.nekiplay.neoscripts.sugar

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.utils.Rotations
import net.minecraft.world.entity.Entity

fun Entity.getRotation(): Pair<Float, Float> {
    return if (this == mc.player && Rotations.rotating) {
        Pair(Rotations.serverYaw, Rotations.serverPitch)
    }
    else {
        Pair(this.yRot, this.xRotO)
    }
}