package com.nekiplay.hypixelcry.features.lua

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.RootCommandNode
import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.text.LuaComponentBuilder
import com.nekiplay.hypixelcry.features.lua.objects.misc.CatboostLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.Creator
import com.nekiplay.hypixelcry.features.lua.objects.misc.DJLLuaTrainer
import com.nekiplay.hypixelcry.features.lua.objects.misc.EncodingLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.FFILib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ImGuiLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.JsonLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.TCPLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ThreadLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.http.HttpClientLib
import com.nekiplay.hypixelcry.features.lua.objects.modules.ModulesObject
import com.nekiplay.hypixelcry.features.lua.objects.player.PlayerObject
import com.nekiplay.hypixelcry.features.lua.objects.render.TwoRenderObject
import com.nekiplay.hypixelcry.features.lua.objects.render.WorldRendererObject
import com.nekiplay.hypixelcry.features.lua.objects.world.WorldObject
import com.nekiplay.hypixelcry.utils.Location
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction
import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.synchronization.ArgumentTypeInfos
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundCommandsPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import org.luaj.vm2.lib.jse.LuajavaLib
import java.util.concurrent.ConcurrentHashMap

class LuaScript(val scriptName: String, private val luaManager: LuaManager) {
    // Локальный стек загрузки для этого конкретного экземпляра скрипта
    private val loadingStack = java.util.Stack<String>()
    private val systemModuleCache = ConcurrentHashMap<String, LuaValue>()
    // Локальный граф зависимостей для этого конкретного экземпляра скрипта
    // Ключ: имя файла, Значение: список имен, которые этот файл запросил через require
    val localDependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()

    // Events
    private val clientTickCallbacks = ArrayList<LuaValue>()
    private val clientPreTickCallbacks = ArrayList<LuaValue>()
    private val blockUpdateCallbacks = ArrayList<LuaValue>()
    private val renderWorldCallbacks = ArrayList<LuaValue>()
    private val render2DCallbacks = ArrayList<LuaValue>()
    private val keyEventCallbacks = ArrayList<LuaValue>()
    private val messageEventCallbacks = ArrayList<LuaValue>()
    private val onSendMessageEventCallbacks = ArrayList<LuaValue>()
    private val onSendCommandEventCallbacks = ArrayList<LuaValue>()
    private val useBlockCallbacks = ArrayList<LuaValue>()
    private val attackBlockCallbacks = ArrayList<LuaValue>()
    private val locationChangeCallbacks = ArrayList<LuaValue>()
    private val imguiRenderCallbacks = ArrayList<LuaValue>()
    private val inventoryItemChangeCallbacks = ArrayList<LuaValue>()
    private val particleCallbacks = ArrayList<LuaValue>()

    // Packet events
    private val serverSideRotationCallbacks = ArrayList<LuaValue>()
    private val serverSideTeleportCallbacks = ArrayList<LuaValue>()

    // Command events
    val commandCallbacks = ConcurrentHashMap<String, LuaValue>()
    val commandSuggestionsCallbacks = ConcurrentHashMap<String, LuaValue>()
    val commandDispatchers = ConcurrentHashMap<String, CommandDispatcher<FabricClientCommandSource>>()

    // Script events
    private val scriptUnloadCallbacks = ArrayList<LuaValue>()

    // Synchronize only when needed
    private val callbacksLock = Any()

    // Script-specific libraries
    private var tcpLib: TCPLib? = null
    private var threadLib: ThreadLib? = null
    var imguiLib: ImGuiLib? = null
    private var djlLibrary: DJLLuaTrainer? = null
    private var ffi: FFILib? = null

    // Dependency tracking for nested requires
    private val dependencies = ConcurrentHashMap<String, MutableList<String>>()

    // Script-specific globals
    var scriptGlobals: Globals = JsePlatform.standardGlobals()

    init {
        // Register standard libraries
        scriptGlobals.load(LuajavaLib())

        registerCustomFunctions()
    }

    private fun registerCustomFunctions() {
        // Register event registration functions
        registerEventRegistrationFunctions()
        
        // Register event unregistration functions
        registerEventUnregistrationFunctions()
        
        // Register command functions
        registerCommandFunctions()
        
        // Register require function with module loading prevention
        registerRequireFunction()
        
        // Register other custom functions
        registerOtherCustomFunctions()
    }

    private fun registerEventRegistrationFunctions() {
        scriptGlobals.set("registerUnloadCallback", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(scriptUnloadCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerSpawnParticle", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(particleCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerInventoryItemChange", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(inventoryItemChangeCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerUseBlock", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(useBlockCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerAttackBlock", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(attackBlockCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerClientTick", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(clientPreTickCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerClientTickPost", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(clientTickCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerClientTickPre", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(clientPreTickCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerBlockUpdate", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(blockUpdateCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerWorldRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(renderWorldCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("register2DRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(render2DCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerKeyEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(keyEventCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(messageEventCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerSendMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(onSendMessageEventCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerSendCommandEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(onSendCommandEventCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerLocationChangeEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(locationChangeCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerImGuiRenderEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(imguiRenderCallbacks.add(callback))
                }
            }
        })

        // Packet events
        scriptGlobals.set("registerServerSideRotationEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverSideRotationCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerServerSideTeleportEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverSideTeleportCallbacks.add(callback))
                }
            }
        })
    }

    private fun registerEventUnregistrationFunctions() {
        scriptGlobals.set("unregisterSpawnParticle", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(particleCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterInventoryItemChange", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(inventoryItemChangeCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterUseBlock", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(useBlockCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterAttackBlock", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(attackBlockCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterClientTick", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(clientPreTickCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterClientTickPost", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(clientTickCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterClientTickPre", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(clientPreTickCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterBlockUpdate", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(blockUpdateCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterWorldRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(renderWorldCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregister2DRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(render2DCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterKeyEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(keyEventCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(messageEventCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterSendMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(onSendMessageEventCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterSendCommandEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(onSendCommandEventCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterLocationChangeEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(locationChangeCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterImGuiRenderEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(imguiRenderCallbacks.remove(callback))
                }
            }
        })

        // Packet events
        scriptGlobals.set("unregisterServerSideRotationEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(serverSideRotationCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterServerSideTeleportEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                synchronized(callbacksLock) {
                    return valueOf(serverSideTeleportCallbacks.remove(callback))
                }
            }
        })
    }

    private fun registerRequireFunction() {
        scriptGlobals.set("require", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val moduleName = args.arg(1).checkjstring()

                // 1. Определяем "родителя" (кто вызвал require)
                val caller = if (loadingStack.isEmpty()) scriptName else loadingStack.peek()

                // 2. Записываем связь в локальное дерево этого объекта LuaScript
                localDependencyGraph.getOrPut(caller) {
                    java.util.Collections.synchronizedSet(LinkedHashSet<String>())
                }.add(moduleName)

                // 3. Загружаем и выполняем (без кэша)
                return requireModule(moduleName)
            }
        })
    }

    private fun registerOtherCustomFunctions() {
        // Register HypixelCry global
        scriptGlobals.set("HypixelCry", CoerceJavaToLua.coerce(HypixelCry.getInstance()))

        scriptGlobals.set("currentScriptName", LuaValue.valueOf(scriptName))

        // Register print function
        scriptGlobals.set("print", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val message = StringBuilder()

                // Обрабатываем все переданные аргументы
                for (i in 1..args.narg()) {
                    if (i > 1) message.append(" ")
                    message.append(args.arg(i).tojstring())
                }
                val messageStr = message.toString()
                HypixelCry.LOGGER.info(HypixelCry.LOG_PREFIX + messageStr)
                return NIL
            }
        })

        // Register registerUnloadCallback function
        scriptGlobals.set("registerUnloadCallback", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addScriptUnloadCallback(callback))
            }
        })
    }

    // Methods for adding callbacks
    fun addScriptUnloadCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return scriptUnloadCallbacks.add(callback)
        }
    }

    fun addCommandCallback(commandName: String, callback: LuaValue, suggestionsCallback: LuaValue? = null): Boolean {
        if (!callback.isfunction()) return false
        if (commandName.isBlank()) return false
        if (suggestionsCallback != null && !suggestionsCallback.isfunction()) return false

        synchronized(callbacksLock) {
            // Проверяем, не зарегистрирована ли уже команда с таким именем
            if (commandCallbacks.containsKey(commandName)) {
                return false
            }
            commandCallbacks[commandName] = callback
            if (suggestionsCallback != null) {
                commandSuggestionsCallbacks[commandName] = suggestionsCallback
            }
            registerMinecraftCommand(commandName)

            return true
        }
    }

    private fun registerCommandFunctions() {
        scriptGlobals.set("registerCommand", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val commandName = args.arg(1).checkjstring()
                val callback = args.arg(2)
                val suggestionsCallback = if (args.narg() >= 3) args.arg(3) else null
                return LuaValue.valueOf(addCommandCallback(commandName, callback, suggestionsCallback))
            }
        })

        scriptGlobals.set("unregisterCommand", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val commandName = args.arg(1).checkjstring()
                return LuaValue.valueOf(removeCommandCallback(commandName))
            }
        })
    }

    private fun registerMinecraftCommand(commandName: String) {
        try {
            // 1. Регистрируем коллбэк для будущих обновлений диспетчера (например, при смене сервера)
            ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
                commandDispatchers[commandName] = dispatcher
                actualRegister(dispatcher, commandName)
            }

            // 2. Попытка немедленной регистрации, если диспетчер уже доступен
            val client = HypixelCry.mc
            val networkHandler = client.connection
            if (networkHandler != null) {
                val currentDispatcher = networkHandler.commands

                client.execute {
                    @Suppress("UNCHECKED_CAST")
                    val fabricDispatcher = currentDispatcher as CommandDispatcher<FabricClientCommandSource>

                    commandDispatchers[commandName] = fabricDispatcher
                    actualRegister(fabricDispatcher, commandName)
                    HypixelCry.LOGGER.info("${HypixelCry.LOG_PREFIX}Registered Minecraft command: /$commandName")
                }
            }
        } catch (e: Exception) {
            HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Failed to register Minecraft command: /$commandName", e)
        }
    }

    private fun actualRegister(dispatcher: CommandDispatcher<FabricClientCommandSource>, commandName: String) {
        // Удаляем старую команду, если она была
        val root = dispatcher.root
        if (root.getChild(commandName) != null) {
            try {
                val childrenField = root.javaClass.getDeclaredField("children")
                childrenField.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                (childrenField.get(root) as MutableMap<String, *>).remove(commandName)

                val literalsField = root.javaClass.getDeclaredField("literals")
                literalsField.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                (literalsField.get(root) as MutableMap<String, *>).remove(commandName)
            } catch (e: Exception) {
                // Ignore
            }
        }

        val commandBuilder = ClientCommandManager.literal(commandName)
            .executes { context ->
                executeLuaCommand(commandName, emptyArray(), context.source)
                1
            }

        // Проверяем, есть ли callback для автодополнения
        val suggestionsCallback = commandSuggestionsCallbacks[commandName]

        if (suggestionsCallback != null) {
            // С автодополнением
            commandBuilder.then(
                ClientCommandManager.argument("args", StringArgumentType.greedyString())
                    .suggests { context, builder ->
                        getSuggestionsFromLua(commandName, context, builder, suggestionsCallback)
                    }
                    .executes { context ->
                        val args = StringArgumentType.getString(context, "args").split(" ").toTypedArray()
                        executeLuaCommand(commandName, args, context.source)
                        1
                    }
            )
        } else {
            // Без автодополнения
            commandBuilder.then(
                ClientCommandManager.argument("args", StringArgumentType.greedyString())
                    .executes { context ->
                        val args = StringArgumentType.getString(context, "args").split(" ").toTypedArray()
                        executeLuaCommand(commandName, args, context.source)
                        1
                    }
            )
        }

        dispatcher.register(commandBuilder)

        if (dispatcher.root.getChild(commandName) == null) {
            HypixelCry.LOGGER.error("Failed to inject node into dispatcher root!")
        } else {
            HypixelCry.LOGGER.info("Successfully injected command: $commandName with suggestions: ${suggestionsCallback != null}")
        }
    }

    private fun getSuggestionsFromLua(
        commandName: String,
        context: CommandContext<FabricClientCommandSource>,
        builder: com.mojang.brigadier.suggestion.SuggestionsBuilder,
        suggestionsCallback: LuaValue
    ): java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> {
        return java.util.concurrent.CompletableFuture.supplyAsync {
            try {
                // Получаем текущий ввод пользователя
                val input = builder.remaining

                // Получаем весь введенный текст команды
                val fullInput = builder.input

                // Создаем таблицу с информацией для Lua
                val infoTable = LuaValue.tableOf()
                infoTable.set("input", LuaValue.valueOf(input))
                infoTable.set("fullInput", LuaValue.valueOf(fullInput))

                // Вызываем Lua callback
                val result = suggestionsCallback.call(infoTable)

                // Обрабатываем результат
                if (result.istable()) {
                    val suggestions = result.checktable()
                    var i = 1
                    while (true) {
                        val suggestion = suggestions.get(i)
                        if (suggestion.isnil()) break

                        // Поддержка как строк, так и таблиц с tooltip
                        if (suggestion.isstring()) {
                            builder.suggest(suggestion.tojstring())
                        } else if (suggestion.istable()) {
                            val suggestionTable = suggestion.checktable()
                            val text = suggestionTable.get("text").tojstring()
                            val tooltip = suggestionTable.get("tooltip")

                            if (!tooltip.isnil()) {
                                builder.suggest(text, Component.literal(tooltip.tojstring()))
                            } else {
                                builder.suggest(text)
                            }
                        }
                        i++
                    }
                } else if (result.isstring()) {
                    // Если вернули строку, добавляем как единственное предложение
                    builder.suggest(result.tojstring())
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error getting suggestions for command /$commandName", e)
            }
            builder.build()
        }
    }

    private fun unregisterCommandInternal(dispatcher: CommandDispatcher<FabricClientCommandSource>, commandName: String) {
        try {
            val root = dispatcher.root // В Kotlin это вызывает getRoot()

            // В Brigadier у узла (CommandNode) есть три карты, в которых хранятся команды.
            // Названия полей в самой библиотеке Brigadier (она не обфусцирована так, как MC):
            // "children", "literals", "arguments"
            val nodeClass = root.javaClass.superclass // CommandNode — родитель для RootCommandNode

            val fieldsToClear = arrayOf("children", "literals", "arguments")

            for (fieldName in fieldsToClear) {
                try {
                    // Ищем поле в классе CommandNode
                    val field = com.mojang.brigadier.tree.CommandNode::class.java.getDeclaredField(fieldName)
                    field.isAccessible = true

                    @Suppress("UNCHECKED_CAST")
                    val map = field.get(root) as MutableMap<String, *>
                    map.remove(commandName)
                } catch (e: NoSuchFieldException) {
                    // Если вдруг библиотека Brigadier в вашей среде имеет другие названия полей
                    HypixelCry.LOGGER.warn("Field $fieldName not found in CommandNode")
                }
            }

            // Удаляем из наших внутренних списков
            commandDispatchers.remove(commandName)
            commandCallbacks.remove(commandName)

            HypixelCry.LOGGER.info("${HypixelCry.LOG_PREFIX}Successfully unregistered Lua command: /$commandName")
        } catch (e: Exception) {
            HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Failed to unregister command /$commandName", e)
        }
    }

    private fun executeLuaCommand(commandName: String, args: Array<String>, source: FabricClientCommandSource?) {
        val callback = commandCallbacks[commandName]
        if (callback != null && callback.isfunction()) {
            try {
                // Преобразуем аргументы в Lua таблицу
                val argsTable = LuaValue.listOf(args.map { LuaValue.valueOf(it) }.toTypedArray())
                callback.call(LuaValue.valueOf(commandName), argsTable, LuaValue.valueOf(source?.player?.name?.string ?: ""))
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error executing Lua command: /$commandName", e)
                source?.sendError(Component.literal("Error executing command: ${e.message}"))
            }
        }
    }

    fun removeCommandCallback(commandName: String): Boolean {
        synchronized(callbacksLock) {
            val dispatcher = commandDispatchers[commandName]
            if (dispatcher != null) {
                unregisterCommandInternal(dispatcher, commandName)
            }
            commandSuggestionsCallbacks.remove(commandName)
            return commandCallbacks.remove(commandName) != null
        }
    }

    // Event handlers
    fun onInventoryItemAChange(slot: Int, stack: ItemStack): Boolean {
        var allow = true
        val callbacks = synchronized(callbacksLock) {
            inventoryItemChangeCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                val res = callback.call(LuaValue.valueOf(slot), LuaItemStack(stack))
                if (res.isboolean() && !res.toboolean()) {
                    allow = false
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in inventory add item callback in ${scriptName}", e)
            }
        }
        return allow
    }

    fun onAttackBlock(pos: BlockPos, direction: Direction, hand: InteractionHand): Boolean {
        var allow = true
        val callbacks = synchronized(callbacksLock) {
            attackBlockCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                val t = LuaValue.tableOf()
                t.set("x", LuaValue.valueOf(pos.x))
                t.set("y", LuaValue.valueOf(pos.y))
                t.set("z", LuaValue.valueOf(pos.z))
                t.set("direction", LuaDirection(direction))
                t.set("hand", LuaValue.valueOf(hand.name))
                val res = callback.call(t)
                if (res.isboolean() && !res.toboolean()) {
                    allow = false
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in attack block callback in ${scriptName}", e)
            }
        }
        return allow
    }

    fun onUseBlock(pos: BlockPos, hand: InteractionHand): Boolean {
        var allow = true
        val callbacks = synchronized(callbacksLock) {
            useBlockCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                val t = LuaValue.tableOf()
                t.set("x", LuaValue.valueOf(pos.x))
                t.set("y", LuaValue.valueOf(pos.y))
                t.set("z", LuaValue.valueOf(pos.z))
                t.set("hand", LuaValue.valueOf(hand.name))
                val res = callback.call(t)
                if (res.isboolean() && !res.toboolean()) {
                    allow = false
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in use block callback in ${scriptName}", e)
            }
        }
        return allow
    }

    fun onClientTick() {
        val callbacks = synchronized(callbacksLock) {
            clientTickCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in client tick callback in ${scriptName}", e)
            }
        }
    }

    fun onClientTickPre() {
        val callbacks = synchronized(callbacksLock) {
            clientPreTickCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in client pre tick callback in ${scriptName}", e)
            }
        }
    }

    fun onRenderTick(context: WorldRendererObject) {
        val callbacks = synchronized(callbacksLock) {
            renderWorldCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call(context)
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in world render callback in ${scriptName}: ${e.message}")
            }
        }
    }

    fun on2DRenderTick(context: GuiGraphics?) {
        val callbacks = synchronized(callbacksLock) {
            render2DCallbacks.toTypedArray()
        }

        val renderContext = TwoRenderObject(context, scriptName)
        for (callback in callbacks) {
            try {
                callback.call(renderContext)
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in 2D render callback in ${scriptName}: ${e.message}")
            }
        }
    }

    fun onKeyEvent(key: Int, type: KeyAction): Boolean {
        val callbacks = synchronized(callbacksLock) {
            keyEventCallbacks.toTypedArray()
        }
        var allow = true
        for (callback in callbacks) {
            try {
                val res = callback.call(LuaValue.valueOf(key), LuaValue.valueOf(type.name))
                if (res.isboolean()) {
                    if (!res.toboolean()) {
                        allow = false
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in key callback in ${scriptName}: ${e.message}")
            }
        }
        return allow
    }

    fun onChatMessageEvent(text: String, overlay: Boolean, json: String): Boolean {
        val callbacks = synchronized(callbacksLock) {
            messageEventCallbacks.toTypedArray()
        }
        var allow = true
        for (callback in callbacks) {
            try {
                val res = callback.call(LuaValue.valueOf(text), LuaValue.valueOf(overlay), LuaValue.valueOf(json))
                if (res.isboolean()) {
                    if (!res.toboolean()) {
                        allow = false
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in message callback in ${scriptName}: ${e.message}")
            }
        }
        return allow
    }

    fun onSendChatMessageEvent(text: String): Boolean {
        val callbacks = synchronized(callbacksLock) {
            onSendMessageEventCallbacks.toTypedArray()
        }
        var allow = true
        for (callback in callbacks) {
            try {
                val res = callback.call(LuaValue.valueOf(text))
                if (res.isboolean()) {
                    if (!res.toboolean()) {
                        allow = false
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in send message callback in ${scriptName}: ${e.message}")
            }
        }
        return allow
    }

    fun onSendChatCommandEvent(text: String): Boolean {
        val callbacks = synchronized(callbacksLock) {
            onSendCommandEventCallbacks.toTypedArray()
        }
        var allow = true
        for (callback in callbacks) {
            try {
                val res = callback.call(LuaValue.valueOf(text))
                if (res.isboolean()) {
                    if (!res.toboolean()) {
                        allow = false
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in send command callback in ${scriptName}: ${e.message}")
            }
        }
        return allow
    }

    fun onBlockUpdateEvent(table: LuaValue): Boolean {
        val callbacks = synchronized(callbacksLock) {
            blockUpdateCallbacks.toTypedArray()
        }
        var allow = true
        for (callback in callbacks) {
            try {
                val res = callback.call(table)
                if (res.isboolean()) {
                    if (!res.toboolean()) {
                        allow = false
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in block update callback in ${scriptName}: ${e.message}")
            }
        }
        return allow
    }

    fun onLocationChangeEvent(location: Location): Boolean {
        val callbacks = synchronized(callbacksLock) {
            locationChangeCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call(LuaValue.valueOf(location.toString()))
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in location change callback in ${scriptName}: ${e.message}")
            }
        }
        return true
    }

    fun onImGuiRenderEvent(): Boolean {
        val callbacks = synchronized(callbacksLock) {
            imguiRenderCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in imgui callback in ${scriptName}: ${e.message}")
            }
        }
        return true
    }

    // Packet events
    fun onSpawnParticleEvent(id: Int, x: Double, y: Double, z: Double, xDist: Float, yDist: Float, zDist: Float, maxSpeed: Float, count: Int): Boolean {
        var allow = true
        val callbacks = synchronized(callbacksLock) {
            particleCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {

                val t = LuaValue.tableOf()
                t.set("id", id)

                t.set("x", x)
                t.set("y", y)
                t.set("z", z)

                t.set("x_dist", xDist.toDouble())
                t.set("y_dist", yDist.toDouble())
                t.set("z_dist", zDist.toDouble())

                t.set("max_speed", maxSpeed.toDouble())
                t.set("count", count)

                callback.call(t)
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in particle callback in ${scriptName}: ${e.message}")
            }
        }
        return allow
    }

    fun onServerSideRotationEvent(yaw: Float, pitch: Float): Boolean {
        var allow = true
        val callbacks = synchronized(callbacksLock) {
            serverSideRotationCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                val res = callback.call(LuaValue.valueOf(yaw.toDouble()), LuaValue.valueOf(pitch.toDouble()))
                if (res.isboolean()) {
                    if (!res.toboolean()) {
                        allow = false
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in server side rotation callback in ${scriptName}: ${e.message}")
            }
        }
        return allow
    }

    private fun getSystemModule(name: String): LuaValue? {
        systemModuleCache[name]?.let { return it }

        val module: LuaValue = when (name) {
            "player" -> PlayerObject()
            "world" -> WorldObject()
            "modules" -> ModulesObject()

            "imgui" -> {
                if (imguiLib == null) imguiLib = ImGuiLib()
                imguiLib!!
            }
            "tcp" -> {
                if (tcpLib == null) tcpLib = TCPLib()
                tcpLib!!
            }
            "threads" -> {
                if (threadLib == null) threadLib = ThreadLib()
                threadLib!!
            }
            "ffi" -> {
                if (ffi == null) ffi = FFILib()
                ffi!!
            }
            "djl", "ai" -> {
                if (djlLibrary == null) djlLibrary = DJLLuaTrainer()
                // DJL требует вызова call для регистрации функций в таблице
                djlLibrary!!.call(LuaValue.valueOf("djl"), scriptGlobals)
                //scriptGlobals.get("djl")
            }
            "json" -> {
                JsonLib()
            }
            "http" -> {
                HttpClientLib()
            }
            "encoding" -> {
                EncodingLib()
            }

            else -> return null
        }

        systemModuleCache[name] = module
        return module
    }

    fun requireModule(moduleName: String): LuaValue {
        val systemModule = getSystemModule(moduleName)
        if (systemModule != null) {
            return systemModule
        }

        val moduleFile = LuaManager.findModuleFile(moduleName)
            ?: throw LuaError("module '$moduleName' not found")

        try {
            // Добавляем в стек перед выполнением
            loadingStack.push(moduleName)

            // Загружаем код из файла
            val chunk = LuaManager.loadChunk(moduleFile, moduleName, scriptGlobals)

            // Выполняем. Результат не сохраняем в кэш, просто возвращаем
            val result = chunk.call()
            return result

        } catch (e: Exception) {
            throw LuaError("error loading module '$moduleName': ${e.message}")
        } finally {
            // Обязательно убираем из стека после завершения
            if (!loadingStack.isEmpty() && loadingStack.peek() == moduleName) {
                loadingStack.pop()
            }
        }
    }

    fun onServerSideTeleportEvent(x: Double, y: Double, z: Double): Boolean {
        var allow = true
        val callbacks = synchronized(callbacksLock) {
            serverSideTeleportCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                val res = callback.call(LuaValue.valueOf(x), LuaValue.valueOf(y), LuaValue.valueOf(z))
                if (res.isboolean()) {
                    if (!res.toboolean()) {
                        allow = false
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in server side rotation callback in ${scriptName}: ${e.message}")
            }
        }
        return allow
    }

    // Cleanup method
    fun cleanup() {
        // Вызываем все callback'и выгрузки скрипта
        scriptUnloadCallbacks.forEach { callback ->
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in script unload callback in ${scriptName}", e)
            }
        }

        // Очищаем библиотеки
        threadLib?.stopAllThreads()
        tcpLib?.cleanup()

        for (command in commandCallbacks.keys) {
            val dispatcher = commandDispatchers[command]
            if (dispatcher != null) {
                unregisterCommandInternal(dispatcher, command)
            }
        }

        // Очищаем все коллбэки
        synchronized(callbacksLock) {
            scriptUnloadCallbacks.clear()
            inventoryItemChangeCallbacks.clear()
            useBlockCallbacks.clear()
            attackBlockCallbacks.clear()
            clientTickCallbacks.clear()
            clientPreTickCallbacks.clear()
            blockUpdateCallbacks.clear()
            renderWorldCallbacks.clear()
            render2DCallbacks.clear()
            keyEventCallbacks.clear()
            messageEventCallbacks.clear()
            onSendMessageEventCallbacks.clear()
            onSendCommandEventCallbacks.clear()
            locationChangeCallbacks.clear()
            imguiRenderCallbacks.clear()
            serverSideRotationCallbacks.clear()
            serverSideTeleportCallbacks.clear()
            commandCallbacks.clear()
            commandSuggestionsCallbacks.clear()
        }
        imguiLib?.queue?.clear()
        djlLibrary?.models?.clear()
        djlLibrary?.predictors?.clear()
        djlLibrary?.inputShapes?.clear()
        djlLibrary?.modelModes?.clear()
        ffi?.loadedLibraries?.forEach { lib ->
            lib.value.dispose()
        }
        ffi?.loadedLibraries?.clear()

        commandDispatchers.clear()

        localDependencyGraph.remove(scriptName)
        // Очищаем зависимости
        dependencies.clear()
        scriptGlobals = JsePlatform.standardGlobals()
    }
}
