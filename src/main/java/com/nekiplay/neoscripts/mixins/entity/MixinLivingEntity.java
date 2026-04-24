package com.nekiplay.neoscripts.mixins.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nekiplay.neoscripts.utils.Rotations;
import com.nekiplay.neoscripts.utils.aiming.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import static com.nekiplay.neoscripts.Main.mc;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {
    @Unique
    private final LivingEntity me = (LivingEntity) ((Object) this);

    @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float getYRot(float original) {
        if (me == mc.player) {
            if (!Float.isNaN(RotationManager.INSTANCE.getYaw())) {
                return RotationManager.INSTANCE.getYaw();
            }
        }
        return original;
    }

}
