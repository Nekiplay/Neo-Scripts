package com.nekiplay.neoscripts.utils.aiming

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.events.ApplyInputEvent
import com.nekiplay.neoscripts.events.ExtractRenderStateEvent
import com.nekiplay.neoscripts.events.PacketEvent
import com.nekiplay.neoscripts.events.PlayerTickEvent
import com.nekiplay.neoscripts.events.RaycastHitEvent
import com.nekiplay.neoscripts.events.RenderEvent
import com.nekiplay.neoscripts.events.SyncPosEvent
import com.nekiplay.neoscripts.events.main.Callback
import com.nekiplay.neoscripts.events.TravelEvent
import com.nekiplay.neoscripts.mixins.minecraft.ClientInputAccessor
import com.nekiplay.neoscripts.mixins.minecraft.packets.ServerboundUseItemPacketAccessor
import com.nekiplay.neoscripts.utils.MathUtil
import com.nekiplay.neoscripts.utils.ServerUtil
import com.nekiplay.neoscripts.utils.animation.AnimationUtil
import com.nekiplay.neoscripts.utils.game.InputUtil
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.entity.player.Input
import net.minecraft.world.phys.Vec2
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sign

object RotationManager {
    var yaw = Float.NaN
        private set
    var pitch = Float.NaN
        private set
    var currentPriority = -1
        private set
    var ticks = -1
        private set

    private var rotationSet = false

    private var bodyYaw = Float.NaN

    private var lastYaw = Float.NaN
    private var lastPitch = Float.NaN
    private var lastBodyYaw = Float.NaN

    private var originalSyncYaw = Float.NaN
    private var originalSyncPitch = Float.NaN

    private var originalTravelYaw = Float.NaN
    private var originalTravelPitch = Float.NaN

    private var originalCrosshairYaw = Float.NaN
    private var originalCrosshairPitch = Float.NaN

    private var originalRenderYaw = Float.NaN
    private var originalRenderLastYaw = Float.NaN
    private var originalRenderBodyYaw = Float.NaN
    private var originalRenderLastBodyYaw = Float.NaN
    private var originalRenderPitch = Float.NaN
    private var originalRenderLastPitch = Float.NaN

    fun getCurrentYaw() = if (yaw.isNaN()) mc?.player?.yRot ?: 0f  else yaw
    fun getCurrentLastYaw() = if (lastYaw.isNaN()) mc?.player?.yRotO ?: 0f  else lastYaw
    fun getCurrentPitch() = if (pitch.isNaN()) mc?.player?.xRot ?: 0f else pitch
    fun getCurrentLastPitch() = if (lastPitch.isNaN()) mc?.player?.xRotO ?: 0f  else lastPitch

    fun rotateTo(yaw : Float, pitch : Float, ticks : Int = 0, priority : Int = 0, correction: Boolean = true, silentCorrection: Boolean = false) {
        Rotations.moveFix = correction
        Rotations.moveFixSilent = silentCorrection


        if (Rotations.pauseInInventory && mc?.screen != null) return

        if (priority >= currentPriority) {
            if (yaw.isNaN() && pitch.isNaN()) resetRotate(priority)
            else {
                val fixedYaw = MathUtil.applyGCD(MathUtil.limitAngle(getCurrentYaw(), yaw, 180F), getCurrentYaw())
                val fixedPitch = MathUtil.applyGCD(pitch, getCurrentPitch()).coerceIn(-90F, 90F)

                this.yaw = fixedYaw
                this.pitch = fixedPitch
                this.ticks = ticks.coerceAtLeast(0)
                this.currentPriority = priority
                this.rotationSet = true
            }
        }
    }

    fun rotateToWithoutFixAndCheck(yaw : Float, pitch : Float, ticks : Int = 0, priority : Int = 0) {
        if (priority >= currentPriority) {
            if (yaw.isNaN() && pitch.isNaN()) resetRotate(priority)
            else {
                val fixedYaw = MathUtil.limitAngle(getCurrentYaw(), yaw, 180F)
                val fixedPitch = pitch.coerceIn(-90F, 90F)

                this.yaw = fixedYaw
                this.pitch = fixedPitch
                this.ticks = ticks.coerceAtLeast(0)
                this.currentPriority = priority
                this.rotationSet = true
            }
        }
    }

    @Callback
    fun onRender(event : RenderEvent) {
        if (!Rotations.clientLook) return

        if (!yaw.isNaN()) mc?.player?.yRot = AnimationUtil.lerp(getCurrentLastYaw(), yaw, mc?.deltaTracker?.getGameTimeDeltaPartialTick(false)!!)
        if (!pitch.isNaN()) mc?.player?.xRot = AnimationUtil.lerp(getCurrentLastPitch(), pitch, mc?.deltaTracker?.getGameTimeDeltaPartialTick(false)!!)
    }

    @Callback(10)
    fun onTick(event : PlayerTickEvent) {
        lastYaw = if (!yaw.isNaN()) yaw else mc?.player?.yRot ?: Float.NaN
        lastPitch = if (!pitch.isNaN()) pitch else mc?.player?.xRot ?: Float.NaN
    }

    @Callback
    fun onApplyInput(event : ApplyInputEvent) {
        if (Rotations.moveFix || !Rotations.moveFixSilent) return

        if (!yaw.isNaN()) {
            val moveInput = InputUtil.calculateCorrectedKeys(

                mc?.player?.input?.keyPresses?.forward ?: false,
                mc?.player?.input?.keyPresses?.backward ?: false,
                mc?.player?.input?.keyPresses?.left ?: false,
                mc?.player?.input?.keyPresses?.right ?: false,
                mc?.player?.yRot ?: 0f, yaw  ?: 0f)

            if (mc?.player?.isSprinting == true && !moveInput.forward) mc?.player?.isSprinting = false

            val acccessed = mc?.player?.input as ClientInputAccessor

            acccessed.setMoveVector(Vec2(
                InputUtil.calculateImpulse(moveInput.left, moveInput.right),
                InputUtil.calculateImpulse(moveInput.forward, moveInput.back)
            ))
            mc?.player?.input?.keyPresses = Input(
                moveInput.forward,
                moveInput.back,
                moveInput.left,
                moveInput.right,
                mc?.player?.input?.keyPresses?.jump ?: false,
                mc?.player?.input?.keyPresses?.shift ?: false,
                mc?.player?.input?.keyPresses?.sprint ?: false
            )
        }
    }

    @Callback
    fun onPacketSend(event : PacketEvent.Send) {
        if (!Rotations.hookCrosshairTarget) return

        if (event.packet is ServerboundUseItemPacket) {
            val packet = event.packet as ServerboundUseItemPacket

            val accessed = packet as ServerboundUseItemPacketAccessor

            if (packet.yRot != getCurrentYaw() || packet.xRot != getCurrentPitch()) {
                accessed.setyRot(getCurrentYaw())
                accessed.setxRot(getCurrentPitch())

                event.modified = true
            }


        }
    }

    @Callback
    fun onSyncPosition(event: SyncPosEvent) {
        val player = mc?.player ?: return
        if (event.pre) {
            if (!yaw.isNaN()) {
                originalSyncYaw = player.yRot
                player.yRot = yaw
                if (ServerUtil.yaw != yaw && player.yRotO != originalSyncYaw) player.yRotO = originalSyncYaw
            }
            if (!pitch.isNaN()) {
                originalSyncPitch = player.xRot
                player.xRot = pitch
                if (ServerUtil.pitch != pitch && player.xRotO != originalSyncPitch) player.xRotO = originalSyncPitch
            }
        } else {
            lastBodyYaw = if (!bodyYaw.isNaN()) bodyYaw else player.yBodyRot
            bodyYaw = calcBodyYaw()
            if (!originalSyncYaw.isNaN()) {
                player.yRot = originalSyncYaw
                originalSyncYaw = Float.NaN
            }
            if (!originalSyncPitch.isNaN()) {
                player.xRot = originalSyncPitch
                originalSyncPitch = Float.NaN
            }
            val shouldReset = !(Rotations.pauseInInventory && mc?.screen != null)
            if (shouldReset) {
                if (rotationSet) {
                    rotationSet = false
                } else {
                    if (!yaw.isNaN()) {
                        if (ticks > 0) ticks--
                        else resetRotate(currentPriority)
                    }
                }
            }
        }
    }

    @Callback
    fun onTravel(event : TravelEvent) {
        if (!Rotations.moveFix) return

        if (event.pre) {
            if (!yaw.isNaN()) {
                originalTravelYaw = mc?.player?.yRot ?: Float.NaN
                mc?.player?.yRot = yaw
            }
            if (!pitch.isNaN()) {
                originalTravelPitch = mc?.player?.xRot ?: Float.NaN
                mc?.player?.xRot = pitch
            }
        } else {
            if (!originalTravelYaw.isNaN()) {
                mc?.player?.yRot = originalTravelYaw
                originalTravelYaw = Float.NaN
            }
            if (!originalTravelPitch.isNaN()) {
                mc?.player?.xRot = originalTravelPitch
                originalTravelPitch = Float.NaN
            }
        }
    }

    @Callback
    fun onRaycastHit(event : RaycastHitEvent) {
        if (!Rotations.hookCrosshairTarget) return

        if (event.pre) {
            if (!yaw.isNaN()) {
                originalCrosshairYaw = mc?.player?.yRot ?: Float.NaN
                mc?.player?.yRot = yaw
            }
            if (!pitch.isNaN()) {
                originalCrosshairPitch = mc?.player?.xRot ?: Float.NaN
                mc?.player?.xRot = pitch
            }
        } else {
            if (!originalCrosshairYaw.isNaN()) {
                mc?.player?.yRot = originalCrosshairYaw
                originalCrosshairYaw = Float.NaN
            }
            if (!originalCrosshairPitch.isNaN()) {
                mc?.player?.xRot = originalCrosshairPitch
                originalCrosshairPitch = Float.NaN
            }
        }
    }

    @Callback
    fun onExtractRenderState(event : ExtractRenderStateEvent) {
        if (event.entity == mc?.player) {
            if (event.pre) {
                val currentYaw = if (!yaw.isNaN()) yaw else mc?.player?.yRot ?: Float.NaN
                val previousYaw = if (!lastYaw.isNaN()) lastYaw else mc?.player?.yRotO ?: Float.NaN

                val currentPitch = if (!pitch.isNaN()) pitch else mc?.player?.xRot ?: Float.NaN
                val previousPitch = if (!lastPitch.isNaN()) lastPitch else mc?.player?.xRotO ?: Float.NaN

                val currentBody = if (!bodyYaw.isNaN()) bodyYaw else mc?.player?.yBodyRot ?: Float.NaN
                val previousBody = if (!lastBodyYaw.isNaN()) lastBodyYaw else mc?.player?.yBodyRotO ?: Float.NaN

                originalRenderYaw = mc?.player?.yRot ?: Float.NaN
                originalRenderLastYaw = mc?.player?.yRotO ?: Float.NaN
                originalRenderPitch = mc?.player?.xRot ?: Float.NaN
                originalRenderLastPitch = mc?.player?.xRotO ?: Float.NaN
                originalRenderBodyYaw = mc?.player?.yBodyRot ?: Float.NaN
                originalRenderLastBodyYaw = mc?.player?.yBodyRotO ?: Float.NaN

                mc?.player?.yRot = currentYaw
                mc?.player?.yRotO = previousYaw

                mc?.player?.yHeadRot = currentYaw
                mc?.player?.yHeadRotO = previousYaw

                mc?.player?.xRot = currentPitch
                mc?.player?.xRotO = previousPitch

                mc?.player?.yBodyRot = currentBody
                mc?.player?.yBodyRotO = previousBody

            } else {
                if (!originalRenderYaw.isNaN()) {
                    mc?.player?.yRot = originalRenderYaw
                    mc?.player?.yHeadRot = originalRenderYaw
                    originalRenderYaw = Float.NaN
                }
                if (!originalRenderLastYaw.isNaN()) {
                    mc?.player?.yRotO = originalRenderLastYaw
                    mc?.player?.yHeadRotO = originalRenderLastYaw
                    originalRenderLastYaw = Float.NaN
                }
                if (!originalRenderPitch.isNaN()) {
                    mc?.player?.xRot = originalRenderPitch
                    originalRenderPitch = Float.NaN
                }
                if (!originalRenderLastPitch.isNaN()) {
                    mc?.player?.xRotO = originalRenderLastPitch
                    originalRenderLastPitch = Float.NaN
                }
                if (!originalRenderBodyYaw.isNaN()) {
                    mc?.player?.yBodyRot = originalRenderBodyYaw
                    originalRenderBodyYaw = Float.NaN
                }
                if (!originalRenderLastBodyYaw.isNaN()) {
                    mc?.player?.yBodyRotO = originalRenderLastBodyYaw
                    originalRenderLastBodyYaw = Float.NaN
                }
            }
        }
    }

    private fun calcBodyYaw(): Float {
        val player = mc?.player ?: return 0f
        val headYaw = when {
            !yaw.isNaN() -> yaw
            !player.yRot.isNaN() -> player.yRot
            else -> 0f
        }
        var renderYaw = if (!bodyYaw.isNaN()) bodyYaw else player.yBodyRot
        if (renderYaw.isNaN()) renderYaw = headYaw
        var offset = renderYaw
        val dx = player.x - player.xOld
        val dz = player.z - player.zOld
        if (dx * dx + dz * dz > 0.0025000002) {
            var moveAngle = (atan2(dz, dx) * 57.295776 - 90.0).toFloat()
            val diff = abs(MathUtil.wrapDegrees(headYaw - moveAngle))
            if (diff > 95.0f && diff < 265.0f) moveAngle -= 180.0f
            offset = moveAngle
        }
        player.attackAnim.let {
            if (it > 0.0f) offset = headYaw
        }
        var proposedBodyYaw = renderYaw + MathUtil.wrapDegrees(offset - renderYaw) * 0.3f
        val headDiff = MathUtil.wrapDegrees(headYaw - proposedBodyYaw)
        if (abs(headDiff) > 50.0f) {
            proposedBodyYaw += headDiff - sign(headDiff) * 50.0f
        }
        return if (proposedBodyYaw.isNaN() || !proposedBodyYaw.isFinite()) headYaw
        else MathUtil.wrapDegrees(proposedBodyYaw)
    }

    fun resetRotate(priority: Int = 0) {
        if (priority >= currentPriority) {
            val player = mc?.player ?: return
            if (!yaw.isNaN()) {
                val diff = MathUtil.wrapDegrees(player.yRot - yaw)
                if (abs(player.yRot - yaw) > 180F) {
                    val finalYaw = yaw + diff
                    player.yRot = finalYaw
                    player.yBob += (player.yRot - player.yBob)
                    player.yBobO = player.yBob
                }
            }
            yaw = Float.NaN
            pitch = Float.NaN
            currentPriority = -1
            rotationSet = false
        }
    }

}