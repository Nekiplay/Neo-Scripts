package com.nekiplay.neoscripts.mixins.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nekiplay.neoscripts.utils.Rotations;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class MixinPlayer  {
    @ModifyExpressionValue(method = {"causeExtraKnockback",
            "doSweepAttack"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float hookFixRotation(float original) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }

        if (!Rotations.rotating) {
            return original;
        }

        return Rotations.serverYaw;
    }

}
