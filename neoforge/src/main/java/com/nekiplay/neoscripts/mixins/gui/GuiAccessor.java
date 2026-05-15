package com.nekiplay.neoscripts.mixins.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {
    @Nullable
    @Accessor("title")
    Component getTitle();
    @Nullable
    @Accessor("subtitle")
    Component getSubtitle();
    @Nullable
    @Accessor("overlayMessageString")
    Component getActionBar();
}
