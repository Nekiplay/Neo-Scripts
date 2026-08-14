package com.nekiplay.neoscripts.features.modules.impl.misc

import com.nekiplay.neoscripts.Main.LUA_MANAGER
import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.events.KeyEvent
import com.nekiplay.neoscripts.events.MouseButtonEvent
import com.nekiplay.neoscripts.events.PacketEvent
import com.nekiplay.neoscripts.events.main.Callback
import com.nekiplay.neoscripts.events.player.AddItemInventoryEvent
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.render.WorldRendererObject
import com.nekiplay.neoscripts.features.modules.ClientModule
import com.nekiplay.neoscripts.sugar.getFormattedString
import com.nekiplay.neoscripts.sugar.getJsonString
import com.nekiplay.neoscripts.utils.aiming.RotationManager
import com.nekiplay.neoscripts.utils.render.LevelRenderExtractionCallback
import com.nekiplay.neoscripts.utils.render.primitive.PrimitiveCollector
import com.nekiplay.neoscripts.utils.scheduler.MessageScheduler
import com.nekiplay.neoscripts.utils.scheduler.Scheduler
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Relative
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import org.luaj.vm2.LuaValue


object LuaEvents : ClientModule() {

    @Callback
    fun onSendPacket(event : PacketEvent.Send) {
        val allow = when (event.packet) {
            is ServerboundMovePlayerPacket -> {
                val packet = event.packet as ServerboundMovePlayerPacket
                var allowed = true
                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    if (!script.onPlayerSendMovement(
                            packet.hasPosition(),
                            packet.getX(0.0),
                            packet.getY(0.0),
                            packet.getZ(0.0),
                            packet.hasPosition(),
                            packet.getYRot(0f),
                            packet.getXRot(0f),
                            packet.isOnGround
                        )) {
                        allowed = false
                    }
                }
                allowed
            }
            else -> true
        }
        if (!allow) event.cancelled = true
    }

    @Callback
    fun onRecivePacket(event : PacketEvent.Receive) {
        when (event.packet) {
            is ClientboundLevelParticlesPacket -> {
                val packet = event.packet as ClientboundLevelParticlesPacket

                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    script.onSpawnParticleEvent(
                        BuiltInRegistries.PARTICLE_TYPE.getId(packet.particle.type),
                        packet.x,
                        packet.y,
                        packet.z,
                        packet.xDist,
                        packet.yDist,
                        packet.zDist,
                        packet.maxSpeed,
                        packet.count
                    )
                }
            }

            is ClientboundSetTimePacket -> {
                val packet = event.packet as ClientboundSetTimePacket

                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    script.onServerSideSetTimeEvent(packet.gameTime, packet.gameTime)
                }
            }

            is ClientboundSoundPacket -> {
                val packet = event.packet as ClientboundSoundPacket

                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    script.onSoundPlay(packet.sound, packet.x, packet.y, packet.z, packet.pitch.toDouble(), packet.volume.toDouble())
                }
            }
        }
        val allow = when (val packet = event.packet) {
            is ClientboundBlockUpdatePacket -> {
                var allowedBlockUpdate = true
                val packet = event.packet as ClientboundBlockUpdatePacket

                val table = LuaValue.tableOf()

                table.set("position", LuaBlockPos(packet.pos))
                val oldState = mc.level?.getBlockState(packet.pos)
                if (oldState != null) {
                    table.set("old", LuaBlockState(oldState))
                }
                if (packet.blockState != null) {
                    table.set("new", LuaBlockState(packet.blockState))
                }

                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    if (!script.onBlockUpdateEvent(table)) {
                        allowedBlockUpdate = false
                    }
                }

                allowedBlockUpdate
            }

            is ClientboundSetTitleTextPacket -> {
                var allowed = true
                val packet = event.packet as ClientboundSetTitleTextPacket

                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    if (!script.onTitle(packet.text.getFormattedString(), false)) {
                        allowed = false
                    }
                }

                allowed
            }

            is ClientboundSetActionBarTextPacket -> {
                var allowed = true
                val packet = event.packet as ClientboundSetActionBarTextPacket

                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    if (!script.onActionBar(packet.text.getFormattedString())) {
                        allowed = false
                    }
                }

                allowed
            }

            is ClientboundSetSubtitleTextPacket -> {
                var allowed = true
                val packet = event.packet as ClientboundSetSubtitleTextPacket

                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    if (!script.onTitle(packet.text.getFormattedString(), true)) {
                        allowed = false
                    }
                }

                allowed
            }

            is ClientboundPlayerPositionPacket -> {
                var rotationAllowed = true
                var teleportAllowed = true

                val player = mc.player

                val targetYaw = if (player != null && packet.relatives().contains(Relative.Y_ROT)) {
                    player.yRot + packet.change.yRot()
                } else {
                    packet.change.yRot()
                }

                val targetPitch = if (player != null && packet.relatives().contains(Relative.X_ROT)) {
                    player.xRot + packet.change.xRot()
                } else {
                    packet.change.xRot()
                }

                val targetX = if (player != null && packet.relatives().contains(Relative.X)) {
                    player.x + packet.change.position.x
                } else {
                    packet.change.position.x
                }

                val targetY = if (player != null && packet.relatives().contains(Relative.Y)) {
                    player.y + packet.change.position.y
                } else {
                    packet.change.position.y
                }

                val targetZ = if (player != null && packet.relatives().contains(Relative.Z)) {
                    player.z + packet.change.position.z
                } else {
                    packet.change.position.z
                }

                val activeYaw = if (RotationManager.getCurrentYaw().isNaN()) {
                    player?.yRot ?: 0f
                } else {
                    RotationManager.getCurrentYaw()
                }

                val activePitch = if (RotationManager.getCurrentPitch().isNaN()) {
                    player?.xRot ?: 0f
                } else {
                    RotationManager.getCurrentPitch()
                }

                // Проверяем наличие фактических изменений
                val rotationChanged = player == null || targetYaw != activeYaw || targetPitch != activePitch
                val positionChanged = player == null || targetX != player.x || targetY != player.y || targetZ != player.z

                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    if (rotationChanged) {
                        // Передаем целевой поворот, который пытается установить сервер
                        if (!script.onServerSideRotationEvent(targetYaw, targetPitch)) {
                            rotationAllowed = false
                        }
                    }
                    if (positionChanged) {
                        if (!script.onServerSideTeleportEvent(targetX, targetY, targetZ)) {
                            teleportAllowed = false
                        }
                    }
                }

                rotationAllowed && teleportAllowed
            }

            else -> true
        }

        if (!allow) event.cancelled = true
    }


    override fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    try {
                        script.onClientTick()
                    } catch (e: Exception) {
                        // Обработка ошибок
                    }
                }
        }

        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    script.onClientTickPre()
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
            try {
                Scheduler.INSTANCE.tick();
                MessageScheduler.INSTANCE.tick();
            }
            catch (e: Exception) {

            }
        }

        LevelRenderExtractionCallback.EVENT.register({ context: PrimitiveCollector? ->
            val renderContext = WorldRendererObject(context)
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    script.onRenderTick(renderContext)
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
        })

        KeyEvent.EVENT.register(KeyEvent.KeyCallback { keyEvent ->
            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onKeyEvent(keyEvent.key, keyEvent.action)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
            if (allow) {
                InteractionResult.PASS
            } else {
                InteractionResult.FAIL
            }
        })

        MouseButtonEvent.EVENT.register(MouseButtonEvent.KeyCallback { mouseButtonEvent ->
            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onKeyEvent(mouseButtonEvent.button, mouseButtonEvent.action)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
            if (allow) {
                InteractionResult.PASS
            } else {
                InteractionResult.FAIL
            }
        })

        UseBlockCallback.EVENT.register(UseBlockCallback { player: Player, world: Level, hand: InteractionHand, hitResult: BlockHitResult ->
            var allow = true
            if (hitResult.type == HitResult.Type.BLOCK || hitResult.type == HitResult.Type.MISS) {
                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    try {
                        if (!script.onUseBlock(hitResult.blockPos, hand)) {
                            allow = false
                        }
                    } catch (e: Exception) {
                        // Обработка ошибок
                    }
                }
            }
            if (allow) {
                InteractionResult.PASS
            } else {
                InteractionResult.FAIL
            }
        })

        AttackBlockCallback.EVENT.register(AttackBlockCallback { player: Player, world: Level, hand: InteractionHand, pos: BlockPos, direction: Direction ->
            var allow = true

            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onAttackBlock(pos, direction, hand)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }

            if (allow) {
                InteractionResult.PASS
            } else {
                InteractionResult.FAIL
            }
        })

        ClientReceiveMessageEvents.ALLOW_GAME.register(ClientReceiveMessageEvents.AllowGame { text, overlay ->
            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onChatMessageEvent(text.getFormattedString(), overlay, text.getJsonString())) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
            allow
        })

        ClientSendMessageEvents.ALLOW_CHAT.register(ClientSendMessageEvents.AllowChat { text ->
            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onSendChatMessageEvent(text)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
            allow
        })

        AddItemInventoryEvent.EVENT.register { event ->
            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onInventoryItemAChange(event.slot, event.item)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }

            if (allow) InteractionResult.PASS else InteractionResult.FAIL
        }

        ClientChunkEvents.CHUNK_LOAD.register { level, chunk ->
            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onChunkLoadEvent(chunk)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }

            if (allow) InteractionResult.PASS else InteractionResult.FAIL
        }

        ClientChunkEvents.CHUNK_UNLOAD.register { level, chunk ->
            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onChunkUnLoadEvent(chunk)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }

            if (allow) InteractionResult.PASS else InteractionResult.FAIL
        }

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { client, level ->
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    script.onLevelChangeEvent()
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
        }
    }

    override fun get_name(): String {
        return "Lua_Events";
    }
}
