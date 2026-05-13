package com.nekiplay.neoscripts.utils.render.primitive;

import com.mojang.blaze3d.vertex.*;
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
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import org.joml.Matrix4f;

public final class BlockRenderer implements PrimitiveRenderer<BlockHologramRenderState> {
    protected static final BlockRenderer INSTANCE = new BlockRenderer();
    private static final Minecraft CLIENT = Minecraft.getInstance();

    private BlockRenderer() {}

    @Override
    public void submitPrimitives(BlockHologramRenderState state, CameraRenderState cameraState) {

    }
}