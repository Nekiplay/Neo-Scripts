package com.nekiplay.neoscripts.mixins.minecraft;

import com.nekiplay.neoscripts.utils.aiming.RotationManager;
import com.nekiplay.neoscripts.utils.aiming.Rotations;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow
    public double xpos;

    @Shadow
    public double ypos;


    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void turnPlayerHook(double d, CallbackInfo ci) {
        if (Rotations.INSTANCE.getClientLook()) {
            if (!Float.isNaN(RotationManager.INSTANCE.getYaw()) && !Float.isNaN(RotationManager.INSTANCE.getPitch())) {
                ci.cancel();
            }
        }
    }
}