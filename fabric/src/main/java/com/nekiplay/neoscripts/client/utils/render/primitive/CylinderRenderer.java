package com.nekiplay.neoscripts.client.utils.render.primitive;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nekiplay.neoscripts.client.utils.render.Renderer;
import com.nekiplay.neoscripts.client.utils.render.SkyblockerRenderPipelines;
import com.nekiplay.neoscripts.client.utils.render.state.CylinderRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

public final class CylinderRenderer implements PrimitiveRenderer<CylinderRenderState> {
    protected static final CylinderRenderer INSTANCE = new CylinderRenderer();

    private CylinderRenderer() {}

    @Override
    public void submitPrimitives(CylinderRenderState state, CameraRenderState cameraState) {
        VertexConsumer buffer = Renderer.getBuffer(state.walls() ? SkyblockerRenderPipelines.CYLINDER_THROUGH_WALLS : SkyblockerRenderPipelines.CYLINDER);
        Matrix4f positionMatrix = new Matrix4f()
                .translate((float) -cameraState.pos.x, (float) -cameraState.pos.y, (float) -cameraState.pos.z);

        float halfHeight = state.height() / 2.0f;

        for (int i = 0; i <= state.segments(); i++) {
            double angle = Math.TAU * i / state.segments();
            float dx = (float) Math.cos(angle) * state.radius();
            float dz = (float) Math.sin(angle) * state.radius();

            buffer.addVertex(positionMatrix, (float) state.centre().x() + dx, (float) state.centre().y() + halfHeight, (float) state.centre().z() + dz).setColor(state.colour());
            buffer.addVertex(positionMatrix, (float) state.centre().x() + dx, (float) state.centre().y() - halfHeight, (float) state.centre().z() + dz).setColor(state.colour());
        }
    }
}