package com.nekiplay.hypixelcry.utils;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.slot.SlotActionType;

import static com.nekiplay.hypixelcry.HypixelCry.mc;

public class InventoryUtils {
    public static void clickSlotWithId(int slotId, int button, SlotActionType actionType, int syncId) {
        if (mc.interactionManager != null && mc.player != null) {
            mc.interactionManager.clickSlot(syncId, slotId, button, actionType, mc.player);
        }
    }

    public static void clickSlot(int slot, int button, SlotActionType actionType) {
        if (mc.player != null && mc.currentScreen != null) {
            if (mc.currentScreen instanceof GenericContainerScreen) {
                clickSlotWithId(slot, button, actionType, (((GenericContainerScreen)mc.currentScreen).getScreenHandler().syncId));
            }
            else if (mc.currentScreen instanceof InventoryScreen) {
                clickSlotWithId(slot, button, actionType, mc.player.playerScreenHandler.syncId);
            }
        }
    }

    public static void swapSlots(int slot, int hotbarSlot) {
        if (mc.player != null && mc.currentScreen != null) {
            if (mc.currentScreen instanceof GenericContainerScreen) {
                clickSlotWithId(slot, hotbarSlot, SlotActionType.SWAP, (((GenericContainerScreen)mc.currentScreen).getScreenHandler().syncId));
            }
            else if (mc.currentScreen instanceof InventoryScreen) {
                clickSlotWithId(slot, hotbarSlot, SlotActionType.SWAP, mc.player.playerScreenHandler.syncId);
            }
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
