package com.nekiplay.neoscripts.mixins.minecraft.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nekiplay.neoscripts.utils.Rotations;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.class)
public abstract class MixinItem {

    @ModifyExpressionValue(method = "getPlayerPOVHitResult", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 hookFixRotation(Vec3 original, Level world, Player player, ClipContext.Fluid fluidHandling) {
        if (player == Minecraft.getInstance().player) {
            if (Rotations.rotating) {
                return Vec3.directionFromRotation(Rotations.serverPitch, Rotations.serverYaw);
            }
        }
        return original;
    }
}