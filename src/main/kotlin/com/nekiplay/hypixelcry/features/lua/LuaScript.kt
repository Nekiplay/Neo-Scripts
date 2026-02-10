package com.nekiplay.hypixelcry.features.lua

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
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
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.commands.CommandBuildContext
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import org.luaj.vm2.lib.jse.LuajavaLib
import java.util.concurrent.ConcurrentHashMap

class LuaScript(val scriptName: String, private val luaManager: LuaManager) {
    private val INSTANCE: LuaScript = this

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
    private val inventoryItemAddCallbacks = ArrayList<LuaValue>()

    // Packet events
    private val serverSideRotationCallbacks = ArrayList<LuaValue>()
    private val serverSideTeleportCallbacks = ArrayList<LuaValue>()

    // Command events
    private val commandCallbacks = ConcurrentHashMap<String, LuaValue>()

    // Script events
    private val scriptUnloadCallbacks = ArrayList<LuaValue>()

    // Synchronize only when needed
    private val callbacksLock = Any()

    // Script-specific libraries
    private val tcpLib: TCPLib
    private val threadLib: ThreadLib

    // Dependency tracking for nested requires
    private val dependencies = ConcurrentHashMap<String, MutableList<String>>()

    // Script-specific globals
    val scriptGlobals: Globals = JsePlatform.standardGlobals()

    init {
        // Register standard libraries
        scriptGlobals.load(LuajavaLib())
        
        registerCustomFunctions()
        
        // Initialize script-specific libraries
        tcpLib = TCPLib()
        threadLib = ThreadLib()
        
        // Load libraries into script-specific globals
        scriptGlobals.load(threadLib)
        scriptGlobals.load(tcpLib)
        
        // Register global objects
        registerGlobalObjects()
    }

    val logger = HypixelCry.LOGGER

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

        scriptGlobals.set("registerInventoryItemAdd", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addInventoryItemAddCallback(callback))
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
                return LuaValue.valueOf(addClientTickCallback(callback))
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
        scriptGlobals.set("unregisterInventoryItemAdd", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeInventoryItemAddCallback(callback))
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

    private fun registerCommandFunctions() {
        scriptGlobals.set("registerCommand", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val commandName = args.arg(1).checkjstring()
                val callback = args.arg(2)
                return LuaValue.valueOf(addCommandCallback(commandName, callback))
            }
        })

        scriptGlobals.set("unregisterCommand", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val commandName = args.arg(1).checkjstring()
                return LuaValue.valueOf(removeCommandCallback(commandName))
            }
        })
    }

    private fun registerRequireFunction() {
        scriptGlobals.set("require", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val moduleName = args.arg(1).checkjstring()

                // Загружаем модуль через LuaManager
                val result = luaManager.requireModule(moduleName, INSTANCE)

                // Добавляем в зависимости только при первом требовании
                addDependency(moduleName)

                return result
            }
        })
    }

    private fun registerOtherCustomFunctions() {
        // Register HypixelCry global
        scriptGlobals.set("HypixelCry", CoerceJavaToLua.coerce(HypixelCry.getInstance()))

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

    private fun registerGlobalObjects() {
        // Register global objects
        scriptGlobals.set("player", luaManager.playerObj)
        scriptGlobals.set("world", luaManager.worldObj)
        scriptGlobals.set("modules", luaManager.modulesObj)

        scriptGlobals.load(luaManager.imguiLib)
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

    fun addInventoryItemAddCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            return inventoryItemAddCallbacks.add(callback)
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

    fun registerMinecraftCommand(commandName: String) {
        try {
            // Регистрируем команду в Minecraft
            ClientCommandRegistrationCallback.EVENT.register { dispatcher: CommandDispatcher<FabricClientCommandSource>, registryAccess: CommandBuildContext ->
                dispatcher.register(
                    ClientCommandManager.literal(commandName)
                        .executes { context: CommandContext<FabricClientCommandSource> ->
                            executeLuaCommand(commandName, emptyArray(), context.source)
                            1
                        }
                        .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .executes { context: CommandContext<FabricClientCommandSource> ->
                                val args = StringArgumentType.getString(context, "args").split(" ").toTypedArray()
                                executeLuaCommand(commandName, args, context.source)
                                1
                            }
                        )
                )
            }
        } catch (e: Exception) {
            HypixelCry.LOGGER.error("Failed to register Minecraft command: /$commandName", e)
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
                HypixelCry.LOGGER.error("Error executing Lua command: /$commandName", e)
                source?.sendError(Component.literal("Error executing command: ${e.message}"))
            }
        }
    }

    fun addCommandCallback(commandName: String, callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        if (commandName.isBlank()) return false

        synchronized(callbacksLock) {
            // Проверяем, не зарегистрирована ли уже команда с таким именем
            if (commandCallbacks.containsKey(commandName)) {
                return false
            }

            commandCallbacks[commandName] = callback

            // Регистрируем команду в Minecraft
            registerMinecraftCommand(commandName)

            return true
        }
    }

    // Methods for removing callbacks
    fun removeInventoryItemAddCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return inventoryItemAddCallbacks.remove(callback)
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
            return commandCallbacks.remove(commandName) != null
        }
    }

    // Event handlers
    fun onInventoryItemAdd(slot: Int, stack: ItemStack): Boolean {
        var allow = true
        val callbacks = synchronized(callbacksLock) {
            inventoryItemAddCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                val res = callback.call(LuaValue.valueOf(slot), LuaItemStack(stack))
                if (res.isboolean() && !res.toboolean()) {
                    allow = false
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("Error in inventory add item callback", e)
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
                HypixelCry.LOGGER.error("Error in use block callback", e)
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
                HypixelCry.LOGGER.error("Error in client tick callback", e)
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
                HypixelCry.LOGGER.error("Error in client pre tick callback", e)
            }
        }
    }

    fun onRenderTick(context: PrimitiveCollector?) {
        val callbacks = synchronized(callbacksLock) {
            renderWorldCallbacks.toTypedArray()
        }

        val renderContext = WorldRendererObject(context)
        for (callback in callbacks) {
            try {
                callback.call(renderContext)
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("Error in world render callback: ${e.message}")
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
                HypixelCry.LOGGER.error("Error in 2D render callback: ${e.message}")
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
                HypixelCry.LOGGER.error("Error in key callback: ${e.message}")
            }
        }
        return allow
    }

    fun onChatMessageEvent(text: String, overlay: Boolean): Boolean {
        val callbacks = synchronized(callbacksLock) {
            messageEventCallbacks.toTypedArray()
        }
        var allow = true
        for (callback in callbacks) {
            try {
                val res = callback.call(LuaValue.valueOf(text), LuaValue.valueOf(overlay))
                if (res.isboolean()) {
                    if (!res.toboolean()) {
                        allow = false
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("Error in message callback: ${e.message}")
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
                HypixelCry.LOGGER.error("Error in send message callback: ${e.message}")
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
                HypixelCry.LOGGER.error("Error in send command callback: ${e.message}")
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
                HypixelCry.LOGGER.error("Error in block update callback: ${e.message}")
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
                HypixelCry.LOGGER.error("Error in location change callback: ${e.message}")
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
                HypixelCry.LOGGER.error("Error in imgui callback: ${e.message}")
            }
        }
        return true
    }

    // Packet events
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
                HypixelCry.LOGGER.error("Error in server side rotation callback: ${e.message}")
            }
        }
        return allow
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
                HypixelCry.LOGGER.error("Error in server side rotation callback: ${e.message}")
            }
        }
        return allow
    }

    // Dependency tracking methods
    fun addDependency(moduleName: String) {
        dependencies.getOrPut(scriptName) { mutableListOf() }.add(moduleName)
    }
    
    fun getDependencies(): List<String> {
        return dependencies[scriptName]?.toList() ?: emptyList()
    }


    // Cleanup method
    fun cleanup() {
        // Вызываем все callback'и выгрузки скрипта
        scriptUnloadCallbacks.forEach { callback ->
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("Error in script unload callback", e)
            }
        }

        // Очищаем все коллбэки
        synchronized(callbacksLock) {
            scriptUnloadCallbacks.clear()
            inventoryItemAddCallbacks.clear()
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
        }

        // Очищаем зависимости
        dependencies.clear()
        
        // Очищаем библиотеки
        threadLib.stopAllThreads()
        tcpLib.cleanup()
    }
}