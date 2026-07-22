package com.nekiplay.neoscripts.mixins;

import com.nekiplay.neoscripts.Main;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void runTickTail(CallbackInfo ci) {
        Main.LUA_MANAGER.getScripts().forEach((name, script) -> {
            if (script.getImguiLib() != null) {
                script.getImguiLib().onFrameRender();
            }
        });
    }
}