package com.nekiplay.hypixelcry.sugar

import com.nekiplay.hypixelcry.HypixelCry.mc
import com.nekiplay.hypixelcry.mixins.ClientPlayerInteractionManagerAccessor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.network.SequencedPacketCreator
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand

fun ClientPlayerInteractionManager.silentUse(useSlot: Int) {
    val player = MinecraftClient.getInstance().player ?: return
    val inventory = player.inventory
    val originalSlot = inventory.selectedSlot

    if (useSlot in 0..8) {
        inventory.selectedSlot = useSlot
        val result = this.interactItem(player, Hand.MAIN_HAND)
        if (result is ActionResult.Success) {
            player.swingHand(Hand.MAIN_HAND)
        }
    }

    inventory.selectedSlot = originalSlot
    syncSelectedSlot()
}

fun ClientPlayerInteractionManager.syncSelectedSlot() {
    (this as ClientPlayerInteractionManagerAccessor).syncSelectedSlot()
}

fun ClientPlayerInteractionManager.sendSequencedPacket(packetCreator: SequencedPacketCreator) {
    (this as ClientPlayerInteractionManagerAccessor).sendSequencedPacket(mc.world, packetCreator)
}