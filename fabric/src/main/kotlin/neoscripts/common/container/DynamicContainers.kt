package com.nekiplay.neoscripts.common.container

import com.nekiplay.neoscripts.ServerMain
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for dynamic container blocks: rawId -> size/title/block/menuType/blockEntityType
 * https://docs.fabricmc.net/develop/blocks/block-containers
 * https://docs.fabricmc.net/develop/blocks/container-menus
 */
object DynamicContainers {
    val containerSizes = ConcurrentHashMap<String, Int>() // rawId -> size
    val containerTitles = ConcurrentHashMap<String, String>()
    val containerSlots = ConcurrentHashMap<String, List<IntArray>>() // rawId -> list of [x,y] per slot
    val containerTextures = ConcurrentHashMap<String, String>() // rawId -> texture Identifier string
    val rawIdToBlock = ConcurrentHashMap<String, Block>()
    val blockToRawId = ConcurrentHashMap<Block, String>()
    val rawIdToMenuType = ConcurrentHashMap<String, MenuType<DynamicContainerMenu>>()
    val rawIdToBlockEntityType = ConcurrentHashMap<String, BlockEntityType<DynamicContainerBlockEntity>>()
    // reverse for entity -> type
    private val blockEntityTypeByBlock = ConcurrentHashMap<Block, BlockEntityType<DynamicContainerBlockEntity>>()

    fun getSize(rawId: String): Int? = containerSizes[rawId]
    fun getTitle(rawId: String): String? = containerTitles[rawId]
    fun getSlots(rawId: String): List<IntArray>? = containerSlots[rawId]
    fun getTexture(rawId: String): String? = containerTextures[rawId]
    fun getRawId(block: Block): String? = blockToRawId[block]
    fun getBlock(rawId: String): Block? = rawIdToBlock[rawId]
    fun getMenuType(rawId: String): MenuType<DynamicContainerMenu>? = rawIdToMenuType[rawId]
    fun getBlockEntityType(block: Block): BlockEntityType<DynamicContainerBlockEntity>? = blockEntityTypeByBlock[block]
    fun isContainerBlock(block: Block): Boolean = blockToRawId.containsKey(block)
    fun isContainerRawId(rawId: String): Boolean = containerSizes.containsKey(rawId)

    fun fallbackType(pos: BlockPos, state: BlockState): BlockEntityType<DynamicContainerBlockEntity> {
        // fallback for BlockEntity creation before registration completed; use any existing type or create dummy
        return rawIdToBlockEntityType.values.firstOrNull() ?: throw IllegalStateException("No container block entity type registered yet for $state")
    }

    fun register(rawId: String, size: Int, title: String?, slots: List<IntArray>?, texture: String?, block: Block, blockEntityType: BlockEntityType<DynamicContainerBlockEntity>, menuType: MenuType<DynamicContainerMenu>) {
        containerSizes[rawId] = size
        if (title != null) containerTitles[rawId] = title
        if (slots != null) containerSlots[rawId] = slots
        if (texture != null) containerTextures[rawId] = texture
        rawIdToBlock[rawId] = block
        blockToRawId[block] = rawId
        rawIdToBlockEntityType[rawId] = blockEntityType
        rawIdToMenuType[rawId] = menuType
        blockEntityTypeByBlock[block] = blockEntityType
        ServerMain.LOGGER?.info("[Neo Scripts] Registered container $rawId size=$size title=${title ?: rawId} slots=${slots?.size ?: "default"} texture=${texture ?: "default"}")
    }

    // legacy overload
    fun register(rawId: String, size: Int, title: String?, block: Block, blockEntityType: BlockEntityType<DynamicContainerBlockEntity>, menuType: MenuType<DynamicContainerMenu>) {
        register(rawId, size, title, null, null, block, blockEntityType, menuType)
    }
}
