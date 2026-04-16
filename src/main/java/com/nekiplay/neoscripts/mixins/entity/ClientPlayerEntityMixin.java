package com.nekiplay.neoscripts.mixins.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import com.nekiplay.neoscripts.events.SendMovementPacketsEvent;
import com.nekiplay.neoscripts.utils.RaycastUtils;
import com.nekiplay.neoscripts.utils.Rotations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {


    @Shadow
    public ClientInput input;

    public ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    // Rotations

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendMovementPacketsHead(CallbackInfo ci) {
        SendMovementPacketsEvent.PRE.invoker().onSendMovementPacketsPre(getYRot(), getXRot());
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendMovementPacketsTail(CallbackInfo info) {
        SendMovementPacketsEvent.POST.invoker().onSendMovementPacketsPost();
    }

    @ModifyExpressionValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult hookRaycast(HitResult original, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        if (camera != Minecraft.getInstance().player) {
            return original;

        }

        if (Rotations.rotating) {
            return RaycastUtils.findCrosshairTarget(camera,
                    camera.getEyePosition(),
                    Rotations.serverYaw,
                    Rotations.serverPitch,
                    ((Player) camera).blockInteractionRange(),
                    ((Player) camera).entityInteractionRange()
            );
        }
        else {
            return original;
        }
    }
}