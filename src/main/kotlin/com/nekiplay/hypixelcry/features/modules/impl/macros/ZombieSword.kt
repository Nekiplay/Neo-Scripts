package com.nekiplay.hypixelcry.features.modules.impl.macros

import com.nekiplay.hypixelcry.features.modules.BindableClientModule
import com.nekiplay.hypixelcry.sugar.findSlotInHotbarByItemId
import com.nekiplay.hypixelcry.sugar.silentUse
import com.nekiplay.hypixelcry.utils.ItemUtils
import net.minecraft.component.ComponentHolder

class ZombieSword : BindableClientModule() {
    private val WAND_IDS = setOf(
        "ZOMBIE_SWORD"
    )

    override fun get_name(): String {
        return "Zombie_Sword";
    }

    override fun getKeybind(): Int {
        return config.macros.items.zombieSword.keybind
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