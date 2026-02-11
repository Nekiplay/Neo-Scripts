package com.nekiplay.hypixelcry.features.lua

import com.mojang.brigadier.CommandDispatcher
import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.misc.CatboostLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.Creator
import com.nekiplay.hypixelcry.features.lua.objects.misc.EncodingLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ImGuiLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.JsonLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.http.HttpClientLib
import com.nekiplay.hypixelcry.features.lua.objects.modules.ModulesObject
import com.nekiplay.hypixelcry.features.lua.objects.player.PlayerObject
import com.nekiplay.hypixelcry.features.lua.objects.render.TwoRenderObject
import com.nekiplay.hypixelcry.features.lua.objects.world.WorldObject
import kotlinx.io.files.FileNotFoundException
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import java.io.File
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

        init {
            moduleSearchPaths.addAll(baseModuleSearchPaths)
        }
    }

    // Initialize objects once
    val playerObj = PlayerObject()
    val worldObj = WorldObject()
    val modulesObj = ModulesObject()

    val imguiLib = ImGuiLib()
    val jsonLib = JsonLib()
    val httpLib = HttpClientLib()
    val catboostLib = CatboostLib()
    val creatorLib = Creator()
    val encodingLib = EncodingLib()


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
