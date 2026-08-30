package com.nekiplay.neoscripts.client.workstation

import com.nekiplay.neoscripts.common.workstation.DynamicWorkstationMenu
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.world.inventory.MenuType

object WorkstationScreenRegistry {
    @JvmStatic
    fun register(menuType: MenuType<DynamicWorkstationMenu>) {
        try {
            MenuScreens.register(menuType, ::DynamicWorkstationScreen)
        } catch (_: Exception) {}
    }
}
