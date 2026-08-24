package com.nekiplay.neoscripts.common.features.lua

import com.nekiplay.neoscripts.client.features.lua.LuaClientScript
import com.nekiplay.neoscripts.client.features.lua.objects.render.TwoRenderObject
import com.nekiplay.neoscripts.server.features.lua.LuaServerScript
import kotlinx.io.files.FileNotFoundException
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import java.io.File
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class LuaManager(val configDir: File?) {
    // Script management
    val scripts = ConcurrentHashMap<String, Script>()

    private val baseModuleSearchPaths = arrayOf(
        "/",
        "libs/",
        "lib/",
        "/libs/",
        "/lib/",
        configDir?.resolve("neoscripts/scripts/libs/").toString() + "/",
        configDir?.resolve("neoscripts/scripts/lib/").toString() + "/",
        configDir?.resolve("neoscripts/scripts/").toString() + "/"
    )

    private val moduleSearchPaths = CopyOnWriteArrayList<String>().apply {
        addAll(baseModuleSearchPaths)
    }

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

    companion object {
        // Cache module file extensions
        private val luaExtensions = arrayOf(".lua", ".luac")
        // Precompute config paths

        fun loadChunk(file: File, name: String, scriptGlobals: Globals): LuaValue {
            return if (file.extension.equals("luac", ignoreCase = true)) {
                file.inputStream().use { stream ->
                    scriptGlobals.load(stream, name, "b", scriptGlobals)
                }
            } else {
                scriptGlobals.load(StringReader(file.readText()), name)
            }
        }
    }

    fun getScript(file: File, serverSide: Boolean, server: MinecraftServer? = null): Script {
        val scriptName = file.nameWithoutExtension
        if (serverSide && server != null) {
            return LuaServerScript(scriptName, this, server)
        }
        else {
            return LuaClientScript(scriptName, this)
        }
    }

    fun executeScript(file: File, script: Script): Any {
        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException("Script file not found: ${file.path}")
        }

        val scriptName = file.nameWithoutExtension

        try {
            scripts[scriptName] = script

            val scriptGlobals = script.scriptGlobals
            val chunk = loadChunk(file, scriptName, scriptGlobals)
            val result = chunk.call()
            if (script is LuaClientScript) {
                script.imguiLib?.onGlfwInit()
            }
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

    fun getLoadedScripts(): List<Script> {
        return scripts.values.toList()
    }

}
