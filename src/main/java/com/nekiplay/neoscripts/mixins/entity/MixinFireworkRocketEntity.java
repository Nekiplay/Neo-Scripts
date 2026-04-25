package com.nekiplay.neoscripts.mixins.entity;

import com.nekiplay.neoscripts.utils.aiming.RotationManager;
import com.nekiplay.neoscripts.utils.aiming.Rotations;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketEntity.class)
public abstract class MixinFireworkRocketEntity {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 getLookAngleHook(LivingEntity instance) {
        if (Rotations.INSTANCE.getMoveFix() && !Float.isNaN(RotationManager.INSTANCE.getCurrentYaw())) {

            return instance.calculateViewVector(
                    RotationManager.INSTANCE.getCurrentPitch(),
                    RotationManager.INSTANCE.getCurrentYaw()
            );

        }

        return instance.getLookAngle();
    }
}