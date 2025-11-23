package com.nekiplay.hypixelcry.features.modules.impl.misc

import com.nekiplay.hypixelcry.HypixelCry.LUA_MANAGER
import com.nekiplay.hypixelcry.events.KeyEvent
import com.nekiplay.hypixelcry.events.MouseButtonEvent
import com.nekiplay.hypixelcry.events.SkyblockEvents
import com.nekiplay.hypixelcry.events.network.PacketEvent
import com.nekiplay.hypixelcry.events.world.BlockUpdateEvent
import com.nekiplay.hypixelcry.events.world.BlockUpdateEvent.BlockUpdateCallback
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.hypixelcry.features.lua.utils.BlockUtil
import com.nekiplay.hypixelcry.features.modules.ClientModule
import com.nekiplay.hypixelcry.utils.render.WorldRenderExtractionCallback
import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRotationS2CPacket
import net.minecraft.util.ActionResult
import org.luaj.vm2.LuaValue


object LuaEvents: ClientModule() {
    override fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            LUA_MANAGER.onClientTick()
        }

        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            LUA_MANAGER.onClientTickPre()
        }

        WorldRenderExtractionCallback.EVENT.register( { context: PrimitiveCollector? ->
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

        ClientReceiveMessageEvents.ALLOW_GAME.register(ClientReceiveMessageEvents.AllowGame { text, overlay ->
            LUA_MANAGER.onChatMessageEvent(text, overlay)
        })

        ClientSendMessageEvents.ALLOW_CHAT.register(ClientSendMessageEvents.AllowChat { text ->
            LUA_MANAGER.onSendChatMessageEvent(text)
        });

        ClientSendMessageEvents.ALLOW_COMMAND.register(ClientSendMessageEvents.AllowCommand { command ->
            LUA_MANAGER.onSendChatCommandEvent(command)
        });

        HudRenderCallback.EVENT.register(HudRenderCallback { context, _ ->
            LUA_MANAGER.on2DRenderTick(context)
        })

        SkyblockEvents.LOCATION_CHANGE.register { location ->
            LUA_MANAGER.onLocationChangeEvent(location)
        }

        BlockUpdateEvent.EVENT.register(BlockUpdateCallback { event ->
            val blockPos = event.blockPos
            val oldState = event.old
            val newState = event.new

            val table = LuaValue.tableOf()
            val oldL = LuaBlockState(oldState)
            val newL = LuaBlockState(newState)

            table.set("x", blockPos.x)
            table.set("y", blockPos.y)
            table.set("z", blockPos.z)
            table.set("old", oldL)
            table.set("new", newL)


            val allow = LUA_MANAGER.onBlockUpdateEvent(table)
            if (allow) {
                ActionResult.PASS
            } else {
                ActionResult.FAIL
            }
        })

        PacketEvent.RECEIVE.register { event ->
            val allow = when (val packet = event.packet) {
                is PlayerRotationS2CPacket -> LUA_MANAGER.onServerSideRotationEvent(packet.yaw, packet.pitch)
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