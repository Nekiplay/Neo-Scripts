package com.nekiplay.hypixelcry.features.lua

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.RootCommandNode
import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.text.LuaComponentBuilder
import com.nekiplay.hypixelcry.features.lua.objects.misc.DJLLuaTrainer
import com.nekiplay.hypixelcry.features.lua.objects.misc.ImGuiLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.TCPLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ThreadLib
import com.nekiplay.hypixelcry.features.lua.objects.render.TwoRenderObject
import com.nekiplay.hypixelcry.features.lua.objects.render.WorldRendererObject
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
    private val tcpLib: TCPLib
    private val threadLib: ThreadLib
    public val imguiLib: ImGuiLib
    private val djlLibrary = DJLLuaTrainer()

    // Dependency tracking for nested requires
    private val dependencies = ConcurrentHashMap<String, MutableList<String>>()

    // Script-specific globals
    var scriptGlobals: Globals = JsePlatform.standardGlobals()

    init {
        // Register standard libraries
        scriptGlobals.load(LuajavaLib())

        registerCustomFunctions()

        // Initialize script-specific libraries
        tcpLib = TCPLib()
        threadLib = ThreadLib()
        imguiLib = ImGuiLib()

        // Load libraries into script-specific globals
        scriptGlobals.load(threadLib)
        scriptGlobals.load(tcpLib)
        scriptGlobals.load(imguiLib)

        // Register global objects
        registerGlobalObjects()
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
                return LuaValue.valueOf(addScriptUnloadCallback(callback))
            }
        })

        scriptGlobals.set("registerSpawnParticle", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addParticleCallback(callback))
            }
        })

        scriptGlobals.set("registerInventoryItemChange", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addInventoryItemChangeCallback(callback))
            }
        })

        scriptGlobals.set("registerUseBlock", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addUseBlockCallback(callback))
            }
        })

        scriptGlobals.set("registerClientTick", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addPreClientTickCallback(callback))
            }
        })

        scriptGlobals.set("registerClientTickPost", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addClientTickCallback(callback))
            }
        })

        scriptGlobals.set("registerClientTickPre", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addPreClientTickCallback(callback))
            }
        })

        scriptGlobals.set("registerBlockUpdate", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addBlockUpdateCallback(callback))
            }
        })

        scriptGlobals.set("registerWorldRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addWorldRendererCallback(callback))
            }
        })

        scriptGlobals.set("register2DRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(add2DRendererCallback(callback))
            }
        })

        scriptGlobals.set("registerKeyEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addKeyEventCallback(callback))
            }
        })

        scriptGlobals.set("registerMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addMessageCallback(callback))
            }
        })

        scriptGlobals.set("registerSendMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addSendMessageCallback(callback))
            }
        })

        scriptGlobals.set("registerSendCommandEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addSendCommandCallback(callback))
            }
        })

        scriptGlobals.set("registerLocationChangeEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addLocationChangeCallback(callback))
            }
        })

        scriptGlobals.set("registerImGuiRenderEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addImguiRenderCallback(callback))
            }
        })

        // Packet events
        scriptGlobals.set("registerServerSideRotationEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addServerSideRotationCallback(callback))
            }
        })

        scriptGlobals.set("registerServerSideTeleportEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addServerSideTeleportCallback(callback))
            }
        })
    }

    private fun registerEventUnregistrationFunctions() {
        scriptGlobals.set("unregisterSpawnParticle", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeParticleCallback(callback))
            }
        })

        scriptGlobals.set("unregisterInventoryItemChange", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeInventoryItemChangeCallback(callback))
            }
        })

        scriptGlobals.set("unregisterUseBlock", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeUseBlockCallback(callback))
            }
        })

        scriptGlobals.set("unregisterClientTick", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeClientTickCallback(callback))
            }
        })

        scriptGlobals.set("unregisterClientTickPost", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeClientTickCallback(callback))
            }
        })

        scriptGlobals.set("unregisterClientTickPre", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeClientPreTickCallback(callback))
            }
        })

        scriptGlobals.set("unregisterBlockUpdate", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeBlockUpdateCallback(callback))
            }
        })

        scriptGlobals.set("unregisterWorldRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeWorldRendererCallback(callback))
            }
        })

        scriptGlobals.set("unregister2DRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(remove2DRendererCallback(callback))
            }
        })

        scriptGlobals.set("unregisterKeyEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeKeyEventCallback(callback))
            }
        })

        scriptGlobals.set("unregisterMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeMessageEventCallback(callback))
            }
        })

        scriptGlobals.set("unregisterSendMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeSendMessageEventCallback(callback))
            }
        })

        scriptGlobals.set("unregisterSendCommandEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeSendCommandEventCallback(callback))
            }
        })

        scriptGlobals.set("unregisterLocationChangeEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeLocationChangeEventCallback(callback))
            }
        })

        scriptGlobals.set("unregisterImGuiRenderEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeImGuiRenderEventCallback(callback))
            }
        })

        // Packet events
        scriptGlobals.set("unregisterServerSideRotationEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeServerSideRotationEventCallback(callback))
            }
        })

        scriptGlobals.set("unregisterServerSideTeleportEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeServerSideTeleportEventCallback(callback))
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

        djlLibrary.call(
            org.luaj.vm2.LuaValue.valueOf("djl"), // имя модуля
            scriptGlobals                                        // окружение, куда регистрировать
        )
    }

    private fun registerGlobalObjects() {
        // Register global objects
        scriptGlobals.set("player", luaManager.playerObj)
        scriptGlobals.set("world", luaManager.worldObj)
        scriptGlobals.set("modules", luaManager.modulesObj)
        scriptGlobals.set("ComponentBuilder", LuaComponentBuilder.createLibrary())

        scriptGlobals.load(luaManager.jsonLib)
        scriptGlobals.load(luaManager.httpLib)
        scriptGlobals.load(luaManager.catboostLib)
        scriptGlobals.load(luaManager.creatorLib)
        scriptGlobals.load(luaManager.encodingLib)
    }

    // Methods for adding callbacks
    fun addScriptUnloadCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return scriptUnloadCallbacks.add(callback)
        }
    }

    fun addParticleCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return particleCallbacks.add(callback)
        }
    }

    fun addInventoryItemChangeCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return inventoryItemChangeCallbacks.add(callback)
        }
    }

    fun addUseBlockCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return useBlockCallbacks.add(callback)
        }
    }

    fun addClientTickCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return clientTickCallbacks.add(callback)
        }
    }

    fun addPreClientTickCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return clientPreTickCallbacks.add(callback)
        }
    }

    fun addBlockUpdateCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return blockUpdateCallbacks.add(callback)
        }
    }

    fun addWorldRendererCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return renderWorldCallbacks.add(callback)
        }
    }

    fun add2DRendererCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return render2DCallbacks.add(callback)
        }
    }

    fun addKeyEventCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return keyEventCallbacks.add(callback)
        }
    }

    fun addMessageCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return messageEventCallbacks.add(callback)
        }
    }

    fun addSendMessageCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return onSendMessageEventCallbacks.add(callback)
        }
    }

    fun addSendCommandCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return onSendCommandEventCallbacks.add(callback)
        }
    }

    fun addLocationChangeCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return locationChangeCallbacks.add(callback)
        }
    }

    fun addImguiRenderCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return imguiRenderCallbacks.add(callback)
        }
    }

    fun addServerSideRotationCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return serverSideRotationCallbacks.add(callback)
        }
    }

    fun addServerSideTeleportCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return serverSideTeleportCallbacks.add(callback)
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

    // Methods for removing callbacks
    fun removeParticleCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return particleCallbacks.remove(callback)
        }
    }

    fun removeInventoryItemChangeCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return inventoryItemChangeCallbacks.remove(callback)
        }
    }

    fun removeUseBlockCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return useBlockCallbacks.remove(callback)
        }
    }

    fun removeClientTickCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return clientTickCallbacks.remove(callback)
        }
    }

    fun removeClientPreTickCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return clientPreTickCallbacks.remove(callback)
        }
    }

    fun removeBlockUpdateCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return blockUpdateCallbacks.remove(callback)
        }
    }

    fun removeWorldRendererCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return renderWorldCallbacks.remove(callback)
        }
    }

    fun remove2DRendererCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return render2DCallbacks.remove(callback)
        }
    }

    fun removeKeyEventCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return keyEventCallbacks.remove(callback)
        }
    }

    fun removeMessageEventCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return messageEventCallbacks.remove(callback)
        }
    }

    fun removeSendMessageEventCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return onSendMessageEventCallbacks.remove(callback)
        }
    }

    fun removeSendCommandEventCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return onSendCommandEventCallbacks.remove(callback)
        }
    }

    fun removeLocationChangeEventCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return locationChangeCallbacks.remove(callback)
        }
    }

    fun removeImGuiRenderEventCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return imguiRenderCallbacks.remove(callback)
        }
    }

    fun removeServerSideRotationEventCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return serverSideRotationCallbacks.remove(callback)
        }
    }

    fun removeServerSideTeleportEventCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return serverSideTeleportCallbacks.remove(callback)
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

    fun requireModule(moduleName: String): LuaValue {
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

    fun hasCommand(name: String): Boolean = commandCallbacks.containsKey(name)

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
        threadLib.stopAllThreads()
        tcpLib.cleanup()

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
        imguiLib.queue.clear()
        djlLibrary.models.clear()
        djlLibrary.predictors.clear()
        djlLibrary.inputShapes.clear()
        djlLibrary.modelModes.clear()

        commandDispatchers.clear()

        localDependencyGraph.remove(scriptName)
        // Очищаем зависимости
        dependencies.clear()
        scriptGlobals = JsePlatform.standardGlobals()
    }
}
