package com.nekiplay.neoscripts.common.workstation

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level

/**
 * Workstation menu (crafting table-like) 3x3 + result + 36 inventory
 * https://docs.fabricmc.net/develop/blocks/workstations
 */
class DynamicWorkstationMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val access: ContainerLevelAccess,
    val rawId: String?
) : AbstractContainerMenu(
    rawId?.let { DynamicWorkstations.getMenuType(it) } ?: throw IllegalStateException("MenuType not registered for $rawId"),
    containerId
) {
    val isFurnace: Boolean = run {
        val t = rawId?.let { DynamicWorkstations.getType(it) }?.lowercase() ?: "crafting"
        t == "furnace" || t == "smelting" || t == "blasting" || t == "smoker"
    }
    val gridSize: Int = run {
        if (isFurnace) return@run 0
        val t = rawId?.let { DynamicWorkstations.getType(it) }?.lowercase() ?: "crafting"
        when {
            t.contains("5x5") || t.contains("5") || t == "large" -> 5
            else -> 3
        }
    }
    val gridSlots = if (isFurnace) 1 else gridSize * gridSize
    // crafting input (TransientCraftingContainer calls slotsChanged)
    val craftSlots: CraftingContainer = TransientCraftingContainer(this, if (isFurnace) 1 else gridSize, if (isFurnace) 1 else gridSize)
    val resultSlots: ResultContainer = ResultContainer()
    // for furnace, extra fuel slot
    val fuelSlots: Container = SimpleContainer(1)

    init {
        if (isFurnace) {
            val customSlots = rawId?.let { DynamicWorkstations.getSlots(it) }
            if (customSlots != null && customSlots.size >= 3) {
                addSlot(Slot(craftSlots, 0, customSlots[0][0], customSlots[0][1]))
                addSlot(Slot(fuelSlots, 0, customSlots[1][0], customSlots[1][1]))
                addSlot(FurnaceResultSlot(playerInventory.player, craftSlots, fuelSlots, resultSlots, 0, customSlots[2][0], customSlots[2][1]))
            } else {
                addSlot(Slot(craftSlots, 0, 56, 17))
                addSlot(Slot(fuelSlots, 0, 56, 53))
                addSlot(FurnaceResultSlot(playerInventory.player, craftSlots, fuelSlots, resultSlots, 0, 116, 35))
            }
            addStandardInventorySlots(playerInventory, 8, 84)
        } else {
            val customSlots = rawId?.let { DynamicWorkstations.getSlots(it) }
        val isCustom = customSlots != null && customSlots.size >= gridSlots + 1
        if (isCustom) {
            for (i in 0 until gridSlots) {
                val pos = customSlots!![i]
                addSlot(Slot(craftSlots, i, pos[0], pos[1]))
            }
            val rPos = customSlots[gridSlots]
            addSlot(ResultSlot(playerInventory.player, craftSlots, resultSlots, 0, rPos[0], rPos[1]))
            val invY = if (gridSize == 5) 120 else 84
            addStandardInventorySlots(playerInventory, 8, invY)
        } else if (gridSize == 5) {
            // 5x5 default at 8,18, result at 134,45
            for (row in 0 until 5) {
                for (col in 0 until 5) {
                    val idx = col + row * 5
                    addSlot(Slot(craftSlots, idx, 8 + col * 18, 18 + row * 18))
                }
            }
            addSlot(ResultSlot(playerInventory.player, craftSlots, resultSlots, 0, 134, 45))
            addStandardInventorySlots(playerInventory, 8, 120)
        } else {
            // 3x3 default at 30,17 result 124,35
            addSlot(ResultSlot(playerInventory.player, craftSlots, resultSlots, 0, 124, 35))
            for (row in 0 until 3) {
                for (col in 0 until 3) {
                    val idx = col + row * 3
                    addSlot(Slot(craftSlots, idx, 30 + col * 18, 17 + row * 18))
                }
            }
            addStandardInventorySlots(playerInventory, 8, 84)
        }
        }
    }

    private fun addStandardInventorySlots(inv: Inventory, x: Int, y: Int) {
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18))
            }
        }
        for (col in 0 until 9) {
            addSlot(Slot(inv, col, x + col * 18, y + 58))
        }
    }

    // Called when craftSlots changes
    override fun slotsChanged(container: Container) {
        super.slotsChanged(container)
        access.execute { level, _ ->
            if (level !is ServerLevel) return@execute
            if (isFurnace && (container === craftSlots || container === fuelSlots)) {
                val inputStack = craftSlots.getItem(0)
                if (inputStack.isEmpty) {
                    resultSlots.setItem(0, ItemStack.EMPTY)
                    setRemoteSlot(2, ItemStack.EMPTY)
                    if (playerInventory.player is ServerPlayer) {
                        (playerInventory.player as ServerPlayer).connection.send(
                            net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 2, ItemStack.EMPTY)
                        )
                    }
                    return@execute
                }
                val input = net.minecraft.world.item.crafting.SingleRecipeInput(inputStack)
                val recipeOpt = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level)
                var result = ItemStack.EMPTY
                if (recipeOpt.isPresent) {
                    val holder = recipeOpt.get()
                    if (resultSlots.setRecipeUsed(playerInventory.player as ServerPlayer, holder)) {
                        result = holder.value().assemble(input)
                        if (!result.isItemEnabled(level.enabledFeatures())) result = ItemStack.EMPTY
                    }
                } else {
                    resultSlots.setRecipeUsed(null)
                }
                resultSlots.setItem(0, result)
                setRemoteSlot(2, result)
                if (playerInventory.player is ServerPlayer) {
                    (playerInventory.player as ServerPlayer).connection.send(
                        net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 2, result)
                    )
                }
            } else if (!isFurnace && container === craftSlots) {
                val input = CraftingInput.of(gridSize, gridSize, craftSlots.getItems())
                var result = ItemStack.EMPTY
                val vanillaOpt = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                if (vanillaOpt.isPresent) {
                    val holder = vanillaOpt.get()
                    val recipe = holder.value()
                    if (resultSlots.setRecipeUsed(playerInventory.player as ServerPlayer, holder)) {
                        val assembled = recipe.assemble(input)
                        if (assembled.isItemEnabled(level.enabledFeatures())) result = assembled
                    }
                } else {
                    resultSlots.setRecipeUsed(null)
                }
                resultSlots.setItem(0, result)
                setRemoteSlot(0, result)
                if (playerInventory.player is ServerPlayer) {
                    (playerInventory.player as ServerPlayer).connection.send(
                        net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result)
                    )
                }
            }
        }
    }

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val copy = stack.copy()
        if (isFurnace) {
            // furnace: 0=input,1=fuel,2=result, 3..39 inventory
            when (slotIndex) {
                2 -> {
                    if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY
                    slot.onQuickCraft(stack, copy)
                }
                in 0..1 -> {
                    if (!moveItemStackTo(stack, 3, 39, false)) return ItemStack.EMPTY
                }
                in 3 until 39 -> {
                    val input = net.minecraft.world.item.crafting.SingleRecipeInput(stack)
                    var moved = false
                    val level = player.level()
                    if (level is ServerLevel) {
                        val recipeOpt = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level)
                        if (recipeOpt.isPresent) {
                            if (moveItemStackTo(stack, 0, 1, false)) moved = true
                        }
                    }
                    if (!moved) {
                        if (moveItemStackTo(stack, 1, 2, false)) moved = true
                    }
                    if (!moved) {
                        if (slotIndex < 30) {
                            if (!moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY
                        } else if (!moveItemStackTo(stack, 3, 30, false)) return ItemStack.EMPTY
                    }
                }
            }
        } else {
            val resultSlot = 0
            val craftStart = 1
            val craftEnd = craftStart + gridSlots
            val invStart = craftEnd
            val invEnd = invStart + 36
            when (slotIndex) {
                resultSlot -> {
                    stack.item.onCraftedBy(stack, player)
                    if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY
                    slot.onQuickCraft(stack, copy)
                }
                in craftStart until craftEnd -> {
                    if (!moveItemStackTo(stack, invStart, invEnd, false)) return ItemStack.EMPTY
                }
                in invStart until invEnd -> {
                    if (!moveItemStackTo(stack, craftStart, craftEnd, false)) {
                        return ItemStack.EMPTY
                    }
                }
                else -> {
                    if (!moveItemStackTo(stack, invStart, invEnd, false)) return ItemStack.EMPTY
                }
            }
        }
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        if (stack.count == copy.count) return ItemStack.EMPTY
        slot.onTake(player, stack)
        if ((isFurnace && slotIndex == 2) || (!isFurnace && slotIndex == 0)) player.drop(stack, false)
        return copy
    }

    override fun stillValid(player: Player): Boolean {
        // check block still exists and within reach
        val block = rawId?.let { DynamicWorkstations.rawIdToBlock[it] }
        return if (block != null) stillValid(access, player, block) else false
    }

    override fun removed(player: Player) {
        super.removed(player)
        access.execute { _, pos ->
            clearContainer(player, craftSlots)
            if (isFurnace) clearContainer(player, fuelSlots)
        }
    }

    override fun canTakeItemForPickAll(carried: ItemStack, target: Slot): Boolean {
        return target.container !== resultSlots && super.canTakeItemForPickAll(carried, target)
    }

    // Custom ResultSlot to handle onTake
    class ResultSlot(
        private val player: Player?,
        craftSlots: CraftingContainer,
        private val resultSlots: ResultContainer,
        slot: Int, x: Int, y: Int
    ) : Slot(resultSlots, slot, x, y) {
        private val craftSlotsRef = craftSlots
        override fun mayPlace(stack: ItemStack): Boolean = false
        override fun isFake(): Boolean = true
        override fun onTake(player: Player, stack: ItemStack) {
            val menu = player.containerMenu
            if (menu is DynamicWorkstationMenu) {
                stack.onCraftedBy(player, stack.count)
                resultSlots.awardUsedRecipes(player, craftSlotsRef.getItems())
                for (i in 0 until craftSlotsRef.containerSize) {
                    val s = craftSlotsRef.getItem(i)
                    if (!s.isEmpty) {
                        craftSlotsRef.removeItem(i, 1)
                    }
                }
            }
            super.onTake(player, stack)
        }
    }

    class FurnaceResultSlot(
        private val player: Player?,
        private val inputSlots: Container,
        private val fuelSlots: Container,
        private val resultSlots: ResultContainer,
        slot: Int, x: Int, y: Int
    ) : Slot(resultSlots, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = false
        override fun isFake(): Boolean = true
        override fun onTake(player: Player, stack: ItemStack) {
            resultSlots.awardUsedRecipes(player, listOf(inputSlots.getItem(0)))
            inputSlots.removeItem(0, 1)
            super.onTake(player, stack)
        }
    }

    companion object {
        fun create(containerId: Int, inv: Inventory, access: ContainerLevelAccess, rawId: String): DynamicWorkstationMenu {
            return DynamicWorkstationMenu(containerId, inv, access, rawId)
        }
        fun clientFactory(rawId: String): (Int, Inventory) -> DynamicWorkstationMenu {
            return { syncId, inv -> DynamicWorkstationMenu(syncId, inv, ContainerLevelAccess.NULL, rawId) }
        }
    }
}
