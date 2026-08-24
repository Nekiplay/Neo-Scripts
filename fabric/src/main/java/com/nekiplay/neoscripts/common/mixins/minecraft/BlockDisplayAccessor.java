package com.nekiplay.neoscripts.common.mixins.minecraft;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayAccessor {
    @Invoker("getBlockState")
    BlockState nsGetBlockState();

    @Invoker("setBlockState")
    void nsSetBlockState(BlockState state);
}
