package com.nekiplay.neoscripts.sugar

import com.nekiplay.neoscripts.Main.LUA_MANAGER
import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.mixins.ClientPlayerInteractionManagerAccessor
import com.nekiplay.neoscripts.utils.RaycastUtils
import com.nekiplay.neoscripts.utils.aiming.RotationManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
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

fun MultiPlayerGameMode.getRotationRaycast(): HitResult {
    var yaw: Float = mc.player?.yRot ?: RotationManager.getCurrentYaw()
    var pitch: Float = mc.player?.xRot ?: RotationManager.getCurrentPitch()
    if (!RotationManager.getCurrentYaw().isNaN()) {
        yaw = RotationManager.getCurrentYaw()
        pitch = RotationManager.getCurrentPitch()
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

object MiningHandler {
    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true

        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (MiningState.isMining) {
                val player = mc.player ?: return@register
                val gameMode = mc.gameMode ?: return@register

                val hitResult = gameMode.getRotationRaycast()
                val targetPos = MiningState.targetPos ?: run { resetMining(); return@register }
                val targetDir = MiningState.targetDir ?: run { resetMining(); return@register }

                val isSameBlock = hitResult.type == HitResult.Type.BLOCK &&
                        (hitResult as BlockHitResult).blockPos == targetPos &&
                        isBlockInRange(targetPos, mc.player!!)

                if (!isSameBlock) {
                    resetMining()
                    return@register
                }
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

fun isBlockInRange(pos: BlockPos, player: net.minecraft.world.entity.player.Player): Boolean {
    val reach = player.blockInteractionRange() // Обычно 4.5 в выживании
    val state = player.level().getBlockState(pos)

    // Получаем реальный хитбокс блока (AABB)
    val shape = state.getShape(player.level(), pos)
    if (shape.isEmpty) return player.eyePosition.distanceToSqr(Vec3.atCenterOf(pos)) < reach * reach

    val aabb = shape.bounds().move(pos)
    // distanceToSqr до AABB возвращает 0.0, если точка внутри,
    // или расстояние до ближайшей точки на поверхности
    return aabb.distanceToSqr(player.eyePosition) <= (reach * reach)
}

fun MultiPlayerGameMode.mineBlock(): Boolean {
    val mc = Minecraft.getInstance()
    val player = mc.player ?: return false
    val level = mc.level ?: return false
    val accessor = this as ClientPlayerInteractionManagerAccessor

    // 1. Получаем то, куда мы реально смотрим
    val hitResult = getRotationRaycast()

    if (hitResult.type != HitResult.Type.BLOCK) {
        if (MiningState.isMining) {
            this.stopDestroyBlock()
            resetMining()
        }
        return false
    }

    val blockHit = hitResult as BlockHitResult
    val pos = blockHit.blockPos
    val dir = blockHit.direction
    val state = level.getBlockState(pos)

    if (state.isAir) return false

    // Дистанция: getRotationRaycast уже учитывает blockInteractionRange().
    // Если raycast попал в блок, значит мы ДОТЯГИВАЕМСЯ до его грани.
    // Дополнительная проверка distanceToSqr здесь больше не нужна.

    val progress = state.getDestroyProgress(player, level, pos)

    // МОМЕНТАЛЬНЫЕ БЛОКИ (Трава, факелы, блоки в креативе)
    if (progress >= 1.0f) {
        accessor.setDestroyDelay(0) // Сброс задержки перед
        if (this.startDestroyBlock(pos, dir)) {
            accessor.setDestroyDelay(0) // Сброс задержки после
            resetMining()
            return true
        }
        return false
    }

    // ОБЫЧНЫЕ БЛОКИ (Процесс копания)
    if (MiningState.isMining && MiningState.targetPos == pos) {
        this.continueDestroyBlock(pos, dir)
        return true
    } else {
        // Начинаем копать новый блок
        accessor.setDestroyDelay(0)
        if (this.startDestroyBlock(pos, dir)) {
            MiningState.isMining = true
            MiningState.targetPos = pos
            MiningState.targetDir = dir
            return true
        }
    }

    return false
}

private fun resetMining() {
    MiningState.isMining = false
    MiningState.targetPos = null
    MiningState.targetDir = null
}

fun MultiPlayerGameMode.attackEntity(): Boolean {
    val player = mc.player ?: return false
    val hitResult = getRotationRaycast()
    if (hitResult.type == HitResult.Type.ENTITY && mc.screen == null && mc.player?.isBlocking == false) {
        this.attack(player, (hitResult as EntityHitResult).entity)
        mc.player?.swing(InteractionHand.MAIN_HAND)
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