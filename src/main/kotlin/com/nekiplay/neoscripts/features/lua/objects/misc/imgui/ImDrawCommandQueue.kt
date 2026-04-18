package com.nekiplay.neoscripts.features.lua.objects.misc.imgui

import com.nekiplay.neoscripts.features.lua.objects.misc.ImGuiLib
import imgui.ImFont
import imgui.ImGui
import imgui.ImVec2
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

    fun renderFilledRect(x1: Float, y1: Float, x2: Float, y2: Float, red: Int, green: Int, blue: Int, alpha: Int, rounding: Float) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addRectFilled(x1, y1, x2, y2, color, rounding)
    }

    fun renderRect(x1: Float, y1: Float, x2: Float, y2: Float, red: Int, green: Int, blue: Int, alpha: Int, rounding: Float) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addRect(x1, y1, x2, y2, color, rounding)
    }

    // Градиенты ало ало
    fun renderFilledRectMultiColor(
        x1: Float, y1: Float, x2: Float, y2: Float,
        redUL: Int, greenUL: Int, blueUL: Int, alphaUL: Int,
        redUR: Int, greenUR: Int, blueUR: Int, alphaUR: Int,
        redBR: Int, greenBR: Int, blueBR: Int, alphaBR: Int,
        redBL: Int, greenBL: Int, blueBL: Int, alphaBL: Int
    ) {
        val colUL = makeImGuiColor(redUL, greenUL, blueUL, alphaUL)
        val colUR = makeImGuiColor(redUR, greenUR, blueUR, alphaUR)
        val colBR = makeImGuiColor(redBR, greenBR, blueBR, alphaBR)
        val colBL = makeImGuiColor(redBL, greenBL, blueBL, alphaBL)
        ImGui.getBackgroundDrawList().addRectFilledMultiColor(x1, y1, x2, y2, colUL, colUR, colBR, colBL)
    }

    fun renderQuad(
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float,
        p4x: Float, p4y: Float,
        red: Int, green: Int, blue: Int, alpha: Int,
        thickness: Float = 1f
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addQuad(p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y, color, thickness)
    }

    fun renderFilledQuad(
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float,
        p4x: Float, p4y: Float,
        red: Int, green: Int, blue: Int, alpha: Int
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addQuadFilled(p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y, color)
    }

    fun renderTriangle(
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float,
        red: Int, green: Int, blue: Int, alpha: Int,
        thickness: Float = 1f
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addTriangle(p1x, p1y, p2x, p2y, p3x, p3y, color, thickness)
    }

    fun renderFilledTriangle(
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float,
        red: Int, green: Int, blue: Int, alpha: Int
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addTriangleFilled(p1x, p1y, p2x, p2y, p3x, p3y, color)
    }

    fun renderCircle(
        cx: Float, cy: Float, radius: Float,
        red: Int, green: Int, blue: Int, alpha: Int,
        numSegments: Int = 0, thickness: Float = 1f
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addCircle(cx, cy, radius, color, numSegments, thickness)
    }

    fun renderFilledCircle(
        cx: Float, cy: Float, radius: Float,
        red: Int, green: Int, blue: Int, alpha: Int,
        numSegments: Int = 0
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addCircleFilled(cx, cy, radius, color, numSegments)
    }

    fun renderNgon(
        cx: Float, cy: Float, radius: Float,
        numSegments: Int,
        red: Int, green: Int, blue: Int, alpha: Int,
        thickness: Float = 1f
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addNgon(cx, cy, radius, color, numSegments, thickness)
    }

    fun renderFilledNgon(
        cx: Float, cy: Float, radius: Float,
        numSegments: Int,
        red: Int, green: Int, blue: Int, alpha: Int
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addNgonFilled(cx, cy, radius, color, numSegments)
    }

    fun renderEllipse(
        cx: Float, cy: Float,
        radiusX: Float, radiusY: Float,
        red: Int, green: Int, blue: Int, alpha: Int,
        rot: Float = 0f, numSegments: Int = 0, thickness: Float = 1f
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        val center = ImVec2(cx, cy)
        val radius = ImVec2(radiusX, radiusY)
        ImGui.getBackgroundDrawList().addEllipse(center, radius, color, rot, numSegments.toFloat(), thickness)
    }

    fun renderFilledEllipse(
        cx: Float, cy: Float,
        radiusX: Float, radiusY: Float,
        red: Int, green: Int, blue: Int, alpha: Int,
        rot: Float = 0f, numSegments: Int = 0
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        val center = ImVec2(cx, cy)
        val radius = ImVec2(radiusX, radiusY)
        ImGui.getBackgroundDrawList().addEllipseFilled(center, radius, color, rot, numSegments.toFloat())
    }

    fun renderText(x: Float, y: Float, text: String, red: Int, green: Int, blue: Int, alpha: Int) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addText(x, y, color, text)
    }

    fun renderTextEx(
        font: ImFont, fontSize: Float,
        x: Float, y: Float,
        text: String,
        red: Int, green: Int, blue: Int, alpha: Int,
        wrapWidth: Float = 0f
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addText(font, fontSize.toInt(), x, y, color, text, "")
    }

    fun renderBezierCubic(
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float,
        p4x: Float, p4y: Float,
        red: Int, green: Int, blue: Int, alpha: Int,
        thickness: Float = 1f, numSegments: Int = 0
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addBezierCubic(
            p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y,
            color, thickness, numSegments
        )
    }

    fun renderBezierQuadratic(
        p1x: Float, p1y: Float,
        p2x: Float, p2y: Float,
        p3x: Float, p3y: Float,
        red: Int, green: Int, blue: Int, alpha: Int,
        thickness: Float = 1f, numSegments: Int = 0
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        ImGui.getBackgroundDrawList().addBezierQuadratic(
            p1x, p1y, p2x, p2y, p3x, p3y,
            color, thickness, numSegments
        )
    }

    fun renderPolyline(
        points: List<Pair<Float, Float>>,
        red: Int, green: Int, blue: Int, alpha: Int,
        flags: Int = 0,
        thickness: Float = 1f
    ) {
        val color = makeImGuiColor(red, green, blue, alpha)
        val imVec2Points = points.map { ImVec2(it.first, it.second) }.toTypedArray()
        ImGui.getBackgroundDrawList().addPolyline(imVec2Points, imVec2Points.size, color, flags, thickness)
    }

    /* Обязательно рисовать точки в порядке с лево на право */
    fun renderFilledConvexPolygon(points: List<Pair<Float, Float>>, red: Int, green: Int, blue: Int, alpha: Int) {
        val color = makeImGuiColor(red, green, blue, alpha)
        val imVec2Points = points.map { ImVec2(it.first, it.second) }.toTypedArray()
        ImGui.getBackgroundDrawList().addConvexPolyFilled(imVec2Points, imVec2Points.size, color)
    }

    fun renderPolygon(points: List<Pair<Float, Float>>, red: Int, green: Int, blue: Int, alpha: Int) {
        val color = makeImGuiColor(red, green, blue, alpha)
        val imVec2Points = points.map { ImVec2(it.first, it.second) }.toTypedArray()
        ImGui.getBackgroundDrawList().addConcavePolyFilled(imVec2Points, imVec2Points.size, color)
    }


    fun renderImage(
        textureID: Long,
        x: Float, y: Float,
        width: Float, height: Float,
        uvMinX: Float = 0f, uvMinY: Float = 0f,
        uvMaxX: Float = 1f, uvMaxY: Float = 1f
    ) {
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

    fun pushClipRect(x1: Float, y1: Float, x2: Float, y2: Float, intersect: Boolean = false) {
        ImGui.getBackgroundDrawList().pushClipRect(x1, y1, x2, y2, intersect)
    }

    fun pushClipRectFullScreen() {
        ImGui.getBackgroundDrawList().pushClipRectFullScreen()
    }

    fun popClipRect() {
        ImGui.getBackgroundDrawList().popClipRect()
    }

    fun pushTextureID(textureID: Long) {
        ImGui.getBackgroundDrawList().pushTextureID(textureID)
    }

    fun popTextureID() {
        ImGui.getBackgroundDrawList().popTextureID()
    }
    
    fun pathClear() {
        ImGui.getBackgroundDrawList().pathClear()
    }

    fun pathLineTo(x: Float, y: Float) {
        ImGui.getBackgroundDrawList().pathLineTo(x, y)
    }

    fun pathStroke(color: Int, flags: Int = 0, thickness: Float = 1.0f) {
        ImGui.getBackgroundDrawList().pathStroke(color, flags, thickness)
    }


    fun executeAndClear() {}
    fun clear() {}
}
