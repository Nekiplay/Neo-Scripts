package com.nekiplay.hypixelcry.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nekiplay.hypixelcry.imgui.ImguiLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSystem.class, remap = false)
public class TailRenderMixin {
    @Inject(at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), method="flipFrame")
    private static void runTickTail(CallbackInfo ci) {
        //ImguiLoader.onFrameRender();
    }
}