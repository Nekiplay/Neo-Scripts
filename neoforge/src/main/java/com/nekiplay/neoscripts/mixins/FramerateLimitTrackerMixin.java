package com.nekiplay.neoscripts.mixins;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public class FramerateLimitTrackerMixin {

    @Shadow
    private int framerateLimit;

    @Inject(
            method = "getFramerateLimit",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        FramerateLimitTracker.FramerateThrottleReason reason =
                ((FramerateLimitTracker)(Object)this).getThrottleReason();

        if (reason == FramerateLimitTracker.FramerateThrottleReason.WINDOW_ICONIFIED
                || reason == FramerateLimitTracker.FramerateThrottleReason.SHORT_AFK
                || reason == FramerateLimitTracker.FramerateThrottleReason.LONG_AFK) {

            cir.setReturnValue(this.framerateLimit);
        }
    }

    @Inject(
            method = "isHeavilyThrottled",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onIsHeavilyThrottled(CallbackInfoReturnable<Boolean> cir) {
        // Всегда возвращаем false, чтобы Minecraft не снижал FPS принудительно
        cir.setReturnValue(false);
    }
}