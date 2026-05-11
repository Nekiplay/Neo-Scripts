package com.nekiplay.neoscripts.events;

import net.minecraft.world.phys.Vec3;

public class PlayerVelocityStrafeEvent extends net.neoforged.bus.api.Event {
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
}