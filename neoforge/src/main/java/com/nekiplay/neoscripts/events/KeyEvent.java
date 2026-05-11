package com.nekiplay.neoscripts.events;

import com.nekiplay.neoscripts.utils.misc.input.KeyAction;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.ICancellableEvent;

public class KeyEvent extends net.neoforged.bus.api.Event implements ICancellableEvent {
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
