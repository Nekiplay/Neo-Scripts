package com.nekiplay.neoscripts.events.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

public class AddItemInventoryEvent {
    public static final Event<ItemUpdateCallback> EVENT = EventFactory.createArrayBacked(
            ItemUpdateCallback.class,
            (listeners) -> (event) -> {
                for (ItemUpdateCallback listener : listeners) {
                    InteractionResult result = listener.update(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    private int slot = 0;
    private ItemStack item = ItemStack.EMPTY;

    public AddItemInventoryEvent(int slot, ItemStack item) {
        this.slot = slot;
        this.item = item;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItem() {
        return item;
    }


    public interface ItemUpdateCallback {
        InteractionResult update(AddItemInventoryEvent event);
    }
}