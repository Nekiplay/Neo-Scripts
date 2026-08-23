package com.nekiplay.neoscripts.client.events;

import com.nekiplay.neoscripts.client.utils.misc.input.KeyAction;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;

public class MouseButtonEvent {
    public static final Event<MouseButtonEvent.KeyCallback> EVENT = EventFactory.createArrayBacked(
            MouseButtonEvent.KeyCallback.class,
            (listeners) -> (event) -> {
                for (MouseButtonEvent.KeyCallback listener : listeners) {
                    InteractionResult result = listener.onKeyEvent(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    private final int button;
    private final KeyAction action;

    public MouseButtonEvent(int button, KeyAction action) {
        this.button = button;
        this.action = action;
    }

    public int getButton() {
        return button;
    }

    public KeyAction getAction() {
        return action;
    }

    public interface KeyCallback {
        InteractionResult onKeyEvent(MouseButtonEvent event);
    }
}
