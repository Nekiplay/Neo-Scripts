package com.nekiplay.hypixelcry.features.lua

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.features.lua.objects.misc.TCPLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ThreadLib
import com.nekiplay.hypixelcry.features.lua.objects.render.TwoRenderObject
import com.nekiplay.hypixelcry.features.lua.objects.render.WorldRendererObject
import com.nekiplay.hypixelcry.utils.Location
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction
import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class LuaScript(private val scriptName: String, private val luaManager: LuaManager) {
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

    // Precompute system globals
    private val systemGlobals = setOf(
        "print", "require", "registerClientTick", "registerWorldRenderer",
        "unregisterClientTick", "unregisterWorldRenderer", "player", "world", "modules"
    )

    // Script-specific globals
    private val persistentGlobals = ConcurrentHashMap<String, LuaValue>()

    // Script-specific libraries
    private val tcpLib: TCPLib
    private val threadLib: ThreadLib

    // Current executing script reference
    private val currentExecutingScript = AtomicReference<String?>()

    // Dependency tracking for nested requires
    private val dependencies = ConcurrentHashMap<String, MutableSet<String>>()
    private val dependencyTree = ConcurrentHashMap<String, MutableMap<String, Set<String>>>()

    init {
        registerCustomFunctions()
        registerScriptEvents()
        
        // Initialize script-specific libraries
        tcpLib = TCPLib()
        threadLib = ThreadLib()
        
        // Load libraries into globals
        luaManager.globals.load(threadLib)
        luaManager.globals.load(tcpLib)
    }

    private fun registerScriptEvents() {
        // Note: registerUnloadCallback is handled by LuaManager directly
        // as it needs access to the current executing script
    }

    // Helper methods to access LuaManager's objects
    fun worldRendererObject(context: PrimitiveCollector?): Any {
        return WorldRendererObject(context)
    }

    fun twoRenderObject(context: GuiGraphics?, scriptName: String): Any {
        return TwoRenderObject(context, scriptName)
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
    }

    private fun registerEventRegistrationFunctions() {
        luaManager.globals.set("registerInventoryItemAdd", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addInventoryItemAddCallback(callback))
            }
        })

        luaManager.globals.set("registerUseBlock", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addUseBlockCallback(callback))
            }
        })

        luaManager.globals.set("registerClientTick", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addClientTickCallback(callback))
            }
        })

        luaManager.globals.set("registerClientTickPost", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addClientTickCallback(callback))
            }
        })

        luaManager.globals.set("registerClientTickPre", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addPreClientTickCallback(callback))
            }
        })

        luaManager.globals.set("registerBlockUpdate", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addBlockUpdateCallback(callback))
            }
        })

        luaManager.globals.set("registerWorldRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addWorldRendererCallback(callback))
            }
        })

        luaManager.globals.set("register2DRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(add2DRendererCallback(callback))
            }
        })

        luaManager.globals.set("registerKeyEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addKeyEventCallback(callback))
            }
        })

        luaManager.globals.set("registerMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addMessageCallback(callback))
            }
        })

        luaManager.globals.set("registerSendMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addSendMessageCallback(callback))
            }
        })

        luaManager.globals.set("registerSendCommandEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addSendCommandCallback(callback))
            }
        })

        luaManager.globals.set("registerLocationChangeEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addLocationChangeCallback(callback))
            }
        })

        luaManager.globals.set("registerImGuiRenderEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addImguiRenderCallback(callback))
            }
        })

        // Packet events
        luaManager.globals.set("registerServerSideRotationEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addServerSideRotationCallback(callback))
            }
        })

        luaManager.globals.set("registerServerSideTeleportEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(addServerSideTeleportCallback(callback))
            }
        })
    }

    private fun registerEventUnregistrationFunctions() {
        luaManager.globals.set("unregisterInventoryItemAdd", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeInventoryItemAddCallback(callback))
            }
        })

        luaManager.globals.set("unregisterUseBlock", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeUseBlockCallback(callback))
            }
        })

        luaManager.globals.set("unregisterClientTick", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeClientTickCallback(callback))
            }
        })

        luaManager.globals.set("unregisterClientTickPost", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeClientTickCallback(callback))
            }
        })

        luaManager.globals.set("unregisterClientTickPre", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeClientPreTickCallback(callback))
            }
        })

        luaManager.globals.set("unregisterBlockUpdate", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeBlockUpdateCallback(callback))
            }
        })

        luaManager.globals.set("unregisterWorldRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeWorldRendererCallback(callback))
            }
        })

        luaManager.globals.set("unregister2DRenderer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(remove2DRendererCallback(callback))
            }
        })

        luaManager.globals.set("unregisterKeyEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeKeyEventCallback(callback))
            }
        })

        luaManager.globals.set("unregisterMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeMessageEventCallback(callback))
            }
        })

        luaManager.globals.set("unregisterSendMessageEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeSendMessageEventCallback(callback))
            }
        })

        luaManager.globals.set("unregisterSendCommandEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeSendCommandEventCallback(callback))
            }
        })

        luaManager.globals.set("unregisterLocationChangeEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeLocationChangeEventCallback(callback))
            }
        })

        luaManager.globals.set("unregisterImGuiRenderEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeImGuiRenderEventCallback(callback))
            }
        })

        // Packet events
        luaManager.globals.set("unregisterServerSideRotationEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeServerSideRotationEventCallback(callback))
            }
        })

        luaManager.globals.set("unregisterServerSideTeleportEvent", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                return LuaValue.valueOf(removeServerSideTeleportEventCallback(callback))
            }
        })
    }

    private fun registerCommandFunctions() {
        luaManager.globals.set("registerCommand", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val commandName = args.arg(1).checkjstring()
                val callback = args.arg(2)
                return LuaValue.valueOf(addCommandCallback(commandName, callback))
            }
        })

        luaManager.globals.set("unregisterCommand", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val commandName = args.arg(1).checkjstring()
                return LuaValue.valueOf(removeCommandCallback(commandName))
            }
        })
    }

    private fun registerRequireFunction() {
        luaManager.globals.set("require", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val moduleName = args.arg(1).checkjstring()

                // Загружаем модуль через LuaManager
                val result = luaManager.requireModule(moduleName, scriptName)
                
                // Добавляем в зависимости только при первом требовании
                addDependency(moduleName)
                
                return result
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
            luaManager.registerMinecraftCommand(commandName)

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
            val removed = commandCallbacks.remove(commandName) != null

            if (removed) {
                luaManager.removeCommandCallback(commandName)
            }

            return removed
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
        dependencies.getOrPut(scriptName) { mutableSetOf() }.add(moduleName)
    }
    
    fun updateNestedDependencies(moduleName: String, nestedDeps: Set<String>) {
        val currentTree = dependencyTree.getOrPut(scriptName) { mutableMapOf() }
        currentTree[moduleName] = nestedDeps
    }
    
    fun getDependencies(): List<String> {
        return dependencies[scriptName]?.toList() ?: emptyList()
    }
    
    fun getDependencyTree(): Map<String, Set<String>> {
        return dependencyTree[scriptName] ?: emptyMap()
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

        // Очищаем persistent globals
        persistentGlobals.clear()
        
        // Очищаем зависимости
        dependencies.clear()
        dependencyTree.clear()
        
        // Очищаем библиотеки
        threadLib.stopAllThreads()
        tcpLib.cleanup()
    }
}