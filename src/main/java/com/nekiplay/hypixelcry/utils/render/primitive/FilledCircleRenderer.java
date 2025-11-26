package com.nekiplay.hypixelcry.utils.render.primitive;

import com.nekiplay.hypixelcry.utils.render.Renderer;
import com.nekiplay.hypixelcry.utils.render.SkyblockerRenderPipelines;
import com.nekiplay.hypixelcry.utils.render.state.FilledCircleRenderState;
import org.joml.Matrix4f;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.state.CameraRenderState;

public final class FilledCircleRenderer implements PrimitiveRenderer<FilledCircleRenderState> {
    protected static final FilledCircleRenderer INSTANCE = new FilledCircleRenderer();

    private FilledCircleRenderer() {}

    @Override
    public void submitPrimitives(FilledCircleRenderState state, CameraRenderState cameraState) {
        BufferBuilder buffer = Renderer.getBuffer(state.throughWalls ? SkyblockerRenderPipelines.CIRCLE_THROUGH_WALLS : SkyblockerRenderPipelines.CIRCLE);
        Matrix4f positionMatrix = new Matrix4f()
                .translate((float) -cameraState.pos.x, (float) -cameraState.pos.y, (float) -cameraState.pos.z);

        for (int i = 0; i <= state.segments; i++) {
            double angle = Math.TAU * i / state.segments;
            float dx = (float) Math.cos(angle) * state.radius;
            float dz = (float) Math.sin(angle) * state.radius;

            buffer.vertex(positionMatrix, (float) state.centre.getX() + dx, (float) state.centre.getY(), (float) state.centre.getZ() + dz).color(state.colour);
        }
    }
}