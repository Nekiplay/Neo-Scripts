package com.nekiplay.neoscripts.modules.impl.misc

import com.mojang.brigadier.CommandDispatcher
import com.nekiplay.neoscripts.Main.LUA_MANAGER
import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.events.PacketEvent
import com.nekiplay.neoscripts.events.main.Callback
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.render.WorldRendererObject
import com.nekiplay.neoscripts.modules.ClientModule
import com.nekiplay.neoscripts.sugar.getFormattedString
import com.nekiplay.neoscripts.sugar.getJsonString
import com.nekiplay.neoscripts.utils.render.RenderHelper
import com.nekiplay.neoscripts.utils.render.primitive.PrimitiveCollector
import com.nekiplay.neoscripts.utils.render.primitive.PrimitiveCollectorImpl
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
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientChatEvent
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
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
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
    fun onRenderWorldAfterSky(event: RenderLevelStageEvent.AfterSky) {
        val renderContext = WorldRendererObject(RenderHelper.collector)
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
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
    fun onMouseInput(event: InputEvent.MouseButton) {
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
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
    fun onSendCommand(event: ClientChatEvent) {
        var allow = true
        val command = event.message
        if (command.startsWith("/")) {
            val cmdName = command.substring(1).split(" ")[0]
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                try {
                    if (!script.onSendChatCommandEvent(command)) {
                        allow = false
                    }
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
        }
        if (!allow) {
            event.isCanceled = true
        }
    }

    override fun get_name(): String {
        return "Lua_Events";
    }
}
