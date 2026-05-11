package com.nekiplay.neoscripts.sugar

import com.nekiplay.neoscripts.Main
import com.nekiplay.neoscripts.Main.LUA_MANAGER
import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.mixins.ClientPlayerInteractionManagerAccessor
import com.nekiplay.neoscripts.utils.RaycastUtils
import com.nekiplay.neoscripts.utils.aiming.RotationManager
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.multiplayer.prediction.PredictiveAction
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Rotations
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent

fun MultiPlayerGameMode.getRotationRaycast(): HitResult {
    var yaw: Float = mc?.player?.yRot ?: RotationManager.getCurrentYaw()
    var pitch: Float = mc?.player?.xRot ?: RotationManager.getCurrentPitch()
    if (!RotationManager.getCurrentYaw().isNaN()) {
        yaw = RotationManager.getCurrentYaw()
        pitch = RotationManager.getCurrentPitch()
    }
    val hitResult = RaycastUtils.findCrosshairTarget(mc?.cameraEntity, mc?.player?.eyePosition, yaw, pitch, mc?.player?.blockInteractionRange() ?: 4.5, mc?.player?.entityInteractionRange() ?: 3.0)
    return hitResult
}

fun MultiPlayerGameMode.silentUse(useSlot: Int): Boolean {
    val player = mc?.player ?: return false
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

    if (hitResult.type == HitResult.Type.BLOCK && mc?.screen == null && mc?.player?.isBlocking == false) {
        val blockHitResult = hitResult as BlockHitResult
        val blockPos = blockHitResult.blockPos
        mc?.level?.getBlockState(blockPos)?.isAir?.let {
            if (!it) {
                this.startDestroyBlock(blockPos, blockHitResult.direction)
                return true
            }
        }
    }
    return false
}

@EventBusSubscriber(modid = Main.ID, value = [Dist.CLIENT])
object MiningHandler {
    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        if (MiningState.isMining) {
            val player = mc?.player ?: return
            val gameMode = mc?.gameMode ?: return

            val hitResult = gameMode.getRotationRaycast() // ваш метод
            val targetPos = MiningState.targetPos ?: run { resetMining(); return }
            val targetDir = MiningState.targetDir ?: run { resetMining(); return }

            val isSameBlock = hitResult.type == HitResult.Type.BLOCK &&
                    (hitResult as BlockHitResult).blockPos == targetPos &&
                    player.distanceToSqr(Vec3.atCenterOf(targetPos)) <= 36.0

            if (!isSameBlock) {
                gameMode.stopDestroyBlock()
                resetMining()
                return
            }
        }
    }


    private fun resetMining() {
        MiningState.isMining = false
        MiningState.targetPos = null
        MiningState.targetDir = null
    }
}

object MiningState {
    var isMining: Boolean = false
    var targetPos: BlockPos? = null
    var targetDir: Direction? = null
}

fun MultiPlayerGameMode.mineBlock(): Boolean {
    val mc = Minecraft.getInstance()
    val player = mc.player ?: return false
    val level = mc.level ?: return false

    if (MiningState.isMining) {
        val hitResult = getRotationRaycast()
        val targetPos = MiningState.targetPos ?: run { resetMining(); return false }
        val targetDir = MiningState.targetDir ?: run { resetMining(); return false }

        val isSameBlock = hitResult.type == HitResult.Type.BLOCK &&
                (hitResult as BlockHitResult).blockPos == targetPos &&
                player.distanceToSqr(Vec3.atCenterOf(targetPos)) <= 36.0

        if (!isSameBlock) {
            this.stopDestroyBlock()
            resetMining()
            return false
        }

        this.continueDestroyBlock(targetPos, targetDir)
        return true
    }

    resetMining()
    if (mc.screen != null || player.isBlocking) return false

    val hitResult = getRotationRaycast()
    if (hitResult.type != HitResult.Type.BLOCK) return false

    val blockHit = hitResult as BlockHitResult
    val pos = blockHit.blockPos
    val dir = blockHit.direction
    val state = level.getBlockState(pos)

    if (state.isAir) return false
    if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 36.0) return false

    if (this.startDestroyBlock(pos, dir)) {
        MiningState.isMining = true
        MiningState.targetPos = pos
        MiningState.targetDir = dir
        return true
    }

    return false
}

private fun resetMining() {
    MiningState.isMining = false
    MiningState.targetPos = null
    MiningState.targetDir = null
}

fun MultiPlayerGameMode.attackEntity(): Boolean {
    val player = mc?.player ?: return false
    val hitResult = getRotationRaycast()
    if (hitResult.type == HitResult.Type.ENTITY && mc?.screen == null && mc?.player?.isBlocking == false) {
        this.attack(player, (hitResult as EntityHitResult).entity)
        mc?.player?.swing(InteractionHand.MAIN_HAND)
        return true
    }
    return false
}

fun MultiPlayerGameMode.interactBlock(): Boolean {
    val player = mc?.player ?: return false
    val hitResult = getRotationRaycast()
    if (hitResult.type == HitResult.Type.BLOCK && mc?.screen == null && mc?.player?.isBlocking == false) {
        for (hand in InteractionHand.entries) {
            val wasSneaking: Boolean = mc?.player?.isShiftKeyDown ?: false
            mc?.player?.isShiftKeyDown = false

            val actionResult2: InteractionResult? = this.useItemOn(player, hand, hitResult as BlockHitResult)
            if (actionResult2 is InteractionResult.Success) {
                if (actionResult2.swingSource() == InteractionResult.SwingSource.CLIENT) {
                    player.swing(hand)
                }
            }
            mc?.player?.isShiftKeyDown = wasSneaking
            return true
        }
    }
    return false
}

fun MultiPlayerGameMode.interactBlock(hitResult: HitResult?): Boolean {
    val player = mc?.player ?: return false
    if (hitResult == null) return false

    if (hitResult.type == HitResult.Type.BLOCK && mc?.screen == null && mc?.player?.isBlocking == false) {
        for (hand in InteractionHand.entries) {
            val wasSneaking: Boolean = mc?.player?.isShiftKeyDown ?: false
            mc?.player?.isShiftKeyDown = false
            val actionResult2: InteractionResult? = this.useItemOn(player, hand, hitResult as BlockHitResult)
            if (actionResult2 is InteractionResult.Success) {
                if (actionResult2.swingSource() == InteractionResult.SwingSource.CLIENT) {
                    player.swing(hand)
                }
            }
            mc?.player?.isShiftKeyDown = wasSneaking
            return true
        }
    }
    return false
}

fun MultiPlayerGameMode.interactEntity(): Boolean {
    val player = mc?.player ?: return false
    val hitResult = getRotationRaycast()
    if (hitResult.type == HitResult.Type.ENTITY && mc?.screen == null) {
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
    if (mc?.screen == null) {
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
    (this as ClientPlayerInteractionManagerAccessor).sendSequencedPacket(mc?.level, packetCreator)
}