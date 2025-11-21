package com.nekiplay.hypixelcry.utils.render.primitive;


import com.nekiplay.hypixelcry.utils.render.Renderer;
import com.nekiplay.hypixelcry.utils.render.SkyblockerRenderPipelines;
import com.nekiplay.hypixelcry.utils.render.state.QuadRenderState;
import org.joml.Matrix4f;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.state.CameraRenderState;

public final class QuadRenderer implements PrimitiveRenderer<QuadRenderState> {
    protected static final QuadRenderer INSTANCE = new QuadRenderer();

    private QuadRenderer() {}

    @Override
    public void submitPrimitives(QuadRenderState state, CameraRenderState cameraState) {
        BufferBuilder buffer = Renderer.getBuffer(state.throughWalls ? SkyblockerRenderPipelines.QUADS_THROUGH_WALLS : RenderPipelines.DEBUG_QUADS);
        Matrix4f positionMatrix = new Matrix4f()
                .translate((float) -cameraState.pos.x, (float) -cameraState.pos.y, (float) -cameraState.pos.z);

        for (int i = 0; i < 4; i++) {
            buffer.vertex(positionMatrix, (float) state.points[i].getX(), (float) state.points[i].getY(), (float) state.points[i].getZ())
                    .color(state.colourComponents[0], state.colourComponents[1], state.colourComponents[2], state.alpha);
        }
    }
}