package com.nekiplay.neoscripts.common.container

import com.nekiplay.neoscripts.common.features.lua.objects.misc.DynamicContent
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.core.HolderLookup
import net.minecraft.network.chat.Component
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.MenuProvider

class DynamicContainerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(
    // type resolved lazily via registry lookup; fallback to first container type if not yet registered (should be registered before block placement)
    DynamicContainers.getBlockEntityType(state.block) ?: DynamicContainers.fallbackType(pos, state),
    pos, state
), ImplementedContainer, MenuProvider {

    private val _items: NonNullList<ItemStack> by lazy {
        val rawId = DynamicContainers.getRawId(blockState.block) ?: ""
        val size = DynamicContainers.getSize(rawId) ?: 27
        NonNullList.withSize(size, ItemStack.EMPTY)
    }

    override fun getItems(): NonNullList<ItemStack> = _items

    override fun stillValid(player: Player): Boolean {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player)
    }

    override fun getDisplayName(): Component {
        val rawId = DynamicContainers.getRawId(blockState.block) ?: ""
        val title = DynamicContainers.getTitle(rawId) ?: rawId
        return Component.literal(title)
    }

    override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu {
        val rawId = DynamicContainers.getRawId(blockState.block) ?: ""
        return DynamicContainerMenu.create(containerId, inventory, this, rawId)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        ContainerHelper.loadAllItems(input, _items)
    }

    override fun saveAdditional(output: ValueOutput) {
        ContainerHelper.saveAllItems(output, _items)
        super.saveAdditional(output)
    }

    override fun preRemoveSideEffects(pos: BlockPos, newState: net.minecraft.world.level.block.state.BlockState) {
        val lvl = level
        if (lvl != null && !lvl.isClientSide) {
            net.minecraft.world.Containers.dropContents(lvl, pos, this)
        }
        super.preRemoveSideEffects(pos, newState)
    }
}
