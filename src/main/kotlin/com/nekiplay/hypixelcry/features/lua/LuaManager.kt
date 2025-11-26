package com.nekiplay.hypixelcry.features.lua

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.misc.ImGuiLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.JsonLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ThreadLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.http.HttpClientLib
import com.nekiplay.hypixelcry.features.lua.objects.modules.ModulesObject
import com.nekiplay.hypixelcry.features.lua.objects.player.PlayerObject
import com.nekiplay.hypixelcry.features.lua.objects.render.TwoRenderObject
import com.nekiplay.hypixelcry.features.lua.objects.render.WorldRendererObject
import com.nekiplay.hypixelcry.features.lua.objects.world.WorldObject
import com.nekiplay.hypixelcry.sugar.getFormattedString
import com.nekiplay.hypixelcry.utils.Location
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction
import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector
import kotlinx.io.files.FileNotFoundException
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class LuaManager() {
    private val globals: Globals = JsePlatform.standardGlobals()
    private val persistentGlobals = ConcurrentHashMap<String, LuaValue>()

    private val scriptDependencies = ConcurrentHashMap<String, MutableSet<String>>()
    private val moduleDependents = ConcurrentHashMap<String, MutableSet<String>>()

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
    private val locationChangeCallbacks = ArrayList<LuaValue>()
    private val imguiRenderCallbacks = ArrayList<LuaValue>()

    // Script events
    private val scriptUnloadCallbacks: MutableMap<String, MutableList<LuaValue>> = mutableMapOf()

    // Packet events
    private val serverSideRotationCallbacks = ArrayList<LuaValue>()
    private val serverSideTeleportCallbacks = ArrayList<LuaValue>()

    // Command events
    private val commandCallbacks = ConcurrentHashMap<String, LuaValue>()
    private val scriptCommands = ConcurrentHashMap<String, MutableSet<String>>()

    // Synchronize only when needed
    @Volatile private var callbacksLock = Any()

    // Precompute system globals
    private val systemGlobals = setOf(
        "print", "require", "registerClientTick", "registerWorldRenderer",
        "unregisterClientTick", "unregisterWorldRenderer", "player", "world", "modules"
    )

    // Cache module file extensions
    private val luaExtensions = arrayOf(".lua", ".luac")

    // Precompute config paths
    private val configDir = FabricLoader.getInstance().configDir
    private val baseModuleSearchPaths = arrayOf(
        "/",
        "libs/",
        "lib/",
        configDir.resolve("hypixelcry/scripts/libs/").toString() + "/",
        configDir.resolve("hypixelcry/scripts/lib/").toString() + "/",
        configDir.resolve("hypixelcry/scripts/").toString() + "/"
    )

    private val moduleSearchPaths = CopyOnWriteArrayList<String>().apply {
        addAll(baseModuleSearchPaths)
    }

    // Initialize objects once
    private val playerObj = PlayerObject()
    private val worldObj = WorldObject()
    private val modulesObj = ModulesObject()

    private val imguiLib = ImGuiLib()
    private val jsonLib = JsonLib()
    private val httpLib = HttpClientLib()

    private val threadLibs = ConcurrentHashMap<String, ThreadLib>()

    private val scriptCallbacks = ConcurrentHashMap<String, MutableList<LuaValue>>()
    private val scriptPersistentGlobals = ConcurrentHashMap<String, ConcurrentHashMap<String, LuaValue>>()

    private val currentExecutingScript = AtomicReference<String?>()

    init {
        registerCustomFunctions(globals)
        registerGlobalObjects(globals)
        registerLibraries(globals)
    }

    private fun registerLibraries(globals: Globals) {
        // Регистрируем библиотеки
        globals.load(imguiLib)
        globals.load(jsonLib)
        globals.load(httpLib)
    }

    private fun registerRequire() {
        globals.set("require", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val moduleName = arg.checkjstring()

                val code: String? = when {
                    moduleName.startsWith("http://") || moduleName.startsWith("https://") -> {
                        loadCodeFromUrl(moduleName)
                            ?: throw LuaError("Failed to load module from URL: $moduleName")
                    }
                    moduleName.startsWith("code://") -> {
                        moduleName.removePrefix("code://")
                    }
                    else -> null
                }

                val result = if (code != null) {
                    val chunk = globals.load(code, moduleName)
                    chunk.call()
                } else {
                    val file = findModuleFile(moduleName)
                        ?: throw LuaError("Module file not found: $moduleName")
                    val chunk = loadChunk(file, moduleName)
                    chunk.call()
                }

                return result
            }
        })
    }

    private fun loadCodeFromUrl(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                conn.inputStream.bufferedReader().use(BufferedReader::readText)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun registerCustomFunctions(globals: Globals) {
        globals.set("HypixelCry", CoerceJavaToLua.coerce(HypixelCry.getInstance()))


        globals.set("print", object : VarArgFunction() {
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

        globals.set("registerUnloadCallback", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                val scriptName = currentExecutingScript.get()
                if (scriptName == null || !callback.isfunction()) return LuaValue.FALSE
                synchronized(scriptUnloadCallbacks) {
                    val list = scriptUnloadCallbacks.getOrPut(scriptName) { mutableListOf() }
                    list.add(callback)
                }
                return LuaValue.TRUE
            }
        })
        globals.set("registerCommand", object : TwoArgFunction() {
            override fun call(commandName: LuaValue, callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addCommandCallback(commandName.checkjstring(), callback))
            }
        })

        globals.set("unregisterCommand", object : OneArgFunction() {
            override fun call(commandName: LuaValue): LuaValue {
                return LuaValue.valueOf(removeCommandCallback(commandName.checkjstring()))
            }
        })

        globals.set("registerClientTick", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addClientTickCallback(callback))
            }
        })
        globals.set("registerClientTickPost", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addClientTickCallback(callback))
            }
        })
        globals.set("registerClientTickPre", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addPreClientTickCallback(callback))
            }
        })
        globals.set("registerBlockUpdate", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addBlockUpdateCallback(callback))
            }
        })
        globals.set("registerWorldRenderer", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addWorldRendererCallback(callback))
            }
        })
        globals.set("register2DRenderer", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(add2DRendererCallback(callback))
            }
        })
        globals.set("registerKeyEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addKeyEventCallback(callback))
            }
        })

        globals.set("registerMessageEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addMessageCallback(callback))
            }
        })

        globals.set("registerSendMessageEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addSendMessageCallback(callback))
            }
        })

        globals.set("registerSendCommandEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addSendCommandCallback(callback))
            }
        })

        globals.set("registerLocationChangeEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addLocationChangeCallback(callback))
            }
        })
        globals.set("registerImGuiRenderEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addImguiRenderCallback(callback))
            }
        })
        // Packets
        globals.set("registerServerSideRotationEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addServerSideRotationCallback(callback))
            }
        })
        globals.set("registerServerSideTeleportEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addServerSideTeleportCallback(callback))
            }
        })

        globals.set("unregisterClientTick", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeClientTickCallback(callback))
            }
        })

        globals.set("unregisterClientTickPost", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeClientTickCallback(callback))
            }
        })

        globals.set("unregisterClientTickPre", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeClientPreTickCallback(callback))
            }
        })
        globals.set("unregisterBlockUpdate", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeBlockUpdateCallback(callback))
            }
        })
        globals.set("unregisterWorldRenderer", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeWorldRendererCallback(callback))
            }
        })
        globals.set("unregister2DRenderer", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(remove2DRendererCallback(callback))
            }
        })
        globals.set("unregisterKeyEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeKeyEventCallback(callback))
            }
        })

        globals.set("unregisterMessageEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeMessageEventCallback(callback))
            }
        })

        globals.set("unregisterSendMessageEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeSendMessageEventCallback(callback))
            }
        })

        globals.set("unregisterSendMessageEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeSendCommandEventCallback(callback))
            }
        })

        globals.set("unregisterLocationChangeEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeLocationChangeEventCallback(callback))
            }
        })
        globals.set("unregisterImGuiRenderEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeImGuiRenderEventCallback(callback))
            }
        })

        // Packets
        globals.set("unregisterServerSideRotationEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeServerSideRotationEventCallback(callback))
            }
        })
        globals.set("unregisterServerSideTeleportEvent", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeServerSideTeleportEventCallback(callback))
            }
        })

        registerRequire()
    }

    private fun registerGlobalObjects(global: Globals) {
        // Register global objects
        globals.set("player", playerObj)
        globals.set("world", worldObj)
        globals.set("modules", modulesObj)
    }

    private fun requireModule(moduleName: String, callingScript: String? = null): LuaValue {
        val moduleFile = findModuleFile(moduleName) ?: throw LuaError("module '$moduleName' not found")

        try {
            val chunk = loadChunk(moduleFile, moduleName)
            val result = chunk.call()

            callingScript?.let { scriptName ->
                updateDependencies(scriptName, moduleName)
            }

            return result
        } catch (e: FileNotFoundException) {
            throw LuaError("module '$moduleName' not found: ${e.message}")
        } catch (e: Exception) {
            throw LuaError("error loading module '$moduleName': ${e.message}")
        }
    }

    private fun loadChunk(file: File, name: String): LuaValue {
        return if (file.extension.equals("luac", ignoreCase = true)) {
            file.inputStream().use { stream ->
                globals.load(stream, name, "b", globals)
            }
        } else {
            globals.load(StringReader(file.readText()), name)
        }
    }

    private fun updateDependencies(scriptName: String, moduleName: String) {
        scriptDependencies.getOrPut(scriptName) { mutableSetOf() }.add(moduleName)
        moduleDependents.getOrPut(moduleName) { mutableSetOf() }.add(scriptName)
    }

    // Optimized module file finding
    private fun findModuleFile(moduleName: String): File? {
        val baseName = if (moduleName.endsWith(".lua") || moduleName.endsWith(".luac")) {
            moduleName.substringBeforeLast('.')
        } else {
            moduleName
        }

        for (path in moduleSearchPaths) {
            // Check direct files
            for (ext in luaExtensions) {
                val file = File("$path$baseName$ext")
                if (file.exists() && file.isFile) {
                    return file
                }
            }
        }
        return null
    }

    // Methods for adding callbacks
    fun addClientTickCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = clientTickCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addPreClientTickCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = clientPreTickCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addBlockUpdateCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = blockUpdateCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addWorldRendererCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = renderWorldCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun add2DRendererCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = render2DCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addKeyEventCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = keyEventCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addMessageCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = messageEventCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addSendMessageCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = onSendMessageEventCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addSendCommandCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = onSendCommandEventCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addLocationChangeCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = locationChangeCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addImguiRenderCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = imguiRenderCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addServerSideRotationCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = serverSideRotationCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addServerSideTeleportCallback(callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false
        synchronized(callbacksLock) {
            val result = serverSideTeleportCallbacks.add(callback)
            if (result) {
                currentExecutingScript.get()?.let { scriptName ->
                    scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
                }
            }
            return result
        }
    }

    fun addCommandCallback(commandName: String, callback: LuaValue): Boolean {
        if (!callback.isfunction()) return false

        if (commandName.isBlank()) return false

        synchronized(callbacksLock) {
            // Проверяем, не зарегистрирована ли уже команда с таким именем
            if (commandCallbacks.containsKey(commandName)) {
                HypixelCry.LOGGER.warn("Command '$commandName' is already registered")
                return false
            }

            commandCallbacks[commandName] = callback

            // Регистрируем команду в Minecraft
            registerMinecraftCommand(commandName)

            // Связываем команду с текущим скриптом
            currentExecutingScript.get()?.let { scriptName ->
                scriptCommands.getOrPut(scriptName) { mutableSetOf() }.add(commandName)
                scriptCallbacks.getOrPut(scriptName) { mutableListOf() }.add(callback)
            }

            HypixelCry.LOGGER.info("Registered Lua command: /$commandName")
            return true
        }
    }

    // Methods for removing callbacks
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
                // Находим скрипт, которому принадлежит команда
                scriptCommands.entries.find { it.value.contains(commandName) }?.let { (scriptName, commands) ->
                    commands.remove(commandName)
                    // Удаляем callback из списка скрипта
                    scriptCallbacks[scriptName]?.removeAll { callback ->
                        try {
                            val result = callback.call()
                            result.isstring() && result.tojstring() == commandName
                        } catch (e: Exception) {
                            false
                        }
                    }
                }

                HypixelCry.LOGGER.info("Unregistered Lua command: /$commandName")
            }

            return removed
        }
    }

    private fun registerMinecraftCommand(commandName: String) {
        try {
            // Регистрируем команду в Minecraft
            ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher: CommandDispatcher<FabricClientCommandSource?>?, registryAccess: CommandRegistryAccess? ->
                dispatcher!!.register(
                    ClientCommandManager.literal(commandName)
                        .executes(Command { context: CommandContext<FabricClientCommandSource?>? ->
                            executeLuaCommand(commandName, emptyArray(), context?.source)
                            1
                        }).then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .executes { context ->
                                val args = StringArgumentType.getString(context, "args").split(" ").toTypedArray()
                                executeLuaCommand(commandName, args, context.source)
                                1
                            }
                        )
                )
            })
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

    // Methods to clear all callbacks
    // Callback methods
    // for multiple handlers
    fun onClientTick() {
        val callbacks = synchronized(callbacksLock) {
            clientTickCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in client tick callback", e)
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
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in client tick callback", e)
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
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in world render callback: ${e.message}")
            }
        }
    }

    fun on2DRenderTick(context: GuiGraphics?) {
        val callbacks = synchronized(callbacksLock) {
            render2DCallbacks.toTypedArray()
        }

        // Используем текущий исполняемый скрипт для создания контекста рендерера
        val currentScript = currentExecutingScript.get()
        val renderContext = TwoRenderObject(context, currentScript)

        for (callback in callbacks) {
            try {
                callback.call(renderContext)
            } catch (e: Exception) {
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in 2D render callback: ${e.message}")
            }
        }
    }

    fun onKeyEvent(key: Int, type: KeyAction) {
        val callbacks = synchronized(callbacksLock) {
            keyEventCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call(LuaValue.valueOf(key), LuaValue.valueOf(type.name))
            } catch (e: Exception) {
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in key callback: ${e.message}")
            }
        }
    }

    fun onChatMessageEvent(text: Component, overlay: Boolean): Boolean {
        val callbacks = synchronized(callbacksLock) {
            messageEventCallbacks.toTypedArray()
        }
        var allow = true
        for (callback in callbacks) {
            try {
                val res = callback.call(LuaValue.valueOf(text.getFormattedString()), LuaValue.valueOf(overlay))
                if (res.isboolean()) {
                    if (!res.toboolean()) {
                        allow = false
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in message callback: ${e.message}")
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
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in send message callback: ${e.message}")
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
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in send command callback: ${e.message}")
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
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in block update callback: ${e.message}")
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
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in location change callback: ${e.message}")
            }
        }
        return true
    }

    fun omImGuiRenderEvent(): Boolean {
        val callbacks = synchronized(callbacksLock) {
            imguiRenderCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in imgui callback: ${e.message}")
            }
        }
        return true
    }

    // Packets
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
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in server side rotation callback: ${e.message}")
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
                HypixelCry.LOGGER.error(HypixelCry.LOG_PREFIX + "Error in server side rotation callback: ${e.message}")
            }
        }
        return allow
    }

    fun executeScript(file: File): Any {
        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException("Script file not found: ${file.path}")
        }

        val scriptName = file.nameWithoutExtension
        val originalRequire = globals.get("require")

        try {
            // Устанавливаем текущий исполняемый скрипт
            currentExecutingScript.set(scriptName)

            // Set up script-specific require
            globals.set("require", createScriptRequireFunction(scriptName))

            saveCurrentGlobals()

            val threadLib = ThreadLib()

            threadLibs[scriptName] = threadLib

            globals.load(threadLib)

            val chunk = loadChunk(file, scriptName)
            val result = chunk.call()
            return result
        } finally {
            // Сбрасываем текущий исполняемый скрипт
            currentExecutingScript.set(null)
            globals.set("require", originalRequire)
            restoreGlobals()
        }
    }

    private fun createScriptRequireFunction(scriptName: String): OneArgFunction {
        return object : OneArgFunction() {
            override fun call(modname: LuaValue): LuaValue {
                val moduleName = modname.checkjstring()

                val code: String? = when {
                    moduleName.startsWith("http://") || moduleName.startsWith("https://") -> {
                        loadCodeFromUrl(moduleName)
                            ?: throw LuaError("Failed to load module from URL: $moduleName")
                    }
                    moduleName.startsWith("code://") -> {
                        moduleName.removePrefix("code://")
                    }
                    else -> null
                }

                return if (code != null) {
                    val chunk = globals.load(code, moduleName)
                    chunk.call()
                } else {
                    requireModule(moduleName, scriptName)
                }
            }
        }
    }

    fun unloadScript(scriptName: String): Boolean {
        val callbacksToRemove = scriptCallbacks[scriptName] ?: return false

        val callbacks = scriptUnloadCallbacks[scriptName]
        if (callbacks != null) {
            callbacks.forEach { callback ->
                try { callback.call() }
                catch (e: Exception) { /* логировать ошибку */ }
            }
            scriptUnloadCallbacks.remove(scriptName)
        }

        synchronized(callbacksLock) {
            clientTickCallbacks.removeAll(callbacksToRemove.toSet())
            renderWorldCallbacks.removeAll(callbacksToRemove.toSet())
            render2DCallbacks.removeAll(callbacksToRemove.toSet())
            keyEventCallbacks.removeAll(callbacksToRemove.toSet())
            messageEventCallbacks.removeAll(callbacksToRemove.toSet())
            clientPreTickCallbacks.removeAll(callbacksToRemove.toSet())
            locationChangeCallbacks.removeAll(callbacksToRemove.toSet())
            imguiRenderCallbacks.removeAll(callbacksToRemove.toSet())
            blockUpdateCallbacks.removeAll(callbacksToRemove.toSet())
            onSendCommandEventCallbacks.removeAll(callbacksToRemove.toSet())
            onSendMessageEventCallbacks.removeAll(callbacksToRemove.toSet())

            // Packets
            serverSideRotationCallbacks.removeAll(callbacksToRemove.toSet())
            serverSideTeleportCallbacks.removeAll(callbacksToRemove.toSet())
        }

        // Clean up dependencies
        scriptDependencies[scriptName]?.let { dependencies ->
            for (moduleName in dependencies) {
                moduleDependents[moduleName]?.remove(scriptName)

                // Unload unused modules
                if (moduleDependents[moduleName].isNullOrEmpty()) {
                    moduleDependents.remove(moduleName)
                }
            }
            scriptDependencies.remove(scriptName)
        }

        scriptCommands[scriptName]?.let { commands ->
            for (commandName in commands) {
                // Удаляем из commandCallbacks
                commandCallbacks.remove(commandName)
            }
            scriptCommands.remove(scriptName)
        }

        threadLibs[scriptName]?.stopThreads()
        threadLibs.remove(scriptName)

        // Clean up stored data
        scriptPersistentGlobals.remove(scriptName)
        scriptCallbacks.remove(scriptName)

        // Очищаем кэш текстур для этого скрипта
        TwoRenderObject.clearScriptCache(scriptName)

        return true
    }

    // Optimized globals management
    private fun saveCurrentGlobals() {
        globals.keys().forEach { key ->
            val name = key.tojstring()
            if (!name.startsWith("_") && !systemGlobals.contains(name)) {
                persistentGlobals[name] = globals.get(key)
            }
        }
    }

    fun getLoadedScripts(): List<String> {
        return scriptCallbacks.keys.toList()
    }

    fun restoreGlobals() {
        persistentGlobals.forEach { (name, value) ->
            globals.set(name, value)
        }
    }

    // Метод для полной очистки всех кэшей
    fun clearAllScriptCaches() {
        TwoRenderObject.clearAllCaches()
    }
}