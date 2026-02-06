package com.nekiplay.hypixelcry.utils.render.primitive;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.nekiplay.hypixelcry.utils.render.Renderer;
import com.nekiplay.hypixelcry.utils.render.SkyblockerRenderPipelines;
import com.nekiplay.hypixelcry.utils.render.state.SphereRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.joml.Matrix4f;

public final class SphereRenderer implements PrimitiveRenderer<SphereRenderState> {
    protected static final SphereRenderer INSTANCE = new SphereRenderer();

    private SphereRenderer() {}

    @Override
    public void submitPrimitives(SphereRenderState state, CameraRenderState cameraState) {
        BufferBuilder buffer = Renderer.getBuffer(state.throughWalls ? SkyblockerRenderPipelines.CYLINDER_THROUGH_WALLS : SkyblockerRenderPipelines.CYLINDER);
        Matrix4f positionMatrix = new Matrix4f()
                .translate((float) -cameraState.pos.x, (float) -cameraState.pos.y, (float) -cameraState.pos.z);

        for (int lat = 0; lat < state.rings; lat++) {
            double lat0 = Math.PI * (double) lat / state.rings;
            double lat1 = Math.PI * (double) (lat + 1) / state.rings;

            float y0 = (float) Math.cos(lat0) * state.radius;
            float y1 = (float) Math.cos(lat1) * state.radius;

            float r0 = (float) Math.sin(lat0) * state.radius;
            float r1 = (float) Math.sin(lat1) * state.radius;

            for (int lon = 0; lon <= state.segments; lon++) {
                double angle = Math.TAU * (double) lon / state.segments;
                float x0 = (float) Math.cos(angle);
                float z0 = (float) Math.sin(angle);

                // First Triangle
                buffer.addVertex(positionMatrix,
                                Math.fma(x0, r0, (float) state.centre.x()),
                                (float) state.centre.y() + y0,
                                Math.fma(z0, r0, (float) state.centre.z()))
                        .setColor(state.colour);

                buffer.addVertex(positionMatrix,
                                Math.fma(x0, r1, (float) state.centre.x()),
                                (float) state.centre.y() + y1,
                                Math.fma(z0, r1, (float) state.centre.z()))
                        .setColor(state.colour);
            }
        }
    }
}