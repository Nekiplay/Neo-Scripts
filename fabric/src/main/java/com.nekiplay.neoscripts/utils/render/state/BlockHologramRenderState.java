package com.nekiplay.neoscripts.utils.render.state;

import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record BlockHologramRenderState(BlockPos pos, BlockState state, float alpha) {
}