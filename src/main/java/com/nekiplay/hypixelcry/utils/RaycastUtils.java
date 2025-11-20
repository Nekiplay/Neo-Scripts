package com.nekiplay.hypixelcry.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.util.List;

import static com.nekiplay.hypixelcry.HypixelCry.mc;

public class RaycastUtils {
    public static HitResult rayTraceToBlocks(Vec3d startVec, Vec3d endVec, List<Block> blocks) {
        return fastRayTrace(startVec, endVec, blocks);
    }


    private static HitResult fastRayTrace(Vec3d startVec, Vec3d endVec, List<Block> targetBlocks) {
        // Convert start and end positions to block coordinates
        int startX = (int) Math.floor(startVec.x);
        int startY = (int) Math.floor(startVec.y);
        int startZ = (int) Math.floor(startVec.z);

        int endX = (int) Math.floor(endVec.x);
        int endY = (int) Math.floor(endVec.y);
        int endZ = (int) Math.floor(endVec.z);

        // Check the starting block first
        BlockPos startPos = new BlockPos(startX, startY, startZ);
        BlockState startState = mc.world.getBlockState(startPos);

        if (startState.isSolidBlock(mc.world, startPos)) {
            BlockHitResult startHit = startState.getCollisionShape(mc.world, startPos).raycast(startVec, endVec, startPos);
            if (startHit != null) {
                return startHit;
            }
        }

        HitResult closestHit = null;
        int maxSteps = 200; // Prevent infinite loops

        while (maxSteps-- >= 0) {
            // Check if we've reached the end block
            if (startX == endX && startY == endY && startZ == endZ) {
                return closestHit;
            }

            // Determine which face of the current block we'll exit through
            boolean xDifferent = startX != endX;
            boolean yDifferent = startY != endY;
            boolean zDifferent = startZ != endZ;

            // Calculate exit points for each axis
            double xExit = xDifferent ? (endX > startX ? startX + 1.0 : startX) : 999.0;
            double yExit = yDifferent ? (endY > startY ? startY + 1.0 : startY) : 999.0;
            double zExit = zDifferent ? (endZ > startZ ? startZ + 1.0 : startZ) : 999.0;

            // Calculate ray direction
            double dx = endVec.x - startVec.x;
            double dy = endVec.y - startVec.y;
            double dz = endVec.z - startVec.z;

            // Calculate distance to each exit plane
            double xDist = xDifferent ? (xExit - startVec.x) / dx : 999.0;
            double yDist = yDifferent ? (yExit - startVec.y) / dy : 999.0;
            double zDist = zDifferent ? (zExit - startVec.z) / dz : 999.0;

            // Fix potential -0.0 values that could cause issues
            if (xDist == -0.0) xDist = -1.0E-4;
            if (yDist == -0.0) yDist = -1.0E-4;
            if (zDist == -0.0) zDist = -1.0E-4;

            Direction exitFace;

            // Determine which plane we hit first
            if (xDist < yDist && xDist < zDist) {
                exitFace = endX > startX ? Direction.WEST : Direction.EAST;
                startVec = new Vec3d(xExit, startVec.y + dy * xDist, startVec.z + dz * xDist);
            } else if (yDist < zDist) {
                exitFace = endY > startY ? Direction.DOWN : Direction.UP;
                startVec = new Vec3d(startVec.x + dx * yDist, yExit, startVec.z + dz * yDist);
            } else {
                exitFace = endZ > startZ ? Direction.NORTH : Direction.SOUTH;
                startVec = new Vec3d(startVec.x + dx * zDist, startVec.y + dy * zDist, zExit);
            }

            // Move to the next block
            startX = MathHelper.floor(startVec.x) - (exitFace == Direction.EAST ? 1 : 0);
            startY = MathHelper.floor(startVec.y) - (exitFace == Direction.UP ? 1 : 0);
            startZ = MathHelper.floor(startVec.z) - (exitFace == Direction.SOUTH ? 1 : 0);

            BlockPos newPos = new BlockPos(startX, startY, startZ);
            BlockState newState = mc.world.getBlockState(newPos);
            Block newBlock = newState.getBlock();

            // Check if we hit a block
            boolean shouldCheckBlock = targetBlocks.isEmpty()
                    ? newState.isSolidBlock(mc.world, newPos)
                    : targetBlocks.contains(newBlock);

            if (shouldCheckBlock) {
                BlockHitResult hit = newState.getCollisionShape(mc.world, newPos).raycast(startVec, endVec, newPos);
                if (hit != null) {
                    return hit;
                }
            }

            // Store the current position as a potential miss
            closestHit = new HitResult(startVec) {
                @Override
                public Type getType() {
                    return Type.MISS;
                }
            };
        }

        return closestHit;
    }

    public static HitResult findCrosshairTarget(Entity camera, double blockInteractionRange, double entityInteractionRange, float tickProgress) {
        double d = Math.max(blockInteractionRange, entityInteractionRange);
        double e = MathHelper.square(d);
        Vec3d vec3d = camera.getCameraPosVec(tickProgress);
        HitResult hitResult = camera.raycast(d, tickProgress, false);
        double f = hitResult.getPos().squaredDistanceTo(vec3d);
        if (hitResult.getType() != HitResult.Type.MISS) {
            e = f;
            d = Math.sqrt(f);
        }

        Vec3d vec3d2 = camera.getRotationVec(tickProgress);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
        float g = 1.0F;
        Box box = camera.getBoundingBox().stretch(vec3d2.multiply(d)).expand((double)1.0F, (double)1.0F, (double)1.0F);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(camera, vec3d, vec3d3, box, EntityPredicates.CAN_HIT, e);
        return entityHitResult != null && entityHitResult.getPos().squaredDistanceTo(vec3d) < f ? ensureTargetInRange(entityHitResult, vec3d, entityInteractionRange) : ensureTargetInRange(hitResult, vec3d, blockInteractionRange);
    }

    private static HitResult ensureTargetInRange(HitResult hitResult, Vec3d cameraPos, double interactionRange) {
        Vec3d vec3d = hitResult.getPos();
        if (!vec3d.isInRange(cameraPos, interactionRange)) {
            Vec3d vec3d2 = hitResult.getPos();
            Direction direction = Direction.getFacing(vec3d2.x - cameraPos.x, vec3d2.y - cameraPos.y, vec3d2.z - cameraPos.z);
            return BlockHitResult.createMissed(vec3d2, direction, BlockPos.ofFloored(vec3d2));
        } else {
            return hitResult;
        }
    }
}
