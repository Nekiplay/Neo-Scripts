package com.nekiplay.neoscripts.mixins.minecraft;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface GamemodeAccessor {
    @Accessor("destroyProgress")
    float neoscripts$getBreakingProgress();

    @Accessor("destroyProgress")
    void neoscripts$setCurrentBreakingProgress(float progress);

    @Accessor("destroyBlockPos")
    BlockPos neoscripts$getCurrentBreakingBlockPos();
}
