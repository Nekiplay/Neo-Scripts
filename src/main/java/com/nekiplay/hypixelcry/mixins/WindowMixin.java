package com.nekiplay.hypixelcry.mixins;

import com.mojang.blaze3d.platform.Window;
import com.nekiplay.hypixelcry.imgui.ImguiLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class WindowMixin {
    @Shadow
    @Final
    private Window window;

    @Inject(at = @At("TAIL"),method = "<init>")
    private void onGlfwInit(GameConfig args, CallbackInfo ci){
        ImguiLoader.onGlfwInit(window.handle());
    }

    @Inject(at = @At("RETURN"),method = "<init>")
    private void onGlfwReturn(GameConfig args, CallbackInfo ci){
        ImguiLoader.onGlfwReturn(window.handle());
    }
}