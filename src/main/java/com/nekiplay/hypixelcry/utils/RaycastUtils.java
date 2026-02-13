package com.nekiplay.hypixelcry.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

import java.util.List;

import static com.nekiplay.hypixelcry.HypixelCry.mc;

public class RaycastUtils {
    public static HitResult rayTraceToBlocks(Vec3 startVec, Vec3 endVec, List<Block> blocks) {
        return fastRayTrace(startVec, endVec, blocks);
    }

    public static HitResult fastRayTrace(Entity sourceEntity, Vec3 startVec, Vec3 endVec, List<Block> targetBlocks) {
        HitResult resultBlockHit = BlockHitResult.miss(
                endVec,
                Direction.getApproximateNearest(
                        endVec.x - startVec.x,
                        endVec.y - startVec.y,
                        endVec.z - startVec.z
                ),
                BlockPos.containing(endVec)
        );

        // Координаты для цикла
        int startX = Mth.floor(startVec.x);
        int startY = Mth.floor(startVec.y);
        int startZ = Mth.floor(startVec.z);

        int endX = Mth.floor(endVec.x);
        int endY = Mth.floor(endVec.y);
        int endZ = Mth.floor(endVec.z);

        double currentX = startVec.x;
        double currentY = startVec.y;
        double currentZ = startVec.z;

        // Проверка стартового блока
        BlockPos startPos = new BlockPos(startX, startY, startZ);
        BlockState startState = mc.level.getBlockState(startPos);

        // Немного оптимизируем проверку стартового блока
        if ((targetBlocks.isEmpty() && startState.isRedstoneConductor(mc.level, startPos)) || targetBlocks.contains(startState.getBlock())) {
            BlockHitResult startHit = startState.getCollisionShape(mc.level, startPos).clip(startVec, endVec, startPos);
            if (startHit != null) {
                resultBlockHit = startHit;
                // Если мы попали в блок прямо в старте, то дальше идти нет смысла,
                // НО мы не возвращаем сразу, так как внутри блока может быть Entity (редко, но бывает)
                // Однако для оптимизации можно считать, что блок перекрыл всё.
                // Для точности - идем к ЭТАПУ 2, используя hit блока как конец.
            }
        }

        // Если в стартовом блоке не застряли, идем по DDA
        if (resultBlockHit.getType() == HitResult.Type.MISS) {
            int maxSteps = 200;

            // Вспомогательные переменные для DDA
            int stepX = endX > startX ? 1 : -1;
            int stepY = endY > startY ? 1 : -1;
            int stepZ = endZ > startZ ? 1 : -1;

            double dx = endVec.x - startVec.x;
            double dy = endVec.y - startVec.y;
            double dz = endVec.z - startVec.z;

            // Текущие координаты в сетке
            int currBlockX = startX;
            int currBlockY = startY;
            int currBlockZ = startZ;

            while (maxSteps-- >= 0) {
                if (currBlockX == endX && currBlockY == endY && currBlockZ == endZ) {
                    break;
                }

                // Твоя логика расчета граней (чуть упрощенная для читаемости, но суть та же)
                boolean xDifferent = currBlockX != endX;
                boolean yDifferent = currBlockY != endY;
                boolean zDifferent = currBlockZ != endZ;

                double xExit = xDifferent ? (stepX > 0 ? currBlockX + 1.0 : currBlockX) : Double.MAX_VALUE;
                double yExit = yDifferent ? (stepY > 0 ? currBlockY + 1.0 : currBlockY) : Double.MAX_VALUE;
                double zExit = zDifferent ? (stepZ > 0 ? currBlockZ + 1.0 : currBlockZ) : Double.MAX_VALUE;

                double xDist = xDifferent ? (xExit - currentX) / dx : Double.MAX_VALUE;
                double yDist = yDifferent ? (yExit - currentY) / dy : Double.MAX_VALUE;
                double zDist = zDifferent ? (zExit - currentZ) / dz : Double.MAX_VALUE;

                // Защита от NaN/-0.0
                if (xDist < 0) xDist = Double.MAX_VALUE;
                if (yDist < 0) yDist = Double.MAX_VALUE;
                if (zDist < 0) zDist = Double.MAX_VALUE;

                Direction exitFace;

                if (xDist < yDist && xDist < zDist) {
                    exitFace = stepX > 0 ? Direction.WEST : Direction.EAST;
                    currentX = xExit;
                    currentY += dy * xDist;
                    currentZ += dz * xDist;
                    currBlockX += stepX;
                } else if (yDist < zDist) {
                    exitFace = stepY > 0 ? Direction.DOWN : Direction.UP;
                    currentX += dx * yDist;
                    currentY = yExit;
                    currentZ += dz * yDist;
                    currBlockY += stepY;
                } else {
                    exitFace = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
                    currentX += dx * zDist;
                    currentY += dy * zDist;
                    currentZ = zExit;
                    currBlockZ += stepZ;
                }

                BlockPos newPos = new BlockPos(currBlockX, currBlockY, currBlockZ);
                BlockState newState = mc.level.getBlockState(newPos);
                Block newBlock = newState.getBlock();

                boolean shouldCheckBlock = targetBlocks.isEmpty()
                        ? newState.isRedstoneConductor(mc.level, newPos) // Или другой предикат коллизии
                        : targetBlocks.contains(newBlock);

                if (shouldCheckBlock) {
                    BlockHitResult hit = newState.getCollisionShape(mc.level, newPos).clip(startVec, endVec, newPos);
                    if (hit != null) {
                        resultBlockHit = hit;
                        break; // Мы нашли блок, прерываем цикл
                    }
                }
            }
        }

        // --- ЭТАП 2: Raytrace Сущностей ---

        // Определяем максимальную дистанцию для проверки энтити.
        // Если мы ударились в блок, мы не должны проверять энтити ЗА стеной.
        Vec3 effectiveEndVec = resultBlockHit.getLocation();
        double distToBlockSqr = startVec.distanceToSqr(effectiveEndVec);

        // Создаем AABB вокруг всего пути луча, чтобы найти кандидатов
        AABB area = new AABB(startVec, effectiveEndVec).inflate(1.0);

        // Используем ванильный ProjectileUtil, он очень быстр и корректен
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                sourceEntity,
                startVec,
                effectiveEndVec,
                area,
                (e) -> !e.isSpectator() && e.isPickable(), // Предикат: можно ли попасть в энтити
                distToBlockSqr
        );

        // --- ЭТАП 3: Выбор победителя ---

        // Если энтити найден, ProjectileUtil уже гарантирует, что он ближе,
        // чем переданная дистанция (distToBlockSqr), либо равен ей.
        // Но на всякий случай проверяем null.

        if (entityHit != null) {
            // Дополнительная проверка: ProjectileUtil ищет ближайшего в направлении вектора,
            // но ограничен дистанцией. Если он вернул результат, значит энтити ближе стены.
            return entityHit;
        }

        return resultBlockHit;
    }


    private static HitResult fastRayTrace(Vec3 startVec, Vec3 endVec, List<Block> targetBlocks) {
        // Convert start and end positions to block coordinates
        int startX = (int) Math.floor(startVec.x);
        int startY = (int) Math.floor(startVec.y);
        int startZ = (int) Math.floor(startVec.z);

        int endX = (int) Math.floor(endVec.x);
        int endY = (int) Math.floor(endVec.y);
        int endZ = (int) Math.floor(endVec.z);

        // Check the starting block first
        BlockPos startPos = new BlockPos(startX, startY, startZ);
        BlockState startState = mc.level.getBlockState(startPos);

        if (startState.isRedstoneConductor(mc.level, startPos)) {
            BlockHitResult startHit = startState.getCollisionShape(mc.level, startPos).clip(startVec, endVec, startPos);
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
                startVec = new Vec3(xExit, startVec.y + dy * xDist, startVec.z + dz * xDist);
            } else if (yDist < zDist) {
                exitFace = endY > startY ? Direction.DOWN : Direction.UP;
                startVec = new Vec3(startVec.x + dx * yDist, yExit, startVec.z + dz * yDist);
            } else {
                exitFace = endZ > startZ ? Direction.NORTH : Direction.SOUTH;
                startVec = new Vec3(startVec.x + dx * zDist, startVec.y + dy * zDist, zExit);
            }

            // Move to the next block
            startX = Mth.floor(startVec.x) - (exitFace == Direction.EAST ? 1 : 0);
            startY = Mth.floor(startVec.y) - (exitFace == Direction.UP ? 1 : 0);
            startZ = Mth.floor(startVec.z) - (exitFace == Direction.SOUTH ? 1 : 0);

            BlockPos newPos = new BlockPos(startX, startY, startZ);
            BlockState newState = mc.level.getBlockState(newPos);
            Block newBlock = newState.getBlock();

            // Check if we hit a block
            boolean shouldCheckBlock = targetBlocks.isEmpty()
                    ? newState.isRedstoneConductor(mc.level, newPos)
                    : targetBlocks.contains(newBlock);

            if (shouldCheckBlock) {
                BlockHitResult hit = newState.getCollisionShape(mc.level, newPos).clip(startVec, endVec, newPos);
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
        double e = Mth.square(d);
        Vec3 vec3d = camera.getEyePosition(tickProgress);
        HitResult hitResult = camera.pick(d, tickProgress, false);
        double f = hitResult.getLocation().distanceToSqr(vec3d);
        if (hitResult.getType() != HitResult.Type.MISS) {
            e = f;
            d = Math.sqrt(f);
        }

        Vec3 vec3d2 = camera.getViewVector(tickProgress);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
        float g = 1.0F;
        AABB box = camera.getBoundingBox().expandTowards(vec3d2.scale(d)).inflate((double)1.0F, (double)1.0F, (double)1.0F);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(camera, vec3d, vec3d3, box, EntitySelector.CAN_BE_PICKED, e);
        return entityHitResult != null && entityHitResult.getLocation().distanceToSqr(vec3d) < f ? ensureTargetInRange(entityHitResult, vec3d, entityInteractionRange) : ensureTargetInRange(hitResult, vec3d, blockInteractionRange);
    }

    private static HitResult ensureTargetInRange(HitResult hitResult, Vec3 cameraPos, double interactionRange) {
        Vec3 vec3d = hitResult.getLocation();
        if (!vec3d.closerThan(cameraPos, interactionRange)) {
            Vec3 vec3d2 = hitResult.getLocation();
            Direction direction = Direction.getApproximateNearest(vec3d2.x - cameraPos.x, vec3d2.y - cameraPos.y, vec3d2.z - cameraPos.z);
            return BlockHitResult.miss(vec3d2, direction, BlockPos.containing(vec3d2));
        } else {
            return hitResult;
        }
    }
}
