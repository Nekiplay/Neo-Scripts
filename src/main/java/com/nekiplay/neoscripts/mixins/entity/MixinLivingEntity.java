package com.nekiplay.neoscripts.mixins.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nekiplay.neoscripts.utils.Rotations;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {
    @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 hookFixRotation(Vec3 original) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }

        if (!Rotations.rotating || !Rotations.movementCorrection) {
            return original;
        }

        float yaw = Rotations.serverYaw * Mth.DEG_TO_RAD;

        return new Vec3(-Mth.sin(yaw) * 0.2F, 0.0, Mth.cos(yaw) * 0.2F);
    }

    @ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    private float hookModifyFallFlyingPitch(float original) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }

        if (!Rotations.rotating || !Rotations.movementCorrection) {
            return original;
        }

        return Rotations.serverPitch;
    }

    @ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 hookModifyFallFlyingRotationVector(Vec3 original) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }

        if (!Rotations.rotating || !Rotations.movementCorrection) {
            return original;
        }

        return Vec3.directionFromRotation(Rotations.serverYaw, Rotations.serverPitch);
    }
}
