package com.nekiplay.neoscripts.common.mixins.minecraft;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorStand.class)
public interface ArmorStandAccessor {
    @Invoker("setSmall")
    void neoscripts$setSmall(boolean small);

    @Invoker("setMarker")
    void neoscripts$setMarker(boolean marker);
}
