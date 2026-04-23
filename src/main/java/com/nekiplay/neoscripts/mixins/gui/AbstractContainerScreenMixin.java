package com.nekiplay.neoscripts.mixins.gui;

import com.nekiplay.neoscripts.features.lua.LuaScript;
import com.nekiplay.neoscripts.utils.Utils;
import com.nekiplay.neoscripts.utils.trackers.PetCache;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.nekiplay.neoscripts.Main.LUA_MANAGER;

@Mixin(value = AbstractContainerScreen.class, priority = 1002)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "slotClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleInventoryMouseClick(IIILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"))
    private void neoscripts$onSlotClick(Slot slot, int slotId, int button, ClickType clickType, CallbackInfo ci) {
        if (Utils.isOnSkyblock()) {
            if (slot != null) {
                String title = getTitle().getString();
                //Pet Caching
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && title.startsWith("Pets")) {
                    PetCache.handlePetEquip(slot, slotId);
                }
            }
        }
        for (LuaScript script : LUA_MANAGER.getScripts().values()) {
            script.onSlotClick(slot.getContainerSlot(), button, clickType.id());
        }
    }
}
