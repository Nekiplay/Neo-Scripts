package com.nekiplay.neoscripts.common.workstation

import com.nekiplay.neoscripts.ServerMain
import net.minecraft.resources.Identifier
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.block.Block
import java.util.concurrent.ConcurrentHashMap

object DynamicWorkstations {
    val workstationTitles = ConcurrentHashMap<String, String>()
    val workstationTextures = ConcurrentHashMap<String, String>()
    val workstationTypes = ConcurrentHashMap<String, String>() // rawId -> type "crafting", "crafting_5x5", "furnace"
    val workstationSlots = ConcurrentHashMap<String, List<IntArray>>() // custom slot positions
    val rawIdToBlock = ConcurrentHashMap<String, Block>()
    val blockToRawId = ConcurrentHashMap<Block, String>()
    val rawIdToMenuType = ConcurrentHashMap<String, MenuType<*>>()

    fun getTitle(rawId: String): String? = workstationTitles[rawId]
    fun getTexture(rawId: String): String? = workstationTextures[rawId]
    fun getType(rawId: String): String = workstationTypes[rawId] ?: "crafting"
    fun getSlots(rawId: String): List<IntArray>? = workstationSlots[rawId]
    fun getRawId(block: Block): String? = blockToRawId[block]
    fun getMenuType(rawId: String): MenuType<*>? = rawIdToMenuType[rawId]
    @Suppress("UNCHECKED_CAST")
    fun <T : AbstractContainerMenu> getMenuTypeTyped(rawId: String): MenuType<T>? = rawIdToMenuType[rawId] as? MenuType<T>

    fun register(rawId: String, title: String?, texture: String?, type: String?, slots: List<IntArray>?, block: Block, menuType: MenuType<*>) {
        if (title != null) workstationTitles[rawId] = title
        if (texture != null) workstationTextures[rawId] = texture
        if (type != null) workstationTypes[rawId] = type
        if (slots != null) workstationSlots[rawId] = slots
        rawIdToBlock[rawId] = block
        blockToRawId[block] = rawId
        rawIdToMenuType[rawId] = menuType
        ServerMain.LOGGER?.info("[Neo Scripts] Registered workstation $rawId type=${type ?: "crafting"} title=${title ?: rawId} texture=${texture ?: "crafting_table"} slots=${slots?.size ?: "default"}")
    }

    fun register(rawId: String, title: String?, texture: String?, type: String?, block: Block, menuType: MenuType<*>) {
        register(rawId, title, texture, type, null, block, menuType)
    }

    // legacy
    fun register(rawId: String, title: String?, texture: String?, block: Block, menuType: MenuType<DynamicWorkstationMenu>) {
        register(rawId, title, texture, null, block, menuType)
    }
}
