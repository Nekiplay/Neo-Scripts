package com.nekiplay.hypixelcry.features.lua.objects.misc.imgui

import imgui.ImGui
import imgui.ImVec4

data class DrawCommand(
    val type: DrawType,
    val data: Map<String, Any>
)

enum class DrawType {
    LINE, POLYGON, IMAGE, TEXT
}

// 🔧 Класс вместо object — позволяет создавать несколько очередей
class ImDrawCommandQueue {
    private val pendingCommands = mutableListOf<DrawCommand>()
    private val lock = Object()

    fun queue(command: DrawCommand) {
        synchronized(lock) {
            pendingCommands.add(command)
        }
    }

    fun executeAndClear() {
        synchronized(lock) {
            for (command in pendingCommands) {
                when (command.type) {
                    DrawType.LINE -> executeLine(command.data)
                    DrawType.POLYGON -> executePolygon(command.data)
                    DrawType.IMAGE -> executeImage(command.data)
                    DrawType.TEXT -> executeText(command.data)
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
        val x1 = (data["x1"] as? Number)?.toFloat() ?: return
        val y1 = (data["y1"] as? Number)?.toFloat() ?: return
        val x2 = (data["x2"] as? Number)?.toFloat() ?: return
        val y2 = (data["y2"] as? Number)?.toFloat() ?: return
        val color = data["color"] as? Int ?: return
        val thickness = (data["thickness"] as? Number)?.toFloat() ?: 1f

        val drawList = ImGui.getBackgroundDrawList()
        drawList.addLine(x1, y1, x2, y2, color, thickness)
    }

    private fun executeText(data: Map<String, Any>) {
        val x1 = (data["x1"] as? Number)?.toFloat() ?: return
        val y1 = (data["y1"] as? Number)?.toFloat() ?: return
        val text = (data["text"] as? String) ?: return


        val color = data["color"] as? Int ?: return

        val drawList = ImGui.getBackgroundDrawList()
        drawList.addText(x1, y1, color, text)
    }

    private fun executePolygon(data: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val points = data["points"] as? List<Pair<Float, Float>> ?: return
        val color = data["color"] as? Int ?: return

        val drawList = ImGui.getBackgroundDrawList()
        drawList.pathClear()
        for (point in points) {
            drawList.pathLineTo(point.first, point.second)
        }
        drawList.pathFillConcave(color)
    }

    private fun executeImage(data: Map<String, Any>) {
        val textureID = data["textureID"] as? Long ?: return
        val pMinX = (data["pMinX"] as? Number)?.toFloat() ?: return
        val pMinY = (data["pMinY"] as? Number)?.toFloat() ?: return
        val pMaxX = (data["pMaxX"] as? Number)?.toFloat() ?: return
        val pMaxY = (data["pMaxY"] as? Number)?.toFloat() ?: return

        val uvMinX = (data["uvMinX"] as? Number)?.toFloat() ?: 0f
        val uvMinY = (data["uvMinY"] as? Number)?.toFloat() ?: 0f
        val uvMaxX = (data["uvMaxX"] as? Number)?.toFloat() ?: 1f
        val uvMaxY = (data["uvMaxY"] as? Number)?.toFloat() ?: 1f

        val color = data["color"] as? Int ?: -1

        val drawList = ImGui.getBackgroundDrawList()
        drawList.addImage(
            textureID,
            pMinX, pMinY, pMaxX, pMaxY,
            uvMinX, uvMinY, uvMaxX, uvMaxY,
            color
        )
    }
}