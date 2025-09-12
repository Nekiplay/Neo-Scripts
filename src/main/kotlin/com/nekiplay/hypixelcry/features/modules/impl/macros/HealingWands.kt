package com.nekiplay.hypixelcry.features.modules.impl.macros

import com.nekiplay.hypixelcry.features.modules.BindableClientModule
import com.nekiplay.hypixelcry.sugar.findSlotInHotbarByItemId
import com.nekiplay.hypixelcry.sugar.silentUse
import com.nekiplay.hypixelcry.utils.ItemUtils
import net.minecraft.component.ComponentHolder

class HealingWands : BindableClientModule() {
    private val WAND_IDS = setOf(
        "WAND_OF_ATONEMENT",
        "WAND_OF_RESTORATION",
        "WAND_OF_MENDING",
        "WAND_OF_HEALING"
    )

    override fun get_name(): String {
        return "Healing_Wands";
    }

    override fun getKeybind(): Int {
        return config.macros.items.healingWands.keybind
    }

    override fun press() {
        if (screen != null) return
        findWand()?.let { slot ->
            interaction?.silentUse(slot)
        }
    }

    private fun findWand(): Int? {
        for (id in WAND_IDS) {
            val slot = player?.inventory?.findSlotInHotbarByItemId(id)
            if (slot != null && slot != -1) {
                val stack = player?.inventory?.getStack(slot)
                if (stack != null && !stack.isEmpty && ItemUtils.getItemId(stack as ComponentHolder)
                        .equals(id, ignoreCase = true)) {
                    return slot
                }
            }
        }
        return null
    }
}