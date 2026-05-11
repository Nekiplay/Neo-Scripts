package com.nekiplay.neoscripts.mixins;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftClientAccessor {
    @Invoker("startUseItem")
    void doItemUse();

    @Invoker(value = "startAttack")
    boolean doAttack();
}
