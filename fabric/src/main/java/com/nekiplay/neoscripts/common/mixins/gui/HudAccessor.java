package com.nekiplay.neoscripts.common.mixins.gui;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface HudAccessor {
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
