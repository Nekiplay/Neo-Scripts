package com.nekiplay.hypixelcry.sugar

import com.nekiplay.hypixelcry.HypixelCry.mc
import com.nekiplay.hypixelcry.mixins.ClientPlayerInteractionManagerAccessor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.network.SequencedPacketCreator
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult

fun ClientPlayerInteractionManager.silentUse(useSlot: Int): Boolean {
    val player = MinecraftClient.getInstance().player ?: return false
    val inventory = player.inventory
    val originalSlot = inventory.selectedSlot

    if (useSlot in 0..8) {
        inventory.selectedSlot = useSlot
        useItem()
    }

    inventory.selectedSlot = originalSlot
    syncSelectedSlot()
    return if (useSlot in 0..8) {
        true
    }
    else {
        false
    }
}

fun ClientPlayerInteractionManager.attackBlock(): Boolean {
    val mc = MinecraftClient.getInstance()

    if (mc.crosshairTarget?.type == HitResult.Type.BLOCK) {
        val blockHitResult = mc.crosshairTarget as BlockHitResult
        val blockPos = blockHitResult.blockPos
        mc.world?.getBlockState(blockPos)?.isAir?.let {
            if (!it) {
                this.attackBlock(blockPos, blockHitResult.side)
                return true
            }
        }
    }
    return false
}

fun ClientPlayerInteractionManager.attackEntity(): Boolean {
    val mc = MinecraftClient.getInstance()
    val player = mc.player ?: return false

    if (mc.crosshairTarget?.type == HitResult.Type.ENTITY) {
        this.attackEntity(player, (mc.crosshairTarget as EntityHitResult).entity)
        return true
    }
    return false
}

fun ClientPlayerInteractionManager.useItem() {
    val player = MinecraftClient.getInstance().player ?: return
    val result = this.interactItem(player, Hand.MAIN_HAND)
    if (result is ActionResult.Success) {
        player.swingHand(Hand.MAIN_HAND)
    }
}

fun ClientPlayerInteractionManager.syncSelectedSlot() {
    (this as ClientPlayerInteractionManagerAccessor).syncSelectedSlot()
}

fun ClientPlayerInteractionManager.sendSequencedPacket(packetCreator: SequencedPacketCreator) {
    (this as ClientPlayerInteractionManagerAccessor).sendSequencedPacket(mc.world, packetCreator)
}