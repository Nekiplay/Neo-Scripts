package com.nekiplay.hypixelcry.features.modules.impl.misc

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
import com.nekiplay.hypixelcry.utils.render.WorldRenderExtractionCallback
import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
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
            LUA_MANAGER.onClientTick()
        }

        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            LUA_MANAGER.onClientTickPre()
        }

        WorldRenderExtractionCallback.EVENT.register( { context: PrimitiveCollector? ->
            LUA_MANAGER.onRenderTick(context)
        })

        KeyEvent.EVENT.register(KeyEvent.KeyCallback { keyEvent ->
            val allow = LUA_MANAGER.onKeyEvent(keyEvent.key, keyEvent.action)
            if (allow) {
                InteractionResult.PASS
            }
            else {
                InteractionResult.FAIL
            }
        })

        MouseButtonEvent.EVENT.register(MouseButtonEvent.KeyCallback { mouseButtonEvent ->
            val allow =  LUA_MANAGER.onKeyEvent(mouseButtonEvent.button, mouseButtonEvent.action)
            if (allow) {
                InteractionResult.PASS
            }
            else {
                InteractionResult.FAIL
            }
        })

        UseBlockCallback.EVENT.register(UseBlockCallback { player: Player, world: Level, hand: InteractionHand, hitResult: BlockHitResult ->
            var allow = true
            if (hitResult.type == HitResult.Type.BLOCK || hitResult.type == HitResult.Type.MISS) {
                allow = LUA_MANAGER.onUseBlock(hitResult.blockPos, hand)
            }
            if (allow) {
                InteractionResult.PASS
            }
            else {
                InteractionResult.FAIL
            }
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

            table.set("x", blockPos.x)
            table.set("y", blockPos.y)
            table.set("z", blockPos.z)
            if (oldState != null) {
                table.set("old", LuaBlockState(oldState))
            }
            if (newState != null) {
                table.set("new", LuaBlockState(newState))
            }


            val allow = LUA_MANAGER.onBlockUpdateEvent(table)
            if (allow) {
                InteractionResult.PASS
            } else {
                InteractionResult.FAIL
            }
        })

        PacketEvent.RECEIVE.register { event ->
            val allow = when (val packet = event.packet) {
                is ClientboundPlayerRotationPacket -> LUA_MANAGER.onServerSideRotationEvent(packet.xRot, packet.yRot)
                is ClientboundPlayerPositionPacket -> {
                    val rotationAllowed = LUA_MANAGER.onServerSideRotationEvent(packet.change.xRot(), packet.change.yRot())
                    val teleportAllowed = LUA_MANAGER.onServerSideTeleportEvent(
                        packet.change.position.x,
                        packet.change.position.y,
                        packet.change.position.z
                    )
                    rotationAllowed && teleportAllowed
                }
                else -> true
            }

            if (allow) InteractionResult.PASS else InteractionResult.FAIL
        }

        AddItemInventoryEvent.EVENT.register { event ->
            val allow = LUA_MANAGER.onInventoryItemAdd(event.slot, event.item)

            if (allow) InteractionResult.PASS else InteractionResult.FAIL
        }
    }

    override fun get_name(): String {
        return "Lua_Events";
    }
}