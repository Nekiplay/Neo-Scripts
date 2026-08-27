package com.nekiplay.neoscripts

import com.nekiplay.neoscripts.client.events.main.EventBus
import com.nekiplay.neoscripts.client.features.commands.impl.LuaCommand
import com.nekiplay.neoscripts.client.features.lua.LuaClientScript
import com.nekiplay.neoscripts.common.features.lua.LuaManager
import com.nekiplay.neoscripts.client.features.lua.objects.misc.Textures
import com.nekiplay.neoscripts.client.features.lua.objects.render.TwoRenderObject
import com.nekiplay.neoscripts.client.features.lua.objects.render.WorldRendererObject
import com.nekiplay.neoscripts.client.features.modules.ModuleManager.registerInbuilt
import com.nekiplay.neoscripts.client.sugar.MiningHandler
import com.nekiplay.neoscripts.client.utils.Utils
import com.nekiplay.neoscripts.client.utils.render.RenderHelper
import com.nekiplay.neoscripts.client.utils.scheduler.Scheduler
import io.github.classgraph.ClassGraph
import net.fabricmc.api.ClientModInitializer
import com.nekiplay.neoscripts.common.network.NeoPacketSenders
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

object ClientMain : ClientModInitializer {
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

    /**
     * Папка автозапуска скриптов клиента: <папка игры>/neoscripts/autostart.
     * Все .lua файлы из неё выполняются при старте игры (Minecraft.onGameLoadFinished).
     */
    @JvmField
    val AUTOSTART_DIR: Path = FabricLoader.getInstance().gameDir.resolve(MOD_ID).resolve("autostart")

    val PREFIX: String =
        ChatFormatting.GRAY.toString() + "[" + ChatFormatting.GOLD + "Neo Scripts" + ChatFormatting.GRAY + "] " + ChatFormatting.RESET
    const val LOG_PREFIX: String = "[Neo Scripts] "
    @JvmField
    var neuDir: File? = null
    @JvmField
    var mc: Minecraft = Minecraft.getInstance()
    var INSTANCE: ClientMain? = null
    /**
     * Do not instantiate this class. Use [.getInstance] instead.
     */

    @Deprecated("")
    fun Main() {
        INSTANCE = this
    }

    fun getInstance(): ClientMain? {
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
        LUA_MANAGER = LuaManager(FabricLoader.getInstance().configDir.toFile())
        val scriptsDir: File = File(neuDir, "scripts")
        if (!scriptsDir.exists()) {
            scriptsDir.mkdir()
        }
        val libsDir = File(scriptsDir, "libs")
        if (!libsDir.exists()) {
            libsDir.mkdir()
        }
        // Папка автозапуска в директории игры
        java.nio.file.Files.createDirectories(AUTOSTART_DIR)

        Runtime.getRuntime().addShutdownHook(Thread(Runnable { saveConfig() }))

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            LuaCommand.register(dispatcher, registryAccess)
        }
        InvalidateRenderStateCallback.EVENT.register {
            WorldRendererObject.clearGlyphCache()
        }        // Load events
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
            .acceptPackages("com.nekiplay.neoscripts.client")
            .rejectPackages("com.nekiplay.neoscripts.common.mixins")
            .scan()
            .use { result ->

                for (classInfo in result.allClasses) {
                    if (!classInfo.isStandardClass) continue
                    classes.add(classInfo.loadClass())
                }

            }

        EventBus.init(classes)

        // Packets: client -> server via mixin-free sender (no reflection) — uses C2S payload
        NeoPacketSenders.clientSender = { payload ->
            ClientPlayNetworking.send(payload)
            true
        }

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("neoscripts", "lua_hud_layer"),
            HudElement { graphics, deltaTracker ->
                // 0. Отрисовка иконок предметов для захвата текстур (textures lib)
                try {
                    Textures.onGuiExtract(graphics)
                } catch (_: Exception) {
                }

                // 1. Обновляем контекст в TwoRenderObject перед отрисовкой
                TwoRenderObject.extractBeforeMiscOverlay(graphics, deltaTracker)

                // 2. Отрисовываем ваши Lua-скрипты
                LUA_MANAGER?.scripts?.values?.forEach { script ->
                    if (script is LuaClientScript) {
                        try {
                            script.on2DRenderTick(graphics)
                        } catch (e: Exception) {
                            // Обработка ошибок скрипта
                        }
                    }
                }
            }
        )

        // Общие скрипты автозапуска (<папка игры>/neoscripts/autostart) НЕ запускаются
        // здесь: ServerMain.onInitialize выполняется и на клиенте, и на сервере
        // (Fabric ModInitializer) и запускает их один раз до заморозки реестров.
        // Устаревшие autoload.lua / startup.lua / init.lua (config/neoscripts/scripts)
        // по-прежнему запускаются позже через WindowMixin.onGameLoadFinished.
    }
}