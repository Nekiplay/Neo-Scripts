package com.nekiplay.neoscripts.common.mixins;

import com.nekiplay.neoscripts.client.events.KeyEvent;
import com.nekiplay.neoscripts.client.utils.misc.input.Input;
import com.nekiplay.neoscripts.client.utils.misc.input.KeyAction;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    public void onKey(long window, int action, net.minecraft.client.input.KeyEvent input, CallbackInfo ci) {
        if (input.input() != GLFW.GLFW_KEY_UNKNOWN) {
            int modifiers = input.modifiers();
            // on Linux/X11 the modifier is not active when the key is pressed and still active when the key is released
            // https://github.com/glfw/glfw/issues/1630
            if (action == GLFW.GLFW_PRESS) {
                modifiers |= Input.getModifier(input.input());
            } else if (action == GLFW.GLFW_RELEASE) {
                modifiers &= ~Input.getModifier(input.input());
            }

            Input.setKeyState(input.input(), action != GLFW.GLFW_RELEASE);

            InteractionResult result = KeyEvent.EVENT.invoker().onKeyEvent(new KeyEvent(input.input(), modifiers, KeyAction.get(action)));

            if (result == InteractionResult.FAIL) {
                ci.cancel();
            }
        }
    }
}