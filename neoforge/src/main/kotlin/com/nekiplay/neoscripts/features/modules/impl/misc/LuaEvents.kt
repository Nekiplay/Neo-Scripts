package com.nekiplay.neoscripts.features.modules.impl.misc

import com.mojang.brigadier.CommandDispatcher
import com.nekiplay.neoscripts.Main
import com.nekiplay.neoscripts.Main.LOGGER
import com.nekiplay.neoscripts.Main.LUA_MANAGER
import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.events.PacketEvent
import com.nekiplay.neoscripts.events.main.Callback
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.render.WorldRendererObject
import com.nekiplay.neoscripts.features.modules.ClientModule
import com.nekiplay.neoscripts.sugar.getFormattedString
import com.nekiplay.neoscripts.sugar.getJsonString
import com.nekiplay.neoscripts.utils.render.RenderHelper
import com.nekiplay.neoscripts.utils.render.WorldRenderExtractionCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.ClientSuggestionProvider
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientChatEvent
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import org.luaj.vm2.LuaValue
import java.lang.reflect.Field


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
                    script.onServerSideSetTimeEvent(packet.dayTime, packet.gameTime, packet.tickDayTime)
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
            is ClientboundPlayerRotationPacket -> {
                var rotationAllowed = true
                LUA_MANAGER?.scripts?.values?.forEach { script ->
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
                val oldState = mc?.level?.getBlockState(packet.pos)
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
                val packet = event.packet as ClientboundSetTitleTextPacket

                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    if (!script.onActionBar(packet.text.getFormattedString())) {
                        allowed = false
                    }
                }

                allowed
            }

            is ClientboundSetSubtitleTextPacket -> {
                var allowed = true
                val packet = event.packet as ClientboundSetTitleTextPacket

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

                LUA_MANAGER?.scripts?.values?.forEach { script ->
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

        if (!allow) event.cancelled = true
    }


    @SubscribeEvent
    fun onClientTickEnd(event: ClientTickEvent.Post) {
        LUA_MANAGER?.scripts?.values?.forEach { script ->
            try {
                script.onClientTick()
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }

    @SubscribeEvent
    fun onClientTickStart(event: ClientTickEvent.Pre) {
        LUA_MANAGER?.scripts?.values?.forEach { script ->
            try {
                script.onClientTickPre()
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorldAfterSky(event: WorldRenderExtractionCallback) {
        val renderContext = WorldRendererObject(event.collector)
        LUA_MANAGER?.scripts?.values?.forEach { script ->
            try {
                script.onRenderTick(renderContext)
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }

    // ==================== Рендер 2D (HUD) ====================
    @SubscribeEvent
    fun onRenderGui(event: RenderGuiEvent.Post) {
        LUA_MANAGER?.scripts?.values?.forEach { script ->
            try {
                script.on2DRenderTick(event.guiGraphics)
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }

    // ==================== Клавиатура и мышь ====================
    @SubscribeEvent
    fun onKeyInput(event: InputEvent.Key) {
        var allow = true
        LUA_MANAGER?.scripts?.values?.forEach { script ->
            try {
                if (!script.onKeyEvent(event.key, event.action)) {
                    allow = false
                }
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }

    @SubscribeEvent
    fun onMouseInput(event: InputEvent.MouseButton.Pre) {
        var allow = true
        LUA_MANAGER?.scripts?.values?.forEach { script ->
            try {
                if (!script.onKeyEvent(event.button, event.action)) {
                    allow = false
                }
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }

    // ==================== Блоки (правый/левый клик) ====================
    @SubscribeEvent
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        if (event.side.isClient) {
            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onUseBlock(event.pos, event.hand)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
            if (!allow) event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onLeftClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
        if (event.side.isClient) {
            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onAttackBlock(event.pos, event.face, event.hand)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
            if (!allow) event.isCanceled = true
        }
    }

    // ==================== Чаты и команды ====================
    @SubscribeEvent
    fun onChatReceived(event: ClientChatReceivedEvent) {
        var allow = true
        val text = event.message.getFormattedString() // или getFormattedString()
        val json = event.message.getJsonString() // упрощённо
        LUA_MANAGER?.scripts?.values?.forEach { script ->
            try {
                if (!script.onChatMessageEvent(text, event.isSystem, json)) {
                    allow = false
                }
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
        if (!allow) event.isCanceled = true
    }

    @SubscribeEvent
    fun onClientChat(event: ClientChatEvent) {
        LOGGER.info("ClientChatEvent fired: '${event.message}'")
        val message = event.message.trim()
        if (message.startsWith("/")) {
            // === Обработка команд ===
            val command = message.removePrefix("/")
            val cmdName = command.split(" ")[0]

            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onSendChatCommandEvent(command)) {
                        allow = false
                    }
                } catch (_: Exception) { }
            }

            if (!allow) {
                event.isCanceled = true
                return
            }

            for (script in LUA_MANAGER?.scripts?.values ?: emptyList()) {
                if (script.commandCallbacks.containsKey(cmdName) && script.commandDispatchers.containsKey(cmdName)) {
                    val player = Minecraft.getInstance().player ?: break
                    try {
                        val source = player.connection.suggestionsProvider
                        val dispatcher = script.commandDispatchers[cmdName]
                        @Suppress("UNCHECKED_CAST")
                        val result = (dispatcher as CommandDispatcher<ClientSuggestionProvider>).execute(command, source)
                        if (result >= 1) {
                            LOGGER?.info("${Main.LOG_PREFIX}Executing command: $command")
                        }
                    } catch (ex: Exception) {
                        LOGGER?.error("${Main.LOG_PREFIX}Error executing command $command", ex)
                    }
                    event.isCanceled = true   // команда обработана локально
                    return
                }
            }
            // если команда не найдена, оставляем событие неотменённым (уйдёт на сервер)
        } else {
            // === Обработка обычных сообщений ===
            var allow = true
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onSendChatMessageEvent(message)) {
                        allow = false
                    }
                } catch (_: Exception) { }
            }
            if (!allow) {
                event.isCanceled = true
            }
        }
    }

    private fun getCommandDispatcher(connection: Connection): CommandDispatcher<ClientSuggestionProvider>? {
        try {
            // 1. Получаем поле packetListener из Connection
            val packetListenerField: Field = Connection::class.java.getDeclaredField("packetListener")
            packetListenerField.isAccessible = true
            val packetListener: Any? = packetListenerField.get(connection) ?: return null

            // 2. Приводим к ClientPacketListener (если не он – вернём null)
            if (packetListener !is ClientPacketListener) return null

            // 3. Из ClientPacketListener вытаскиваем диспетчер.
            //    В маппингах 1.21.1 поле называется "commands" (CommandDispatcher<SharedSuggestionProvider>),
            //    но на самом деле у клиента оно имеет тип CommandDispatcher<ClientSuggestionProvider>.
            val commandsField = ClientPacketListener::class.java.getDeclaredField("commands")
            commandsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return commandsField.get(packetListener) as? CommandDispatcher<ClientSuggestionProvider>
        } catch (e: Exception) {
            Main.LOGGER?.error("${Main.LOG_PREFIX}Failed to get command dispatcher via reflection", e)
            return null
        }
    }

    // Обработчик входа на сервер – перерегистрация команд в новом диспетчере
    @SubscribeEvent
    fun onPlayerLoggingIn(event: ClientPlayerNetworkEvent.LoggingIn) {
        val dispatcher = getCommandDispatcher(event.connection) ?: return

        LUA_MANAGER?.scripts?.values?.forEach { script ->
            for (cmd in script.registeredCommands.values) {
                script.commandDispatchers[cmd.name] = dispatcher
                script.actualRegister(dispatcher, cmd.name)
            }
        }
    }

    override fun get_name(): String {
        return "Lua_Events";
    }
}