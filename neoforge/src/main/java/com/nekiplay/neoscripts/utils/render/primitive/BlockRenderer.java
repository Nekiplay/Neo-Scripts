package com.nekiplay.neoscripts.utils.render.primitive;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nekiplay.neoscripts.utils.render.MatrixHelper;
import com.nekiplay.neoscripts.utils.render.Renderer;
import com.nekiplay.neoscripts.utils.render.state.BlockHologramRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;

public final class BlockRenderer implements PrimitiveRenderer<BlockHologramRenderState> {
    protected static final BlockRenderer INSTANCE = new BlockRenderer();
    private static final Minecraft CLIENT = Minecraft.getInstance();

    private BlockRenderer() {}

    @Override
    public void submitPrimitives(BlockHologramRenderState state, CameraRenderState cameraState) {
        Matrix4f positionMatrix = new Matrix4f()
                .translate((float) (state.pos.getX() - cameraState.pos.x()), (float) (state.pos.getY() - cameraState.pos.y()), (float) (state.pos.getZ() - cameraState.pos.z()));
        PoseStack matrices = MatrixHelper.toStack(positionMatrix);
        BlockStateModel model = CLIENT.getBlockRenderer().getBlockModel(state.state);

        MultiBufferSource bufferSource = _type -> Renderer.getBuffer(RenderPipelines.SOLID_BLOCK, TextureSetup.singleTextureWithLightmap(CLIENT.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView(), RenderTypes.MOVING_BLOCK_SAMPLER.get()), true);
        //CLIENT.getBlockRenderer().getModelRenderer().renderModel(CLIENT.level, model, state.state, state.pos, matrices, RenderLayerHelper.movingDelegate(bufferSource), true, state.state.getSeed(state.pos), 0);
    }
}