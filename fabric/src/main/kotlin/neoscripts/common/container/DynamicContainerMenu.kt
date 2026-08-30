package com.nekiplay.neoscripts.common.container

import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * Generic container menu for dynamic blocks.
 * Supports any size 1..54; layout 9 columns, rows = ceil(size/9).
 * https://docs.fabricmc.net/develop/blocks/container-menus
 */
class DynamicContainerMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    val container: Container,
    val rawId: String?
) : AbstractContainerMenu(
    // MenuType resolved via DynamicContainers for server; fallback for client generic
    rawId?.let { DynamicContainers.getMenuType(it) } ?: throw IllegalStateException("MenuType not registered for $rawId"),
    containerId
) {
    private val containerSize = container.containerSize
    private val rows = (containerSize + 8) / 9

    // layout constants similar to generic_54
    private val containerStartX = 8
    private val containerStartY = 18
    private val inventoryStartY = containerStartY + rows * 18 + 14

    init {
        checkContainerSize(container, containerSize)
        container.startOpen(playerInventory.player)

        // container slots: custom positions if provided, else grid 9xN
        val customSlots = rawId?.let { DynamicContainers.getSlots(it) }
        if (customSlots != null && customSlots.size >= containerSize) {
            for (idx in 0 until containerSize) {
                val pos = customSlots[idx]
                addSlot(Slot(container, idx, pos[0], pos[1]))
            }
        } else {
            for (row in 0 until rows) {
                for (col in 0 until 9) {
                    val idx = col + row * 9
                    if (idx >= containerSize) break
                    addSlot(Slot(container, idx, containerStartX + col * 18, containerStartY + row * 18))
                }
            }
        }
        // player inventory 3 rows + hotbar
        addStandardInventorySlots(playerInventory, 8, inventoryStartY)
    }

    private fun addStandardInventorySlots(inv: Inventory, x: Int, y: Int) {
        // 3 rows inventory
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18))
            }
        }
        // hotbar
        for (col in 0 until 9) {
            addSlot(Slot(inv, col, x + col * 18, y + 58))
        }
    }

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val copy = stack.copy()
        val containerEnd = containerSize
        val inventoryStart = containerEnd
        val inventoryEnd = inventoryStart + 36 // 27 + 9

        if (slotIndex < containerEnd) {
            if (!moveItemStackTo(stack, inventoryStart, inventoryEnd, true)) return ItemStack.EMPTY
        } else {
            if (!moveItemStackTo(stack, 0, containerEnd, false)) return ItemStack.EMPTY
        }
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        return copy
    }

    override fun stillValid(player: Player): Boolean = container.stillValid(player)

    override fun removed(player: Player) {
        super.removed(player)
        container.stopOpen(player)
    }

    companion object {
        fun create(containerId: Int, inv: Inventory, container: Container, rawId: String): DynamicContainerMenu {
            return DynamicContainerMenu(containerId, inv, container, rawId)
        }

        // Client side factory for MenuType registration: uses SimpleContainer with size from registry
        fun clientFactory(rawId: String): (Int, Inventory) -> DynamicContainerMenu {
            return { syncId, inv ->
                val size = DynamicContainers.getSize(rawId) ?: 27
                DynamicContainerMenu(syncId, inv, SimpleContainer(size), rawId)
            }
        }
    }
}
