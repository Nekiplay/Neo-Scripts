package com.nekiplay.neoscripts

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nekiplay.neoscripts.events.main.EventBus
import com.nekiplay.neoscripts.features.commands.impl.LuaCommand
import com.nekiplay.neoscripts.features.lua.LuaManager
import com.nekiplay.neoscripts.features.modules.impl.misc.LuaEvents
import io.github.classgraph.ClassGraph
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist
import java.io.File
import java.util.logging.Level

/**
 * Main mod class.
 */
@Mod(Main.ID)
object Main {
    const val ID = "neoscripts"
    lateinit var mc: Minecraft
    lateinit var LUA_MANAGER: LuaManager

    @JvmField
    var neuDir: File? = null

    @JvmStatic
    fun id(path: String): Identifier {
        return Identifier.fromNamespaceAndPath(ID, path)
    }

    val PREFIX: String =
        ChatFormatting.GRAY.toString() + "[" + ChatFormatting.GOLD + "Neo Scripts" + ChatFormatting.GRAY + "] " + ChatFormatting.RESET
    const val LOG_PREFIX: String = "[Neo Scripts] "

    @JvmField
    val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    @JvmField
    val GSON_COMPACT: Gson = GsonBuilder().create()

    // the logger for our mod
    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(ID)

    init {
        MOD_BUS.addListener(::onCommonSetup)
        MOD_BUS.addListener(::onClientSetup)

        neuDir = FMLPaths.CONFIGDIR.get().resolve("neoscripts").toFile()
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

        LOGGER.info("Initializing client...")
        NeoForge.EVENT_BUS.register(LuaEvents)
        NeoForge.EVENT_BUS.register(LuaCommand)
    }

    fun saveConfig(){
        for (script in LUA_MANAGER?.getLoadedScripts() ?: emptyList()) {
            LUA_MANAGER?.unloadScript(script.scriptName)
        }
    }

    /**
     * This is used for initializing client specific
     * things such as renderers and keymaps
     * Fired on the mod specific event bus.
     */
    @SubscribeEvent
    private fun onClientSetup(event: FMLClientSetupEvent) {
        mc = Minecraft.getInstance()
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { saveConfig() }))
    }

    @SubscribeEvent
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        LOGGER.info("Hello! This is working!")
    }
}
