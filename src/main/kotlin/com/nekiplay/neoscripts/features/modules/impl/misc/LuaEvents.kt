package com.nekiplay.neoscripts.features.modules.impl.misc

import com.mojang.brigadier.CommandDispatcher
import com.nekiplay.neoscripts.Main
import com.nekiplay.neoscripts.Main.LUA_MANAGER
import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.events.KeyEvent
import com.nekiplay.neoscripts.events.MouseButtonEvent
import com.nekiplay.neoscripts.events.SkyblockEvents
import com.nekiplay.neoscripts.events.network.PacketEvent
import com.nekiplay.neoscripts.events.player.AddItemInventoryEvent
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.render.WorldRendererObject
import com.nekiplay.neoscripts.features.modules.ClientModule
import com.nekiplay.neoscripts.mixins.packets.ServerboundMovePlayerPacketAccessor
import com.nekiplay.neoscripts.sugar.getFormattedString
import com.nekiplay.neoscripts.sugar.getJsonString
import com.nekiplay.neoscripts.utils.render.WorldRenderExtractionCallback
import com.nekiplay.neoscripts.utils.render.primitive.PrimitiveCollector
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import org.luaj.vm2.LuaValue


object LuaEvents : ClientModule() {
    override fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            LUA_MANAGER.scripts.values.forEach { script ->
                try {
                    script.onClientTick()
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
        }

        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            LUA_MANAGER.scripts.values.forEach { script ->
                try {
                    script.onClientTickPre()
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
        }

        WorldRenderExtractionCallback.EVENT.register({ context: PrimitiveCollector? ->
            val renderContext = WorldRendererObject(context)
            LUA_MANAGER.scripts.values.forEach { script ->
                try {
                    script.onRenderTick(renderContext)
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
        })

        KeyEvent.EVENT.register(KeyEvent.KeyCallback { keyEvent ->
            var allow = true
            LUA_MANAGER.scripts.values.forEach { script ->
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
            LUA_MANAGER.scripts.values.forEach { script ->
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
                LUA_MANAGER.scripts.values.forEach { script ->
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

            LUA_MANAGER.scripts.values.forEach { script ->
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
            LUA_MANAGER.scripts.values.forEach { script ->
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
            LUA_MANAGER.scripts.values.forEach { script ->
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

        ClientSendMessageEvents.ALLOW_COMMAND.register { command ->
            var allow = true
            val cmdName = command.split(" ")[0]
            LUA_MANAGER.scripts.values.forEach { script ->
                try {
                    if (!script.onSendChatCommandEvent(command)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }

            if (allow) {
                var founded = false
                LUA_MANAGER.scripts.values.forEach { script ->
                    if (script.commandCallbacks.containsKey(cmdName) && script.commandDispatchers.containsKey(cmdName)) {
                        founded = true
                        player?.let {
                            try {
                                val connection = player!!.connection
                                val source = connection.suggestionsProvider
                                // Выполняем команду
                                val dispatcher = script.commandDispatchers[cmdName]
                                @Suppress("UNCHECKED_CAST")
                                val result = (dispatcher as CommandDispatcher<SharedSuggestionProvider>).execute(command, source)
                                if (result >= 1) {
                                    Main.LOGGER.info("${Main.LOG_PREFIX}Executing command: $command")
                                    return@register false
                                }

                            } catch (e: Exception) {

                            }
                        }
                    }
                }
                if (founded) {
                    return@register false
                }
            }

            return@register allow
        }

        HudRenderCallback.EVENT.register(HudRenderCallback { context, _ ->
            LUA_MANAGER.scripts.values.forEach { script ->
                try {
                    script.on2DRenderTick(context)
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
        })

        SkyblockEvents.LOCATION_CHANGE.register { location ->
            LUA_MANAGER.scripts.values.forEach { script ->
                try {
                    script.onLocationChangeEvent(location)
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
        }
        PacketEvent.SEND.register { event ->
            val allow = when (event.packet) {
                is ServerboundMovePlayerPacket -> {
                    val packet = event.packet as ServerboundMovePlayerPacket
                    var allowed = true
                    LUA_MANAGER.scripts.values.forEach { script ->
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
            if (allow) InteractionResult.PASS else InteractionResult.FAIL
        }
        PacketEvent.SENT.register { event ->
            val allow = when (event.packet) {
                is ServerboundMovePlayerPacket -> {
                    val packet = event.packet as ServerboundMovePlayerPacket
                    var allowed = true
                    LUA_MANAGER.scripts.values.forEach { script ->
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
            if (allow) InteractionResult.PASS else InteractionResult.FAIL
        }
        PacketEvent.RECEIVE.register { event ->
            when (event.packet) {
                is ClientboundLevelParticlesPacket -> {
                    val packet = event.packet as ClientboundLevelParticlesPacket

                    LUA_MANAGER.scripts.values.forEach { script ->
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

                    LUA_MANAGER.scripts.values.forEach { script ->
                        script.onServerSideSetTimeEvent(packet.dayTime, packet.gameTime, packet.tickDayTime)
                    }
                }

                is ClientboundSoundPacket -> {
                    val packet = event.packet as ClientboundSoundPacket

                    LUA_MANAGER.scripts.values.forEach { script ->
                        script.onSoundPlay(packet.sound, packet.x, packet.y, packet.z, packet.pitch.toDouble(), packet.volume.toDouble())
                    }
                }
            }
            val allow = when (val packet = event.packet) {
                is ClientboundPlayerRotationPacket -> {
                    var rotationAllowed = true
                    LUA_MANAGER.scripts.values.forEach { script ->
                        if (!script.onServerSideRotationEvent(packet.xRot, packet.yRot)) {
                            rotationAllowed = false
                        }
                    }
                    rotationAllowed
                }

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

                    LUA_MANAGER.scripts.values.forEach { script ->
                        if (!script.onBlockUpdateEvent(table)) {
                            allowedBlockUpdate = false
                        }
                    }

                    allowedBlockUpdate
                }

                is ClientboundPlayerPositionPacket -> {
                    var rotationAllowed = true
                    var teleportAllowed = true

                    LUA_MANAGER.scripts.values.forEach { script ->
                        if (!script.onServerSideRotationEvent(packet.change.xRot(), packet.change.yRot())) {
                            rotationAllowed = false
                        }
                        if (!script.onServerSideTeleportEvent(
                                packet.change.position.x,
                                packet.change.position.y,
                                packet.change.position.z
                            )
                        ) {
                            teleportAllowed = false
                        }
                    }

                    rotationAllowed && teleportAllowed
                }

                else -> true
            }

            if (allow) InteractionResult.PASS else InteractionResult.FAIL
        }
        AddItemInventoryEvent.EVENT.register { event ->
            var allow = true
            LUA_MANAGER.scripts.values.forEach { script ->
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
    }

    override fun get_name(): String {
        return "Lua_Events";
    }
}
