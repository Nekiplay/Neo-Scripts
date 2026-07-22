package com.nekiplay.neoscripts

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nekiplay.neoscripts.events.main.EventBus
import com.nekiplay.neoscripts.features.commands.impl.LuaCommand
import com.nekiplay.neoscripts.features.lua.LuaManager
import com.nekiplay.neoscripts.features.lua.objects.render.TwoRenderObject
import com.nekiplay.neoscripts.features.modules.ModuleManager.registerInbuilt
import com.nekiplay.neoscripts.sugar.MiningHandler
import com.nekiplay.neoscripts.utils.Utils
import com.nekiplay.neoscripts.utils.render.RenderHelper
import com.nekiplay.neoscripts.utils.scheduler.Scheduler
import io.github.classgraph.ClassGraph
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

object Main : ClientModInitializer {
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
    var neuDir: File? = null
    @JvmField
    var mc: Minecraft = Minecraft.getInstance()
    var INSTANCE: Main? = null
    @JvmField
    val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    @JvmField
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

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            LuaCommand.register(dispatcher, registryAccess)
        }
        // Load events
        RenderHelper.init()
        registerInbuilt()
        Utils.init()
        MiningHandler.init()

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

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("neoscripts", "lua_hud_layer"),
            HudElement { graphics, deltaTracker ->
                // 1. Обновляем контекст в TwoRenderObject перед отрисовкой
                TwoRenderObject.extractBeforeMiscOverlay(graphics, deltaTracker)

                // 2. Отрисовываем ваши Lua-скрипты
                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    try {
                        script.on2DRenderTick(graphics)
                    } catch (e: Exception) {
                        // Обработка ошибок скрипта
                    }
                }
            }
        )
    }
}