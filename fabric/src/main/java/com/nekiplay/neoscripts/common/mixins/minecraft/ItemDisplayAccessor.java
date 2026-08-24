package com.nekiplay.neoscripts.common.mixins.minecraft;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayAccessor {
    @Invoker("getItemStack")
    ItemStack neoscripts$getItemStack();

    @Invoker("setItemStack")
    void neoscripts$setItemStack(ItemStack stack);
}
