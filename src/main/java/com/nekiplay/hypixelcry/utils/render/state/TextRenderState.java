package com.nekiplay.hypixelcry.utils.render.state;

import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public class TextRenderState {
    public Font.PreparedText glyphs;
    public Vec3 pos;
    public float scale;
    public float yOffset;
    public boolean throughWalls;
    @Nullable
    public Quaternionf quaternion = null;
}