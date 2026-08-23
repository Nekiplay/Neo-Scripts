package com.nekiplay.neoscripts.common.mixins.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nekiplay.neoscripts.client.utils.aiming.RotationManager;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import static com.nekiplay.neoscripts.ClientMain.mc;

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
