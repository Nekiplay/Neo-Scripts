package com.nekiplay.neoscripts.features.modules.impl.misc

import com.mojang.brigadier.CommandDispatcher
import com.nekiplay.neoscripts.Main
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
import net.minecraft.client.multiplayer.ClientSuggestionProvider
import net.minecraft.core.registries.BuiltInRegistries
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
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientChatEvent
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import org.luaj.vm2.LuaValue


@EventBusSubscriber(modid = Main.ID, value = [Dist.CLIENT])
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
    fun onSendChat(event: ClientChatEvent) {
        var allow = true
        LUA_MANAGER?.scripts?.values?.forEach { script ->
            try {
                if (!script.onSendChatMessageEvent(event.message)) {
                    allow = false
                }
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
        if (!allow) event.isCanceled = true
    }

    @SubscribeEvent
    fun onSendCommand(event: ClientChatEvent) {
        val message = event.message.trim()
        if (!message.startsWith("/")) return

        val command = message.substring(1) // строка команды без слеша
        val cmdName = command.split(" ")[0]
        var allow = true

        // 1. Даём Lua-скриптам возможность запретить команду
        LUA_MANAGER?.scripts?.values?.forEach { script ->
            try {
                if (!script.onSendChatCommandEvent(command)) {
                    allow = false
                }
            } catch (e: Exception) {
                // игнорируем ошибки в Lua
            }
        }

        if (!allow) {
            event.isCanceled = true
            return
        }

        // 2. Ищем скрипт, в котором зарегистрирована эта команда
        var found = false
        for (script in LUA_MANAGER?.scripts?.values ?: emptyList()) {
            if (script.commandCallbacks.containsKey(cmdName) && script.commandDispatchers.containsKey(cmdName)) {
                found = true
                val player = Minecraft.getInstance().player ?: continue
                try {
                    val connection = player.connection
                    val source = connection.suggestionsProvider // ClientSuggestionProvider
                    val dispatcher = script.commandDispatchers[cmdName]
                    @Suppress("UNCHECKED_CAST")
                    val result = (dispatcher as CommandDispatcher<ClientSuggestionProvider>).execute(command, source)
                    if (result >= 1) {
                        Main.LOGGER?.info("${Main.LOG_PREFIX}Executing command: $command")
                    }
                } catch (ex: Exception) {
                    Main.LOGGER?.error("${Main.LOG_PREFIX}Error executing command $command", ex)
                }
                break // команда обработана первым подходящим скриптом
            }
        }

        if (found) {
            event.isCanceled = true // не даём команде уйти на сервер
        }
    }

    override fun get_name(): String {
        return "Lua_Events";
    }
}
