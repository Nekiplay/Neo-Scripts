package com.nekiplay.neoscripts.sugar

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.mixins.ClientPlayerInteractionManagerAccessor
import com.nekiplay.neoscripts.utils.RaycastUtils
import com.nekiplay.neoscripts.utils.Rotations
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.multiplayer.prediction.PredictiveAction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult


fun MultiPlayerGameMode.getRotationRaycast(): HitResult {
    var yaw: Float = mc.player?.yRot ?: Rotations.serverYaw
    var pitch: Float = mc.player?.xRot ?: Rotations.serverPitch
    if (Rotations.rotating) {
        yaw = Rotations.serverYaw
        pitch = Rotations.serverPitch
    }
    val hitResult = RaycastUtils.findCrosshairTarget(mc.cameraEntity, mc.player?.eyePosition, yaw, pitch, mc.player?.blockInteractionRange() ?: 4.5, mc.player?.entityInteractionRange() ?: 3.0)
    return hitResult
}

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
    val hitResult = getRotationRaycast()

    if (hitResult.type == HitResult.Type.BLOCK && mc.screen == null && mc.player?.isBlocking == false) {
        val blockHitResult = hitResult as BlockHitResult
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

fun MultiPlayerGameMode.mineBlock(): Boolean {
    val hitResult = getRotationRaycast()

    if (hitResult.type == HitResult.Type.BLOCK && mc.screen == null && mc.player?.isBlocking == false) {
        val blockHitResult = hitResult as BlockHitResult
        val blockPos = blockHitResult.blockPos
        mc.level?.getBlockState(blockPos)?.isAir?.let {
            if (!it) {
                if (this.isDestroying) { this.continueDestroyBlock(blockPos, blockHitResult.direction) }
                else { this.startDestroyBlock(blockPos, blockHitResult.direction) }
                return true
            }
        }
    }
    return false
}

fun MultiPlayerGameMode.attackEntity(): Boolean {
    val player = mc.player ?: return false
    val hitResult = getRotationRaycast()
    if (hitResult.type == HitResult.Type.ENTITY && mc.screen == null && mc.player?.isBlocking == false) {
        this.attack(player, (hitResult as EntityHitResult).entity)
        return true
    }
    return false
}

fun MultiPlayerGameMode.interactBlock(): Boolean {
    val player = mc.player ?: return false
    val hitResult = getRotationRaycast()
    if (hitResult.type == HitResult.Type.BLOCK && mc.screen == null && mc.player?.isBlocking == false) {
        for (hand in InteractionHand.entries) {
            val wasSneaking: Boolean = mc.player?.isShiftKeyDown ?: false
            mc.player?.isShiftKeyDown = false

            val actionResult2: InteractionResult? = this.useItemOn(player, hand, hitResult as BlockHitResult)
            if (actionResult2 is InteractionResult.Success) {
                if (actionResult2.swingSource() == InteractionResult.SwingSource.CLIENT) {
                    player.swing(hand)
                }
            }
            mc.player?.isShiftKeyDown = wasSneaking
            return true
        }
    }
    return false
}

fun MultiPlayerGameMode.interactBlock(hitResult: HitResult?): Boolean {
    val player = mc.player ?: return false
    if (hitResult == null) return false

    if (hitResult.type == HitResult.Type.BLOCK && mc.screen == null && mc.player?.isBlocking == false) {
        for (hand in InteractionHand.entries) {
            val wasSneaking: Boolean = mc.player?.isShiftKeyDown ?: false
            mc.player?.isShiftKeyDown = false
            val actionResult2: InteractionResult? = this.useItemOn(player, hand, hitResult as BlockHitResult)
            if (actionResult2 is InteractionResult.Success) {
                if (actionResult2.swingSource() == InteractionResult.SwingSource.CLIENT) {
                    player.swing(hand)
                }
            }
            mc.player?.isShiftKeyDown = wasSneaking
            return true
        }
    }
    return false
}

fun MultiPlayerGameMode.interactEntity(): Boolean {
    val player = mc.player ?: return false
    val hitResult = getRotationRaycast()
    if (hitResult.type == HitResult.Type.ENTITY && mc.screen == null) {
        for (hand in InteractionHand.entries) {
            val actionResult2: InteractionResult =
                this.interact(player, (hitResult as EntityHitResult).entity, hand)
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