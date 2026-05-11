package com.nekiplay.neoscripts.utils.render.primitive;


import com.mojang.blaze3d.vertex.BufferBuilder;
import com.nekiplay.neoscripts.utils.render.Renderer;
import com.nekiplay.neoscripts.utils.render.SkyblockerRenderPipelines;
import com.nekiplay.neoscripts.utils.render.state.QuadRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.joml.Matrix4f;

public final class QuadRenderer implements PrimitiveRenderer<QuadRenderState> {
    protected static final QuadRenderer INSTANCE = new QuadRenderer();

    private QuadRenderer() {}

    @Override
    public void submitPrimitives(QuadRenderState state, CameraRenderState cameraState) {
        BufferBuilder buffer = Renderer.getBuffer(state.throughWalls ? SkyblockerRenderPipelines.QUADS_THROUGH_WALLS : RenderPipelines.DEBUG_QUADS);
        Matrix4f positionMatrix = new Matrix4f()
                .translate((float) -cameraState.pos.x, (float) -cameraState.pos.y, (float) -cameraState.pos.z);

        for (int i = 0; i < 4; i++) {
            buffer.addVertex(positionMatrix, (float) state.points[i].x(), (float) state.points[i].y(), (float) state.points[i].z())
                    .setColor(state.colourComponents[0], state.colourComponents[1], state.colourComponents[2], state.alpha);
        }
    }
}