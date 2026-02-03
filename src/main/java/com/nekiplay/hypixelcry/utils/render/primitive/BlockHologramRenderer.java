package com.nekiplay.hypixelcry.utils.render.primitive;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nekiplay.hypixelcry.utils.render.MatrixHelper;
import com.nekiplay.hypixelcry.utils.render.RenderHelper;
import com.nekiplay.hypixelcry.utils.render.Renderer;
import com.nekiplay.hypixelcry.utils.render.state.BlockHologramRenderState;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;

import net.fabricmc.fabric.api.renderer.v1.render.RenderLayerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.state.CameraRenderState;

public final class BlockHologramRenderer implements PrimitiveRenderer<BlockHologramRenderState> {
    protected static final BlockHologramRenderer INSTANCE = new BlockHologramRenderer();
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final boolean SODIUM_LOADED = FabricLoader.getInstance().isModLoaded("sodium");

    private BlockHologramRenderer() {}

    @Override
    public void submitPrimitives(BlockHologramRenderState state, CameraRenderState cameraState) {
        Matrix4f positionMatrix = new Matrix4f()
                .translate((float) (state.pos.getX() - cameraState.pos.x()), (float) (state.pos.getY() - cameraState.pos.y()), (float) (state.pos.getZ() - cameraState.pos.z()));
        PoseStack matrices = MatrixHelper.toStack(positionMatrix);
        BlockStateModel model = CLIENT.getBlockRenderer().getBlockModel(state.state);

        MultiBufferSource bufferSource = _type -> Renderer.getBuffer(RenderPipelines.TRANSLUCENT_MOVING_BLOCK, TextureSetup.singleTextureWithLightmap(CLIENT.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView(), RenderTypes.MOVING_BLOCK_SAMPLER.get()), true);
        CLIENT.getBlockRenderer().getModelRenderer().render(CLIENT.level, model, state.state, state.pos, matrices, RenderLayerHelper.movingDelegate(bufferSource), true, state.state.getSeed(state.pos), 0);
    }
}