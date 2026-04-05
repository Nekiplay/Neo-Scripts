package com.nekiplay.neoscripts.mixins;

import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Frustum.class)
public interface FrustumInvoker {
    @Invoker
    int invokeCubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}