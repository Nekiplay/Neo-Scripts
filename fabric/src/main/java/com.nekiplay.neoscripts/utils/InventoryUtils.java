package com.nekiplay.neoscripts.utils;

import static com.nekiplay.neoscripts.Main.mc;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

public class InventoryUtils {
    public static void clickSlotWithId(int slotId, int button, ContainerInput actionType, int syncId) {
        if (mc.gameMode != null && mc.player != null) {
            mc.gameMode.handleContainerInput(syncId, slotId, button, actionType, mc.player);
        }
    }

    public static void clickSlot(int slot, int button, ContainerInput actionType) {
        if (mc.player != null && mc.screen != null) {
            if (mc.player.containerMenu instanceof AbstractContainerMenu) {
                clickSlotWithId(slot, button, actionType, mc.player.containerMenu.containerId);
            }
            else if (mc.screen instanceof InventoryScreen) {
                clickSlotWithId(slot, button, actionType, mc.player.inventoryMenu.containerId);
            }
        }
    }

    // Дополнительные полезные методы
    public static void leftClickSlot(int slot) {
        clickSlot(slot, 0, ContainerInput.PICKUP);
    }

    public static void rightClickSlot(int slot) {
        clickSlot(slot, 1, ContainerInput.PICKUP);
    }

    public static void shiftLeftClickSlot(int slot) {
        clickSlot(slot, 0, ContainerInput.QUICK_MOVE);
    }

    public static void shiftRightClickSlot(int slot) {
        clickSlot(slot, 1, ContainerInput.QUICK_MOVE);
    }

    public static void middleClickSlot(int slot) {
        clickSlot(slot, 2, ContainerInput.CLONE);
    }

    public static void dropSlot(int slot) {
        clickSlot(slot, 0, ContainerInput.THROW);
    }

    public static void dropAllFromSlot(int slot) {
        clickSlot(slot, 1, ContainerInput.THROW);
    }
}
