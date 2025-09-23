package com.nekiplay.hypixelcry.features.lua

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.misc.JsonLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.http.HttpClientLib
import com.nekiplay.hypixelcry.features.lua.objects.modules.ModulesObject
import com.nekiplay.hypixelcry.features.lua.objects.player.PlayerObject
import com.nekiplay.hypixelcry.features.lua.objects.render.TwoRenderObject
import com.nekiplay.hypixelcry.features.lua.objects.render.WorldRendererObject
import com.nekiplay.hypixelcry.features.lua.objects.world.WorldObject
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction
import kotlinx.io.files.FileNotFoundException
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.DrawContext
import net.minecraft.network.message.SignedMessage
import net.minecraft.text.Text
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class LuaManager() {
    private val globals: Globals = JsePlatform.standardGlobals()
    private val persistentGlobals = ConcurrentHashMap<String, LuaValue>()

    private val loadedModules = ConcurrentHashMap<String, LuaValue>()
    private val scriptDependencies = ConcurrentHashMap<String, MutableSet<String>>()
    private val moduleDependents = ConcurrentHashMap<String, MutableSet<String>>()

    // Use more efficient collections
    private val clientTickCallbacks = ArrayList<LuaValue>()
    private val renderWorldCallbacks = ArrayList<LuaValue>()
    private val render2DCallbacks = ArrayList<LuaValue>()
    private val keyEventCallbacks = ArrayList<LuaValue>()
    private val messageEventCallbacks = ArrayList<LuaValue>()

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

    private val jsonLib = JsonLib()
    private val httpLib = HttpClientLib()

    private val scriptCallbacks = ConcurrentHashMap<String, MutableList<LuaValue>>()
    private val scriptPersistentGlobals = ConcurrentHashMap<String, ConcurrentHashMap<String, LuaValue>>()

    // Текущий исполняемый скрипт (для определения контекста рендерера)
    private val currentExecutingScript = AtomicReference<String?>()

    init {
        registerCustomFunctions(globals)
        registerGlobalObjects(globals)
        registerLibraries(globals)
    }

    private fun registerLibraries(globals: Globals) {
        // Регистрируем библиотеки
        globals.load(jsonLib)
        globals.load(httpLib)
    }

    private fun registerCustomFunctions(globals: Globals) {
        globals.set("print", object : OneArgFunction() {
            override fun call(message: LuaValue): LuaValue {
                HypixelCry.LOGGER.info(message.tojstring());
                return NIL
            }
        })

        globals.set("registerClientTick", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addClientTickCallback(callback))
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

        globals.set("unregisterClientTick", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeClientTickCallback(callback))
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

        globals.set("require", object : OneArgFunction() {
            override fun call(modname: LuaValue): LuaValue {
                val moduleName = modname.checkjstring()
                return requireModule(moduleName)
            }
        })
    }

    private fun registerGlobalObjects(global: Globals) {
        // Register global objects
        globals.set("player", playerObj)
        globals.set("world", worldObj)
        globals.set("modules", modulesObj)
    }

    private fun requireModule(moduleName: String, callingScript: String? = null): LuaValue {
        // Double-checked locking for module loading
        loadedModules[moduleName]?.let { cachedModule ->
            callingScript?.let { scriptName ->
                updateDependencies(scriptName, moduleName)
            }
            return cachedModule
        }

        return synchronized(loadedModules) {
            // Check again after synchronization
            loadedModules[moduleName]?.let { cachedModule ->
                callingScript?.let { scriptName ->
                    updateDependencies(scriptName, moduleName)
                }
                return@synchronized cachedModule
            }

            val moduleFile = findModuleFile(moduleName) ?: throw LuaError("module '$moduleName' not found")

            try {
                val chunk = loadChunk(moduleFile, moduleName)
                val result = chunk.call()

                loadedModules[moduleName] = result
                callingScript?.let { scriptName ->
                    updateDependencies(scriptName, moduleName)
                }

                result
            } catch (e: FileNotFoundException) {
                throw LuaError("module '$moduleName' not found: ${e.message}")
            } catch (e: Exception) {
                throw LuaError("error loading module '$moduleName': ${e.message}")
            }
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

    // Methods for removing callbacks
    fun removeClientTickCallback(callback: LuaValue): Boolean {
        synchronized(callbacksLock) {
            return clientTickCallbacks.remove(callback)
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

    // Methods to clear all callbacks
    fun clearAllCallbacks() {
        synchronized(callbacksLock) {
            clientTickCallbacks.clear()
            renderWorldCallbacks.clear()
            render2DCallbacks.clear()
            keyEventCallbacks.clear()
            messageEventCallbacks.clear()
        }
    }

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
                HypixelCry.LOGGER.error("Error in client tick callback", e)
            }
        }
    }

    fun onRenderTick(context: WorldRenderContext?) {
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

    fun on2DRenderTick(context: DrawContext?) {
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
                HypixelCry.LOGGER.error("Error in 2D render callback: ${e.message}")
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
                HypixelCry.LOGGER.error("Error in key callback: ${e.message}")
            }
        }
    }

    fun onChatMessageEvent(text: Text, message: SignedMessage?) {
        val callbacks = synchronized(callbacksLock) {
            messageEventCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call(LuaValue.valueOf(text.string), LuaValue.valueOf(message?.content?.string))
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("Error in message callback: ${e.message}")
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
            // Устанавливаем текущий исполняемый скрипт
            currentExecutingScript.set(scriptName)

            // Set up script-specific require
            globals.set("require", createScriptRequireFunction(scriptName))

            saveCurrentGlobals()

            val chunk = loadChunk(file, scriptName)
            val result = chunk.call()
            // saveScriptCallbacks(scriptName)
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
                return requireModule(moduleName, scriptName)
            }
        }
    }

    private fun saveScriptCallbacks(scriptName: String) {
        // Сохраняем все колбэки, которые были добавлены во время выполнения этого скрипта
        val scriptCallbacksList = mutableListOf<LuaValue>()
        scriptCallbacks[scriptName] = scriptCallbacksList

        // Сохраняем persistent globals для этого скрипта
        scriptPersistentGlobals[scriptName] = ConcurrentHashMap(persistentGlobals)
    }

    fun unloadScript(scriptName: String): Boolean {
        val callbacksToRemove = scriptCallbacks[scriptName] ?: return false

        synchronized(callbacksLock) {
            clientTickCallbacks.removeAll(callbacksToRemove.toSet())
            renderWorldCallbacks.removeAll(callbacksToRemove.toSet())
            render2DCallbacks.removeAll(callbacksToRemove.toSet())
            keyEventCallbacks.removeAll(callbacksToRemove.toSet())
            messageEventCallbacks.removeAll(callbacksToRemove.toSet())
        }

        // Clean up dependencies
        scriptDependencies[scriptName]?.let { dependencies ->
            for (moduleName in dependencies) {
                moduleDependents[moduleName]?.remove(scriptName)

                // Unload unused modules
                if (moduleDependents[moduleName].isNullOrEmpty()) {
                    loadedModules.remove(moduleName)
                    moduleDependents.remove(moduleName)
                }
            }
            scriptDependencies.remove(scriptName)
        }

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

    private fun isSystemGlobal(name: String): Boolean {
        val systemGlobals = listOf("print", "require", "registerClientTick",
            "registerWorldRenderer", "unregisterClientTick",
            "unregisterWorldRenderer", "player", "world", "modules")
        return systemGlobals.contains(name)
    }

    // Метод для полной очистки всех кэшей
    fun clearAllScriptCaches() {
        TwoRenderObject.clearAllCaches()
    }
}