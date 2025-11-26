package com.nekiplay.hypixelcry.events;

import com.nekiplay.hypixelcry.utils.misc.input.KeyAction;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;

public class KeyEvent {
    public static final Event<KeyCallback> EVENT = EventFactory.createArrayBacked(
            KeyCallback.class,
            (listeners) -> (event) -> {
                for (KeyCallback listener : listeners) {
                    InteractionResult result = listener.onKeyEvent(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    private int key;
    private int modifiers;
    private KeyAction action;

    public KeyEvent(int key, int modifiers, KeyAction action) {
        this.key = key;
        this.modifiers = modifiers;
        this.action = action;
    }

    public int getKey() {
        return key;
    }

    public int getModifiers() {
        return modifiers;
    }

    public KeyAction getAction() {
        return action;
    }

    public interface KeyCallback {
        InteractionResult onKeyEvent(KeyEvent event);
    }
}
