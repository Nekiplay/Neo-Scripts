package com.nekiplay.hypixelcry.features.lua

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.features.lua.objects.misc.CatboostLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.Creator
import com.nekiplay.hypixelcry.features.lua.objects.misc.EncodingLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ImGuiLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.JsonLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ThreadLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.TCPLib
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
import net.minecraft.commands.CommandBuildContext
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import org.luaj.vm2.lib.jse.LuajavaLib
import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class LuaManager() {
    val globals: Globals = JsePlatform.standardGlobals()
    private val persistentGlobals = ConcurrentHashMap<String, LuaValue>()

    // Command management
    private val commandCallbacks = ConcurrentHashMap<String, LuaValue>()
    private val scriptCommands = ConcurrentHashMap<String, MutableSet<String>>()
    private val scriptCallbacks = ConcurrentHashMap<String, MutableList<LuaValue>>()
    private val scriptPersistentGlobals = ConcurrentHashMap<String, ConcurrentHashMap<String, LuaValue>>()

    // Script management
    public val scripts = ConcurrentHashMap<String, LuaScript>()

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
    private val catboostLib = CatboostLib()
    private val creatorLib = Creator()
    private val encodingLib = EncodingLib()


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
        globals.load(creatorLib)
        globals.load(catboostLib)
        globals.load(encodingLib)
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
                val script = scripts[scriptName] ?: return LuaValue.FALSE
                return LuaValue.valueOf(script.addScriptUnloadCallback(callback))
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
    
    private fun requireModuleWithTracking(moduleName: String, callingScript: String?, currentDepth: Int = 0): LuaValue {
        val moduleFile = findModuleFile(moduleName) ?: throw LuaError("module '$moduleName' not found")

        try {
            // Track the current require depth to prevent infinite recursion
            if (currentDepth > 100) {
                throw LuaError("Maximum require depth exceeded for module '$moduleName'")
            }

            val chunk = loadChunk(moduleFile, moduleName)
            
            // Create a temporary require function that tracks nested dependencies
            val originalRequire = globals.get("require")
            val nestedDependencies = mutableSetOf<String>()
            
            globals.set("require", object : OneArgFunction() {
                override fun call(modname: LuaValue): LuaValue {
                    val nestedModuleName = modname.checkjstring()
                    nestedDependencies.add(nestedModuleName)
                    
                    // Recursively track nested dependencies
                    return requireModuleWithTracking(nestedModuleName, callingScript, currentDepth + 1)
                }
            })
            
            try {
                val result = chunk.call()
                
                // Update the dependency tree with nested dependencies
                callingScript?.let { scriptName ->
                    updateNestedDependencies(scriptName, moduleName, nestedDependencies.toSet())
                }
                
                return result
            } finally {
                // Restore original require
                globals.set("require", originalRequire)
            }
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
        val script = scripts[scriptName]
        script?.addDependency(moduleName)
    }
    
    private fun updateNestedDependencies(scriptName: String, moduleName: String, nestedDeps: Set<String>) {
        val script = scripts[scriptName]
        script?.updateNestedDependencies(moduleName, nestedDeps)
    }
    
    fun getScriptDependencyTree(scriptName: String): Map<String, Set<String>> {
        val script = scripts[scriptName]
        return script?.getDependencyTree() ?: emptyMap()
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


    fun executeScript(file: File): Any {
        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException("Script file not found: ${file.path}")
        }

        val scriptName = file.nameWithoutExtension
        val originalRequire = globals.get("require")

        try {
            // Создаем новый экземпляр LuaScript для этого скрипта
            val script = LuaScript(scriptName, this)
            scripts[scriptName] = script

            // Устанавливаем текущий исполняемый скрипт
            currentExecutingScript.set(scriptName)

            // Set up script-specific require
            globals.set("require", createScriptRequireFunction(scriptName))

            saveCurrentGlobals()

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
                    requireModuleWithTracking(moduleName, scriptName)
                }
            }
        }
    }

    fun unloadScript(scriptName: String): Boolean {
        val script = scripts[scriptName] ?: return false

        // Вызываем cleanup у скрипта, который сам обработает все свои callback'и
        script.cleanup()

        scriptCommands[scriptName]?.let { commands ->
            for (commandName in commands) {
                // Удаляем из commandCallbacks
                commandCallbacks.remove(commandName)
            }
            scriptCommands.remove(scriptName)
        }

        // Clean up stored data
        scriptPersistentGlobals.remove(scriptName)
        scriptCallbacks.remove(scriptName)

        // Очищаем кэш текстур для этого скрипта
        TwoRenderObject.clearScriptCache(scriptName)

        // Удаляем сам скрипт из списка
        scripts.remove(scriptName)

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
        return scripts.keys.toList()
    }
    
    fun getScriptDependencies(scriptName: String): List<String> {
        val script = scripts[scriptName]
        return script?.getDependencies() ?: emptyList()
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