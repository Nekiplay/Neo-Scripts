package com.nekiplay.neoscripts

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nekiplay.neoscripts.events.main.EventBus
import com.nekiplay.neoscripts.features.commands.impl.LuaCommand
import com.nekiplay.neoscripts.features.lua.LuaManager
import com.nekiplay.neoscripts.features.modules.ModuleManager.registerInbuilt
import com.nekiplay.neoscripts.utils.Utils
import com.nekiplay.neoscripts.utils.scheduler.Scheduler
import io.github.classgraph.ClassGraph
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

object Main : ClientModInitializer {
    const val MOD_ID: String = "neoscripts"
    @JvmField
    val LOGGER: Logger? = LoggerFactory.getLogger(MOD_ID)
    @JvmField
    var LUA_MANAGER: LuaManager? = null
    @JvmField
    val CONFIG_DIR: Path = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID)

    val PREFIX: String =
        ChatFormatting.GRAY.toString() + "[" + ChatFormatting.GOLD + "Neo Scripts" + ChatFormatting.GRAY + "] " + ChatFormatting.RESET
    const val LOG_PREFIX: String = "[Neo Scripts] "
    @JvmField
    var neuDir: File? = null
    @JvmField
    var mc: Minecraft = Minecraft.getInstance()
    var INSTANCE: Main? = null
    @JvmField
    val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    val GSON_COMPACT: Gson = GsonBuilder().create()

    /**
     * Do not instantiate this class. Use [.getInstance] instead.
     */
    @Deprecated("")
    fun Main() {
        INSTANCE = this
    }

    fun getInstance(): Main? {
        return INSTANCE
    }

    fun saveConfig(){
        for (script in LUA_MANAGER!!.getLoadedScripts()) {
            LUA_MANAGER!!.unloadScript(script.scriptName)
        }
    }
    /**
     * Ticks the scheduler. Called once at the end of every client tick through
     * [ClientTickEvents.END_CLIENT_TICK].
     *
     * @param client the Minecraft client.
     */
    fun tick(client: Minecraft?) {
        Scheduler.INSTANCE.tick()
    }

    /**
     * This method is responsible for initializing all classes.
     * To have your class initialized you must annotate its initializer method with the `@Init` annotation.
     * At compile time, ASM completely overwrites the content of this method, so adding a call here will do nothing.
     *
     * @see Init
     */
    private fun init() {
    }

    override fun onInitializeClient() {
        neuDir = FabricLoader.getInstance().getConfigDir().resolve("neoscripts").toFile()
        neuDir!!.mkdirs()
        LUA_MANAGER = LuaManager()
        val scriptsDir: File = File(neuDir, "scripts")
        if (!scriptsDir.exists()) {
            scriptsDir.mkdir()
        }
        val libsDir = File(scriptsDir, "libs")
        if (!libsDir.exists()) {
            libsDir.mkdir()
        }

        Runtime.getRuntime().addShutdownHook(Thread(Runnable { saveConfig() }))

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: Minecraft? -> this.tick(client) })
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            LuaCommand.register(dispatcher, registryAccess)
        }

        init()

        registerInbuilt()

        Scheduler.INSTANCE.scheduleCyclic(Runnable { Utils.update() }, 20)

        val classes = HashSet<Class<*>>()

        ClassGraph()
            .enableClassInfo()
            .enableMethodInfo()
            .enableAnnotationInfo()
            .ignoreClassVisibility()
            .ignoreMethodVisibility()
            .ignoreFieldVisibility()
            .acceptPackages("com.nekiplay.neoscripts")
            .rejectPackages("com.nekiplay.neoscripts.mixins")
            .scan()
            .use { result ->

                for (classInfo in result.allClasses) {
                    if (!classInfo.isStandardClass) continue
                    classes.add(classInfo.loadClass())
                }

            }

        EventBus.init(classes)
    }
}