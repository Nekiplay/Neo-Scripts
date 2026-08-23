package com.nekiplay.neoscripts.client.utils.render;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class CameraState {
    private static Matrix4f viewMatrix;
    private static Matrix4f projectionMatrix;
    private static int viewportWidth;
    private static int viewportHeight;
    private static boolean valid;

    public static void capture(Matrix4f viewRotation, Matrix4f projection, Vec3 cameraPos, int vpW, int vpH) {
        viewMatrix = new Matrix4f(viewRotation);
        viewMatrix.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
        projectionMatrix = new Matrix4f(projection);
        viewportWidth = vpW;
        viewportHeight = vpH;
        valid = true;
    }

    public static Matrix4f getViewMatrix() {
        return valid ? new Matrix4f(viewMatrix) : null;
    }

    public static Matrix4f getProjectionMatrix() {
        return valid ? new Matrix4f(projectionMatrix) : null;
    }

    public static int getViewportWidth() {
        return viewportWidth;
    }

    public static int getViewportHeight() {
        return viewportHeight;
    }

    public static boolean isValid() {
        return valid;
    }
}
