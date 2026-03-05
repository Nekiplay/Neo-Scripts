package com.nekiplay.hypixelcry.features.lua.objects.misc.imgui

import imgui.ImGui
import imgui.ImVec4  // Добавлен импорт для ImVec4

data class DrawCommand(
    val type: DrawType,
    val data: Map<String, Any>
)

enum class DrawType {
    LINE, POLYGON
}

object ImDrawCommandQueue {
    private val pendingCommands = mutableListOf<DrawCommand>()
    private val lock = Object()

    fun queue(command: DrawCommand) {
        synchronized(lock) {
            pendingCommands.add(command)
        }
    }

    fun executeAndClear() {
        synchronized(lock) {
            // 🔧 ИСПРАВЛЕНИЕ 1: Убрано неверное деструктурирование (_, command)
            // pendingCommands — это MutableList<DrawCommand>, а не список пар
            for (command in pendingCommands) {
                when (command.type) {
                    DrawType.LINE -> executeLine(command.data)
                    DrawType.POLYGON -> executePolygon(command.data)
                }
            }
            pendingCommands.clear()
        }
    }

    fun clear() {
        synchronized(lock) {
            pendingCommands.clear()
        }
    }

    // 🔧 ИСПРАВЛЕНИЕ 2: Исправлено преобразование цвета
    // getColorU32() предназначен для получения цвета из стиля ImGui по индексу
    // Для конвертации RGBA в ImU32 нужно использовать colorConvertFloat4ToU32()
    fun makeImGuiColor(red: Int, green: Int, blue: Int, alpha: Int): Int {
        return ImGui.colorConvertFloat4ToU32(
            ImVec4(
                red / 255f,
                green / 255f,
                blue / 255f,
                alpha / 255f
            )
        )
    }

    private fun executeLine(data: Map<String, Any>) {
        val x1 = (data["x1"] as Number).toFloat()
        val y1 = (data["y1"] as Number).toFloat()
        val x2 = (data["x2"] as Number).toFloat()
        val y2 = (data["y2"] as Number).toFloat()
        val color = data["color"] as Int
        val thickness = (data["thickness"] as Number).toFloat()

        val drawList = ImGui.getBackgroundDrawList()
        drawList.addLine(x1, y1, x2, y2, color, thickness)
    }

    private fun executePolygon(data: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val points = data["points"] as List<Pair<Float, Float>>
        val color = data["color"] as Int

        val drawList = ImGui.getBackgroundDrawList()
        drawList.pathClear()
        for (point in points) {
            drawList.pathLineTo(point.first, point.second)
        }
        // ⚠️ pathFillConvex работает только с выпуклыми полигонами!
        // Для сложных форм потребуется использовать pathStroke() или триангуляцию
        drawList.pathFillConvex(color)
    }
}