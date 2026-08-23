package com.nekiplay.neoscripts.common.mixins.minecraft;

import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(LevelRenderState.class)
public interface LevelRenderStateAccessor {
    @Accessor("blockBreakingRenderStates")
    List<BlockBreakingRenderState> neoscripts$getBlockBreakingRenderStates();
}