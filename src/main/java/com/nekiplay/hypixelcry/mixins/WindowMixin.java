package com.nekiplay.hypixelcry.mixins;

import com.nekiplay.hypixelcry.imgui.ImguiLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class WindowMixin {
    @Shadow
    @Final
    private Window window;

    @Inject(at = @At("TAIL"),method = "<init>")
    private void onGlfwInit(RunArgs args, CallbackInfo ci){
        ImguiLoader.onGlfwInit(window.getHandle());
    }

    @Inject(at = @At("RETURN"),method = "<init>")
    private void onGlfwReturn(RunArgs args, CallbackInfo ci){
        ImguiLoader.onGlfwReturn(window.getHandle());
    }
}