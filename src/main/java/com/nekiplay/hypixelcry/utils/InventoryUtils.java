package com.nekiplay.hypixelcry.utils;

import net.minecraft.screen.slot.SlotActionType;

import static com.nekiplay.hypixelcry.HypixelCry.mc;

public class InventoryUtils {
    public static void clickSlotWithId(int slotId, int button, SlotActionType actionType, int syncId) {
        if (mc.interactionManager != null && mc.player != null) {
            mc.interactionManager.clickSlot(syncId, slotId, button, actionType, mc.player);
        }
    }

    public static void clickContainerSlot(int slot, int button, SlotActionType actionType) {
        if (mc.player != null && mc.player.currentScreenHandler != null) {
            clickSlotWithId(slot, button, actionType, mc.player.currentScreenHandler.syncId);
        }
    }

    public static void clickSlot(int slot, int button, SlotActionType actionType) {
        if (mc.player != null && mc.player.playerScreenHandler != null) {
            clickSlotWithId(slot, button, actionType, mc.player.playerScreenHandler.syncId);
        }
    }

    public static void swapSlots(int slot, int hotbarSlot) {
        if (mc.player != null && mc.player.playerScreenHandler != null) {
            clickSlotWithId(slot, hotbarSlot, SlotActionType.SWAP, mc.player.playerScreenHandler.syncId);
        }
    }

    // Дополнительные полезные методы
    public static void leftClickSlot(int slot) {
        clickSlot(slot, 0, SlotActionType.PICKUP);
    }

    public static void rightClickSlot(int slot) {
        clickSlot(slot, 1, SlotActionType.PICKUP);
    }

    public static void dropSlot(int slot) {
        clickSlot(slot, 0, SlotActionType.THROW);
    }

    public static void dropAllFromSlot(int slot) {
        clickSlot(slot, 1, SlotActionType.THROW);
    }
}
