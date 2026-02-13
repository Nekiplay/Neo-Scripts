package com.nekiplay.hypixelcry.mixins.gui;

import com.nekiplay.hypixelcry.events.player.AddItemInventoryEvent;
import com.nekiplay.hypixelcry.utils.Utils;
import com.nekiplay.hypixelcry.utils.trackers.PetCache;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {
    @Inject(method = "setItem", at = @At("HEAD"))
    private void onSetStackInSlot(int slot, int revision, ItemStack itemStack, CallbackInfo ci) {
        AddItemInventoryEvent.EVENT.invoker().update(new AddItemInventoryEvent(slot, itemStack));
    }
}