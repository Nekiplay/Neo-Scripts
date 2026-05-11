package com.nekiplay.neoscripts.mixins.gui;

import com.nekiplay.neoscripts.events.player.AddItemInventoryEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {
    @Inject(method = "setItem", at = @At("HEAD"))
    private void onSetStackInSlot(int slot, int revision, ItemStack itemStack, CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new AddItemInventoryEvent(slot, itemStack));
    }
}