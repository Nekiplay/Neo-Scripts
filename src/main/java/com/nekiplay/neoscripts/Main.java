package com.nekiplay.neoscripts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nekiplay.neoscripts.annotations.Init;
import com.nekiplay.neoscripts.features.commands.impl.LuaCommand;
import com.nekiplay.neoscripts.features.lua.LuaScript;
import com.nekiplay.neoscripts.features.modules.ModuleManager;
import com.nekiplay.neoscripts.utils.Rotations;
import com.nekiplay.neoscripts.utils.Utils;
import com.nekiplay.neoscripts.utils.scheduler.Scheduler;
import com.nekiplay.neoscripts.features.lua.LuaManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "neoscripts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static LuaManager LUA_MANAGER;
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);

    public static final String PREFIX = ChatFormatting.GRAY + "[" + ChatFormatting.GOLD + "Neo Scripts" + ChatFormatting.GRAY + "] " + ChatFormatting.RESET;
    public static final String LOG_PREFIX = "[Neo Scripts] ";

    public static File neuDir;
    public static Minecraft mc = Minecraft.getInstance();
    private static Main INSTANCE;

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Gson GSON_COMPACT = new GsonBuilder().create();

    /**
     * Do not instantiate this class. Use {@link #getInstance()} instead.
     */
    @Deprecated
    public Main() {
        INSTANCE = this;
    }

    public static Main getInstance() {
        return INSTANCE;
    }

    public static void saveConfig() {
        for (LuaScript script : LUA_MANAGER.getLoadedScripts()) {
            LUA_MANAGER.unloadScript(script.getScriptName());
        }
    }

    @Override
    public void onInitializeClient() {

        neuDir = FabricLoader.getInstance().getConfigDir().resolve("neoscripts").toFile();
        neuDir.mkdirs();
        LUA_MANAGER = new LuaManager();
        File scriptsDir = new File(neuDir, "scripts");
        if (!scriptsDir.exists()) {
            scriptsDir.mkdir();
        }
        File libsDir = new File(scriptsDir, "libs");
        if (!libsDir.exists()) {
            libsDir.mkdir();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(Main::saveConfig));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientCommandRegistrationCallback.EVENT.register(LuaCommand.INSTANCE::register);

        init();

        ModuleManager.INSTANCE.registerInbuilt();

        Scheduler.INSTANCE.scheduleCyclic(Utils::update, 20);
        loadStartupScripts(scriptsDir);
        Rotations.init();
    }


    private final ArrayList<String> startUpScriptNames = new ArrayList<String>() {{
        add("autoload.lua");
        add("startup.lua");
        add("init.lua");
    }};

    private void loadStartupScripts(File dir) {
        // Автозагрузка скриптов при старте
        for (String name : startUpScriptNames) {
            File autoLoadScript = new File(dir, name);
            if (autoLoadScript.exists()) {
                try {
                    LUA_MANAGER.executeScript(autoLoadScript);
                    System.out.println("Autoload script \"" + name + "\" executed successfully");
                } catch (Exception e) {
                    System.out.println("Error executing autoload script \"" + name + "\": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Ticks the scheduler. Called once at the end of every client tick through
     * {@link ClientTickEvents#END_CLIENT_TICK}.
     *
     * @param client the Minecraft client.
     */
    public void tick(Minecraft client) {
        Scheduler.INSTANCE.tick();
    }

    /**
     * This method is responsible for initializing all classes.
     * To have your class initialized you must annotate its initializer method with the {@code @Init} annotation.
     * At compile time, ASM completely overwrites the content of this method, so adding a call here will do nothing.
     *
     * @see Init
     */
    private static void init() {
    }
}
