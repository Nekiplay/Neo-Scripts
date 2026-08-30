package com.nekiplay.neoscripts.client.container

import com.nekiplay.neoscripts.common.container.DynamicContainerMenu
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.world.inventory.MenuType

object ContainerScreenRegistry {
    @JvmStatic
    fun register(menuType: MenuType<DynamicContainerMenu>) {
        try {
            MenuScreens.register(menuType, ::DynamicContainerScreen)
        } catch (_: Exception) {}
    }
}
