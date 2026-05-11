package com.nekiplay.neoscripts.utils;

import com.nekiplay.neoscripts.utils.helper.Angle;
import net.minecraft.world.phys.Vec3;

import static com.nekiplay.neoscripts.Main.mc;

public class PlayerUtils {
    public static Vec3 getEyePosition() {
        return new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
    }

    public static Vec3 getLookEndPos(Vec3 target, float distance) {
        Angle angle = AngleUtils.getRotation(target);
        Vec3 look = AngleUtils.getVectorForRotation(angle.pitch, angle.yaw);
        return getEyePosition().add(look.x * distance, look.y * distance, look.z * distance);
    }

    public static Vec3 getLookEndPos(float distance) {
        Vec3 look = AngleUtils.getVectorForRotation(mc.player.getXRot(), mc.player.getYRot());
        return getEyePosition().add(look.x * distance, look.y * distance, look.z * distance);
    }
}

