package com.nekiplay.hypixelcry.utils.render.primitive;

import com.nekiplay.hypixelcry.utils.render.Renderer;
import com.nekiplay.hypixelcry.utils.render.SkyblockerRenderPipelines;
import com.nekiplay.hypixelcry.utils.render.state.CylinderRenderState;
import org.joml.Matrix4f;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.state.CameraRenderState;

public final class CylinderRenderer implements PrimitiveRenderer<CylinderRenderState> {
    protected static final CylinderRenderer INSTANCE = new CylinderRenderer();

    private CylinderRenderer() {}

    @Override
    public void submitPrimitives(CylinderRenderState state, CameraRenderState cameraState) {
        BufferBuilder buffer = Renderer.getBuffer(SkyblockerRenderPipelines.CYLINDER);
        Matrix4f positionMatrix = new Matrix4f()
                .translate((float) -cameraState.pos.x, (float) -cameraState.pos.y, (float) -cameraState.pos.z);

        float halfHeight = state.height / 2.0f;

        for (int i = 0; i <= state.segments; i++) {
            double angle = Math.TAU * i / state.segments;
            float dx = (float) Math.cos(angle) * state.radius;
            float dz = (float) Math.sin(angle) * state.radius;

            buffer.vertex(positionMatrix, (float) state.centre.getX() + dx, (float) state.centre.getY() + halfHeight, (float) state.centre.getZ() + dz).color(state.colour);
            buffer.vertex(positionMatrix, (float) state.centre.getX() + dx, (float) state.centre.getY() - halfHeight, (float) state.centre.getZ() + dz).color(state.colour);
        }
    }
}