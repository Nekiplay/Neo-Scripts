package com.nekiplay.neoscripts.mixins.minecraft.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nekiplay.neoscripts.utils.aiming.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity {
    @Shadow
    private LivingEntity attachedToEntity;

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 getRotationVector(Vec3 original) {
        if (attachedToEntity != Minecraft.getInstance().player) {
            return original;
        }

        if (!Float.isNaN(RotationManager.INSTANCE.getCurrentYaw())) {
            return Vec3.directionFromRotation(RotationManager.INSTANCE.getCurrentPitch(), RotationManager.INSTANCE.getCurrentYaw());
        }

        return original;
    }
}
