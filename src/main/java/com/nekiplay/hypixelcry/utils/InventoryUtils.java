package com.nekiplay.hypixelcry.utils;

import static com.nekiplay.hypixelcry.HypixelCry.mc;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;

public class InventoryUtils {
    public static void clickSlotWithId(int slotId, int button, ClickType actionType, int syncId) {
        if (mc.gameMode != null && mc.player != null) {
            mc.gameMode.handleInventoryMouseClick(syncId, slotId, button, actionType, mc.player);
        }
    }

    public static void clickSlot(int slot, int button, ClickType actionType) {
        if (mc.player != null && mc.screen != null) {
            if (mc.player.containerMenu instanceof ChestMenu) {
                clickSlotWithId(slot, button, actionType, mc.player.containerMenu.containerId);
            }
            else if (mc.screen instanceof InventoryScreen) {
                clickSlotWithId(slot, button, actionType, mc.player.inventoryMenu.containerId);
            }
        }
    }

    // Дополнительные полезные методы
    public static void leftClickSlot(int slot) {
        clickSlot(slot, 0, ClickType.PICKUP);
    }

    public static void rightClickSlot(int slot) {
        clickSlot(slot, 1, ClickType.PICKUP);
    }

    public static void shiftLeftClickSlot(int slot) {
        clickSlot(slot, 0, ClickType.PICKUP_ALL);
    }

    public static void shiftRightClickSlot(int slot) {
        clickSlot(slot, 1, ClickType.PICKUP_ALL);
    }

    public static void middleClickSlot(int slot) {
        clickSlot(slot, 2, ClickType.CLONE);
    }

    public static void dropSlot(int slot) {
        clickSlot(slot, 0, ClickType.THROW);
    }

    public static void dropAllFromSlot(int slot) {
        clickSlot(slot, 1, ClickType.THROW);
    }
}
