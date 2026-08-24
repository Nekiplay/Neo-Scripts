package com.nekiplay.neoscripts

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nekiplay.neoscripts.client.events.main.EventBus
import com.nekiplay.neoscripts.client.features.commands.impl.LuaCommand
import com.nekiplay.neoscripts.common.features.lua.LuaManager
import com.nekiplay.neoscripts.server.features.modules.ModuleManager.registerInbuilt
import io.github.classgraph.ClassGraph
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


object ServerMain : ModInitializer {
    const val NAMESPACE: String = "neoscripts"

    @JvmStatic
    fun id(path: String): Identifier {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path)
    }

    const val MOD_ID: String = "neoscripts"
    @JvmField
    val LOGGER: Logger? = LoggerFactory.getLogger(MOD_ID)
    @JvmField
    var LUA_MANAGER: LuaManager? = null
    @JvmField
    val CONFIG_DIR: Path = FabricLoader.getInstance().configDir.resolve(MOD_ID)

    val PREFIX: String =
        ChatFormatting.GRAY.toString() + "[" + ChatFormatting.GOLD + "Neo Scripts" + ChatFormatting.GRAY + "] " + ChatFormatting.RESET
    const val LOG_PREFIX: String = "[Neo Scripts] "
    @JvmField
    val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    @JvmField
    val GSON_COMPACT: Gson = GsonBuilder().create()
    @JvmField
    var SERVER: MinecraftServer? = null
    @JvmField
    var scriptsDir: File? = null

    fun saveConfig(){
        LUA_MANAGER?.getLoadedScripts()?.forEach { script ->
            LUA_MANAGER?.unloadScript(script.scriptName)
        }
    }

    private val startUpScriptNames = arrayOf("autoload.lua", "startup.lua", ("init.lua"))

    override fun onInitialize() {

        Runtime.getRuntime().addShutdownHook(Thread(Runnable { saveConfig() }))

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            LuaCommand.register(dispatcher, registryAccess)
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, _ ->
            com.nekiplay.neoscripts.server.features.commands.impl.LuaCommand.register(dispatcher, registryAccess)
        }
        registerInbuilt()

        val classes = HashSet<Class<*>>()

        ClassGraph()
            .enableClassInfo()
            .enableMethodInfo()
            .enableAnnotationInfo()
            .ignoreClassVisibility()
            .ignoreMethodVisibility()
            .ignoreFieldVisibility()
            .acceptPackages("com.nekiplay.neoscripts.server")
            .rejectPackages("com.nekiplay.neoscripts.common.mixins")
            .scan()
            .use { result ->

                for (classInfo in result.allClasses) {
                    if (!classInfo.isStandardClass) continue
                    classes.add(classInfo.loadClass())
                }

            }

        EventBus.init(classes)

        ServerLifecycleEvents.SERVER_STARTED.register(ServerStarted { server: MinecraftServer? ->
            SERVER = server
            val worldRoot = server?.getWorldPath(LevelResource.ROOT)


            val neuDir = worldRoot!!.resolve("neoscripts")
            Files.createDirectories(neuDir)

            val scriptsDir2 = neuDir.resolve("scripts")
            Files.createDirectories(scriptsDir2)
            scriptsDir = scriptsDir2.toFile()

            val libsDir = scriptsDir2.resolve("libs")
            Files.createDirectories(libsDir)

            LUA_MANAGER = LuaManager(scriptsDir2.toFile())
            LUA_MANAGER?.addSearchPath(worldRoot.toString())
            LUA_MANAGER?.addSearchPath(neuDir.toString())

            for (name in startUpScriptNames) {
                val autoLoadScript: File = File(scriptsDir2.toFile(), name)
                if (autoLoadScript.exists()) {
                    try {
                        val script = LUA_MANAGER!!.getScript(autoLoadScript, true, server)
                        LUA_MANAGER?.executeScript(autoLoadScript, script)
                        println("Autoload script \"" + name + "\" executed successfully")
                    } catch (e: Exception) {
                        println("Error executing autoload script \"" + name + "\": " + e.message)
                        e.printStackTrace()
                    }
                }
            }
        })

        ServerLifecycleEvents.SERVER_STOPPED.register(ServerStopped { server: MinecraftServer? ->
            LUA_MANAGER?.scripts?.values?.forEach { script ->
                script.cleanup()
            }
            LUA_MANAGER = null
            SERVER = null
        })
    }
}