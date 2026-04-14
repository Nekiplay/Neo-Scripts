package com.nekiplay.neoscripts.sugar

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.mixins.ClientPlayerInteractionManagerAccessor
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.multiplayer.prediction.PredictiveAction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

fun MultiPlayerGameMode.silentUse(useSlot: Int): Boolean {
    val player = mc.player ?: return false
    val inventory = player.inventory
    val originalSlot = inventory.selectedSlot

    if (useSlot in 0..8) {
        inventory.selectedSlot = useSlot
        useItem()
    }

    inventory.selectedSlot = originalSlot
    syncSelectedSlot()
    return useSlot in 0..8
}

fun MultiPlayerGameMode.attackBlock(): Boolean {
    if (mc.hitResult == null) return false

    if (mc.hitResult?.type == HitResult.Type.BLOCK && mc.screen == null && mc.player?.isBlocking == false) {
        val blockHitResult = mc.hitResult as BlockHitResult
        val blockPos = blockHitResult.blockPos
        mc.level?.getBlockState(blockPos)?.isAir?.let {
            if (!it) {
                this.startDestroyBlock(blockPos, blockHitResult.direction)
                return true
            }
        }
    }
    return false
}

fun MultiPlayerGameMode.attackEntity(): Boolean {
    val player = mc.player ?: return false
    if (mc.hitResult == null) return false

    if (mc.hitResult!!.type == HitResult.Type.ENTITY && mc.screen == null && mc.player?.isBlocking == false) {
        this.attack(player, (mc.hitResult as EntityHitResult).entity)
        return true
    }
    return false
}

fun MultiPlayerGameMode.interactBlock(): Boolean {
    val player = mc.player ?: return false
    if (mc.hitResult == null) return false

    if (mc.hitResult!!.type == HitResult.Type.BLOCK && mc.screen == null && mc.player?.isBlocking == false) {
        for (hand in InteractionHand.entries) {
            val actionResult2: InteractionResult? = this.useItemOn(player, hand, mc.hitResult as BlockHitResult)
            if (actionResult2 is InteractionResult.Success) {
                if (actionResult2.swingSource() == InteractionResult.SwingSource.CLIENT) {
                    player.swing(hand)
                }
                return true
            }
        }
    }
    return false
}

fun MultiPlayerGameMode.interactEntity(): Boolean {
    val player = mc.player ?: return false
    if (mc.hitResult == null) return false

    if (mc.hitResult!!.type == HitResult.Type.ENTITY && mc.screen == null) {
        for (hand in InteractionHand.entries) {
            val actionResult2: InteractionResult =
                this.interact(player, (mc.hitResult as EntityHitResult).entity, hand)
            if (actionResult2 is InteractionResult.Success) {
                if (actionResult2.swingSource() == InteractionResult.SwingSource.CLIENT) {
                    player.swing(hand)
                }
                return true
            }
        }
    }
    return false
}

fun MultiPlayerGameMode.useItem(): Boolean {
    if (mc.screen == null) {
        val player = Minecraft.getInstance().player ?: return false
        val result = this.useItem(player, InteractionHand.MAIN_HAND)
        if (result is InteractionResult.Success) {
            player.swing(InteractionHand.MAIN_HAND)
        }
        if (result is InteractionResult.Success || result is InteractionResult.Pass) {
            return true
        }
    }
   return false
}

fun MultiPlayerGameMode.syncSelectedSlot(): Boolean {
    (this as ClientPlayerInteractionManagerAccessor).syncSelectedSlot()
    return true
}

fun MultiPlayerGameMode.sendSequencedPacket(packetCreator: PredictiveAction) {
    (this as ClientPlayerInteractionManagerAccessor).sendSequencedPacket(mc.level, packetCreator)
}