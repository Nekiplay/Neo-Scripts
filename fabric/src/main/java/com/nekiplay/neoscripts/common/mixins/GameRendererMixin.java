package com.nekiplay.neoscripts.common.mixins;

import com.nekiplay.neoscripts.ClientMain;
import com.nekiplay.neoscripts.client.features.lua.LuaClientScript;
import com.nekiplay.neoscripts.client.features.lua.objects.misc.Textures;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void runTickTail(CallbackInfo ci) {
        try {
            Textures.onFrameRendered();
        } catch (Exception ignored) {
        }
        ClientMain.LUA_MANAGER.getScripts().forEach((name, script) -> {
            if (script instanceof LuaClientScript clientScript && clientScript.getImguiLib() != null) {
                clientScript.getImguiLib().onFrameRender();
            }
        });
    }
}