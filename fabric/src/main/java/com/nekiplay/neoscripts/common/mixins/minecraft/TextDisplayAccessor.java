package com.nekiplay.neoscripts.common.mixins.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
    @Invoker("getText")
    Component neoscripts$getText();

    @Invoker("setText")
    void neoscripts$setText(Component text);

    @Invoker("getLineWidth")
    int neoscripts$getLineWidth();

    @Invoker("setLineWidth")
    void neoscripts$setLineWidth(int width);

    @Invoker("getTextOpacity")
    byte neoscripts$getTextOpacity();

    @Invoker("setTextOpacity")
    void neoscripts$setTextOpacity(byte opacity);

    @Invoker("getBackgroundColor")
    int neoscripts$getBackgroundColor();

    @Invoker("setBackgroundColor")
    void neoscripts$setBackgroundColor(int color);
}
