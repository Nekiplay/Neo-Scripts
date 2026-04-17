package com.nekiplay.neoscripts.mixins.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nekiplay.neoscripts.utils.Rotations;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
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
        if (!Rotations.rotating) {
            return;
        }

        this.yRot = Rotations.serverYaw;
        this.xRot =  Rotations.serverPitch;
    }
}