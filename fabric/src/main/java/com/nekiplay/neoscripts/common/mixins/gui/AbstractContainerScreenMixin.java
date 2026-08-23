package com.nekiplay.neoscripts.common.mixins.gui;

import com.nekiplay.neoscripts.client.features.lua.LuaClientScript;
import com.nekiplay.neoscripts.common.features.lua.Script;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.nekiplay.neoscripts.ClientMain.LUA_MANAGER;

@Mixin(value = AbstractContainerScreen.class, priority = 1002)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "slotClicked", at = @At(value = "HEAD"))
    private void neoscripts$onSlotClickHead(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        assert LUA_MANAGER != null;
        for (Script script : LUA_MANAGER.getScripts().values()) {
            if (script instanceof LuaClientScript clientScript) {
                clientScript.onSlotClick(slotId, buttonNum, containerInput.id());
            }
        }
    }
}
