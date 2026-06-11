package com.nekiplay.neoscripts.utils.render.state;

import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockHologramRenderState {
    public AltModelBlockRenderer altModelBlockRenderer;
    public BlockPos pos;
    public BlockState state;
    public float alpha;
}