package com.nekiplay.hypixelcry.features.modules.impl.misc

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.HypixelCry.LUA_MANAGER
import com.nekiplay.hypixelcry.events.KeyEvent
import com.nekiplay.hypixelcry.events.MouseButtonEvent
import com.nekiplay.hypixelcry.events.SkyblockEvents
import com.nekiplay.hypixelcry.events.network.PacketEvent
import com.nekiplay.hypixelcry.events.player.AddItemInventoryEvent
import com.nekiplay.hypixelcry.events.world.BlockUpdateEvent
import com.nekiplay.hypixelcry.events.world.BlockUpdateEvent.BlockUpdateCallback
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.hypixelcry.features.modules.ClientModule
import com.nekiplay.hypixelcry.sugar.getFormattedString
import com.nekiplay.hypixelcry.utils.render.WorldRenderExtractionCallback
import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import org.luaj.vm2.LuaValue


object LuaEvents: ClientModule() {
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

        WorldRenderExtractionCallback.EVENT.register( { context: PrimitiveCollector? ->
            LUA_MANAGER.scripts.values.forEach { script ->
                try {
                    script.onRenderTick(context)
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

        ClientReceiveMessageEvents.ALLOW_GAME.register(ClientReceiveMessageEvents.AllowGame { text, overlay ->
            var allow = true
            LUA_MANAGER.scripts.values.forEach { script ->
                try {
                    if (!script.onChatMessageEvent(text.getFormattedString(), overlay)) {
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

        ClientSendMessageEvents.ALLOW_COMMAND.register(ClientSendMessageEvents.AllowCommand { command ->
            var allow = true
            LUA_MANAGER.scripts.values.forEach { script ->
                try {
                    if (!script.onSendChatCommandEvent(command)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }

            val cmdName = command.split(" ")[0]

            // Проверяем, есть ли такая команда в каком-либо из скриптов
            val scriptExists = LUA_MANAGER.scripts.values.find { it.commandCallbacks.containsKey(cmdName) }

            if (scriptExists != null && allow) {
                val client = Minecraft.getInstance()
                val player = client.player

                if (player != null) {
                    // Выполняем в основном потоке клиента
                    client.execute {
                        try {
                            val connection = player.connection
                            val source = connection.suggestionsProvider
                            // Выполняем команду
                            @Suppress("UNCHECKED_CAST")
                            //(dispatcher as CommandDispatcher<SharedSuggestionProvider>).execute(command, source)
                            val dispatcher2 = scriptExists.commandDispatchers[cmdName]
                            dispatcher2?.execute(command, source as FabricClientCommandSource)
                            HypixelCry.LOGGER.info("${HypixelCry.LOG_PREFIX}Executing command: $command")

                        } catch (e: Exception) {
                            player.displayClientMessage(Component.literal("${HypixelCry.LOG_PREFIX}§cError executing Lua command: ${e.message}"), false)
                            e.printStackTrace()
                        }
                    }
                }
            }

            allow
        })

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

        BlockUpdateEvent.EVENT.register(BlockUpdateCallback { event ->
            val blockPos = event.blockPos
            val oldState = event.old
            val newState = event.new

            val table = LuaValue.tableOf()

            table.set("x", blockPos.x)
            table.set("y", blockPos.y)
            table.set("z", blockPos.z)
            if (oldState != null) {
                table.set("old", LuaBlockState(oldState))
            }
            if (newState != null) {
                table.set("new", LuaBlockState(newState))
            }

            var allow = true
            LUA_MANAGER.scripts.values.forEach { script ->
                try {
                    if (!script.onBlockUpdateEvent(table)) {
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

        PacketEvent.RECEIVE.register { event ->
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
                            )) {
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
                    if (!script.onInventoryItemAdd(event.slot, event.item)) {
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
