package com.nekiplay.hypixelcry.features.lua.objects.misc

import com.nekiplay.hypixelcry.features.lua.objects.misc.imgui.DrawCommand
import com.nekiplay.hypixelcry.features.lua.objects.misc.imgui.DrawType
import com.nekiplay.hypixelcry.features.lua.objects.misc.imgui.ImDrawCommandQueue
import com.nekiplay.hypixelcry.features.lua.objects.misc.imgui.ImGuiTexture
import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiColorEditFlags
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiTableFlags
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import imgui.type.ImDouble
import imgui.type.ImFloat
import imgui.type.ImInt
import imgui.type.ImString
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import java.util.concurrent.atomic.AtomicInteger

class ImGuiLib(val L: Lua) {
    public val queue: ImDrawCommandQueue = ImDrawCommandQueue()

    fun register() {
        L.newTable() // Основная таблица imgui (на индексе -1)

        // Window management
        setFunc("begin") { begin(it) }
        setFunc("endBegin") { endFunc(it) }
        setFunc("newFrame") { newFrame(it) }
        setFunc("render") { render(it) }

        // Text
        setFunc("text") { text(it) }
        setFunc("textColored") { textColored(it) }
        setFunc("textDisabled") { textDisabled(it) }
        setFunc("bulletText") { bulletText(it) }

        // Images
        setFunc("createImageObject") { createImageObject(it) }
        setFunc("image") { image(it) }

        // Buttons
        setFunc("button") { button(it) }
        setFunc("smallButton") { smallButton(it) }
        setFunc("arrowButton") { arrowButton(it) }
        setFunc("checkbox") { checkbox(it) }

        // Input
        setFunc("inputText") { inputText(it) }
        setFunc("inputTextMultiline") { inputTextMultiline(it) }
        setFunc("inputInt") { inputInt(it) }
        setFunc("inputFloat") { inputFloat(it) }
        setFunc("inputDouble") { inputDouble(it) }

        // Layout / Groups / Indentation / Cursor... (по аналогии)
        setFunc("sameLine") { sameLine(it) }
        setFunc("newLine") { newLine(it) }
        setFunc("spacing") { spacing(it) }
        setFunc("separator") { separator(it) }
        setFunc("beginGroup") { beginGroup(it) }
        setFunc("endGroup") { endGroup(it) }
        setFunc("indent") { indent(it) }
        setFunc("unindent") { unindent(it) }
        setFunc("setCursorPos") { setCursorPos(it) }
        setFunc("getCursorPos") { getCursorPos(it) }

        // Tree / Selectables / Lists / Tooltips / Popups / Menus / Tabs / Style / Font / Tables...
        // Для краткости регистрируем основные категории:
        setFunc("beginTable") { beginTable(it) }
        setFunc("tableSetupColumn") { tableSetupColumn(it) }
        setFunc("endTable") { endTable(it) }
        setFunc("pushStyleColor") { pushStyleColor(it) }
        setFunc("popStyleColor") { popStyleColor(it) }

        // --- Константы (Constants) ---
        L.newTable()
        // Window Flags
        addC("WindowFlags_None", ImGuiWindowFlags.None.toInt())
        addC("WindowFlags_NoTitleBar", ImGuiWindowFlags.NoTitleBar.toInt())
        addC("WindowFlags_NoResize", ImGuiWindowFlags.NoResize.toInt())
        addC("WindowFlags_NoMove", ImGuiWindowFlags.NoMove.toInt())
        addC("WindowFlags_NoScrollbar", ImGuiWindowFlags.NoScrollbar.toInt())
        addC("WindowFlags_NoCollapse", ImGuiWindowFlags.NoCollapse.toInt())

        // Condition Flags
        addC("Cond_Always", ImGuiCond.Always.toInt())
        addC("Cond_Once", ImGuiCond.Once.toInt())
        addC("Cond_FirstUseEver", ImGuiCond.FirstUseEver.toInt())

        // Color Edit Flags
        addC("ColorEditFlags_None", ImGuiColorEditFlags.None.toInt())
        addC("ColorEditFlags_NoAlpha", ImGuiColorEditFlags.NoAlpha.toInt())
        addC("ColorEditFlags_NoPicker", ImGuiColorEditFlags.NoPicker.toInt())

        // Colors (ImGuiCol)
        addC("Col_Text", ImGuiCol.Text.toInt())
        addC("Col_TextDisabled", ImGuiCol.TextDisabled.toInt())
        addC("Col_WindowBg", ImGuiCol.WindowBg.toInt())
        addC("Col_ChildBg", ImGuiCol.ChildBg.toInt())
        addC("Col_PopupBg", ImGuiCol.PopupBg.toInt())
        addC("Col_Border", ImGuiCol.Border.toInt())
        addC("Col_BorderShadow", ImGuiCol.BorderShadow.toInt())
        addC("Col_FrameBg", ImGuiCol.FrameBg.toInt())
        addC("Col_FrameBgHovered", ImGuiCol.FrameBgHovered.toInt())
        addC("Col_FrameBgActive", ImGuiCol.FrameBgActive.toInt())
        addC("Col_TitleBg", ImGuiCol.TitleBg.toInt())
        addC("Col_TitleBgActive", ImGuiCol.TitleBgActive.toInt())
        addC("Col_TitleBgCollapsed", ImGuiCol.TitleBgCollapsed.toInt())
        addC("Col_MenuBarBg", ImGuiCol.MenuBarBg.toInt())
        addC("Col_ScrollbarBg", ImGuiCol.ScrollbarBg.toInt())
        addC("Col_ScrollbarGrab", ImGuiCol.ScrollbarGrab.toInt())
        addC("Col_ScrollbarGrabHovered", ImGuiCol.ScrollbarGrabHovered.toInt())
        addC("Col_ScrollbarGrabActive", ImGuiCol.ScrollbarGrabActive.toInt())
        addC("Col_CheckMark", ImGuiCol.CheckMark.toInt())
        addC("Col_SliderGrab", ImGuiCol.SliderGrab.toInt())
        addC("Col_SliderGrabActive", ImGuiCol.SliderGrabActive.toInt())
        addC("Col_Button", ImGuiCol.Button.toInt())
        addC("Col_ButtonHovered", ImGuiCol.ButtonHovered.toInt())
        addC("Col_ButtonActive", ImGuiCol.ButtonActive.toInt())
        addC("Col_Header", ImGuiCol.Header.toInt())
        addC("Col_HeaderHovered", ImGuiCol.HeaderHovered.toInt())
        addC("Col_HeaderActive", ImGuiCol.HeaderActive.toInt())
        addC("Col_Separator", ImGuiCol.Separator.toInt())
        addC("Col_SeparatorHovered", ImGuiCol.SeparatorHovered.toInt())
        addC("Col_SeparatorActive", ImGuiCol.SeparatorActive.toInt())
        addC("Col_ResizeGrip", ImGuiCol.ResizeGrip.toInt())
        addC("Col_ResizeGripHovered", ImGuiCol.ResizeGripHovered.toInt())
        addC("Col_ResizeGripActive", ImGuiCol.ResizeGripActive.toInt())
        addC("Col_Tab", ImGuiCol.Tab.toInt())
        addC("Col_TabHovered", ImGuiCol.TabHovered.toInt())
        addC("Col_TabActive", ImGuiCol.TabActive.toInt())
        addC("Col_TabUnfocused", ImGuiCol.TabUnfocused.toInt())
        addC("Col_TabUnfocusedActive", ImGuiCol.TabUnfocusedActive.toInt())
        addC("Col_PlotLines", ImGuiCol.PlotLines.toInt())
        addC("Col_PlotLinesHovered", ImGuiCol.PlotLinesHovered.toInt())
        addC("Col_PlotHistogram", ImGuiCol.PlotHistogram.toInt())
        addC("Col_PlotHistogramHovered", ImGuiCol.PlotHistogramHovered.toInt())
        addC("Col_TableHeaderBg", ImGuiCol.TableHeaderBg.toInt())
        addC("Col_TableBorderStrong", ImGuiCol.TableBorderStrong.toInt())
        addC("Col_TableBorderLight", ImGuiCol.TableBorderLight.toInt())
        addC("Col_TableRowBg", ImGuiCol.TableRowBg.toInt())
        addC("Col_TableRowBgAlt", ImGuiCol.TableRowBgAlt.toInt())
        addC("Col_TextSelectedBg", ImGuiCol.TextSelectedBg.toInt())
        addC("Col_DragDropTarget", ImGuiCol.DragDropTarget.toInt())
        addC("Col_NavHighlight", ImGuiCol.NavHighlight.toInt())
        addC("Col_NavWindowingHighlight", ImGuiCol.NavWindowingHighlight.toInt())
        addC("Col_NavWindowingDimBg", ImGuiCol.NavWindowingDimBg.toInt())
        addC("Col_ModalWindowDimBg", ImGuiCol.ModalWindowDimBg.toInt())

        // Style Vars
        addC("StyleVar_Alpha", ImGuiStyleVar.Alpha.toInt())
        addC("StyleVar_DisabledAlpha", ImGuiStyleVar.DisabledAlpha.toInt())
        addC("StyleVar_WindowPadding", ImGuiStyleVar.WindowPadding.toInt())
        addC("StyleVar_WindowRounding", ImGuiStyleVar.WindowRounding.toInt())
        addC("StyleVar_WindowBorderSize", ImGuiStyleVar.WindowBorderSize.toInt())
        addC("StyleVar_WindowMinSize", ImGuiStyleVar.WindowMinSize.toInt())
        addC("StyleVar_WindowTitleAlign", ImGuiStyleVar.WindowTitleAlign.toInt())
        addC("StyleVar_ChildRounding", ImGuiStyleVar.ChildRounding.toInt())
        addC("StyleVar_ChildBorderSize", ImGuiStyleVar.ChildBorderSize.toInt())
        addC("StyleVar_PopupRounding", ImGuiStyleVar.PopupRounding.toInt())
        addC("StyleVar_PopupBorderSize", ImGuiStyleVar.PopupBorderSize.toInt())
        addC("StyleVar_FramePadding", ImGuiStyleVar.FramePadding.toInt())
        addC("StyleVar_FrameRounding", ImGuiStyleVar.FrameRounding.toInt())
        addC("StyleVar_FrameBorderSize", ImGuiStyleVar.FrameBorderSize.toInt())
        addC("StyleVar_ItemSpacing", ImGuiStyleVar.ItemSpacing.toInt())
        addC("StyleVar_ItemInnerSpacing", ImGuiStyleVar.ItemInnerSpacing.toInt())
        addC("StyleVar_IndentSpacing", ImGuiStyleVar.IndentSpacing.toInt())
        addC("StyleVar_CellPadding", ImGuiStyleVar.CellPadding.toInt())
        addC("StyleVar_ScrollbarSize", ImGuiStyleVar.ScrollbarSize.toInt())
        addC("StyleVar_ScrollbarRounding", ImGuiStyleVar.ScrollbarRounding.toInt())
        addC("StyleVar_GrabMinSize", ImGuiStyleVar.GrabMinSize.toInt())
        addC("StyleVar_GrabRounding", ImGuiStyleVar.GrabRounding.toInt())
        addC("StyleVar_TabRounding", ImGuiStyleVar.TabRounding.toInt())
        addC("StyleVar_ButtonTextAlign", ImGuiStyleVar.ButtonTextAlign.toInt())
        addC("StyleVar_SelectableTextAlign", ImGuiStyleVar.SelectableTextAlign.toInt())

        // Table Flags
        addC("TableFlags_BordersInner", ImGuiTableFlags.BordersInner.toInt())
        addC("TableFlags_BordersInnerH", ImGuiTableFlags.BordersInnerH.toInt())
        addC("TableFlags_BordersInnerV", ImGuiTableFlags.BordersInnerV.toInt())
        addC("TableFlags_Resizable", ImGuiTableFlags.Resizable.toInt())

        // Привязываем таблицу constants к основной таблице (индекс -2)
        L.setField(-2, "constants")

        // --- DrawList (dl) ---
        L.newTable()
        L.push(JFunction { renderDLLine(it) }); L.setField(-2, "renderLine")
        L.push(JFunction { renderDLPolygon(it) }); L.setField(-2, "renderPolygon")
        L.push(JFunction { renderDLImage(it) }); L.setField(-2, "renderImage")
        L.push(JFunction { renderDLText(it) }); L.setField(-2, "renderText")
        L.setField(-2, "dl")

        L.setGlobal("imgui")
    }

    fun addC(name: String, value: Int) {
        L.push(value.toDouble())
        L.setField(-2, name)
    }

    private fun setFunc(name: String, func: (Lua) -> Int) {
        L.push(JFunction { func(it) })
        L.setField(-2, name)
    }

    private fun renderDLText(l: Lua): Int {
        if (l.isTable(1)) {
            val t = 1 // Индекс таблицы-аргумента в стеке

            l.getField(t, "x"); val x1 = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
            l.getField(t, "y"); val y1 = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)

            l.getField(t, "red"); val r = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)
            l.getField(t, "green"); val g = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)
            l.getField(t, "blue"); val b = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)
            l.getField(t, "alpha"); val a = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)

            l.getField(t, "text"); val text = l.toString(-1) ?: ""; l.pop(1)

            queue.queue(DrawCommand(DrawType.TEXT, mapOf(
                "x1" to x1, "y1" to y1,
                "color" to queue.makeImGuiColor(r, g, b, a),
                "text" to text
            )))
            l.push(true)
            return 1
        }
        l.pushNil()
        return 1
    }

    private fun renderDLImage(l: Lua): Int {
        if (l.isTable(1)) {
            val t = 1

            l.getField(t, "textureID"); val textureID = if (l.isNumber(-1)) l.toNumber(-1).toLong() else 0L; l.pop(1)

            l.getField(t, "x"); val pMinX = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
            l.getField(t, "y"); val pMinY = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
            l.getField(t, "width"); val pMaxX = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
            l.getField(t, "height"); val pMaxY = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)

            l.getField(t, "uvMinX"); val uvMinX = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
            l.getField(t, "uvMinY"); val uvMinY = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
            l.getField(t, "uvMaxX"); val uvMaxX = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 1f; l.pop(1)
            l.getField(t, "uvMaxY"); val uvMaxY = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 1f; l.pop(1)

            queue.queue(DrawCommand(DrawType.IMAGE, mapOf(
                "textureID" to textureID,
                "pMinX" to pMinX, "pMinY" to pMinY,
                "pMaxX" to pMaxX, "pMaxY" to pMaxY,
                "uvMinX" to uvMinX, "uvMinY" to uvMinY,
                "uvMaxX" to uvMaxX, "uvMaxY" to uvMaxY
            )))
            l.push(true)
            return 1
        }
        l.pushNil()
        return 1
    }

    private fun renderDLLine(l: Lua): Int {
        if (l.isTable(1)) {
            val t = 1

            l.getField(t, "x1"); val x1 = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
            l.getField(t, "y1"); val y1 = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
            l.getField(t, "x2"); val x2 = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
            l.getField(t, "y2"); val y2 = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)

            l.getField(t, "red"); val r = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)
            l.getField(t, "green"); val g = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)
            l.getField(t, "blue"); val b = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)
            l.getField(t, "alpha"); val a = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)

            l.getField(t, "thickness"); val thickness = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 1f; l.pop(1)

            queue.queue(DrawCommand(DrawType.LINE, mapOf(
                "x1" to x1, "y1" to y1, "x2" to x2, "y2" to y2,
                "color" to queue.makeImGuiColor(r, g, b, a),
                "thickness" to thickness
            )))
            l.push(true)
            return 1
        }
        l.pushNil()
        return 1
    }

    private fun renderDLPolygon(l: Lua): Int {
        if (l.isTable(1)) {
            val t = 1
            l.getField(t, "points")
            if (l.isTable(-1)) {
                val pointsIdx = l.getTop() // Индекс таблицы точек

                l.getField(t, "red"); val r = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)
                l.getField(t, "green"); val g = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)
                l.getField(t, "blue"); val b = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)
                l.getField(t, "alpha"); val a = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)

                val points = mutableListOf<Pair<Float, Float>>()
                val len = l.rawLength(pointsIdx)

                for (i in 1..len) {
                    l.rawGetI(pointsIdx, i) // Получаем таблицу {x=..., y=...}
                    if (l.isTable(-1)) {
                        l.getField(-1, "x"); val px = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
                        l.getField(-1, "y"); val py = if (l.isNumber(-1)) l.toNumber(-1).toFloat() else 0f; l.pop(1)
                        points.add(px to py)
                    }
                    l.pop(1) // Убираем таблицу точки
                }

                if (points.size >= 3) {
                    queue.queue(DrawCommand(DrawType.POLYGON, mapOf(
                        "points" to points,
                        "color" to queue.makeImGuiColor(r, g, b, a)
                    )))
                    l.pop(1) // Убираем таблицу points
                    l.push(true)
                    return 1
                }
            }
            l.pop(1) // Убираем результат getField("points"), если он там остался
            l.push(false)
            return 1
        }
        l.pushNil()
        return 1
    }

    private fun sliderFloat(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val value = floatArrayOf(l.toNumber(2).toFloat())
        val min = l.toNumber(3).toFloat()
        val max = l.toNumber(4).toFloat()
        val format = if (l.isString(5)) l.toString(5) else "%.3f"
        val flags = if (l.isNumber(6)) l.toNumber(6).toInt() else 0

        val changed = ImGui.sliderFloat(label, value, min, max, format ?: "%.3f", flags)

        l.push(changed)
        l.push(value[0].toDouble())
        return 2 // Возвращаем два значения: changed и новое число
    }

    private fun sliderInt(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val value = intArrayOf(l.toNumber(2).toInt())
        val min = l.toNumber(3).toInt()
        val max = l.toNumber(4).toInt()
        val format = if (l.isString(5)) l.toString(5) else "%d"
        val flags = if (l.isNumber(6)) l.toNumber(6).toInt() else 0

        val changed = ImGui.sliderInt(label, value, min, max, format ?: "%d", flags)

        l.push(changed)
        l.push(value[0].toDouble())
        return 2
    }

    private fun vSliderFloat(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val sizeX = l.toNumber(2).toFloat()
        val sizeY = l.toNumber(3).toFloat()
        val value = floatArrayOf(l.toNumber(4).toFloat())
        val min = l.toNumber(5).toFloat()
        val max = l.toNumber(6).toFloat()
        val format = if (l.isString(7)) l.toString(7) else "%.3f"
        val flags = if (l.isNumber(8)) l.toNumber(8).toInt() else 0

        val changed = ImGui.vSliderFloat(label, sizeX, sizeY, value, min, max, format ?: "%.3f", flags)

        l.push(changed)
        l.push(value[0].toDouble())
        return 2
    }

    private fun vSliderInt(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val sizeX = l.toNumber(2).toFloat()
        val sizeY = l.toNumber(3).toFloat()
        val value = intArrayOf(l.toNumber(4).toInt())
        val min = l.toNumber(5).toInt()
        val max = l.toNumber(6).toInt()
        val format = if (l.isString(7)) l.toString(7) else "%d"
        val flags = if (l.isNumber(8)) l.toNumber(8).toInt() else 0

        val changed = ImGui.vSliderInt(label, sizeX, sizeY, value, min, max, format ?: "%d", flags)

        l.push(changed)
        l.push(value[0].toDouble())
        return 2
    }

    private fun beginTable(l: Lua): Int {
        val name = l.toString(1) ?: ""
        val column = l.toNumber(2).toInt()
        val flags = l.toNumber(3).toInt()
        ImGui.beginTable(name, column, flags)
        return 0
    }

    private fun endTable(l: Lua): Int {
        ImGui.endTable()
        return 0
    }

    private fun tableSetColumnIndex(l: Lua): Int {
        ImGui.tableSetColumnIndex(l.toNumber(1).toInt())
        return 0
    }

    private fun tableNextRow(l: Lua): Int {
        ImGui.tableNextRow()
        return 0
    }

    private fun tableHeadersRow(l: Lua): Int {
        ImGui.tableHeadersRow()
        return 0
    }

    private fun tableSetupColumn(l: Lua): Int {
        ImGui.tableSetupColumn(l.toString(1) ?: "")
        return 0
    }

    private fun setNextItemWidth(l: Lua): Int {
        val width = l.toNumber(1).toFloat()
        // Предполагается вызов ImGui.setNextItemWidth(width)
        return 0
    }

    private fun createImageObject(l: Lua): Int {
        // Возвращаем объект через push() обертки SimpleLuaWrapper
        l.push(ImGuiTexture(l, AtomicInteger(0)).push())
        return 1
    }

    private fun image(l: Lua): Int {
        if (!l.isNoneOrNil(1) && !l.isNoneOrNil(2) && !l.isNoneOrNil(3)) {
            val firstArg = l.toJavaObject(1)
            val imageId: Long? = when {
                firstArg is ImGuiTexture -> firstArg.texture.get().toLong()
                firstArg is AtomicInteger -> firstArg.get().toLong()
                l.isNumber(1) -> l.toNumber(1).toLong()
                else -> null
            }

            if (imageId != null && imageId > 0) {
                val nArg = l.getTop()
                when (nArg) {
                    7 -> ImGui.image(imageId, l.toNumber(2).toFloat(), l.toNumber(3).toFloat(), l.toNumber(4).toFloat(), l.toNumber(5).toFloat(), l.toNumber(6).toFloat(), l.toNumber(7).toFloat())
                    5 -> ImGui.image(imageId, l.toNumber(2).toFloat(), l.toNumber(3).toFloat(), l.toNumber(4).toFloat(), l.toNumber(5).toFloat())
                    else -> ImGui.image(imageId, l.toNumber(2).toFloat(), l.toNumber(3).toFloat())
                }
                l.push(true)
                return 1
            }
        }
        l.push(false)
        return 1
    }

    private fun pathClear(l: Lua): Int {
        ImGui.getBackgroundDrawList().pathClear()
        l.push(true)
        return 1
    }

    private fun pathLineTo(l: Lua): Int {
        val x = l.toNumber(1).toFloat()
        val y = l.toNumber(2).toFloat()
        ImGui.getBackgroundDrawList().pathLineTo(x, y)
        l.push(true)
        return 1
    }

    private fun pathStroke(l: Lua): Int {
        val color = l.toNumber(1).toInt()
        val flags = if (l.isNumber(2)) l.toNumber(2).toInt() else 0
        val thickness = if (l.isNumber(3)) l.toNumber(3).toFloat() else 1.0f
        ImGui.getBackgroundDrawList().pathStroke(color, flags, thickness)
        l.push(true)
        return 1
    }

    // --- Frame Lifecycle ---
    private fun newFrame(l: Lua): Int {
        ImGui.newFrame()
        l.push(true)
        return 1
    }

    private fun render(l: Lua): Int {
        ImGui.render()
        l.push(true)
        return 1
    }

    // --- Windows ---
    private fun begin(l: Lua): Int {
        val name = l.toString(1) ?: ""
        val flags = if (l.isNumber(2)) l.toNumber(2).toInt() else 0
        val opened = ImGui.begin(name, flags)
        l.push(opened)
        return 1
    }

    private fun endFunc(l: Lua): Int {
        ImGui.end()
        l.push(true)
        return 1
    }

    // --- Text ---
    private fun text(l: Lua): Int {
        val content = l.toString(1) ?: ""
        ImGui.text(content)
        return 0 // NIL в оригинале (ничего не пушим)
    }

    private fun textColored(l: Lua): Int {
        val r = l.toNumber(1).toFloat()
        val g = l.toNumber(2).toFloat()
        val b = l.toNumber(3).toFloat()
        val a = if (l.isNumber(4)) l.toNumber(4).toFloat() else 1.0f
        val content = l.toString(5) ?: ""
        ImGui.textColored(r, g, b, a, content)
        return 0
    }

    private fun textDisabled(l: Lua): Int {
        val content = l.toString(1) ?: ""
        ImGui.textDisabled(content)
        return 0
    }

    private fun bulletText(l: Lua): Int {
        val content = l.toString(1) ?: ""
        ImGui.bulletText(content)
        return 0
    }

    // --- Buttons ---
    private fun button(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val width = if (l.isNumber(2)) l.toNumber(2).toFloat() else 0f
        val height = if (l.isNumber(3)) l.toNumber(3).toFloat() else 0f
        val clicked = ImGui.button(label, width, height)
        l.push(clicked)
        return 1
    }

    private fun smallButton(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val clicked = ImGui.smallButton(label)
        l.push(clicked)
        return 1
    }

    private fun arrowButton(l: Lua): Int {
        val strId = l.toString(1) ?: ""
        val dir = l.toNumber(2).toInt()
        val clicked = ImGui.arrowButton(strId, dir)
        l.push(clicked)
        return 1
    }

    private fun checkbox(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val value = ImBoolean(l.toBoolean(2))
        val changed = ImGui.checkbox(label, value)

        l.push(changed)
        l.push(value.get())
        return 2 // Возвращаем (changed, current_value)
    }

    private fun inputText(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val initialText = l.toString(2) ?: ""
        val text = ImString(initialText, 256)
        val flags = if (l.isNumber(3)) l.toNumber(3).toInt() else 0

        val changed = ImGui.inputText(label, text, flags)

        l.push(changed)
        l.push(text.get())
        return 2
    }

    private fun inputTextMultiline(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val initialText = l.toString(2) ?: ""
        val text = ImString(initialText, 1024)
        val width = if (l.isNumber(3)) l.toNumber(3).toFloat() else 0f
        val height = if (l.isNumber(4)) l.toNumber(4).toFloat() else 0f
        val flags = if (l.isNumber(5)) l.toNumber(5).toInt() else 0

        val changed = ImGui.inputTextMultiline(label, text, width, height, flags)

        l.push(changed)
        l.push(text.get())
        return 2
    }

    private fun inputInt(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val value = ImInt(l.toNumber(2).toInt())
        val step = if (l.isNumber(3)) l.toNumber(3).toInt() else 1
        val stepFast = if (l.isNumber(4)) l.toNumber(4).toInt() else 100
        val flags = if (l.isNumber(5)) l.toNumber(5).toInt() else 0

        val changed = ImGui.inputInt(label, value, step, stepFast, flags)

        l.push(changed)
        l.push(value.get().toDouble())
        return 2
    }

    private fun inputFloat(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val value = ImFloat(l.toNumber(2).toFloat())
        val step = if (l.isNumber(3)) l.toNumber(3).toFloat() else 0.0f
        val stepFast = if (l.isNumber(4)) l.toNumber(4).toFloat() else 0.0f
        val format = if (l.isString(5)) l.toString(5) else "%.3f"
        val flags = if (l.isNumber(6)) l.toNumber(6).toInt() else 0

        val changed = ImGui.inputFloat(label, value, step, stepFast, format ?: "%.3f", flags)

        l.push(changed)
        l.push(value.get().toDouble())
        return 2
    }

    private fun inputDouble(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val value = ImDouble(l.toNumber(2))
        val step = if (l.isNumber(3)) l.toNumber(3) else 0.0
        val stepFast = if (l.isNumber(4)) l.toNumber(4) else 0.0
        val format = if (l.isString(5)) l.toString(5) else "%.6f"
        val flags = if (l.isNumber(6)) l.toNumber(6).toInt() else 0

        val changed = ImGui.inputDouble(label, value, step, stepFast, format ?: "%.6f", flags)

        l.push(changed)
        l.push(value.get())
        return 2
    }

    private fun sameLine(l: Lua): Int {
        val offset = if (l.isNumber(1)) l.toNumber(1).toFloat() else 0.0f
        val spacing = if (l.isNumber(2)) l.toNumber(2).toFloat() else -1.0f
        ImGui.sameLine(offset, spacing)
        return 0
    }

    private fun newLine(l: Lua): Int {
        ImGui.newLine()
        return 0
    }

    private fun spacing(l: Lua): Int {
        ImGui.spacing()
        return 0
    }

    private fun separator(l: Lua): Int {
        ImGui.separator()
        return 0
    }

    private fun beginGroup(l: Lua): Int {
        ImGui.beginGroup()
        return 0
    }

    private fun endGroup(l: Lua): Int {
        ImGui.endGroup()
        return 0
    }

    private fun indent(l: Lua): Int {
        val width = if (l.isNumber(1)) l.toNumber(1).toFloat() else 0.0f
        ImGui.indent(width)
        return 0
    }

    private fun unindent(l: Lua): Int {
        val width = if (l.isNumber(1)) l.toNumber(1).toFloat() else 0.0f
        ImGui.unindent(width)
        return 0
    }

    private fun setCursorPos(l: Lua): Int {
        val x = l.toNumber(1).toFloat()
        val y = l.toNumber(2).toFloat()
        ImGui.setCursorPos(x, y)
        return 0
    }

    private fun getCursorPos(l: Lua): Int {
        val pos = ImGui.getCursorPos()
        l.push(pos.x.toDouble())
        l.push(pos.y.toDouble())
        return 2
    }

    private fun getCursorScreenPos(l: Lua): Int {
        val pos = ImGui.getCursorScreenPos()
        l.push(pos.x.toDouble())
        l.push(pos.y.toDouble())
        return 2
    }

    private fun treeNode(l: Lua): Int {
        val label = l.toString(1) ?: ""
        l.push(ImGui.treeNode(label))
        return 1
    }

    private fun treeNodeEx(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val flags = if (l.isNumber(2)) l.toNumber(2).toInt() else 0
        l.push(ImGui.treeNodeEx(label, flags))
        return 1
    }

    private fun treePop(l: Lua): Int {
        ImGui.treePop()
        return 0
    }

    private fun collapsingHeader(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val flags = if (l.isNumber(2)) l.toNumber(2).toInt() else 0
        l.push(ImGui.collapsingHeader(label, flags))
        return 1
    }

    private fun selectable(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val selected = if (l.isBoolean(2)) l.toBoolean(2) else false
        val flags = if (l.isNumber(3)) l.toNumber(3).toInt() else 0
        val width = if (l.isNumber(4)) l.toNumber(4).toFloat() else 0f
        val height = if (l.isNumber(5)) l.toNumber(5).toFloat() else 0f

        val clicked = ImGui.selectable(label, selected, flags, width, height)

        l.push(clicked)
        l.push(selected)
        return 2
    }

    private fun listBox(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val currentItemRef = ImInt(l.toNumber(2).toInt())

        if (!l.isTable(3)) {
            l.push(currentItemRef.get().toDouble())
            return 1
        }

        // Получаем массив строк из таблицы Lua
        val itemsTableIdx = 3
        val itemsCount = l.rawLength(itemsTableIdx)
        val itemsArray = Array(itemsCount) { i ->
            l.rawGetI(itemsTableIdx, i + 1)
            val str = l.toString(-1) ?: ""
            l.pop(1) // Убираем строку из стека
            str
        }

        val heightInItems = if (l.isNumber(4)) l.toNumber(4).toInt() else -1

        ImGui.listBox(label, currentItemRef, itemsArray, heightInItems)

        l.push(currentItemRef.get().toDouble())
        return 1
    }

    private fun setTooltip(l: Lua): Int {
        ImGui.setTooltip(l.toString(1) ?: "")
        return 0
    }

    private fun beginTooltip(l: Lua): Int {
        ImGui.beginTooltip()
        return 0
    }

    private fun endTooltip(l: Lua): Int {
        ImGui.endTooltip()
        return 0
    }

    private fun beginPopup(l: Lua): Int {
        val strId = l.toString(1) ?: ""
        val flags = if (l.isNumber(2)) l.toNumber(2).toInt() else 0
        l.push(ImGui.beginPopup(strId, flags))
        return 1
    }

    private fun beginPopupModal(l: Lua): Int {
        val name = l.toString(1) ?: ""
        val flags = if (l.isNumber(2)) l.toNumber(2).toInt() else 0
        l.push(ImGui.beginPopupModal(name, flags))
        return 1
    }

    private fun endPopup(l: Lua): Int {
        ImGui.endPopup()
        return 0
    }

    private fun openPopup(l: Lua): Int {
        val strId = l.toString(1) ?: ""
        val flags = if (l.isNumber(2)) l.toNumber(2).toInt() else 0
        ImGui.openPopup(strId, flags)
        return 0
    }

    private fun closeCurrentPopup(l: Lua): Int {
        ImGui.closeCurrentPopup()
        return 0
    }

    private fun beginMenuBar(l: Lua): Int {
        l.push(ImGui.beginMenuBar())
        return 1
    }

    private fun endMenuBar(l: Lua): Int {
        ImGui.endMenuBar()
        return 0
    }

    private fun beginMainMenuBar(l: Lua): Int {
        l.push(ImGui.beginMainMenuBar())
        return 1
    }

    private fun endMainMenuBar(l: Lua): Int {
        ImGui.endMainMenuBar()
        return 0
    }

    private fun beginMenu(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val enabled = if (l.isBoolean(2)) l.toBoolean(2) else true
        l.push(ImGui.beginMenu(label, enabled))
        return 1
    }

    private fun endMenu(l: Lua): Int {
        ImGui.endMenu()
        return 0
    }

    private fun menuItem(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val shortcut = if (l.isString(2)) l.toString(2) else ""
        val selected = if (l.isBoolean(3)) l.toBoolean(3) else false
        val enabled = if (l.isBoolean(4)) l.toBoolean(4) else true

        val clicked = ImGui.menuItem(label, shortcut, selected, enabled)

        l.push(clicked)
        l.push(selected)
        return 2
    }

    private fun beginTabBar(l: Lua): Int {
        val strId = l.toString(1) ?: ""
        val flags = if (l.isNumber(2)) l.toNumber(2).toInt() else 0
        l.push(ImGui.beginTabBar(strId, flags))
        return 1
    }

    private fun endTabBar(l: Lua): Int {
        ImGui.endTabBar()
        return 0
    }

    private fun beginTabItem(l: Lua): Int {
        val label = l.toString(1) ?: ""
        val flags = if (l.isNumber(2)) l.toNumber(2).toInt() else 0
        l.push(ImGui.beginTabItem(label, flags))
        return 1
    }

    private fun endTabItem(l: Lua): Int {
        ImGui.endTabItem()
        return 0
    }

    private fun beginChild(l: Lua): Int {
        val strId = l.toString(1) ?: ""
        val width = if (l.isNumber(2)) l.toNumber(2).toFloat() else 0f
        val height = if (l.isNumber(3)) l.toNumber(3).toFloat() else 0f
        val border = if (l.isBoolean(4)) l.toBoolean(4) else false
        val flags = if (l.isNumber(5)) l.toNumber(5).toInt() else 0
        l.push(ImGui.beginChild(strId, width, height, border, flags))
        return 1
    }

    private fun endChild(l: Lua): Int {
        ImGui.endChild()
        return 0
    }

    private fun pushStyleColor(l: Lua): Int {
        val idx = l.toNumber(1).toInt()
        val r = l.toNumber(2).toFloat()
        val g = l.toNumber(3).toFloat()
        val b = l.toNumber(4).toFloat()
        val a = if (l.isNumber(5)) l.toNumber(5).toFloat() else 1.0f
        ImGui.pushStyleColor(idx, r, g, b, a)
        return 0
    }

    private fun popStyleColor(l: Lua): Int {
        val count = if (l.isNumber(1)) l.toNumber(1).toInt() else 1
        ImGui.popStyleColor(count)
        return 0
    }

    private fun pushStyleVar(l: Lua): Int {
        val idx = l.toNumber(1).toInt()
        val x = l.toNumber(2).toFloat()
        val y = if (l.isNumber(3)) l.toNumber(3).toFloat() else 0.0f
        ImGui.pushStyleVar(idx, x, y)
        return 0
    }

    private fun popStyleVar(l: Lua): Int {
        val count = if (l.isNumber(1)) l.toNumber(1).toInt() else 1
        ImGui.popStyleVar(count)
        return 0
    }

    private fun pushFont(l: Lua): Int {
        // Логика была пустой в оригинале
        return 0
    }

    private fun popFont(l: Lua): Int {
        ImGui.popFont()
        return 0
    }

    private fun pushID(l: Lua): Int {
        val strId = l.toString(1) ?: ""
        ImGui.pushID(strId)
        return 0
    }

    private fun popID(l: Lua): Int {
        ImGui.popID()
        return 0
    }

    private fun isItemHovered(l: Lua): Int {
        val flags = if (l.isNumber(1)) l.toNumber(1).toInt() else 0
        l.push(ImGui.isItemHovered(flags))
        return 1
    }

    private fun isItemClicked(l: Lua): Int {
        val mouseButton = if (l.isNumber(1)) l.toNumber(1).toInt() else 0
        l.push(ImGui.isItemClicked(mouseButton))
        return 1
    }

    private fun isItemActive(l: Lua): Int {
        l.push(ImGui.isItemActive())
        return 1
    }

    private fun isWindowAppearing(l: Lua): Int {
        l.push(ImGui.isWindowAppearing())
        return 1
    }

    private fun isWindowCollapsed(l: Lua): Int {
        l.push(ImGui.isWindowCollapsed())
        return 1
    }

    private fun isWindowFocused(l: Lua): Int {
        val flags = if (l.isNumber(1)) l.toNumber(1).toInt() else 0
        l.push(ImGui.isWindowFocused(flags))
        return 1
    }

    private fun isWindowHovered(l: Lua): Int {
        val flags = if (l.isNumber(1)) l.toNumber(1).toInt() else 0
        l.push(ImGui.isWindowHovered(flags))
        return 1
    }

    private fun setNextWindowSize(l: Lua): Int {
        val width = l.toNumber(1).toFloat()
        val height = l.toNumber(2).toFloat()
        val cond = if (l.isNumber(3)) l.toNumber(3).toInt() else 0
        ImGui.setNextWindowSize(width, height, cond)
        return 0
    }

    private fun setNextWindowPos(l: Lua): Int {
        val x = l.toNumber(1).toFloat()
        val y = l.toNumber(2).toFloat()
        val cond = if (l.isNumber(3)) l.toNumber(3).toInt() else 0
        val pivotX = if (l.isNumber(4)) l.toNumber(4).toFloat() else 0f
        val pivotY = if (l.isNumber(5)) l.toNumber(5).toFloat() else 0f
        ImGui.setNextWindowPos(x, y, cond, pivotX, pivotY)
        return 0
    }

    private fun setNextWindowCollapsed(l: Lua): Int {
        val collapsed = l.toBoolean(1)
        val cond = if (l.isNumber(2)) l.toNumber(2).toInt() else 0
        ImGui.setNextWindowCollapsed(collapsed, cond)
        return 0
    }

    private fun setNextWindowFocus(l: Lua): Int {
        ImGui.setNextWindowFocus()
        return 0
    }

    private fun getWindowSize(l: Lua): Int {
        val size = ImGui.getWindowSize()
        l.push(size.x.toDouble())
        l.push(size.y.toDouble())
        return 2
    }

    private fun getWindowPos(l: Lua): Int {
        val pos = ImGui.getWindowPos()
        l.push(pos.x.toDouble())
        l.push(pos.y.toDouble())
        return 2
    }

    private fun getWindowWidth(l: Lua): Int {
        l.push(ImGui.getWindowWidth().toDouble())
        return 1
    }

    private fun getWindowHeight(l: Lua): Int {
        l.push(ImGui.getWindowHeight().toDouble())
        return 1
    }
}