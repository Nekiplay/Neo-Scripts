package com.nekiplay.neoscripts.common.mixins.minecraft;

import net.minecraft.world.entity.Interaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Interaction.class)
public interface InteractionAccessor {
    @Invoker("getWidth")
    float neoscripts$getWidth();

    @Invoker("setWidth")
    void neoscripts$setWidth(float width);

    @Invoker("getHeight")
    float neoscripts$getHeight();

    @Invoker("setHeight")
    void neoscripts$setHeight(float height);

    @Invoker("getResponse")
    boolean neoscripts$getResponse();

    @Invoker("setResponse")
    void neoscripts$setResponse(boolean response);
}
