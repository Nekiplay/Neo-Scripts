package com.nekiplay.neoscripts.client.events.world;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;

public class BlockUpdateEvent {
    public static final Event<BlockUpdateCallback> EVENT = EventFactory.createArrayBacked(
            BlockUpdateCallback.class,
            (listeners) -> (event) -> {
                for (BlockUpdateCallback listener : listeners) {
                    InteractionResult result = listener.update(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    private final BlockPos pos;
    private final BlockState old;
    private final BlockState current;

    public BlockUpdateEvent(BlockPos pos, BlockState old, BlockState current) {
        this.pos = pos;
        this.old = old;
        this.current = current;
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public BlockState getOld() {
        return old;
    }

    public BlockState getNew() {
        return current;
    }

    public interface BlockUpdateCallback {
        InteractionResult update(BlockUpdateEvent event);
    }
}