package com.nekiplay.neoscripts.utils;

import com.nekiplay.neoscripts.annotations.Init;
import com.nekiplay.neoscripts.events.SendMovementPacketsEvent;
import com.nekiplay.neoscripts.events.player.PlayerVelocityStrafeEvent;
import com.nekiplay.neoscripts.events.world.TickEvent;
import com.nekiplay.neoscripts.utils.misc.Pool;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.nekiplay.neoscripts.Main.mc;

public class Rotations {
    public static double getYaw(Entity entity) {
        return mc.player.getYRot() + Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(entity.getZ() - mc.player.getZ(), entity.getX() - mc.player.getX())) - 90f - mc.player.getYRot());
    }

    public static double getYaw(Vec3 pos) {
        return mc.player.getYRot() + Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(pos.z() - mc.player.getZ(), pos.x() - mc.player.getX())) - 90f - mc.player.getYRot());
    }

    public static double getPitch(Vec3 pos) {
        double diffX = pos.x() - mc.player.getX();
        double diffY = pos.y() - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = pos.z() - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getXRot() + Mth.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getXRot());
    }

    public static Vec3 getDirectionFromYawPitch(float yaw, float pitch) {
        // Конвертируем градусы в радианы
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Вычисляем компоненты вектора
        double x = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double y = -Mth.sin(pitchRad);
        double z = Mth.cos(yawRad) * Mth.cos(pitchRad);

        return new Vec3(x, y, z).normalize();
    }

    public static void rotate(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
        Rotation rotation = rotationPool.get();
        rotation.set(yaw, pitch, priority, clientSide, callback);

        int i = 0;
        for (; i < rotations.size(); i++) {
            if (priority > rotations.get(i).priority) break;
        }

        rotations.add(i, rotation);
    }

    public static void rotate(double yaw, double pitch, int priority, Runnable callback) {
        rotate(yaw, pitch, priority, false, callback);
    }

    public static void rotate(double yaw, double pitch, Runnable callback) {
        rotate(yaw, pitch, 0, callback);
    }

    public static void rotate(double yaw, double pitch, int priority) {
        rotate(yaw, pitch, priority, null);
    }

    public static void rotate(double yaw, double pitch) {
        rotate(yaw, pitch, 0, null);
    }

    // From Meteor Client
    private static final Pool<Rotation> rotationPool = new Pool<>(Rotation::new);
    private static final List<Rotation> rotations = new ArrayList<>();
    public static float serverYaw;
    public static float serverPitch;
    public static int rotationTimer;
    private static float preYaw, prePitch;
    private static int i = 0;

    private static Rotation lastRotation;
    private static int lastRotationTimer;
    private static boolean sentLastRotation;
    public static boolean rotating = false;

    private static void resetLastRotation() {
        if (lastRotation != null) {
            rotationPool.free(lastRotation);

            lastRotation = null;
            lastRotationTimer = 0;
        }
    }

    private static void setupMovementPacketRotation(Rotation rotation) {
        setClientRotation(rotation);
        setCamRotation(rotation.yaw, rotation.pitch);
    }

    public static void setCamRotation(double yaw, double pitch) {
        serverYaw = (float) yaw;
        serverPitch = (float) pitch;
        rotationTimer = 0;
    }

    private static void setClientRotation(Rotation rotation) {
        assert mc.player != null;
        preYaw = mc.player.yRotO;
        prePitch = mc.player.xRotO;

        mc.player.setYRot((float) rotation.yaw);
        mc.player.setXRot((float) rotation.pitch);
    }

    private static void resetPreRotation() {
        assert mc.player != null;
        mc.player.setYRot(preYaw);
        mc.player.setXRot(prePitch);
    }

    public static void init() {
        SendMovementPacketsEvent.POST.register(Rotations::onSendMovementPacketsPost);
        SendMovementPacketsEvent.PRE.register(Rotations::onSendMovementPacketsPre);
        ClientTickEvents.START_CLIENT_TICK.register(Rotations::onTick);
        PlayerVelocityStrafeEvent.EVENT.register(Rotations::playerVelocity);
    }

    private static InteractionResult playerVelocity(PlayerVelocityStrafeEvent event) {
        if (rotating) {
            event.velocity = Entity.getInputVector(
                    event.movementInput,
                    event.speed,
                    serverYaw
            );
        }
        return InteractionResult.PASS;
    }

    private static void onSendMovementPacketsPre(float v, float v1) {
        if (mc.getCameraEntity() != mc.player) return;
        sentLastRotation = false;

        if (!rotations.isEmpty()) {
            rotating = true;
            resetLastRotation();

            Rotation rotation = rotations.get(i);
            setupMovementPacketRotation(rotation);

            if (rotations.size() > 1) rotationPool.free(rotation);

            i++;
        } else if (lastRotation != null) {
            if (lastRotationTimer >= 1) {
                resetLastRotation();
                rotating = false;

                serverYaw = mc.player.getYRot();
                serverPitch = mc.player.getXRot();
            } else {
                setupMovementPacketRotation(lastRotation);
                sentLastRotation = true;

                lastRotationTimer++;
            }
        } else {
            rotating = false;
            serverYaw = mc.player.getYRot();
            serverPitch = mc.player.getXRot();
        }
    }

    private static void onTick(Minecraft minecraft) {
        rotationTimer++;
    }

    private static void onSendMovementPacketsPost() {
        if (!rotations.isEmpty()) {
            if (mc.getCameraEntity() == mc.player) {
                rotations.get(i - 1).runCallback();

                if (rotations.size() == 1) lastRotation = rotations.get(i - 1);

                resetPreRotation();
            }

            for (; i < rotations.size(); i++) {
                Rotation rotation = rotations.get(i);

                setCamRotation(rotation.yaw, rotation.pitch);
                if (rotation.clientSide) setClientRotation(rotation);
                rotation.sendPacket();
                if (rotation.clientSide) resetPreRotation();

                if (i == rotations.size() - 1) lastRotation = rotation;
                else rotationPool.free(rotation);
            }

            rotations.clear();
            i = 0;
        } else if (sentLastRotation) {
            resetPreRotation();
        }
    }


    private static class Rotation {
        public double yaw, pitch;
        public int priority;
        public boolean clientSide;
        public Runnable callback;

        public void set(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.priority = priority;
            this.clientSide = clientSide;
            this.callback = callback;
        }

        public void sendPacket() {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot((float) yaw, (float) pitch, mc.player.onGround(), mc.player.horizontalCollision));
            runCallback();
        }

        public void runCallback() {
            if (callback != null) callback.run();
        }
    }
}