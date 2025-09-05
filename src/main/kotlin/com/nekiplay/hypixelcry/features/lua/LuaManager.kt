package com.nekiplay.hypixelcry.features.lua

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.misc.JsonLib
import com.nekiplay.hypixelcry.features.lua.objects.modules.ModulesObject
import com.nekiplay.hypixelcry.features.lua.objects.player.PlayerObject
import com.nekiplay.hypixelcry.features.lua.objects.modules.PathFinderRendererObject
import com.nekiplay.hypixelcry.features.lua.objects.render.WorldRendererObject
import com.nekiplay.hypixelcry.features.lua.objects.world.WorldObject
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction
import kotlinx.io.files.FileNotFoundException
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.loader.api.FabricLoader
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import net.minecraft.client.MinecraftClient
import org.luaj.vm2.LuaError
import org.luaj.vm2.lib.OneArgFunction
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class LuaManager() {
    private val globals: Globals = JsePlatform.standardGlobals()
    private val persistentGlobals = ConcurrentHashMap<String, LuaValue>()

    private val clientTickCallbacks = CopyOnWriteArrayList<LuaValue>()
    private val renderWorldCallbacks = CopyOnWriteArrayList<LuaValue>()
    private val keyEvetCallbacks = CopyOnWriteArrayList<LuaValue>()

    private val scriptDependencies = ConcurrentHashMap<String, MutableSet<String>>()
    private val moduleDependents = ConcurrentHashMap<String, MutableSet<String>>()

    private val loadedModules = ConcurrentHashMap<String, LuaValue>()
    private val moduleSearchPaths = CopyOnWriteArrayList<String>().apply {
        add("libs/")     // дополнительный путь
        add("lib/")     // дополнительный путь

        // Добавляем пути из конфигурационной директории
        val configDir = FabricLoader.getInstance().configDir
        add(configDir.resolve("hypixelcry/scripts/libs/").toString() + "/")
        add(configDir.resolve("hypixelcry/scripts/lib/").toString() + "/")
    }

    private val scriptCallbacks = ConcurrentHashMap<String, MutableList<LuaValue>>()
    private val scriptPersistentGlobals = ConcurrentHashMap<String, ConcurrentHashMap<String, LuaValue>>()

    init {
        registerCustomFunctions(globals)
        registerGlobalObjects(globals)
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

        globals.set("require", object : OneArgFunction() {
            override fun call(modname: LuaValue): LuaValue {
                val moduleName = modname.checkjstring()
                return requireModule(moduleName)
            }
        })

        globals.load(JsonLib());
    }

    private fun requireModule(moduleName: String, callingScript: String? = null): LuaValue {
        // Проверяем, не загружен ли уже модуль
        loadedModules[moduleName]?.let {
            // Если модуль уже загружен, добавляем зависимость
            callingScript?.let { scriptName ->
                scriptDependencies.getOrPut(scriptName) { mutableSetOf() }.add(moduleName)
                moduleDependents.getOrPut(moduleName) { mutableSetOf() }.add(scriptName)
            }
            return it
        }

        // Ищем файл модуля
        val moduleFile = findModuleFile(moduleName) ?: throw LuaError("module '$moduleName' not found")

        try {
            // Загружаем и выполняем модуль
            val inputStream = moduleFile.inputStream()
            val chunk = globals.load(inputStream, moduleName, "t", globals)
            val result = chunk.call()

            // Сохраняем результат модуля
            loadedModules[moduleName] = result

            // Добавляем зависимость
            callingScript?.let { scriptName ->
                scriptDependencies.getOrPut(scriptName) { mutableSetOf() }.add(moduleName)
                moduleDependents.getOrPut(moduleName) { mutableSetOf() }.add(scriptName)
            }

            return result

        } catch (e: FileNotFoundException) {
            throw LuaError("module '$moduleName' not found: ${e.message}")
        } catch (e: Exception) {
            throw LuaError("error loading module '$moduleName': ${e.message}")
        }
    }

    private fun findModuleFile(moduleName: String): File? {
        val fileName = if (moduleName.endsWith(".lua")) moduleName else "$moduleName.lua"

        for (path in moduleSearchPaths) {
            val file = File(path + fileName)
            if (file.exists() && file.isFile) {
                return file
            }

            // Также проверяем с путем вида moduleName/init.lua
            val initFile = File("$path$moduleName/init.lua")
            if (initFile.exists() && initFile.isFile) {
                return initFile
            }
        }
        return null
    }

    // Methods for adding callbacks
    fun addClientTickCallback(callback: LuaValue): Boolean {
        if (callback.isfunction()) {
            clientTickCallbacks.add(callback)
            return true
        }
        return false
    }

    fun addWorldRendererCallback(callback: LuaValue): Boolean {
        if (callback.isfunction()) {
            renderWorldCallbacks.add(callback)
            return true
        }
        return false
    }

    fun addKeyEventCallback(callback: LuaValue): Boolean {
        if (callback.isfunction()) {
            keyEvetCallbacks.add(callback)
            return true
        }
        return false
    }

    // Methods for removing callbacks
    fun removeClientTickCallback(callback: LuaValue): Boolean {
        return clientTickCallbacks.remove(callback)
    }
    fun removeWorldRendererCallback(callback: LuaValue): Boolean {
        return renderWorldCallbacks.remove(callback)
    }

    fun removeKeyEventCallback(callback: LuaValue): Boolean {
        return keyEvetCallbacks.remove(callback)
    }

    // Methods to clear all callbacks
    fun clearAllCallbacks() {
        clientTickCallbacks.clear()
        renderWorldCallbacks.clear()
        keyEvetCallbacks.clear()
    }

    private fun registerGlobalObjects(global: Globals) {
        // Register global objects
        globals.set("player", PlayerObject())
        globals.set("world", WorldObject())
        globals.set("modules", ModulesObject())
    }

    // Callback methods
    // for multiple handlers
    fun onClientTick() {
        clientTickCallbacks.forEach { callback ->
            try {
                callback.call()
            } catch (e: Exception) {
                println("Error in client tick callback: ${e.message}")
            }
        }
    }

    fun onRenderTick(context: WorldRenderContext?) {
        renderWorldCallbacks.forEach { callback ->
            val renderContext = WorldRendererObject(context)
            try {
                callback.call(renderContext)
            } catch (e: Exception) {
                println("Error in world render callback: ${e.message}")
            }
        }
    }

    fun onKeyEvent(key: Int, type: KeyAction) {
        keyEvetCallbacks.forEach { callback ->
            try {
                callback.call(LuaValue.valueOf(key), LuaValue.valueOf(type.name))
            } catch (e: Exception) {
                println("Error in key callback: ${e.message}")
            }
        }
    }

    fun addModuleSearchPath(path: String) {
        val normalizedPath = if (path.endsWith("/")) path else "$path/"
        moduleSearchPaths.add(normalizedPath)
    }

    fun removeModuleSearchPath(path: String) {
        val normalizedPath = if (path.endsWith("/")) path else "$path/"
        moduleSearchPaths.remove(normalizedPath)
    }

    fun getModuleSearchPaths(): List<String> {
        return moduleSearchPaths.toList()
    }

    fun executeScript(script: String, scriptName: String = "anonymous"): Any {
        saveCurrentGlobals()

        // Временно сохраняем имя текущего скрипта для отслеживания зависимостей
        val originalRequire = globals.get("require")
        globals.set("require", object : OneArgFunction() {
            override fun call(modname: LuaValue): LuaValue {
                val moduleName = modname.checkjstring()
                return requireModule(moduleName, scriptName) // Передаем имя скрипта
            }
        })

        try {
            val chunk = globals.load(script, scriptName)
            val result = chunk.call()

            // Сохраняем callbacks для этого скрипта
            saveScriptCallbacks(scriptName)

            return result
        } finally {
            // Восстанавливаем оригинальный require
            globals.set("require", originalRequire)
            restoreGlobals()
        }
    }

    private fun saveScriptCallbacks(scriptName: String) {
        val scriptCallbacksList = mutableListOf<LuaValue>().apply {
            addAll(clientTickCallbacks)
            addAll(renderWorldCallbacks)
            addAll(keyEvetCallbacks)
        }
        scriptCallbacks[scriptName] = scriptCallbacksList

        // Сохраняем persistent globals для этого скрипта
        scriptPersistentGlobals[scriptName] = ConcurrentHashMap(persistentGlobals)
    }

    fun unloadScript(scriptName: String): Boolean {
        // Удаляем callbacks этого скрипта
        val callbacksToRemove = scriptCallbacks[scriptName] ?: return false

        clientTickCallbacks.removeAll(callbacksToRemove)
        renderWorldCallbacks.removeAll(callbacksToRemove)
        keyEvetCallbacks.removeAll(callbacksToRemove)

        // Удаляем зависимости скрипта и выгружаем неиспользуемые модули
        val dependencies = scriptDependencies[scriptName]
        if (dependencies != null) {
            for (moduleName in dependencies) {
                // Удаляем скрипт из списка зависимых для модуля
                val dependents = moduleDependents[moduleName]
                dependents?.remove(scriptName)

                // Если у модуля больше нет зависимых скриптов, выгружаем его
                if (dependents.isNullOrEmpty()) {
                    loadedModules.remove(moduleName)
                    moduleDependents.remove(moduleName)
                }
            }
            scriptDependencies.remove(scriptName)
        }

        // Удаляем persistent globals этого скрипта
        scriptPersistentGlobals.remove(scriptName)
        scriptCallbacks.remove(scriptName)

        return true
    }

    fun getLoadedScripts(): List<String> {
        return scriptCallbacks.keys.toList()
    }

    public fun restoreGlobals() {
        persistentGlobals.forEach { (name, value) ->
            globals.set(name, value)
        }
    }


    private fun saveCurrentGlobals() {
        // Сохраняем только пользовательские переменные, не системные
        globals.keys().forEach { key ->
            val name = key.tojstring()
            if (!name.startsWith("_") && !isSystemGlobal(name)) {
                persistentGlobals[name] = globals.get(key)
            }
        }
    }

    private fun isSystemGlobal(name: String): Boolean {
        val systemGlobals = listOf("print", "require", "registerClientTick",
            "registerWorldRenderer", "unregisterClientTick",
            "unregisterWorldRenderer", "player", "world", "modules")
        return systemGlobals.contains(name)
    }
}