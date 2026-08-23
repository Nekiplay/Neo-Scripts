package com.nekiplay.neoscripts.client.utils.render.primitive;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nekiplay.neoscripts.client.utils.render.Renderer;
import com.nekiplay.neoscripts.client.utils.render.SkyblockerRenderPipelines;
import com.nekiplay.neoscripts.client.utils.render.state.FilledCircleRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

public final class FilledCircleRenderer implements PrimitiveRenderer<FilledCircleRenderState> {
    protected static final FilledCircleRenderer INSTANCE = new FilledCircleRenderer();

    private FilledCircleRenderer() {}

    @Override
    public void submitPrimitives(FilledCircleRenderState state, CameraRenderState cameraState) {
        VertexConsumer buffer = Renderer.getBuffer(state.walls() ? SkyblockerRenderPipelines.CIRCLE_THROUGH_WALLS : SkyblockerRenderPipelines.CIRCLE);
        Matrix4f positionMatrix = new Matrix4f()
                .translate((float) -cameraState.pos.x, (float) -cameraState.pos.y, (float) -cameraState.pos.z);

        for (int i = 0; i <= state.segments(); i++) {
            double angle = Math.TAU * i / state.segments();
            float dx = (float) Math.cos(angle) * state.radius();
            float dz = (float) Math.sin(angle) * state.radius();

            buffer.addVertex(positionMatrix, (float) state.centre().x() + dx, (float) state.centre().y(), (float) state.centre().z() + dz).setColor(state.colour());
        }
    }
}