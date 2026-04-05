package com.nekiplay.neoscripts.mixins;

import com.nekiplay.neoscripts.events.MouseButtonEvent;
import com.nekiplay.neoscripts.utils.misc.input.Input;
import com.nekiplay.neoscripts.utils.misc.input.KeyAction;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo info) {
        Input.setButtonState(input.input(), action != GLFW_RELEASE);

        InteractionResult result = MouseButtonEvent.EVENT.invoker().onKeyEvent(new MouseButtonEvent(input.input(), KeyAction.get(action)));

        if (result == InteractionResult.FAIL) {
            info.cancel();
        }
    }
}