package com.nekiplay.neoscripts.events.player;

import com.nekiplay.neoscripts.utils.DirectionalInput;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;

public class MovementInputEvent {
    public static final Event<MovementInput> EVENT = EventFactory.createArrayBacked(
            MovementInput.class,
            (listeners) -> (event) -> {
                for (MovementInput listener : listeners) {
                    InteractionResult result = listener.update(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    public DirectionalInput directionalInput;
    public boolean jump;
    public boolean shift;

    public MovementInputEvent(DirectionalInput directionalInput, boolean jump, boolean shift) {
        this.directionalInput = directionalInput;
        this.jump = jump;
        this.shift = shift;
    }

    public interface MovementInput {
        InteractionResult update(MovementInputEvent event);
    }
}