package com.nekiplay.hypixelcry.features.modules.impl.misc

import com.nekiplay.hypixelcry.HypixelCry.LUA_MANAGER
import com.nekiplay.hypixelcry.events.KeyEvent
import com.nekiplay.hypixelcry.events.MouseButtonEvent
import com.nekiplay.hypixelcry.events.SkyblockEvents
import com.nekiplay.hypixelcry.events.network.PacketEvent
import com.nekiplay.hypixelcry.features.modules.ClientModule
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AfterTranslucent
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRotationS2CPacket
import net.minecraft.util.ActionResult

object LuaEvents: ClientModule() {
    override fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            LUA_MANAGER.onClientTick()
        }

        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            LUA_MANAGER.onClientTickPre()
        }

        WorldRenderEvents.AFTER_TRANSLUCENT.register(AfterTranslucent { context: WorldRenderContext? ->
            LUA_MANAGER.onRenderTick(context)
        })

        KeyEvent.EVENT.register(KeyEvent.KeyCallback { keyEvent ->
            LUA_MANAGER.onKeyEvent(keyEvent.key, keyEvent.action)
            ActionResult.PASS
        })

        MouseButtonEvent.EVENT.register(MouseButtonEvent.KeyCallback { mouseButtonEvent ->
            LUA_MANAGER.onKeyEvent(mouseButtonEvent.button, mouseButtonEvent.action)
            ActionResult.PASS
        })

        ClientReceiveMessageEvents.ALLOW_GAME.register(ClientReceiveMessageEvents.AllowGame { text, b ->
            LUA_MANAGER.onChatMessageEvent(text, b)
        })

        HudRenderCallback.EVENT.register(HudRenderCallback { context, _ ->
            LUA_MANAGER.on2DRenderTick(context)
        })

        SkyblockEvents.LOCATION_CHANGE.register { location ->
            LUA_MANAGER.onLocationChangeEvent(location)
        }

        PacketEvent.RECEIVE.register { event ->
            val allow = when (val packet = event.packet) {
                is PlayerRotationS2CPacket -> LUA_MANAGER.onServerSideRotationEvent(packet.xRot, packet.yRot)
                is PlayerPositionLookS2CPacket -> {
                    val rotationAllowed = LUA_MANAGER.onServerSideRotationEvent(packet.change.yaw, packet.change.pitch)
                    val teleportAllowed = LUA_MANAGER.onServerSideTeleportEvent(
                        packet.change.position.x,
                        packet.change.position.y,
                        packet.change.position.z
                    )
                    rotationAllowed && teleportAllowed
                }
                else -> true
            }

            if (allow) ActionResult.PASS else ActionResult.FAIL
        }
    }

    override fun get_name(): String {
        return "Lua_Events";
    }
}