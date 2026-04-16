package com.nekiplay.neoscripts.mixins.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.nekiplay.neoscripts.events.player.PlayerVelocityStrafeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract  class MixinEntity {

    @Shadow
    public boolean noPhysics;

    @Shadow
    public abstract boolean onGround();

    @Shadow
    public abstract boolean isPassenger();

    @Shadow
    public abstract boolean isAlwaysTicking();

    @Shadow
    public abstract Level level();

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow public abstract float getYRot();

    @ModifyExpressionValue(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"))
    public Vec3 hookVelocity(Vec3 original, @Local(argsOnly = true) Vec3 movementInput, @Local(argsOnly = true) float speed, @Local(argsOnly = true) float yaw) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }

        var event = new PlayerVelocityStrafeEvent(movementInput, speed, yaw, original);

        PlayerVelocityStrafeEvent.EVENT.invoker().update(event);

        return event.velocity;
    }
}
