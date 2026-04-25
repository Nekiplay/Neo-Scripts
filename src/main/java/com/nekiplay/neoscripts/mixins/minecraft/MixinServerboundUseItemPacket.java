package com.nekiplay.neoscripts.mixins.minecraft;

import com.nekiplay.neoscripts.utils.aiming.RotationManager;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Rotation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundUseItemPacket.class)
public abstract class MixinServerboundUseItemPacket {

    @Mutable
    @Shadow
    @Final
    private float yRot;

    @Mutable
    @Shadow
    @Final
    private float xRot;

    @Inject(method = "<init>(Lnet/minecraft/world/InteractionHand;IFF)V", at = @At("RETURN"))
    private void modifyRotation(InteractionHand hand, int sequence, float yaw, float pitch, CallbackInfo ci) {
        if (Float.isNaN(RotationManager.INSTANCE.getCurrentYaw())) {
            return;
        }

        this.yRot = RotationManager.INSTANCE.getCurrentYaw();
        this.xRot =  RotationManager.INSTANCE.getCurrentPitch();
    }
}