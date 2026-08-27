package com.nekiplay.neoscripts.server.features.modules.misc

import com.nekiplay.neoscripts.ServerMain
import com.nekiplay.neoscripts.server.features.lua.LuaServerScript
import com.nekiplay.neoscripts.server.features.lua.objects.ServerWorldObject
import com.nekiplay.neoscripts.server.features.modules.ServerModule
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponent
import com.nekiplay.neoscripts.common.network.NeoLuaPacketPayload
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.BlockEvents
import net.fabricmc.fabric.api.event.player.ItemEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.PlayerChatMessage
import net.minecraft.server.MinecraftServer as McServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import java.util.concurrent.CompletableFuture

object LuaEvents : ServerModule() {

    override fun init() {
        registerTickEvents()
        registerLifecycleEvents()
        registerInteractionEvents()
        registerMessageEvents()
        registerPacketEvents()
    }

    private fun registerTickEvents() {
        ServerTickEvents.START_SERVER_TICK.register { server ->
            ServerMain.LUA_MANAGER?.scripts?.values?.forEach { script ->
                if (script is LuaServerScript) {
                    script.onServerTickPre()
                }
            }
        }
        ServerTickEvents.END_SERVER_TICK.register { server ->
            ServerMain.LUA_MANAGER?.scripts?.values?.forEach { script ->
                if (script is LuaServerScript) {
                    script.onServerTick()
                }
            }
        }

        ServerTickEvents.START_LEVEL_TICK.register { level ->
            val obj = ServerWorldObject(level)
            ServerMain.LUA_MANAGER?.scripts?.values?.forEach { script ->
                if (script is LuaServerScript) {
                    script.onServerWorldTickPre(obj)
                }
            }
        }
        ServerTickEvents.END_LEVEL_TICK.register { level ->
            val obj = ServerWorldObject(level)
            ServerMain.LUA_MANAGER?.scripts?.values?.forEach { script ->
                if (script is LuaServerScript) {
                    script.onServerWorldTick(obj)
                }
            }
        }
    }

    /**
     * SERVER_STOPPING срабатывает при выключении сервера до выгрузки миров:
     * все уровни ещё загружены и доступны скриптам.
     */
    private fun registerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            for (level in server.allLevels) {
                val obj = ServerWorldObject(level)
                ServerMain.LUA_MANAGER?.scripts?.values?.forEach { script ->
                    if (script is LuaServerScript) {
                        script.onServerStopping(obj)
                    }
                }
            }
        }
    }

    /**
     * Вызывается из ServerMain на первом тике после старта: сервер полностью
     * запущен, все измерения загружены, автозагрузка выполнена.
     * worldLoaded диспатчится здесь для каждого измерения — события загрузки
     * миров до этого момента недоступны (скрипты ещё не загружены).
     */
    fun dispatchServerStarted(server: McServer) {
        dispatchNotify({ it.hasServerStartedCallbacks }, { it.onServerStarted() })
        for (level in server.allLevels) {
            dispatchWorldLoaded(level)
        }
    }

    private fun dispatchWorldLoaded(level: ServerLevel) {
        val obj = ServerWorldObject(level)
        dispatchNotify({ it.hasWorldLoadedCallbacks }, { it.onWorldLoaded(obj) })
    }

    // ═══ Хелперы диспетчеризации ═══

    /**
     * Возвращает false только если событие используется скриптом
     * и хотя бы один колбэк вернул false. Иначе - true (пропуск по умолчанию).
     */
    private inline fun dispatchCancellable(
        has: (LuaServerScript) -> Boolean,
        fire: (LuaServerScript) -> Boolean
    ): Boolean {
        val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts() ?: return true

        var allow = true
        for (script in scripts) {
            if (script !is LuaServerScript || !has(script)) continue
            try {
                if (!fire(script)) allow = false
            } catch (e: Exception) {
                ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in server event callback in ${script.scriptName}", e)
            }
        }
        return allow
    }

    private fun dispatchResult(allow: Boolean): InteractionResult =
        if (allow) InteractionResult.PASS else InteractionResult.FAIL

    /**
     * Для новых BlockEvents/ItemEvents событий: @Nullable контракт.
     * null = проброс дальше к vanilla, не-PASS результат = "обработано".
     * Поэтому отмена - FAIL, а пропуск - именно null.
     */
    private fun dispatchNullable(allow: Boolean): InteractionResult? =
        if (allow) null else InteractionResult.FAIL

    private inline fun dispatchNotify(
        has: (LuaServerScript) -> Boolean,
        fire: (LuaServerScript) -> Unit
    ) {
        val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts() ?: return

        for (script in scripts) {
            if (script !is LuaServerScript || !has(script)) continue
            try {
                fire(script)
            } catch (e: Exception) {
                ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in server event callback in ${script.scriptName}", e)
            }
        }
    }

    private inline fun dispatchPick(
        has: (LuaServerScript) -> Boolean,
        fire: (LuaServerScript) -> ItemStack?
    ): ItemStack? {
        val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts() ?: return null

        var override: ItemStack? = null
        for (script in scripts) {
            if (script !is LuaServerScript || !has(script)) continue
            try {
                val res = fire(script)
                if (override == null) override = res
            } catch (e: Exception) {
                ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in server event callback in ${script.scriptName}", e)
            }
        }
        return override
    }

    // ═══ Interaction events ═══

    private fun registerInteractionEvents() {
        // Общие fabric-ивенты стреляют и на клиенте, и на сервере.
        // Клиентские вызовы (ClientLevel) фильтруются проверкой level is ServerLevel:
        // в одиночке интегрированный сервер передаст ServerLevel и событие сработает,
        // а на клиентской стороне вернется PASS без диспетчеризации.

        AttackBlockCallback.EVENT.register(AttackBlockCallback { player, level, hand, pos, direction ->
            if (level !is ServerLevel) return@AttackBlockCallback InteractionResult.PASS
            dispatchResult(dispatchCancellable({ it.hasAttackBlockCallbacks }, { it.onAttackBlock(player, level, hand, pos, direction) }))
        })

        UseBlockCallback.EVENT.register(UseBlockCallback { player, level, hand, hit ->
            if (level !is ServerLevel) return@UseBlockCallback InteractionResult.PASS
            dispatchResult(dispatchCancellable({ it.hasUseBlockCallbacks }, { it.onUseBlock(player, level, hand, hit) }))
        })

        BlockEvents.USE_ITEM_ON.register(BlockEvents.UseItemOnCallback { stack, state, level, pos, player, hand, _ ->
            if (level !is ServerLevel) return@UseItemOnCallback null
            dispatchNullable(dispatchCancellable({ it.hasUseItemOnBlockCallbacks }, { it.onUseItemOnBlock(stack, state, level, pos, player, hand) }))
        })

        BlockEvents.USE_WITHOUT_ITEM.register(BlockEvents.UseWithoutItemCallback { state, level, pos, player, _ ->
            if (level !is ServerLevel) return@UseWithoutItemCallback null
            dispatchNullable(dispatchCancellable({ it.hasUseWithoutItemCallbacks }, { it.onUseWithoutItem(state, level, pos, player) }))
        })

        PlayerBlockBreakEvents.BEFORE.register(PlayerBlockBreakEvents.Before { level, player, pos, state, blockEntity ->
            if (level !is ServerLevel) return@Before true
            dispatchCancellable({ it.hasBreakBlockBeforeCallbacks }, { it.onBreakBlockBefore(level, player, pos, state, blockEntity) })
        })

        PlayerBlockBreakEvents.AFTER.register(PlayerBlockBreakEvents.After { level, player, pos, state, blockEntity ->
            if (level !is ServerLevel) return@After
            dispatchNotify({ it.hasBreakBlockAfterCallbacks }, { it.onBreakBlockAfter(level, player, pos, state, blockEntity) })
        })

        PlayerBlockBreakEvents.CANCELED.register(PlayerBlockBreakEvents.Canceled { level, player, pos, state, blockEntity ->
            if (level !is ServerLevel) return@Canceled
            dispatchNotify({ it.hasBreakBlockCancelCallbacks }, { it.onBreakBlockCancel(level, player, pos, state, blockEntity) })
        })

        AttackEntityCallback.EVENT.register(AttackEntityCallback { player, level, hand, entity, hit ->
            if (level !is ServerLevel) return@AttackEntityCallback InteractionResult.PASS
            dispatchResult(dispatchCancellable({ it.hasAttackEntityCallbacks }, { it.onAttackEntity(player, level, hand, entity, hit) }))
        })

        UseEntityCallback.EVENT.register(UseEntityCallback { player, level, hand, entity, hit ->
            if (level !is ServerLevel) return@UseEntityCallback InteractionResult.PASS
            dispatchResult(dispatchCancellable({ it.hasUseEntityCallbacks }, { it.onUseEntity(player, level, hand, entity, hit) }))
        })

        UseItemCallback.EVENT.register(UseItemCallback { player, level, hand ->
            if (level !is ServerLevel) return@UseItemCallback InteractionResult.PASS
            dispatchResult(dispatchCancellable({ it.hasUseItemCallbacks }, { it.onUseItem(player, level, hand) }))
        })

        ItemEvents.USE.register(ItemEvents.UseCallback { level, player, hand ->
            if (level !is ServerLevel) return@UseCallback null
            dispatchNullable(dispatchCancellable({ it.hasUseItemCallbacks }, { it.onUseItem(player, level, hand) }))
        })

        ItemEvents.USE_ON.register(ItemEvents.UseOnCallback { context ->
            if (context.level !is ServerLevel) return@UseOnCallback null
            dispatchNullable(dispatchCancellable({ it.hasUseItemOnCallbacks }, { it.onUseItemOn(context) }))
        })

        PlayerPickItemEvents.BLOCK.register(PlayerPickItemEvents.PickItemFromBlock { player, pos, state, includeData ->
            if (player.level() !is ServerLevel) return@PickItemFromBlock null
            dispatchPick({ it.hasPickItemFromBlockCallbacks }, { it.onPickItemFromBlock(player, pos, state, includeData) })
        })

        PlayerPickItemEvents.ENTITY.register(PlayerPickItemEvents.PickItemFromEntity { player, entity, includeData ->
            if (player.level() !is ServerLevel) return@PickItemFromEntity null
            dispatchPick({ it.hasPickItemFromEntityCallbacks }, { it.onPickItemFromEntity(player, entity, includeData) })
        })
    }

    private fun registerMessageEvents() {
        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE) { sender: ServerPlayer?, message: Component ->
            val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts()
            if (scripts == null) return@register message

            val senderEntity = sender?.let { LuaEntity(it) }
            val messageComponent = LuaComponent(message)
            var result = message
            for (script in scripts) {
                if (script !is LuaServerScript) continue
                if (!script.hasMessageDecoratorContentCallbacks) continue
                try {
                    val luaScript = script as LuaServerScript
                    val res = luaScript.onMessageDecoratorContent(senderEntity, result, messageComponent)
                    if (res != null) result = res
                } catch (e: Exception) {
                    ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in message decorator content callback in ${script.scriptName}", e)
                }
            }
            result
        }

        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.STYLING_PHASE) { sender: ServerPlayer?, message: Component ->
            val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts()
            if (scripts == null) return@register message

            val senderEntity = sender?.let { LuaEntity(it) }
            val messageComponent = LuaComponent(message)
            var result = message
            for (script in scripts) {
                if (script !is LuaServerScript) continue
                if (!script.hasMessageDecoratorStylingCallbacks) continue
                try {
                    val luaScript = script as LuaServerScript
                    val res = luaScript.onMessageDecoratorStyling(senderEntity, result, messageComponent)
                    if (res != null) result = res
                } catch (e: Exception) {
                    ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in message decorator styling callback in ${script.scriptName}", e)
                }
            }
            result
        }

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message: PlayerChatMessage, sender: ServerPlayer, boundChatType: ChatType.Bound ->
            val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts() ?: return@register true

            val senderEntity = LuaEntity(sender)
            val messageComponent = LuaComponent(message.decoratedContent())
            var allow = true
            for (script in scripts) {
                if (script !is LuaServerScript) continue
                if (!script.hasAllowChatMessageCallbacks) continue
                try {
                    val luaScript = script as LuaServerScript
                    if (!luaScript.onAllowChatMessage(message, senderEntity, messageComponent, boundChatType)) allow = false
                } catch (e: Exception) {
                    ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in allow chat message callback in ${script.scriptName}", e)
                }
            }
            allow
        }

        ServerMessageEvents.ALLOW_GAME_MESSAGE.register { server: McServer, message: Component, overlay: Boolean ->
            val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts() ?: return@register true

            val messageComponent = LuaComponent(message)
            var allow = true
            for (script in scripts) {
                if (script !is LuaServerScript || !script.hasAllowGameMessageCallbacks) continue
                try {
                    if (!script.onAllowGameMessage(server, messageComponent, overlay)) allow = false
                } catch (e: Exception) {
                    ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in allow game message callback in ${script.scriptName}", e)
                }
            }
            allow
        }

        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register { message: PlayerChatMessage, source: CommandSourceStack, boundChatType: ChatType.Bound ->
            val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts() ?: return@register true

            val sourceEntity = source.entity?.let { LuaEntity(it) }
            val messageComponent = LuaComponent(message.decoratedContent())
            var allow = true
            for (script in scripts) {
                if (script !is LuaServerScript) continue
                if (!script.hasAllowCommandMessageCallbacks) continue
                try {
                    val luaScript = script as LuaServerScript
                    if (!luaScript.onAllowCommandMessage(message, sourceEntity, messageComponent, boundChatType)) allow = false
                } catch (e: Exception) {
                    ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in allow command message callback in ${script.scriptName}", e)
                }
            }
            allow
        }

        ServerMessageEvents.CHAT_MESSAGE.register { message: PlayerChatMessage, sender: ServerPlayer, boundChatType: ChatType.Bound ->
            val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts()
            if (scripts == null) return@register

            val senderEntity = LuaEntity(sender)
            val messageComponent = LuaComponent(message.decoratedContent())
            for (script in scripts) {
                if (script !is LuaServerScript) continue
                if (!script.hasChatMessageCallbacks) continue
                try {
                    val luaScript = script as LuaServerScript
                    luaScript.onChatMessage(message, senderEntity, messageComponent, boundChatType)
                } catch (e: Exception) {
                    ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in chat message callback in ${script.scriptName}", e)
                }
            }
        }

        ServerMessageEvents.GAME_MESSAGE.register { server: McServer, message: Component, overlay: Boolean ->
            val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts()
            if (scripts == null) return@register

            val messageComponent = LuaComponent(message)
            for (script in scripts) {
                if (script !is LuaServerScript || !script.hasGameMessageCallbacks) continue
                try {
                    script.onGameMessage(server, messageComponent, overlay)
                } catch (e: Exception) {
                    ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in game message callback in ${script.scriptName}", e)
                }
            }
        }

        ServerMessageEvents.COMMAND_MESSAGE.register { message: PlayerChatMessage, source: CommandSourceStack, boundChatType: ChatType.Bound ->
            val scripts = ServerMain.LUA_MANAGER?.getLoadedScripts()
            if (scripts == null) return@register

            val sourceEntity = source.entity?.let { LuaEntity(it) }
            val messageComponent = LuaComponent(message.decoratedContent())
            for (script in scripts) {
                if (script !is LuaServerScript) continue
                if (!script.hasCommandMessageCallbacks) continue
                try {
                    val luaScript = script as LuaServerScript
                    luaScript.onCommandMessage(message, sourceEntity, messageComponent, boundChatType)
                } catch (e: Exception) {
                    ServerMain.LOGGER?.error("${ServerMain.LOG_PREFIX}Error in command message callback in ${script.scriptName}", e)
                }
            }
        }
    }

    private fun registerPacketEvents() {
        try {
            PayloadTypeRegistry.clientboundPlay().register(NeoLuaPacketPayload.TYPE, NeoLuaPacketPayload.CODEC)
        } catch (_: Exception) {}
        try {
            PayloadTypeRegistry.serverboundPlay().register(NeoLuaPacketPayload.TYPE, NeoLuaPacketPayload.CODEC)
        } catch (_: Exception) {}
        try {
            ServerPlayNetworking.registerGlobalReceiver(NeoLuaPacketPayload.TYPE) { payload, context ->
                val channel = payload.channel
                val json = payload.json
                val player = context.player()
                // dispatch on server thread
                context.server().execute {
                    ServerMain.LUA_MANAGER?.scripts?.values?.forEach { script ->
                        if (script is LuaServerScript && script.hasCustomPacketCallbacks) {
                            try { script.onCustomPacket(channel, json, player) } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }


    override fun get_name(): String {
        return "Lua_Events";
    }
}
