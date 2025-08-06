package com.nekiplay.hypixelcry.features.modules.impl.macros

import com.nekiplay.hypixelcry.features.modules.BindableClientModule
import com.nekiplay.hypixelcry.sugar.findSlotInHotbarByItemId
import com.nekiplay.hypixelcry.sugar.silentUse
import com.nekiplay.hypixelcry.utils.ItemUtils
import net.minecraft.component.ComponentHolder

object ZombieSword : BindableClientModule() {
    private val WAND_IDS = setOf(
        "ZOMBIE_SWORD"
    )

    override fun getKeybind(): Int {
        return config.macros.items.zombieSword.keybind
    }

    override fun press() {
        findWand()?.let { slot ->
            interaction?.silentUse(slot)
        }
    }

    private fun findWand(): Int? {
        return WAND_IDS.firstNotNullOfOrNull { id ->
            player?.inventory?.findSlotInHotbarByItemId(id)?.takeIf { slot ->
                val stack = player?.inventory?.getStack(slot)
                stack != null && !stack.isEmpty && ItemUtils.getItemId(stack as ComponentHolder)
                    .equals(id, ignoreCase = true)
            }
        }
    }
}