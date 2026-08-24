package com.nekiplay.neoscripts.common.mixins.minecraft;

import net.minecraft.world.entity.Interaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Interaction.class)
public interface InteractionAccessor {
    @Invoker("getWidth")
    float nsGetWidth();

    @Invoker("setWidth")
    void nsSetWidth(float width);

    @Invoker("getHeight")
    float nsGetHeight();

    @Invoker("setHeight")
    void nsSetHeight(float height);

    @Invoker("getResponse")
    boolean nsGetResponse();

    @Invoker("setResponse")
    void nsSetResponse(boolean response);
}
