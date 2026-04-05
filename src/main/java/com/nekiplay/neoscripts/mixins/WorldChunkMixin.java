package com.nekiplay.neoscripts.mixins;

import com.nekiplay.neoscripts.events.world.BlockUpdateEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class WorldChunkMixin {
    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
    private void onSetBlockState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        InteractionResult result = BlockUpdateEvent.EVENT.invoker().update(new BlockUpdateEvent(pos, cir.getReturnValue(), state));

        if (result == InteractionResult.FAIL) {
            cir.setReturnValue(cir.getReturnValue());
            cir.cancel();
        }
    }
}