package com.nekiplay.neoscripts.client.utils.render.state;

public record OutlinedBoxRenderState(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float[] colourComponents, float alpha, float lineWidth, boolean throughWalls) {
}