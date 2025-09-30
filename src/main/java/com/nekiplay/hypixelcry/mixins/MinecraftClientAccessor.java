package com.nekiplay.hypixelcry.mixins;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Invoker("doItemUse")
    void doItemUse();

    @Invoker(value = "doAttack")
    boolean doAttack();
}
