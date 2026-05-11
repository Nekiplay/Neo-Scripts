package com.nekiplay.neoscripts.mixins.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import com.nekiplay.neoscripts.events.*;
import com.nekiplay.neoscripts.events.main.EventBus;
import com.nekiplay.neoscripts.utils.RaycastUtils;
import com.nekiplay.neoscripts.utils.aiming.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends AbstractClientPlayer {


    @Shadow
    public ClientInput input;

    public MixinLocalPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickHook(CallbackInfo ci) {
        EventBus.INSTANCE.send(new PlayerTickEvent());
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void sendPositionHook(CallbackInfo ci) {
        EventBus.INSTANCE.send(new SyncPosEvent(true));
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void sendPostPositionHook(CallbackInfo ci) {
        EventBus.INSTANCE.send(new SyncPosEvent(false));
    }

    @Inject(method = "applyInput", at = @At("HEAD"), cancellable = true)
    private void applyInputHook(CallbackInfo ci) {
        if (EventBus.INSTANCE.sendCancellable(new ApplyInputEvent())) ci.cancel();
    }

    @Inject(method = "raycastHitResult", at = @At("RETURN"))
    private void raycastPostHitResultHook(float f, Entity entity, CallbackInfoReturnable<HitResult> cir) {
        EventBus.INSTANCE.send(new RaycastHitEvent(false));
    }
    @ModifyExpressionValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult hookRaycast(HitResult original, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        if (camera != Minecraft.getInstance().player) {
            return original;

        }

        if (!Float.isNaN(RotationManager.INSTANCE.getCurrentYaw())) {
            return RaycastUtils.findCrosshairTarget(camera,
                    camera.getEyePosition(),
                    RotationManager.INSTANCE.getCurrentYaw(),
                    RotationManager.INSTANCE.getCurrentPitch(),
                    ((Player) camera).blockInteractionRange(),
                    ((Player) camera).entityInteractionRange()
            );
        }
        else {
            return original;
        }
    }

    @ModifyExpressionValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 hookRotationVector(Vec3 original, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        if (camera != Minecraft.getInstance().player) {
            return original;
        }

        if (!Float.isNaN(RotationManager.INSTANCE.getCurrentYaw())) {
            return Vec3.directionFromRotation(RotationManager.INSTANCE.getCurrentYaw(), RotationManager.INSTANCE.getCurrentPitch());
        }
        else {
            return original;
        }
    }
}