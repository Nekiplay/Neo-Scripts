package com.nekiplay.hypixelcry.mixins;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Comparator;
import java.util.List;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {
    @Accessor("ENTRY_ORDERING")
    static Comparator<PlayerListEntry> getEntryOrdering_hypixel_cry() {
        throw new AssertionError();
    }

    @Invoker("collectPlayerEntries")
    List<PlayerListEntry> collectPlayerEntries_hypixel_cry();

    @Accessor("footer")
    @Nullable Text getFooter_hypixel_cry();

    @Accessor("header")
    @Nullable Text getHeader_hypixel_cry();
}
