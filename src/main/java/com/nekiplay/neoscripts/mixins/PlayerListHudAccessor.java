package com.nekiplay.neoscripts.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

@Mixin(PlayerTabOverlay.class)
public interface PlayerListHudAccessor {
    @Accessor("PLAYER_COMPARATOR")
    static Comparator<PlayerInfo> getEntryOrdering_hypixel_cry() {
        throw new AssertionError();
    }

    @Invoker("getPlayerInfos")
    List<PlayerInfo> collectPlayerEntries_hypixel_cry();

    @Accessor("footer")
    @Nullable Component getFooter_hypixel_cry();

    @Accessor("header")
    @Nullable Component getHeader_hypixel_cry();
}
