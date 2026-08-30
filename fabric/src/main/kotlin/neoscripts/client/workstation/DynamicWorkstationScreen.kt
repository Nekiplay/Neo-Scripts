package com.nekiplay.neoscripts.client.workstation

import com.nekiplay.neoscripts.common.workstation.DynamicWorkstationMenu
import com.nekiplay.neoscripts.common.workstation.DynamicWorkstations
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory

class DynamicWorkstationScreen(
    menu: DynamicWorkstationMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<DynamicWorkstationMenu>(menu, inventory, title, 176, 166) {

    private val isFurnace = menu.isFurnace
    private val gridSize = menu.gridSize

    private val texture: Identifier = run {
        val custom = menu.rawId?.let { DynamicWorkstations.getTexture(it) }
        if (custom != null) {
            try { Identifier.parse(custom) } catch (_: Exception) { null }
        } else null
    } ?: when {
        isFurnace -> Identifier.withDefaultNamespace("textures/gui/container/furnace.png")
        gridSize == 5 -> Identifier.withDefaultNamespace("textures/gui/container/generic_54.png")
        else -> Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png")
    }

    init {
        // fix imageHeight for 5x5 / furnace (imageHeight is final, use reflection)
        val targetHeight = when {
            isFurnace -> 166
            gridSize == 5 -> 222
            else -> 166
        }
        if (targetHeight != 166) {
            try {
                val f = AbstractContainerScreen::class.java.getDeclaredField("imageHeight")
                f.isAccessible = true
                f.setInt(this, targetHeight)
            } catch (_: Exception) {}
        }
        titleLabelX = 28
        titleLabelY = 6
        inventoryLabelX = 8
        inventoryLabelY = imageHeight - 94
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractBackground(graphics, mouseX, mouseY, delta)
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)
    }
}
