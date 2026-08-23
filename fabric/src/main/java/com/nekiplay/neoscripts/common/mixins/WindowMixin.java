package com.nekiplay.neoscripts.common.mixins;

import com.mojang.blaze3d.platform.Window;
import com.nekiplay.neoscripts.ClientMain;
import com.nekiplay.neoscripts.client.features.lua.LuaClientScript;
import com.nekiplay.neoscripts.common.features.lua.Script;
import net.minecraft.client.GameLoadCookie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.ArrayList;

import static com.nekiplay.neoscripts.ClientMain.neuDir;

@Mixin(Minecraft.class)
public class WindowMixin {
    @Shadow
    @Final
    private Window window;

    @Unique
    private final ArrayList<String> startUpScriptNames = new ArrayList<String>() {{
        add("autoload.lua");
        add("startup.lua");
        add("init.lua");
    }};

    @Unique
    private void loadStartupScripts(File dir) {
        // Автозагрузка скриптов при старте
        for (String name : startUpScriptNames) {
            File autoLoadScript = new File(dir, name);
            if (autoLoadScript.exists()) {
                try {
                    Script script = ClientMain.LUA_MANAGER.getScript(autoLoadScript, false, null);
                    ClientMain.LUA_MANAGER.executeScript(autoLoadScript, script);
                    System.out.println("Autoload script \"" + name + "\" executed successfully");
                } catch (Exception e) {
                    System.out.println("Error executing autoload script \"" + name + "\": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"),method = "onGameLoadFinished")
    private void onGlfwInit(GameLoadCookie cookie, CallbackInfo ci){
        File scriptsDir = new File(neuDir, "scripts");
        if (!scriptsDir.exists()) {
            scriptsDir.mkdir();
        }
        loadStartupScripts(scriptsDir);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initImGui(GameConfig gameConfig, CallbackInfo ci) {
        ClientMain.LUA_MANAGER.getScripts().forEach((name, script) -> {
            if (script instanceof LuaClientScript clientScript) {
                if (clientScript.getImguiLib() != null) {
                    clientScript.getImguiLib().onGlfwInit();
                }
            }
        });
    }
}