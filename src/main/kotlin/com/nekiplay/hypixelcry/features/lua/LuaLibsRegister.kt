package com.nekiplay.hypixelcry.features.lua

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.text.LuaComponentBuilder
import com.nekiplay.hypixelcry.features.lua.objects.misc.Creator
import com.nekiplay.hypixelcry.features.lua.objects.misc.DJLLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.EncodingLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ImGuiLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.JsonLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.TCPLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ThreadLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.http.HttpClientLib
import com.nekiplay.hypixelcry.features.lua.objects.modules.ModulesLib
import com.nekiplay.hypixelcry.features.lua.objects.player.PlayerObject
import com.nekiplay.hypixelcry.features.lua.objects.world.WorldObject
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import party.iroiro.luajava.ExternalLoader
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import java.io.File
import java.nio.ByteBuffer

class LuaLibsRegister {
    // Теперь переменные могут быть null
    var djlLibrary: DJLLib? = null
    var encoding: EncodingLib? = null
    var creator: Creator? = null
    var json: JsonLib? = null
    var tcp: TCPLib? = null
    var threads: ThreadLib? = null
    var http: HttpClientLib? = null
    var imgui: ImGuiLib? = null
    var modules: ModulesLib? = null

    fun register(lua: Lua) {
        djlLibrary = DJLLib(lua).register()
        encoding = EncodingLib(lua).register()
        creator = Creator(lua).register()
        json = JsonLib(lua).register()
        tcp = TCPLib(lua).register()
        threads = ThreadLib(lua).register()
        threads?.register().register()
        http = HttpClientLib(lua).register()
        imgui = ImGuiLib(lua).register()
        modules = ModulesLib(lua).register()
        
        LuaComponentBuilder.register(lua)

        lua.openLibraries()
        lua.setExternalLoader(object : ExternalLoader {
            override fun load(moduleName: String, l: Lua): ByteBuffer? {
                // 1. Normalize the path (handle both '.' and '/' in require)
                val normalizedName = moduleName.replace('.', File.separatorChar)
                    .replace('/', File.separatorChar)
                val fileName = "$normalizedName.lua"

                // 2. Define the search folders
                // We search in the root scripts folder AND the libs folder
                val scriptsRoot = File(mc.gameDirectory, "config/hypixelcry/scripts")
                val libsFolder = File(scriptsRoot, "libs")

                val candidatePaths = listOf(
                    File(scriptsRoot, fileName),
                    File(libsFolder, fileName)
                )

                for (scriptFile in candidatePaths) {
                    // DEBUG: Uncomment this to see in console where it is looking
                    // println("LuaLoader: Checking path: ${scriptFile.absolutePath}")

                    if (scriptFile.exists()) {
                        try {
                            val bytes = scriptFile.readBytes()
                            val buffer = ByteBuffer.allocateDirect(bytes.size)
                            buffer.put(bytes)
                            buffer.flip()
                            return buffer
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return null
                        }
                    }
                }

                // If we reach here, the file wasn't found in any candidate path
                return null
            }
        })

        lua.push(JFunction { l ->
            val n = l.getTop() // Получаем количество переданных аргументов
            val message = StringBuilder()

            for (i in 1..n) {
                if (i > 1) message.append("\t") // В стандартном Lua print использует табуляцию

                // l.toString(i) преобразует значение в строку (аналог tojstring)
                // Если значение nil или его нельзя превратить в строку, вернется null
                val str = l.toString(i) ?: "nil"
                message.append(str)
            }

            val finalMessage = message.toString()

            // Вывод в лог Minecraft
            HypixelCry.LOGGER.info("${HypixelCry.LOG_PREFIX}$finalMessage")

            // print в Lua обычно ничего не возвращает
            0
        })
        lua.setGlobal("print")

        WorldObject(lua).register()
        PlayerObject(lua).register()
    }

    fun close() {
        threads?.stopAllThreads()
        tcp?.cleanup()
        imgui?.queue?.clear()
        djlLibrary?.models?.clear()
        djlLibrary?.predictors?.clear()
        djlLibrary?.inputShapes?.clear()
        djlLibrary?.modelModes?.clear()
    }
}
