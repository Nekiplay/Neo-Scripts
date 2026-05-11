package com.nekiplay.neoscripts.mixins;

import com.mojang.blaze3d.platform.Window;
import com.nekiplay.neoscripts.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.ArrayList;

import static com.nekiplay.neoscripts.Main.neuDir;

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
                    Main.LUA_MANAGER.executeScript(autoLoadScript);
                    System.out.println("Autoload script \"" + name + "\" executed successfully");
                } catch (Exception e) {
                    System.out.println("Error executing autoload script \"" + name + "\": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @Inject(at = @At("HEAD"),method = "createTitle")
    private void onGlfwInit(CallbackInfoReturnable<String> cir){
        File scriptsDir = new File(neuDir, "scripts");
        if (!scriptsDir.exists()) {
            scriptsDir.mkdir();
        }
        loadStartupScripts(scriptsDir);
    }

    @Inject(at = @At("RETURN"),method = "<init>")
    private void onGlfwReturn(GameConfig args, CallbackInfo ci){
        //ImguiLoader.onGlfwInit();
    }
}