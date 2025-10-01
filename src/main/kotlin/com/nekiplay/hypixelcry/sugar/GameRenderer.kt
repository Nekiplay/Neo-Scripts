package com.nekiplay.hypixelcry.sugar

import com.nekiplay.hypixelcry.mixins.renderer.GameRendererAccessor
import net.minecraft.client.render.Camera
import net.minecraft.client.render.GameRenderer

fun GameRenderer.getFov(camera: Camera, tickProgress: Float, changingFov: Boolean): Float {
    return (this as GameRendererAccessor).getFov(camera, tickProgress, changingFov)
}