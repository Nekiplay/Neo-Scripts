package com.nekiplay.neoscripts.features.lua

import com.nekiplay.neoscripts.features.lua.objects.render.TwoRenderObject
import net.neoforged.fml.loading.FMLPaths
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import java.io.File
import java.io.FileNotFoundException
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class LuaManager() {
    // Script management
    val scripts = ConcurrentHashMap<String, LuaScript>()

    companion object {
        // Cache module file extensions
        private val luaExtensions = arrayOf(".lua", ".luac")
        // Precompute config paths
        val configDir = FMLPaths.CONFIGDIR.get()
        private val baseModuleSearchPaths = arrayOf(
            "/",
            "libs/",
            "lib/",
            configDir.resolve("neoscripts/scripts/libs/").toString() + "/",
            configDir.resolve("neoscripts/scripts/lib/").toString() + "/",
            configDir.resolve("neoscripts/scripts/").toString() + "/"
        )

        private val moduleSearchPaths = CopyOnWriteArrayList<String>().apply {
            addAll(baseModuleSearchPaths)
        }

        fun loadChunk(file: File, name: String, scriptGlobals: Globals): LuaValue {
            return if (file.extension.equals("luac", ignoreCase = true)) {
                file.inputStream().use { stream ->
                    scriptGlobals.load(stream, name, "b", scriptGlobals)
                }
            } else {
                scriptGlobals.load(StringReader(file.readText()), name)
            }
        }

        // Optimized module file finding
        fun findModuleFile(moduleName: String): File? {
            val baseName = if (moduleName.endsWith(".lua") || moduleName.endsWith(".luac")) {
                moduleName.substringBeforeLast('.').replace(".", "/")
            } else {
                moduleName.replace(".", "/")
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

        init {
            moduleSearchPaths.addAll(baseModuleSearchPaths)
        }
    }

    fun executeScript(file: File): Any {
        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException("Script file not found: ${file.path}")
        }

        val scriptName = file.nameWithoutExtension

        try {
            // Создаем новый экземпляр LuaScript для этого скрипта
            val script = LuaScript(scriptName, this)
            scripts[scriptName] = script

            val scriptGlobals = script.scriptGlobals
            val chunk = loadChunk(file, scriptName, scriptGlobals)
            val result = chunk.call()
            script.imguiLib?.onGlfwInit()
            return result
        } finally {

        }
    }


    fun unloadScript(scriptName: String): Boolean {
        val script = scripts[scriptName] ?: return false
        script.cleanup()
        // Очищаем кэш текстур для этого скрипта
        TwoRenderObject.clearScriptCache(scriptName)
        // Удаляем сам скрипт из списка
        scripts.remove(scriptName)
        return true
    }

    fun getLoadedScripts(): List<LuaScript> {
        return scripts.values.toList()
    }

}
