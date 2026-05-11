package com.nekiplay.neoscripts.utils.render;

import com.nekiplay.neoscripts.mixins.FrustumInvoker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;

public class FrustumUtils {

    public static boolean isVisible(Frustum frustum, AABB box) {
        return isVisible(frustum, box);
    }

    public static boolean isVisible(Frustum frustum, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int plane = ((FrustumInvoker) frustum).invokeCubeInFrustum(minX, minY, minZ, maxX, maxY, maxZ);

        return plane == FrustumIntersection.INSIDE || plane == FrustumIntersection.INTERSECT;
    }
}