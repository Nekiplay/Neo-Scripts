package com.nekiplay.neoscripts.utils.render.primitive;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.nekiplay.neoscripts.utils.render.Renderer;
import com.nekiplay.neoscripts.utils.render.SkyblockerRenderPipelines;
import com.nekiplay.neoscripts.utils.render.state.OutlinedBoxRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.joml.Matrix4f;

public final class OutlinedBoxRenderer implements PrimitiveRenderer<OutlinedBoxRenderState> {
    protected static final OutlinedBoxRenderer INSTANCE = new OutlinedBoxRenderer();

    private OutlinedBoxRenderer() {}

    @Override
    public void submitPrimitives(OutlinedBoxRenderState state, CameraRenderState cameraState) {
        BufferBuilder buffer = Renderer.getBuffer(state.throughWalls ? SkyblockerRenderPipelines.LINES_THROUGH_WALLS : RenderPipelines.LINES);
        Matrix4f positionMatrix = new Matrix4f()
                .translate((float) -cameraState.pos.x, (float) -cameraState.pos.y, (float) -cameraState.pos.z);
        float minX = (float) state.minX;
        float minY = (float) state.minY;
        float minZ = (float) state.minZ;
        float maxX = (float) state.maxX;
        float maxY = (float) state.maxY;
        float maxZ = (float) state.maxZ;
        float red = state.colourComponents[0];
        float green = state.colourComponents[1];
        float blue = state.colourComponents[2];
        float alpha = state.alpha;

        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha).setNormal(1.0f, 0.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha).setNormal(1.0f, 0.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha).setNormal(0.0f, 1.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(0.0f, 1.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha).setNormal(0.0f, 0.0f, 1.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(0.0f, 0.0f, 1.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha).setNormal(0.0f, 1.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(0.0f, 1.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(-1.0f, 0.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(-1.0f, 0.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(0.0f, 0.0f, 1.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(0.0f, 0.0f, 1.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(0.0f, -1.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(0.0f, -1.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(1.0f, 0.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(1.0f, 0.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(0.0f, 0.0f, -1.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha).setNormal(0.0f, 0.0f, -1.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(1.0f, 0.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(1.0f, 0.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(0.0f, 1.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(0.0f, 1.0f, 0.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(0.0f, 0.0f, 1.0f).setLineWidth(state.lineWidth);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(0.0f, 0.0f, 1.0f).setLineWidth(state.lineWidth);
    }
}