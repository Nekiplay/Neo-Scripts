package com.nekiplay.neoscripts.common.container

import net.minecraft.core.NonNullList
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * Helper от Fabric docs https://docs.fabricmc.net/develop/blocks/block-containers
 */
interface ImplementedContainer : Container {
    fun getItems(): NonNullList<ItemStack>

    override fun getContainerSize(): Int = getItems().size

    override fun isEmpty(): Boolean {
        for (i in 0 until containerSize) if (!getItem(i).isEmpty) return false
        return true
    }

    override fun getItem(slot: Int): ItemStack = getItems()[slot]

    override fun removeItem(slot: Int, count: Int): ItemStack {
        val result = ContainerHelper.removeItem(getItems(), slot, count)
        if (!result.isEmpty) setChanged()
        return result
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(getItems(), slot)

    override fun setItem(slot: Int, stack: ItemStack) {
        getItems()[slot] = stack
        stack.limitSize(getMaxStackSize(stack))
        setChanged()
    }

    override fun clearContent() = getItems().clear()

    override fun stillValid(player: Player): Boolean = true

    companion object {
        fun of(items: NonNullList<ItemStack>): ImplementedContainer = object : ImplementedContainer {
            override fun getItems(): NonNullList<ItemStack> = items
            override fun setChanged() {}
            override fun stillValid(player: Player): Boolean = true
        }
        fun ofSize(size: Int): ImplementedContainer = of(NonNullList.withSize(size, ItemStack.EMPTY))
    }
}
