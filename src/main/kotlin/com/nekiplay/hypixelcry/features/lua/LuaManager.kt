package com.nekiplay.hypixelcry.features.lua

import com.nekiplay.hypixelcry.features.lua.objects.render.TwoRenderObject
import kotlinx.io.files.FileNotFoundException
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class LuaManager() {
    // Script management
    val scripts = ConcurrentHashMap<String, LuaScript>()

    companion object {
        // Cache module file extensions
        val configDir = FabricLoader.getInstance().configDir
    }

    fun executeScript(file: File): Any {
        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException("Script file not found: ${file.path}")
        }

        val scriptName = file.nameWithoutExtension

        try {

            val bytes = file.readBytes()
            val buffer = ByteBuffer.allocateDirect(bytes.size)
            buffer.put(bytes)
            buffer.flip()

            val script = LuaScript(scriptName, this)
            scripts[scriptName] = script

            val scriptGlobals = script.L
            scriptGlobals.load(buffer, scriptName)
            scriptGlobals.pCall(0, 1)

            return scriptGlobals.get()
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
