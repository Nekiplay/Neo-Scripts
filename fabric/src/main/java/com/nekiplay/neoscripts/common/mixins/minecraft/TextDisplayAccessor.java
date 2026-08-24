package com.nekiplay.neoscripts.common.mixins.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
    @Invoker("getText")
    Component nsGetText();

    @Invoker("setText")
    void nsSetText(Component text);

    @Invoker("getLineWidth")
    int nsGetLineWidth();

    @Invoker("setLineWidth")
    void nsSetLineWidth(int width);

    @Invoker("getTextOpacity")
    byte nsGetTextOpacity();

    @Invoker("setTextOpacity")
    void nsSetTextOpacity(byte opacity);

    @Invoker("getBackgroundColor")
    int nsGetBackgroundColor();

    @Invoker("setBackgroundColor")
    void nsSetBackgroundColor(int color);
}
