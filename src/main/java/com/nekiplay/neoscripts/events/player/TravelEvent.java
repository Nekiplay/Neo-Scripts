package com.nekiplay.neoscripts.events.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

public class TravelEvent {
    public static final Event<TravelEventCallback> EVENT = EventFactory.createArrayBacked(
            TravelEventCallback.class,
            (listeners) -> (event) -> {
                for (TravelEventCallback listener : listeners) {
                    InteractionResult result = listener.update(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    public boolean traveled;

    public TravelEvent(boolean traveled) {
        this.traveled = traveled;
    }

    public interface TravelEventCallback {
        InteractionResult update(TravelEvent event);
    }
}