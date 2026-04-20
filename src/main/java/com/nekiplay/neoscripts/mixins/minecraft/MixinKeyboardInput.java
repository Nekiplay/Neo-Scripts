package com.nekiplay.neoscripts.mixins.minecraft;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nekiplay.neoscripts.events.player.MovementInputEvent;
import com.nekiplay.neoscripts.utils.DirectionalInput;
import com.nekiplay.neoscripts.utils.Rotations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import static com.mojang.math.Constants.DEG_TO_RAD;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput {

    @Shadow
    @Final
    private Options options;


    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input modifyInput(Input original) {
        var event = new MovementInputEvent(new DirectionalInput(original), original.jump(), original.shift());
        MovementInputEvent.EVENT.invoker().update(event);
        var untransformedDirectionalInput = event.directionalInput;
        var directionalInput = transformDirection(untransformedDirectionalInput);

        return new Input(
                directionalInput.getForwards(),
                directionalInput.getBackwards(),
                directionalInput.getLeft(),
                directionalInput.getRight(),
                event.jump,
                event.shift,
                original.sprint()
        );
    }

    @Unique
    private DirectionalInput transformDirection(DirectionalInput input) {
        var player = Minecraft.getInstance().player;

        float z = KeyboardInput.calculateImpulse(input.getForwards(), input.getBackwards());
        float x = KeyboardInput.calculateImpulse(input.getLeft(), input.getRight());

        if (!Rotations.rotating || !Rotations.movementCorrection
        || !Rotations.silentMovementCorrection || player == null) {
            return input;
        }

        float deltaYaw = player.getYRot() - Rotations.serverYaw;

        float newX = x * Mth.cos(deltaYaw * DEG_TO_RAD) - z *
                Mth.sin(deltaYaw * DEG_TO_RAD);
        float newZ = z * Mth.cos(deltaYaw * DEG_TO_RAD) + x *
                Mth.sin(deltaYaw * DEG_TO_RAD);

        var movementSideways = Math.round(newX);
        var movementForward = Math.round(newZ);

        return new DirectionalInput(movementForward, movementSideways);
    }

}