package com.nekiplay.neoscripts.utils.render.primitive;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nekiplay.neoscripts.utils.render.MatrixHelper;
import com.nekiplay.neoscripts.utils.render.Renderer;
import com.nekiplay.neoscripts.utils.render.state.BlockHologramRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public final class BlockRenderer implements PrimitiveRenderer<BlockHologramRenderState> {
    protected static final BlockRenderer INSTANCE = new BlockRenderer();
    private static final Minecraft CLIENT = Minecraft.getInstance();

    private BlockRenderer() {}

    @Override
    public void submitPrimitives(BlockHologramRenderState state, CameraRenderState cameraState) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(
                state.pos.getX() - cameraState.pos.x(),
                state.pos.getY() - cameraState.pos.y(),
                state.pos.getZ() - cameraState.pos.z()
        );

        BlockState blockState = state.state;
        BlockRenderDispatcher dispatcher = CLIENT.getBlockRenderer();
        BlockStateModel model = dispatcher.getBlockModel(blockState);

        // Создаём ByteBufferBuilder и оборачиваем в MultiBufferSource
        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(786432);
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(byteBufferBuilder);
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.solidMovingBlock());

        // Используем deprecated метод renderModel, который принимает VertexConsumer
        ModelBlockRenderer.renderModel(
                poseStack.last(),
                consumer,
                model,
                1.0F, 1.0F, 1.0F,
                0x00F000F0,
                OverlayTexture.NO_OVERLAY
        );

        bufferSource.endBatch();
    }
}