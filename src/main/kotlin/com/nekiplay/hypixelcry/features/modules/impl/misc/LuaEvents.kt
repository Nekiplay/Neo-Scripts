package com.nekiplay.hypixelcry.features.modules.impl.misc

import com.mojang.brigadier.CommandDispatcher
import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.HypixelCry.LUA_MANAGER
import com.nekiplay.hypixelcry.HypixelCry.mc
import com.nekiplay.hypixelcry.events.KeyEvent
import com.nekiplay.hypixelcry.events.MouseButtonEvent
import com.nekiplay.hypixelcry.events.SkyblockEvents
import com.nekiplay.hypixelcry.events.network.PacketEvent
import com.nekiplay.hypixelcry.events.player.AddItemInventoryEvent
import com.nekiplay.hypixelcry.events.world.BlockUpdateEvent
import com.nekiplay.hypixelcry.events.world.BlockUpdateEvent.BlockUpdateCallback
import com.nekiplay.hypixelcry.features.lua.LuaScript
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.hypixelcry.features.lua.objects.render.WorldRendererObject
import com.nekiplay.hypixelcry.features.modules.ClientModule
import com.nekiplay.hypixelcry.imgui.ImguiLoader
import com.nekiplay.hypixelcry.sugar.getFormattedString
import com.nekiplay.hypixelcry.sugar.getJsonString
import com.nekiplay.hypixelcry.utils.render.WorldRenderExtractionCallback
import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.client.Minecraft
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction8
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
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
                                    HypixelCry.LOGGER.info("${HypixelCry.LOG_PREFIX}Executing command: $command")
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
        PacketEvent.RECEIVE.register { event ->
            if (event.packet is ClientboundLevelParticlesPacket) {
                val packet = event.packet as ClientboundLevelParticlesPacket

                LUA_MANAGER.scripts.values.forEach { script ->
                    try {
                        script.onSpawnParticleEvent(BuiltInRegistries.PARTICLE_TYPE.getId(packet.particle.type), packet.x, packet.y, packet.z, packet.xDist, packet.yDist, packet.zDist, packet.maxSpeed, packet.count)
                    } catch (e: Exception) {
                        // Обработка ошибок
                    }
                }
            }
            else if (event.packet is ClientboundBlockUpdatePacket) {
                val packet = event.packet as ClientboundBlockUpdatePacket

                val table = LuaValue.tableOf()

                table.set("x", packet.pos.x)
                table.set("y", packet.pos.y)
                table.set("z", packet.pos.z)
                val oldState = mc.level?.getBlockState(packet.pos)
                if (oldState != null) {
                    table.set("old", LuaBlockState(oldState))
                }
                if (packet.blockState != null) {
                    table.set("new", LuaBlockState(packet.blockState))
                }

                LUA_MANAGER.scripts.values.forEach { script ->
                    try {
                        script.onBlockUpdateEvent(table)
                    } catch (e: Exception) {
                        // Обработка ошибок
                    }
                }
            }
            val allow = when (val packet = event.packet) {
                is ClientboundPlayerRotationPacket -> {
                    var rotationAllowed = true
                    LUA_MANAGER.scripts.values.forEach { script ->
                        try {
                            if (!script.onServerSideRotationEvent(packet.xRot, packet.yRot)) {
                                rotationAllowed = false
                            }
                        } catch (e: Exception) {
                            // Обработка ошибок
                        }
                    }
                    rotationAllowed
                }

                is ClientboundPlayerPositionPacket -> {
                    var rotationAllowed = true
                    var teleportAllowed = true

                    LUA_MANAGER.scripts.values.forEach { script ->
                        try {
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
                        } catch (e: Exception) {
                            // Обработка ошибок
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
