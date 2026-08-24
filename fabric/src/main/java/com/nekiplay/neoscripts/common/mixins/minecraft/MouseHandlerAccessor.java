package com.nekiplay.neoscripts.common.mixins.minecraft;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("mouseGrabbed")
    boolean getMouseGrabbed();

    @Accessor("mouseGrabbed")
    void setMouseGrabbed(boolean grabbed);
}
