package com.nekiplay.neoscripts.client.utils.render.state;

import net.minecraft.world.phys.Vec3;

public record CursorLineRenderState(Vec3 point, float[] colourComponents, float alpha, float lineWidth) {
}