package com.nekiplay.neoscripts.mixins;

import com.mojang.blaze3d.systems.GpuSurface;
import com.nekiplay.neoscripts.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GpuSurface.class, remap = false)   // цель – класс поверхности
public class TailRenderMixin {

    @Inject(
            method = "present",                    // метод, выполняющий смену буферов
            at = @At("RETURN")                     // после выполнения
    )
    private void runTickTail(CallbackInfo ci) {
        assert Main.LUA_MANAGER != null;
        Main.LUA_MANAGER.getScripts().forEach((name, script) -> {
            if (script.getImguiLib() != null) {
                script.getImguiLib().onFrameRender();
            }
        });
    }
}