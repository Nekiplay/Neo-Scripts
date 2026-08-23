package com.nekiplay.neoscripts.client.utils.helper;

import com.nekiplay.neoscripts.client.utils.AngleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Target {

    private Vec3 vec;
    private Entity entity;
    private BlockPos blockPos;
    private Angle angle;
    private float additionalY = (float) (1 + Math.random()) * 0.75f;

    public Target(Vec3 vec) {
        this.vec = vec;
    }

    public Target(Entity entity) {
        this.entity = entity;
    }

    public Target(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public Target(Angle angle) {
        this.angle = angle;
    }

    // Ensures Rotation Always Ends
    public Angle getTargetAngle() {
        if (blockPos != null) {
            return AngleUtils.getRotation(blockPos);
        }

        if (vec != null) {
            return AngleUtils.getRotation(vec);
        }

        if (entity != null) {
            return AngleUtils.getRotation(entity.position().add(0, additionalY, 0));
        }

        return angle;
    }

    @Override
    public String toString() {
        return "Vec3: " + this.vec + ", Ent: " + (this.entity != null ? this.entity.getId() : "null") + ", Pos: " + this.blockPos + ", Angle: " + this.angle;
    }
}