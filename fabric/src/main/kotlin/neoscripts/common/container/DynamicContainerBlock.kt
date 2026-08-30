package com.nekiplay.neoscripts.common.container

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class DynamicContainerBlock(
    props: BlockBehaviour.Properties,
    val rawId: String
) : BaseEntityBlock(props) {

    override fun codec(): MapCodec<out BaseEntityBlock> = MapCodec.unit(this)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return DynamicContainerBlockEntity(pos, state)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!level.isClientSide) {
            val be = level.getBlockEntity(pos)
            if (be is DynamicContainerBlockEntity) {
                player.openMenu(be)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? = null
}
