package com.nekiplay.neoscripts.events.player;

import net.minecraft.world.item.ItemStack;

public class AddItemInventoryEvent extends net.neoforged.bus.api.Event {
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
}