package com.nekiplay.neoscripts.events.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PlayerVelocityStrafeEvent {
    public static final Event<PlayerVelocityCallback> EVENT = EventFactory.createArrayBacked(
            PlayerVelocityCallback.class,
            (listeners) -> (event) -> {
                for (PlayerVelocityCallback listener : listeners) {
                    InteractionResult result = listener.update(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    public Vec3 movementInput;
    public float speed;
    public float yaw;
    public Vec3 velocity;

    public PlayerVelocityStrafeEvent(Vec3 movementInput, float speed, float yaw, Vec3 velocity) {
        this.movementInput = movementInput;
        this.speed = speed;
        this.velocity = velocity;
        this.yaw = yaw;
    }

    public interface PlayerVelocityCallback {
        InteractionResult update(PlayerVelocityStrafeEvent event);
    }
}
