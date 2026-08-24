package com.nekiplay.neoscripts.common.mixins.minecraft;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorStand.class)
public interface ArmorStandAccessor {
    @Invoker("setSmall")
    void nsSetSmall(boolean small);

    @Invoker("setMarker")
    void nsSetMarker(boolean marker);
}
