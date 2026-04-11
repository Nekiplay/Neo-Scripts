package com.nekiplay.neoscripts.features.lua.objects.misc.imgui

import imgui.ImGui
import imgui.ImVec4

class ImDrawCommandQueue {
    // Очередь больше не нужна. Функции выполняются мгновенно.

    fun makeImGuiColor(red: Int, green: Int, blue: Int, alpha: Int): Int {
        return ImGui.colorConvertFloat4ToU32(
            red / 255f,
            green / 255f,
            blue / 255f,
            alpha / 255f
        )
    }

    fun renderLine(x1: Float, y1: Float, x2: Float, y2: Float, red: Int, green: Int, blue: Int, alpha: Int, thickness: Float) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addLine(x1, y1, x2, y2, color, thickness)
    }

    fun renderText(x: Float, y: Float, text: String, red: Int, green: Int, blue: Int, alpha: Int) {
        val color = makeImGuiColor(red, green, blue, alpha)
        // addText использует текущий шрифт из стека ImGui (pushFont)
        ImGui.getBackgroundDrawList().addText(x, y, color, text)
    }

    fun renderPolygon(points: List<Pair<Float, Float>>, red: Int, green: Int, blue: Int, alpha: Int) {
        val color = makeImGuiColor(red, green, blue, alpha)
        val drawList = ImGui.getBackgroundDrawList()
        drawList.pathClear()
        for (point in points) {
            drawList.pathLineTo(point.first, point.second)
        }
        drawList.pathFillConcave(color)
    }

    fun renderImage(textureID: Long, x: Float, y: Float, width: Float, height: Float, uvMinX: Float, uvMinY: Float, uvMaxX: Float, uvMaxY: Float) {
        // Рисуем изображение напрямую
        ImGui.getBackgroundDrawList().addImage(
            textureID,
            x, y, x + width, y + height,
            uvMinX, uvMinY, uvMaxX, uvMaxY
        )
    }

    fun renderImageQuad(
        textureID: Long,
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float,
        p4x: Float, p4y: Float,
        uvMinX: Float, uvMinY: Float,
        uvMaxX: Float, uvMaxY: Float,
        color: Int
    ) {
        // Рисуем skewed/transformed image quad с 4 углами и UV координатами
        ImGui.getBackgroundDrawList().addImageQuad(
            textureID,
            p1x, p1y, // top-left
            p2x, p2y, // top-right
            p3x, p3y, // bottom-right
            p4x, p4y, // bottom-left
            uvMinX, uvMinY, // UV top-left
            uvMaxX, uvMinY, // UV top-right
            uvMaxX, uvMaxY, // UV bottom-right
            uvMinX, uvMaxY, // UV bottom-left
            color
        )
    }

    // Эти методы можно оставить пустыми или удалить
    fun executeAndClear() {}
    fun clear() {}
}