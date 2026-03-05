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
import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import java.util.concurrent.atomic.AtomicInteger

class ImGuiLib : TwoArgFunction() {
    public val queue: ImDrawCommandQueue = ImDrawCommandQueue()


    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val library = LuaTable()

        // Window management
        library.set("begin", begin())
        library.set("endBegin", endFunc())
        library.set("newFrame", newFrame())
        library.set("render", render())

        // Text
        library.set("text", text())
        library.set("textColored", textColored())
        library.set("textDisabled", textDisabled())
        library.set("bulletText", bulletText())

        // Images
        library.set("createImageObject", createImageObject())
        library.set("image", image())

        // Buttons
        library.set("button", button())
        library.set("smallButton", smallButton())
        library.set("arrowButton", arrowButton())
        library.set("checkbox", checkbox())

        // Input
        library.set("inputText", inputText())
        library.set("inputTextMultiline", inputTextMultiline())
        library.set("inputInt", inputInt())
        library.set("inputFloat", inputFloat())
        library.set("inputDouble", inputDouble())

        // Layout
        library.set("sameLine", sameLine())
        library.set("newLine", newLine())
        library.set("spacing", spacing())
        library.set("separator", separator())

        // Groups
        library.set("beginGroup", beginGroup())
        library.set("endGroup", endGroup())

        // Indentation
        library.set("indent", indent())
        library.set("unindent", unindent())

        // Cursor position
        library.set("setCursorPos", setCursorPos())
        library.set("getCursorPos", getCursorPos())
        library.set("getCursorScreenPos", getCursorScreenPos())

        // Tree nodes
        library.set("treeNode", treeNode())
        library.set("treeNodeEx", treeNodeEx())
        library.set("treePop", treePop())
        library.set("collapsingHeader", collapsingHeader())

        // Selectables
        library.set("selectable", selectable())

        // Lists
        library.set("listBox", listBox())

        // Tooltips
        library.set("setTooltip", setTooltip())
        library.set("beginTooltip", beginTooltip())
        library.set("endTooltip", endTooltip())

        // Popups
        library.set("beginPopup", beginPopup())
        library.set("beginPopupModal", beginPopupModal())
        library.set("endPopup", endPopup())
        library.set("openPopup", openPopup())
        library.set("closeCurrentPopup", closeCurrentPopup())

        // Menus
        library.set("beginMenuBar", beginMenuBar())
        library.set("endMenuBar", endMenuBar())
        library.set("beginMainMenuBar", beginMainMenuBar())
        library.set("endMainMenuBar", endMainMenuBar())
        library.set("beginMenu", beginMenu())
        library.set("endMenu", endMenu())
        library.set("menuItem", menuItem())

        // Tabs
        library.set("beginTabBar", beginTabBar())
        library.set("endTabBar", endTabBar())
        library.set("beginTabItem", beginTabItem())
        library.set("endTabItem", endTabItem())

        // Child windows
        library.set("beginChild", beginChild())
        library.set("endChild", endChild())

        // Style
        library.set("pushStyleColor", pushStyleColor())
        library.set("popStyleColor", popStyleColor())
        library.set("pushStyleVar", pushStyleVar())
        library.set("popStyleVar", popStyleVar())

        // Font
        library.set("pushFont", pushFont())
        library.set("popFont", popFont())

        // ID stack
        library.set("pushID", pushID())
        library.set("popID", popID())

        // Utilities
        library.set("setNextItemWidth", setNextItemWidth())
        library.set("isItemHovered", isItemHovered())
        library.set("isItemClicked", isItemClicked())
        library.set("isItemActive", isItemActive())
        library.set("isWindowAppearing", isWindowAppearing())
        library.set("isWindowCollapsed", isWindowCollapsed())
        library.set("isWindowFocused", isWindowFocused())
        library.set("isWindowHovered", isWindowHovered())

        // Window manipulation
        library.set("setNextWindowSize", setNextWindowSize())
        library.set("setNextWindowPos", setNextWindowPos())
        library.set("setNextWindowCollapsed", setNextWindowCollapsed())
        library.set("setNextWindowFocus", setNextWindowFocus())

        // State queries
        library.set("getWindowSize", getWindowSize())
        library.set("getWindowPos", getWindowPos())
        library.set("getWindowWidth", getWindowWidth())
        library.set("getWindowHeight", getWindowHeight())

        // Table
        library.set("beginTable", beginTable())
        library.set("tableSetupColumn", tableSetupColumn())
        library.set("tableHeadersRow", tableHeadersRow())
        library.set("tableNextRow", tableNextRow())
        library.set("tableSetColumnIndex", tableSetColumnIndex())
        library.set("endTable", endTable())

        // Sliders
        library.set("sliderFloat", sliderFloat())
        library.set("sliderInt", sliderInt())
        library.set("vSliderFloat", vSliderFloat())
        library.set("vSliderInt", vSliderInt())

        // Constants
        val constants = LuaTable()
        constants.set("WindowFlags_None", ImGuiWindowFlags.None.toInt())
        constants.set("WindowFlags_NoTitleBar", ImGuiWindowFlags.NoTitleBar.toInt())
        constants.set("WindowFlags_NoResize", ImGuiWindowFlags.NoResize.toInt())
        constants.set("WindowFlags_NoMove", ImGuiWindowFlags.NoMove.toInt())
        constants.set("WindowFlags_NoScrollbar", ImGuiWindowFlags.NoScrollbar.toInt())
        constants.set("WindowFlags_NoCollapse", ImGuiWindowFlags.NoCollapse.toInt())

        constants.set("Cond_Always", ImGuiCond.Always.toInt())
        constants.set("Cond_Once", ImGuiCond.Once.toInt())
        constants.set("Cond_FirstUseEver", ImGuiCond.FirstUseEver.toInt())

        constants.set("ColorEditFlags_None", ImGuiColorEditFlags.None.toInt())
        constants.set("ColorEditFlags_NoAlpha", ImGuiColorEditFlags.NoAlpha.toInt())
        constants.set("ColorEditFlags_NoPicker", ImGuiColorEditFlags.NoPicker.toInt())

        constants.set("Col_Text", ImGuiCol.Text.toInt())
        constants.set("Col_TextDisabled", ImGuiCol.TextDisabled.toInt())
        constants.set("Col_WindowBg", ImGuiCol.WindowBg.toInt())
        constants.set("Col_ChildBg", ImGuiCol.ChildBg.toInt())
        constants.set("Col_PopupBg", ImGuiCol.PopupBg.toInt())
        constants.set("Col_Border", ImGuiCol.Border.toInt())
        constants.set("Col_BorderShadow", ImGuiCol.BorderShadow.toInt())
        constants.set("Col_FrameBg", ImGuiCol.FrameBg.toInt())
        constants.set("Col_FrameBgHovered", ImGuiCol.FrameBgHovered.toInt())
        constants.set("Col_FrameBgActive", ImGuiCol.FrameBgActive.toInt())
        constants.set("Col_TitleBg", ImGuiCol.TitleBg.toInt())
        constants.set("Col_TitleBgActive", ImGuiCol.TitleBgActive.toInt())
        constants.set("Col_TitleBgCollapsed", ImGuiCol.TitleBgCollapsed.toInt())
        constants.set("Col_MenuBarBg", ImGuiCol.MenuBarBg.toInt())
        constants.set("Col_ScrollbarBg", ImGuiCol.ScrollbarBg.toInt())
        constants.set("Col_ScrollbarGrab", ImGuiCol.ScrollbarGrab.toInt())
        constants.set("Col_ScrollbarGrabHovered", ImGuiCol.ScrollbarGrabHovered.toInt())
        constants.set("Col_ScrollbarGrabActive", ImGuiCol.ScrollbarGrabActive.toInt())
        constants.set("Col_CheckMark", ImGuiCol.CheckMark.toInt())
        constants.set("Col_SliderGrab", ImGuiCol.SliderGrab.toInt())
        constants.set("Col_SliderGrabActive", ImGuiCol.SliderGrabActive.toInt())
        constants.set("Col_Button", ImGuiCol.Button.toInt())
        constants.set("Col_ButtonHovered", ImGuiCol.ButtonHovered.toInt())
        constants.set("Col_ButtonActive", ImGuiCol.ButtonActive.toInt())
        constants.set("Col_Header", ImGuiCol.Header.toInt())
        constants.set("Col_HeaderHovered", ImGuiCol.HeaderHovered.toInt())
        constants.set("Col_HeaderActive", ImGuiCol.HeaderActive.toInt())
        constants.set("Col_Separator", ImGuiCol.Separator.toInt())
        constants.set("Col_SeparatorHovered", ImGuiCol.SeparatorHovered.toInt())
        constants.set("Col_SeparatorActive", ImGuiCol.SeparatorActive.toInt())
        constants.set("Col_ResizeGrip", ImGuiCol.ResizeGrip.toInt())
        constants.set("Col_ResizeGripHovered", ImGuiCol.ResizeGripHovered.toInt())
        constants.set("Col_ResizeGripActive", ImGuiCol.ResizeGripActive.toInt())
        constants.set("Col_Tab", ImGuiCol.Tab.toInt())
        constants.set("Col_TabHovered", ImGuiCol.TabHovered.toInt())
        constants.set("Col_TabActive", ImGuiCol.TabActive.toInt())
        constants.set("Col_TabUnfocused", ImGuiCol.TabUnfocused.toInt())
        constants.set("Col_TabUnfocusedActive", ImGuiCol.TabUnfocusedActive.toInt())
        constants.set("Col_PlotLines", ImGuiCol.PlotLines.toInt())
        constants.set("Col_PlotLinesHovered", ImGuiCol.PlotLinesHovered.toInt())
        constants.set("Col_PlotHistogram", ImGuiCol.PlotHistogram.toInt())
        constants.set("Col_PlotHistogramHovered", ImGuiCol.PlotHistogramHovered.toInt())
        constants.set("Col_TableHeaderBg", ImGuiCol.TableHeaderBg.toInt())
        constants.set("Col_TableBorderStrong", ImGuiCol.TableBorderStrong.toInt())
        constants.set("Col_TableBorderLight", ImGuiCol.TableBorderLight.toInt())
        constants.set("Col_TableRowBg", ImGuiCol.TableRowBg.toInt())
        constants.set("Col_TableRowBgAlt", ImGuiCol.TableRowBgAlt.toInt())
        constants.set("Col_TextSelectedBg", ImGuiCol.TextSelectedBg.toInt())
        constants.set("Col_DragDropTarget", ImGuiCol.DragDropTarget.toInt())
        constants.set("Col_NavHighlight", ImGuiCol.NavHighlight.toInt())
        constants.set("Col_NavWindowingHighlight", ImGuiCol.NavWindowingHighlight.toInt())
        constants.set("Col_NavWindowingDimBg", ImGuiCol.NavWindowingDimBg.toInt())
        constants.set("Col_ModalWindowDimBg", ImGuiCol.ModalWindowDimBg.toInt())

        // Style vars
        constants.set("StyleVar_Alpha", ImGuiStyleVar.Alpha.toInt())
        constants.set("StyleVar_DisabledAlpha", ImGuiStyleVar.DisabledAlpha.toInt())
        constants.set("StyleVar_WindowPadding", ImGuiStyleVar.WindowPadding.toInt())
        constants.set("StyleVar_WindowRounding", ImGuiStyleVar.WindowRounding.toInt())
        constants.set("StyleVar_WindowBorderSize", ImGuiStyleVar.WindowBorderSize.toInt())
        constants.set("StyleVar_WindowMinSize", ImGuiStyleVar.WindowMinSize.toInt())
        constants.set("StyleVar_WindowTitleAlign", ImGuiStyleVar.WindowTitleAlign.toInt())
        constants.set("StyleVar_ChildRounding", ImGuiStyleVar.ChildRounding.toInt())
        constants.set("StyleVar_ChildBorderSize", ImGuiStyleVar.ChildBorderSize.toInt())
        constants.set("StyleVar_PopupRounding", ImGuiStyleVar.PopupRounding.toInt())
        constants.set("StyleVar_PopupBorderSize", ImGuiStyleVar.PopupBorderSize.toInt())
        constants.set("StyleVar_FramePadding", ImGuiStyleVar.FramePadding.toInt())
        constants.set("StyleVar_FrameRounding", ImGuiStyleVar.FrameRounding.toInt())
        constants.set("StyleVar_FrameBorderSize", ImGuiStyleVar.FrameBorderSize.toInt())
        constants.set("StyleVar_ItemSpacing", ImGuiStyleVar.ItemSpacing.toInt())
        constants.set("StyleVar_ItemInnerSpacing", ImGuiStyleVar.ItemInnerSpacing.toInt())
        constants.set("StyleVar_IndentSpacing", ImGuiStyleVar.IndentSpacing.toInt())
        constants.set("StyleVar_CellPadding", ImGuiStyleVar.CellPadding.toInt())
        constants.set("StyleVar_ScrollbarSize", ImGuiStyleVar.ScrollbarSize.toInt())
        constants.set("StyleVar_ScrollbarRounding", ImGuiStyleVar.ScrollbarRounding.toInt())
        constants.set("StyleVar_GrabMinSize", ImGuiStyleVar.GrabMinSize.toInt())
        constants.set("StyleVar_GrabRounding", ImGuiStyleVar.GrabRounding.toInt())
        constants.set("StyleVar_TabRounding", ImGuiStyleVar.TabRounding.toInt())
        constants.set("StyleVar_ButtonTextAlign", ImGuiStyleVar.ButtonTextAlign.toInt())
        constants.set("StyleVar_SelectableTextAlign", ImGuiStyleVar.SelectableTextAlign.toInt())

        constants.set("TableFlags_BordersInner", ImGuiTableFlags.BordersInner.toInt())
        constants.set("TableFlags_BordersInnerH", ImGuiTableFlags.BordersInnerH.toInt())
        constants.set("TableFlags_BordersInnerV", ImGuiTableFlags.BordersInnerV.toInt())
        constants.set("TableFlags_Resizable", ImGuiTableFlags.Resizable.toInt())
        library.set("constants", constants)

        // Constants
        val dl = LuaTable()
        dl.set("renderLine", RenderLineFunction())
        dl.set("renderPolygon", RenderPolygonFunction())
        library.set("dl", dl)

        env.set("imgui", library)
        return library
    }

    private inner class RenderLineFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable()) {
                val x1: Float = if (table.get("x1").isnumber()) table.get("x1").tofloat() else 0f
                val y1: Float = if (table.get("y1").isnumber()) table.get("y1").tofloat() else 0f
                val x2: Float = if (table.get("x2").isnumber()) table.get("x2").tofloat() else 0f
                val y2: Float = if (table.get("y2").isnumber()) table.get("y2").tofloat() else 0f

                val red = if (table.get("red").isnumber()) table.get("red").toint() else 255
                val green = if (table.get("green").isnumber()) table.get("green").toint() else 255
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 255
                val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 255

                val thickness: Float = if (table.get("thickness").isnumber()) table.get("thickness").tofloat() else 1f

                // создаём запрос для отрисовки линии
                queue.queue(DrawCommand(DrawType.LINE, mapOf(
                    "x1" to x1, "y1" to y1, "x2" to x2, "y2" to y2,
                    "color" to queue.makeImGuiColor(red, green, blue, alpha),
                    "thickness" to thickness
                )))
                return TRUE
            }
            return NIL
        }
    }

    private inner class RenderPolygonFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable()) {
                val pointsTable = table.get("points")
                if (pointsTable.istable()) {
                    val red = if (table.get("red").isnumber()) table.get("red").toint() else 255
                    val green = if (table.get("green").isnumber()) table.get("green").toint() else 255
                    val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 255
                    val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 255

                    val points = mutableListOf<Pair<Float, Float>>()
                    var i = 1
                    while (true) {
                        val pointTable = pointsTable.get(i)
                        if (pointTable.istable()) {
                            val x = if (pointTable.get("x").isnumber()) pointTable.get("x").tofloat() else 0f
                            val y = if (pointTable.get("y").isnumber()) pointTable.get("y").tofloat() else 0f
                            points.add(x to y)
                            i++
                        } else {
                            break
                        }
                    }

                    if (points.size >= 3) {
                        queue.queue(
                            DrawCommand(
                                DrawType.POLYGON, mapOf(
                                    "points" to points,
                                    "color" to queue.makeImGuiColor(red, green, blue, alpha)
                                )
                            )
                        )
                        return TRUE
                    }
                }
                return FALSE
            }
            return NIL
        }
    }

    inner class sliderFloat : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val value = floatArrayOf(args.checkdouble(2).toFloat())
            val min = args.checkdouble(3).toFloat()
            val max = args.checkdouble(4).toFloat()
            val format = if (args.narg() > 4) args.checkjstring(5) else "%.3f"
            val flags = if (args.narg() > 5) args.checkint(6) else 0
            val changed = ImGui.sliderFloat(label, value, min, max, format, flags)
            return LuaValue.varargsOf(LuaValue.valueOf(changed), LuaValue.valueOf(value[0].toDouble()))
        }
    }

    inner class sliderInt : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val value = intArrayOf(args.checkint(2))
            val min = args.checkint(3)
            val max = args.checkint(4)
            val format = if (args.narg() > 4) args.checkjstring(5) else "%d"
            val flags = if (args.narg() > 5) args.checkint(6) else 0
            val changed = ImGui.sliderInt(label, value, min, max, format, flags)
            return LuaValue.varargsOf(LuaValue.valueOf(changed), LuaValue.valueOf(value[0]))
        }
    }

    inner class vSliderFloat : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val sizeX = args.checkdouble(2).toFloat()
            val sizeY = args.checkdouble(3).toFloat()
            val value = floatArrayOf(args.checkdouble(4).toFloat())
            val min = args.checkdouble(5).toFloat()
            val max = args.checkdouble(6).toFloat()
            val format = if (args.narg() > 6) args.checkjstring(7) else "%.3f"
            val flags = if (args.narg() > 7) args.checkint(8) else 0
            val changed = ImGui.vSliderFloat(label, sizeX, sizeY, value, min, max, format, flags)
            return LuaValue.varargsOf(LuaValue.valueOf(changed), LuaValue.valueOf(value[0].toDouble()))
        }
    }

    inner class vSliderInt : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val sizeX = args.checkdouble(2).toFloat()
            val sizeY = args.checkdouble(3).toFloat()
            val value = intArrayOf(args.checkint(4))
            val min = args.checkint(5)
            val max = args.checkint(6)
            val format = if (args.narg() > 6) args.checkjstring(7) else "%d"
            val flags = if (args.narg() > 7) args.checkint(8) else 0
            val changed = ImGui.vSliderInt(label, sizeX, sizeY, value, min, max, format, flags)
            return LuaValue.varargsOf(LuaValue.valueOf(changed), LuaValue.valueOf(value[0]))
        }
    }

    inner class beginTable : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val name = args.checkjstring(1)
            val column = args.checkint(2)
            val flags = args.checkint(3)
            ImGui.beginTable(name, column, flags)
            return LuaValue.NIL
        }
    }

    inner class endTable : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            ImGui.endTable()

            return LuaValue.NIL
        }
    }

    inner class tableSetColumnIndex : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val index = args.checkint(1)
            ImGui.tableSetColumnIndex(index)

            return LuaValue.NIL
        }
    }

    inner class tableNextRow : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            ImGui.tableNextRow()

            return LuaValue.NIL
        }
    }

    inner class tableHeadersRow : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            ImGui.tableHeadersRow()

            return LuaValue.NIL
        }
    }

    inner class tableSetupColumn : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val name = args.checkjstring(1)
            ImGui.tableSetupColumn(name)

            return LuaValue.NIL
        }
    }

    inner class setNextItemWidth : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val width = args.checkdouble(1).toFloat()

            return LuaValue.NIL
        }
    }

    inner class createImageObject : ZeroArgFunction() {
        override fun call(): LuaValue {
            return ImGuiTexture(AtomicInteger(0))
        }
    }

    inner class image : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.isvalue(1) && args.isvalue(2) && args.isvalue(3)) {
                val image = when {
                    args.arg(1).isuserdata() && args.arg(1).touserdata() is ImGuiTexture -> (args.arg(1).touserdata() as ImGuiTexture).texture.get().toLong()
                    args.arg(1).isuserdata() && args.arg(1).touserdata() is AtomicInteger -> (args.arg(1).touserdata() as AtomicInteger).get().toLong()
                    args.arg(1).isnumber() -> args.arg(1).tolong()
                    else -> null
                }

                if (image != null && image > 0) {
                    if (args.narg() == 7) {
                        ImGui.image(image, args.arg(2).tofloat(), args.arg(3).tofloat(), args.arg(4).tofloat(), args.arg(5).tofloat(), args.arg(6).tofloat(), args.arg(7).tofloat())
                    }
                    else if (args.narg() == 5) {
                        ImGui.image(image, args.arg(2).tofloat(), args.arg(3).tofloat(), args.arg(4).tofloat(), args.arg(5).tofloat())
                    }
                    else {
                        ImGui.image(image, args.arg(2).tofloat(), args.arg(53).tofloat())
                    }
                    return TRUE
                }
            }
            return FALSE
        }
    }

    inner class pathClear : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.getBackgroundDrawList().pathClear()
            return LuaValue.TRUE
        }
    }

    inner class pathLineTo : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.checkdouble(1).toFloat()
            val y = args.checkdouble(2).toFloat()
            ImGui.getBackgroundDrawList().pathLineTo(x, y)
            return LuaValue.TRUE
        }
    }

    inner class pathStroke : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val color = args.checkint(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val thickness = if (args.narg() > 2) args.checkdouble(3).toFloat() else 1.0f
            ImGui.getBackgroundDrawList().pathStroke(color, flags, thickness)
            return LuaValue.TRUE
        }
    }

    inner class newFrame : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.newFrame()
            return LuaValue.TRUE
        }
    }

    inner class render : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.render()
            return LuaValue.TRUE
        }
    }

    inner class begin : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val name = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.begin(name, flags)
            return LuaValue.valueOf(opened)
        }
    }

    inner class endFunc : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.end()
            return LuaValue.TRUE
        }
    }

    inner class text : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.text(args.checkjstring(1))
            return LuaValue.NIL
        }
    }

    inner class textColored : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val r = args.checkdouble(1).toFloat()
            val g = args.checkdouble(2).toFloat()
            val b = args.checkdouble(3).toFloat()
            val a = if (args.narg() > 3) args.checkdouble(4).toFloat() else 1.0f
            ImGui.textColored(r, g, b, a, args.checkjstring(5))
            return LuaValue.NIL
        }
    }

    inner class textDisabled : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.textDisabled(args.checkjstring(1))
            return LuaValue.NIL
        }
    }

    inner class bulletText : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.bulletText(args.checkjstring(1))
            return LuaValue.NIL
        }
    }

    inner class button : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val width = if (args.narg() > 1) args.checkdouble(2).toFloat() else 0f
            val height = if (args.narg() > 2) args.checkdouble(3).toFloat() else 0f
            val clicked = ImGui.button(label, width, height)
            return LuaValue.valueOf(clicked)
        }
    }

    inner class smallButton : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val clicked = ImGui.smallButton(args.checkjstring(1))
            return LuaValue.valueOf(clicked)
        }
    }

    inner class arrowButton : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            val dir = args.checkint(2)
            val clicked = ImGui.arrowButton(strId, dir)
            return LuaValue.valueOf(clicked)
        }
    }

    inner class checkbox : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val value = ImBoolean(args.checkboolean(2))
            val changed = ImGui.checkbox(label, value)
            return LuaValue.varargsOf(LuaValue.valueOf(changed), LuaValue.valueOf(value.get()))
        }
    }

    inner class inputText : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val text = ImString(args.checkjstring(2), 256)
            val flags = if (args.narg() > 2) args.checkint(3) else 0
            val changed = ImGui.inputText(label, text, flags)
            return LuaValue.varargsOf(LuaValue.valueOf(changed), LuaValue.valueOf(text.get()))
        }
    }

    inner class inputTextMultiline : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val text = ImString(args.checkjstring(2), 1024)
            val width = if (args.narg() > 2) args.checkdouble(3).toFloat() else 0f
            val height = if (args.narg() > 3) args.checkdouble(4).toFloat() else 0f
            val flags = if (args.narg() > 4) args.checkint(5) else 0
            val changed = ImGui.inputTextMultiline(label, text, width, height, flags)
            return LuaValue.varargsOf(LuaValue.valueOf(changed), LuaValue.valueOf(text.get()))
        }
    }

    inner class inputInt : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val value = ImInt(args.checkint(2))
            val step = if (args.narg() > 2) args.checkint(3) else 1
            val stepFast = if (args.narg() > 3) args.checkint(4) else 100
            val flags = if (args.narg() > 4) args.checkint(5) else 0
            val changed = ImGui.inputInt(label, value, step, stepFast, flags)
            return LuaValue.varargsOf(LuaValue.valueOf(changed), LuaValue.valueOf(value.get()))
        }
    }

    inner class inputFloat : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val value = ImFloat(args.checkdouble(2).toFloat())
            val step = if (args.narg() > 2) args.checkdouble(3).toFloat() else 0.0f
            val stepFast = if (args.narg() > 3) args.checkdouble(4).toFloat() else 0.0f
            val format = if (args.narg() > 4) args.checkjstring(5) else "%.3f"
            val flags = if (args.narg() > 5) args.checkint(6) else 0
            val changed = ImGui.inputFloat(label, value, step, stepFast, format, flags)
            return LuaValue.varargsOf(LuaValue.valueOf(changed), LuaValue.valueOf(value.get().toDouble()))
        }
    }

    inner class inputDouble : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val value = ImDouble(args.checkdouble(2))
            val step = if (args.narg() > 2) args.checkdouble(3) else 0.0
            val stepFast = if (args.narg() > 3) args.checkdouble(4) else 0.0
            val format = if (args.narg() > 4) args.checkjstring(5) else "%.6f"
            val flags = if (args.narg() > 5) args.checkint(6) else 0
            val changed = ImGui.inputDouble(label, value, step, stepFast, format, flags)
            return LuaValue.varargsOf(LuaValue.valueOf(changed), LuaValue.valueOf(value.get()))
        }
    }

    inner class sameLine : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val offsetFromStartX = if (args.narg() > 0) args.checkdouble(1).toFloat() else 0.0f
            val spacing = if (args.narg() > 1) args.checkdouble(2).toFloat() else -1.0f
            ImGui.sameLine(offsetFromStartX, spacing)
            return LuaValue.NIL
        }
    }

    inner class newLine : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.newLine()
            return LuaValue.NIL
        }
    }

    inner class spacing : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.spacing()
            return LuaValue.NIL
        }
    }

    inner class separator : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.separator()
            return LuaValue.NIL
        }
    }

    inner class beginGroup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.beginGroup()
            return LuaValue.NIL
        }
    }

    inner class endGroup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endGroup()
            return LuaValue.NIL
        }
    }

    inner class indent : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val indentWidth = if (args.narg() > 0) args.checkdouble(1).toFloat() else 0.0f
            ImGui.indent(indentWidth)
            return LuaValue.NIL
        }
    }

    inner class unindent : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val indentWidth = if (args.narg() > 0) args.checkdouble(1).toFloat() else 0.0f
            ImGui.unindent(indentWidth)
            return LuaValue.NIL
        }
    }

    inner class setCursorPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.checkdouble(1).toFloat()
            val y = args.checkdouble(2).toFloat()
            ImGui.setCursorPos(x, y)
            return LuaValue.NIL
        }
    }

    inner class getCursorPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val pos = ImGui.getCursorPos()
            return LuaValue.varargsOf(LuaValue.valueOf(pos.x.toDouble()), LuaValue.valueOf(pos.y.toDouble()))
        }
    }

    inner class getCursorScreenPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val pos = ImGui.getCursorScreenPos()
            return LuaValue.varargsOf(LuaValue.valueOf(pos.x.toDouble()), LuaValue.valueOf(pos.y.toDouble()))
        }
    }

    inner class treeNode : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val opened = ImGui.treeNode(args.checkjstring(1))
            return LuaValue.valueOf(opened)
        }
    }

    inner class treeNodeEx : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.treeNodeEx(label, flags)
            return LuaValue.valueOf(opened)
        }
    }

    inner class treePop : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.treePop()
            return LuaValue.NIL
        }
    }

    inner class collapsingHeader : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.collapsingHeader(label, flags)
            return LuaValue.valueOf(opened)
        }
    }

    inner class selectable : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val selected = if (args.narg() > 1) args.checkboolean(2) else false
            val flags = if (args.narg() > 2) args.checkint(3) else 0
            val width = if (args.narg() > 3) args.checkdouble(4).toFloat() else 0f
            val height = if (args.narg() > 4) args.checkdouble(5).toFloat() else 0f
            val clicked = ImGui.selectable(label, selected, flags, width, height)
            return LuaValue.varargsOf(LuaValue.valueOf(clicked), LuaValue.valueOf(selected))
        }
    }

    inner class listBox : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val currentItemRef = ImInt(args.checkint(2))
            val items = args.checktable(3)
            val itemsCount = items.length().toInt()
            val itemsArray = Array(itemsCount) { i -> items.get(i + 1).checkjstring() }
            val heightInItems = if (args.narg() > 3) args.checkint(4) else -1
            ImGui.listBox(label, currentItemRef, itemsArray, heightInItems)
            return LuaValue.valueOf(currentItemRef.get())
        }
    }

    inner class setTooltip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.setTooltip(args.checkjstring(1))
            return LuaValue.NIL
        }
    }

    inner class beginTooltip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.beginTooltip()
            return LuaValue.NIL
        }
    }

    inner class endTooltip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endTooltip()
            return LuaValue.NIL
        }
    }

    inner class beginPopup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.beginPopup(strId, flags)
            return LuaValue.valueOf(opened)
        }
    }

    inner class beginPopupModal : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val name = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.beginPopupModal(name, flags)
            return LuaValue.valueOf(opened)
        }
    }

    inner class endPopup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endPopup()
            return LuaValue.NIL
        }
    }

    inner class openPopup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            ImGui.openPopup(strId, flags)
            return LuaValue.NIL
        }
    }

    inner class closeCurrentPopup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.closeCurrentPopup()
            return LuaValue.NIL
        }
    }

    inner class beginMenuBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val opened = ImGui.beginMenuBar()
            return LuaValue.valueOf(opened)
        }
    }

    inner class endMenuBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endMenuBar()
            return LuaValue.NIL
        }
    }

    inner class beginMainMenuBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val opened = ImGui.beginMainMenuBar()
            return LuaValue.valueOf(opened)
        }
    }

    inner class endMainMenuBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endMainMenuBar()
            return LuaValue.NIL
        }
    }

    inner class beginMenu : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val enabled = if (args.narg() > 1) args.checkboolean(2) else true
            val opened = ImGui.beginMenu(label, enabled)
            return LuaValue.valueOf(opened)
        }
    }

    inner class endMenu : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endMenu()
            return LuaValue.NIL
        }
    }

    inner class menuItem : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val shortcut = if (args.narg() > 1) args.optjstring(2, "") else ""
            val selected = if (args.narg() > 2) args.checkboolean(3) else false
            val enabled = if (args.narg() > 3) args.checkboolean(4) else true
            val clicked = ImGui.menuItem(label, shortcut, selected, enabled)
            return LuaValue.varargsOf(LuaValue.valueOf(clicked), LuaValue.valueOf(selected))
        }
    }

    inner class beginTabBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.beginTabBar(strId, flags)
            return LuaValue.valueOf(opened)
        }
    }

    inner class endTabBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endTabBar()
            return LuaValue.NIL
        }
    }

    inner class beginTabItem : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.beginTabItem(label, flags)
            return LuaValue.valueOf(opened)
        }
    }

    inner class endTabItem : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endTabItem()
            return LuaValue.NIL
        }
    }

    inner class beginChild : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            val width = if (args.narg() > 1) args.checkdouble(2).toFloat() else 0f
            val height = if (args.narg() > 2) args.checkdouble(3).toFloat() else 0f
            val border = if (args.narg() > 3) args.checkboolean(4) else false
            val flags = if (args.narg() > 4) args.checkint(5) else 0
            val opened = ImGui.beginChild(strId, width, height, border, flags)
            return LuaValue.valueOf(opened)
        }
    }

    inner class endChild : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endChild()
            return LuaValue.NIL
        }
    }

    inner class pushStyleColor : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val idx = args.checkint(1)
            val r = args.checkdouble(2).toFloat()
            val g = args.checkdouble(3).toFloat()
            val b = args.checkdouble(4).toFloat()
            val a = if (args.narg() > 4) args.checkdouble(5).toFloat() else 1.0f
            ImGui.pushStyleColor(idx, r, g, b, a)
            return LuaValue.NIL
        }
    }

    inner class popStyleColor : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val count = if (args.narg() > 0) args.checkint(1) else 1
            ImGui.popStyleColor(count)
            return LuaValue.NIL
        }
    }

    inner class pushStyleVar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val idx = args.checkint(1)
            val x = args.checkdouble(2).toFloat()
            val y = if (args.narg() > 2) args.checkdouble(3).toFloat() else 0.0f
            ImGui.pushStyleVar(idx, x, y)
            return LuaValue.NIL
        }
    }

    inner class popStyleVar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val count = if (args.narg() > 0) args.checkint(1) else 1
            ImGui.popStyleVar(count)
            return LuaValue.NIL
        }
    }

    inner class pushFont : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            // Note: This would require font handling
            return LuaValue.NIL
        }
    }

    inner class popFont : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.popFont()
            return LuaValue.NIL
        }
    }

    inner class pushID : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            ImGui.pushID(strId)
            return LuaValue.NIL
        }
    }

    inner class popID : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.popID()
            return LuaValue.NIL
        }
    }

    inner class isItemHovered : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val flags = if (args.narg() > 0) args.checkint(1) else 0
            val hovered = ImGui.isItemHovered(flags)
            return LuaValue.valueOf(hovered)
        }
    }

    inner class isItemClicked : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val mouseButton = if (args.narg() > 0) args.checkint(1) else 0
            val clicked = ImGui.isItemClicked(mouseButton)
            return LuaValue.valueOf(clicked)
        }
    }

    inner class isItemActive : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val active = ImGui.isItemActive()
            return LuaValue.valueOf(active)
        }
    }

    inner class isWindowAppearing : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val appearing = ImGui.isWindowAppearing()
            return LuaValue.valueOf(appearing)
        }
    }

    inner class isWindowCollapsed : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val collapsed = ImGui.isWindowCollapsed()
            return LuaValue.valueOf(collapsed)
        }
    }

    inner class isWindowFocused : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val flags = if (args.narg() > 0) args.checkint(1) else 0
            val focused = ImGui.isWindowFocused(flags)
            return LuaValue.valueOf(focused)
        }
    }

    inner class isWindowHovered : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val flags = if (args.narg() > 0) args.checkint(1) else 0
            val hovered = ImGui.isWindowHovered(flags)
            return LuaValue.valueOf(hovered)
        }
    }

    inner class setNextWindowSize : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val width = args.checkdouble(1).toFloat()
            val height = args.checkdouble(2).toFloat()
            val cond = if (args.narg() > 2) args.checkint(3) else 0
            ImGui.setNextWindowSize(width, height, cond)
            return LuaValue.NIL
        }
    }

    inner class setNextWindowPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.checkdouble(1).toFloat()
            val y = args.checkdouble(2).toFloat()
            val cond = if (args.narg() > 2) args.checkint(3) else 0
            val pivotX = if (args.narg() > 3) args.checkdouble(4).toFloat() else 0f
            val pivotY = if (args.narg() > 4) args.checkdouble(5).toFloat() else 0f
            ImGui.setNextWindowPos(x, y, cond, pivotX, pivotY)
            return LuaValue.NIL
        }
    }

    inner class setNextWindowCollapsed : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val collapsed = args.checkboolean(1)
            val cond = if (args.narg() > 1) args.checkint(2) else 0
            ImGui.setNextWindowCollapsed(collapsed, cond)
            return LuaValue.NIL
        }
    }

    inner class setNextWindowFocus : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.setNextWindowFocus()
            return LuaValue.NIL
        }
    }

    inner class getWindowSize : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val size = ImGui.getWindowSize()
            return LuaValue.varargsOf(LuaValue.valueOf(size.x.toDouble()), LuaValue.valueOf(size.y.toDouble()))
        }
    }

    inner class getWindowPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val pos = ImGui.getWindowPos()
            return LuaValue.varargsOf(LuaValue.valueOf(pos.x.toDouble()), LuaValue.valueOf(pos.y.toDouble()))
        }
    }

    inner class getWindowWidth : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val width = ImGui.getWindowWidth()
            return LuaValue.valueOf(width.toDouble())
        }
    }

    inner class getWindowHeight : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val height = ImGui.getWindowHeight()
            return LuaValue.valueOf(height.toDouble())
        }
    }
}