package com.nekiplay.hypixelcry.mixins;

import com.nekiplay.hypixelcry.events.KeyEvent;
import com.nekiplay.hypixelcry.utils.misc.input.Input;
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    public void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        if (input.getKeycode() != GLFW.GLFW_KEY_UNKNOWN) {
            int modifiers = input.modifiers();
            // on Linux/X11 the modifier is not active when the key is pressed and still active when the key is released
            // https://github.com/glfw/glfw/issues/1630
            if (action == GLFW.GLFW_PRESS) {
                modifiers |= Input.getModifier(input.getKeycode());
            } else if (action == GLFW.GLFW_RELEASE) {
                modifiers &= ~Input.getModifier(input.getKeycode());
            }

            Input.setKeyState(input.getKeycode(), action != GLFW.GLFW_RELEASE);

            ActionResult result = KeyEvent.EVENT.invoker().onKeyEvent(new KeyEvent(input.getKeycode(), modifiers, KeyAction.get(action)));

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }
}