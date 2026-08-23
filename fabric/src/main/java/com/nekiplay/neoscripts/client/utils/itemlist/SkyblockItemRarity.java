package com.nekiplay.neoscripts.client.utils.itemlist;

import java.util.Arrays;
import java.util.Optional;

import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.StringRepresentable;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.nekiplay.neoscripts.client.utils.EnumUtils;

public enum SkyblockItemRarity implements StringRepresentable {
    COMMON(TextColor.WHITE),
    UNCOMMON(TextColor.GREEN),
    RARE(TextColor.BLUE),
    EPIC(TextColor.DARK_PURPLE),
    LEGENDARY(TextColor.GOLD),
    MYTHIC(TextColor.LIGHT_PURPLE),
    DIVINE(TextColor.AQUA),
    SPECIAL(TextColor.RED),
    VERY_SPECIAL(TextColor.RED),
    ULTIMATE(TextColor.DARK_RED),
    ADMIN(TextColor.DARK_RED),
    UNKNOWN(TextColor.DARK_GRAY);

    public static final Codec<SkyblockItemRarity> CODEC = StringRepresentable.fromEnum(SkyblockItemRarity::values);
    public final TextColor textColor;
    public final int color;
    public final float r;
    public final float g;
    public final float b;

    SkyblockItemRarity(TextColor textColor) {
        this.textColor = textColor;
        //noinspection DataFlowIssue
        this.color = textColor.getValue();

        this.r = ((color >> 16) & 0xFF) / 255f;
        this.g = ((color >> 8) & 0xFF) / 255f;
        this.b = (color & 0xFF) / 255f;
    }

    @Override
    public String getSerializedName() {
        return name();
    }

    public SkyblockItemRarity next() {
        return EnumUtils.cycle(this);
    }

    public static Optional<SkyblockItemRarity> containsName(String name) {
        // Find last because "UNCOMMON" contains "COMMON" and "VERY_SPECIAL" contains "SPECIAL"
        return Streams.findLast(Arrays.stream(SkyblockItemRarity.values())
                .filter(rarity -> name.contains(rarity.name()))
        );
    }

    public static SkyblockItemRarity fromColor(int color) {
        return Arrays.stream(SkyblockItemRarity.values())
                .filter(rarity -> ARGB.colorFromFloat(1f, rarity.r, rarity.g, rarity.b) == ARGB.opaque(color))
                .findFirst()
                .orElse(UNKNOWN);
    }
}