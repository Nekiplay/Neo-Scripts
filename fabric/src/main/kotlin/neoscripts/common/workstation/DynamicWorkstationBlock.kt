package com.nekiplay.neoscripts.common.workstation

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class DynamicWorkstationBlock(
    props: BlockBehaviour.Properties,
    val rawId: String
) : Block(props) {

    override fun codec(): MapCodec<out Block> = MapCodec.unit(this)

    override fun getMenuProvider(state: BlockState, level: Level, pos: BlockPos): MenuProvider? {
        val title = DynamicWorkstations.getTitle(rawId) ?: rawId
        return object : MenuProvider {
            override fun getDisplayName(): Component = Component.literal(title)
            override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu {
                return DynamicWorkstationMenu(containerId, inventory, ContainerLevelAccess.create(level, pos), rawId)
            }
        }
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!level.isClientSide) {
            val provider = state.getMenuProvider(level, pos)
            if (provider != null) {
                player.openMenu(provider)
            }
        }
        return InteractionResult.SUCCESS
    }
}
