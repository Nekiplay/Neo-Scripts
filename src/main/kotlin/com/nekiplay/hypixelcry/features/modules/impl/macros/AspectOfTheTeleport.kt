package com.nekiplay.hypixelcry.features.modules.impl.macros

import com.nekiplay.hypixelcry.features.modules.BindableClientModule
import com.nekiplay.hypixelcry.sugar.findSlotInHotbarByItemId
import com.nekiplay.hypixelcry.sugar.silentUse
import com.nekiplay.hypixelcry.utils.ItemUtils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.component.ComponentHolder

object AspectOfTheTeleport : BindableClientModule() {
    private val WAND_IDS = setOf(
        "ASPECT_OF_THE_VOID",
        "ASPECT_OF_THE_END"
    )

    private var timer = 0
    private var enabled = false

    override fun getKeybind(): Int {
        return config.macros.items.aspectOfTheTeleports.keybind
    }

    override fun init() {
        ClientTickEvents.END_CLIENT_TICK.register(::onTick)
    }

    override fun press() {
        timer = 0
        enabled = true
    }

    override fun release() {
        enabled = false
    }

    private fun onTick(client: MinecraftClient?) {
        if (!enabled || player == null || world == null || screen != null) return

        if (timer <= 0) {
            findWand()?.let { slot ->
                interaction?.silentUse(slot)
            }
            timer = config.macros.items.aspectOfTheTeleports.delay
        } else {
            timer--
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