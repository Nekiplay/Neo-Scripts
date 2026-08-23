package com.nekiplay.neoscripts.client.events.world;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ClientChunkLoadEvent {
    public static final Event<ClientChunkLoadCallback> EVENT = EventFactory.createArrayBacked(
            ClientChunkLoadCallback.class,
            (listeners) -> (world, chunk) -> {
                for (ClientChunkLoadCallback listener : listeners) {
                    listener.onChunkLoad(world, chunk);
                }
            }
    );

    public interface ClientChunkLoadCallback {
        void onChunkLoad(ClientLevel world, ChunkAccess chunk);
    }
}
