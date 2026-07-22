package com.nekiplay.neoscripts.utils.render.state;

import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public record TextRenderState(Font.PreparedText glyphs, Vec3 pos, float scale, float yOffset, boolean throughWalls) {
}