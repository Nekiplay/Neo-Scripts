package com.nekiplay.neoscripts.client.container

import com.nekiplay.neoscripts.common.container.DynamicContainerMenu
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory

/**
 * Screen for dynamic container. Chooses texture based on rows.
 */
class DynamicContainerScreen(
    menu: DynamicContainerMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<DynamicContainerMenu>(
    menu, inventory, title,
    176, 114 + ((menu.container.containerSize + 8) / 9) * 18
) {

    private val rows = (menu.container.containerSize + 8) / 9

    private val texture: Identifier = run {
        val custom = menu.rawId?.let { com.nekiplay.neoscripts.common.container.DynamicContainers.getTexture(it) }
        if (custom != null) {
            // allow file path or identifier; if contains ":" treat as Identifier, else try parse
            try {
                if (custom.contains(":") || custom.contains("/")) Identifier.parse(custom) else Identifier.withDefaultNamespace(custom)
            } catch (_: Exception) { null }
        } else null
    } ?: Identifier.withDefaultNamespace("textures/gui/container/generic_54.png")

    init {
        inventoryLabelY = imageHeight - 94
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractBackground(graphics, mouseX, mouseY, delta)
        val x = leftPos
        val y = topPos
        val containerHeight = 17 + rows * 18
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, imageWidth, containerHeight, 256, 256)
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y + containerHeight, 0f, 126f, imageWidth, 96, 256, 256)
    }
}
