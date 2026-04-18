package com.nekiplay.neoscripts.features.lua.objects.misc

import com.nekiplay.neoscripts.Main
import com.nekiplay.neoscripts.features.lua.LuaScript
import com.nekiplay.neoscripts.features.lua.objects.misc.imgui.*
import com.nekiplay.neoscripts.imgui.ImguiLoader
import imgui.ImFont
import imgui.ImFontConfig
import imgui.ImFontGlyphRangesBuilder
import imgui.ImGui
import imgui.flag.*
import imgui.flag.ImDrawFlags
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import imgui.type.*
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class ImGuiLib(val script: LuaScript) : LuaValue() {
    public val queue: ImDrawCommandQueue = ImDrawCommandQueue()

    override fun typename(): String = "imgui"
    override fun tojstring(): String = "ImGuiObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    val constants = LuaTable()
    val dl = LuaTable()

    init {
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
        constants.set("TableFlags_BordersOuter", ImGuiTableFlags.BordersOuter.toInt())
        constants.set("TableFlags_BordersOuterH", ImGuiTableFlags.BordersOuterH.toInt())
        constants.set("TableFlags_BordersOuterV", ImGuiTableFlags.BordersOuterV.toInt())
        constants.set("TableFlags_Borders", ImGuiTableFlags.Borders.toInt())
        constants.set("TableFlags_RowBg", ImGuiTableFlags.RowBg.toInt())
        constants.set("TableFlags_ScrollX", ImGuiTableFlags.ScrollX.toInt())
        constants.set("TableFlags_ScrollY", ImGuiTableFlags.ScrollY.toInt())
        constants.set("TableFlags_SizingFixedFit", ImGuiTableFlags.SizingFixedFit.toInt())
        constants.set("TableFlags_SizingStretchSame", ImGuiTableFlags.SizingStretchSame.toInt())
        constants.set("TableFlags_NoHostExtendX", ImGuiTableFlags.NoHostExtendX.toInt())

        // Флаги InputText
        constants.set("InputTextFlags_None", ImGuiInputTextFlags.None.toInt())
        constants.set("InputTextFlags_CharsDecimal", ImGuiInputTextFlags.CharsDecimal.toInt())
        constants.set("InputTextFlags_CharsHexadecimal", ImGuiInputTextFlags.CharsHexadecimal.toInt())
        constants.set("InputTextFlags_CharsUppercase", ImGuiInputTextFlags.CharsUppercase.toInt())
        constants.set("InputTextFlags_CharsNoBlank", ImGuiInputTextFlags.CharsNoBlank.toInt())
        constants.set("InputTextFlags_AutoSelectAll", ImGuiInputTextFlags.AutoSelectAll.toInt())
        constants.set("InputTextFlags_EnterReturnsTrue", ImGuiInputTextFlags.EnterReturnsTrue.toInt())
        constants.set("InputTextFlags_Password", ImGuiInputTextFlags.Password.toInt())
        constants.set("InputTextFlags_ReadOnly", ImGuiInputTextFlags.ReadOnly.toInt())
        constants.set("InputTextFlags_NoHorizontalScroll", ImGuiInputTextFlags.NoHorizontalScroll.toInt())

        // Флаги TreeNode
        constants.set("TreeNodeFlags_None", ImGuiTreeNodeFlags.None.toInt())
        constants.set("TreeNodeFlags_Selected", ImGuiTreeNodeFlags.Selected.toInt())
        constants.set("TreeNodeFlags_Framed", ImGuiTreeNodeFlags.Framed.toInt())
        constants.set("TreeNodeFlags_AllowItemOverlap", ImGuiTreeNodeFlags.AllowItemOverlap.toInt())
        constants.set("TreeNodeFlags_NoTreePushOnOpen", ImGuiTreeNodeFlags.NoTreePushOnOpen.toInt())
        constants.set("TreeNodeFlags_NoAutoOpenOnLog", ImGuiTreeNodeFlags.NoAutoOpenOnLog.toInt())
        constants.set("TreeNodeFlags_DefaultOpen", ImGuiTreeNodeFlags.DefaultOpen.toInt())
        constants.set("TreeNodeFlags_OpenOnDoubleClick", ImGuiTreeNodeFlags.OpenOnDoubleClick.toInt())
        constants.set("TreeNodeFlags_OpenOnArrow", ImGuiTreeNodeFlags.OpenOnArrow.toInt())
        constants.set("TreeNodeFlags_Leaf", ImGuiTreeNodeFlags.Leaf.toInt())
        constants.set("TreeNodeFlags_Bullet", ImGuiTreeNodeFlags.Bullet.toInt())
        constants.set("TreeNodeFlags_CollapsingHeader", ImGuiTreeNodeFlags.CollapsingHeader.toInt())

        // Флаги Selectable
        constants.set("SelectableFlags_None", ImGuiSelectableFlags.None.toInt())
        constants.set("SelectableFlags_DontClosePopups", ImGuiSelectableFlags.DontClosePopups.toInt())
        constants.set("SelectableFlags_SpanAllColumns", ImGuiSelectableFlags.SpanAllColumns.toInt())
        constants.set("SelectableFlags_AllowDoubleClick", ImGuiSelectableFlags.AllowDoubleClick.toInt())
        constants.set("SelectableFlags_Disabled", ImGuiSelectableFlags.Disabled.toInt())
        constants.set("SelectableFlags_AllowItemOverlap", ImGuiSelectableFlags.AllowItemOverlap.toInt())

        // Флаги Slider
        constants.set("SliderFlags_None", ImGuiSliderFlags.None.toInt())
        constants.set("SliderFlags_AlwaysClamp", ImGuiSliderFlags.AlwaysClamp.toInt())
        constants.set("SliderFlags_Logarithmic", ImGuiSliderFlags.Logarithmic.toInt())
        constants.set("SliderFlags_NoRoundToFormat", ImGuiSliderFlags.NoRoundToFormat.toInt())
        constants.set("SliderFlags_NoInput", ImGuiSliderFlags.NoInput.toInt())

        // Флаги WindowFlags (дополнительные)
        constants.set("WindowFlags_NoScrollWithMouse", ImGuiWindowFlags.NoScrollWithMouse.toInt())
        constants.set("WindowFlags_NoBackground", ImGuiWindowFlags.NoBackground.toInt())
        constants.set("WindowFlags_NoSavedSettings", ImGuiWindowFlags.NoSavedSettings.toInt())
        constants.set("WindowFlags_MenuBar", ImGuiWindowFlags.MenuBar.toInt())
        constants.set("WindowFlags_HorizontalScrollbar", ImGuiWindowFlags.HorizontalScrollbar.toInt())
        constants.set("WindowFlags_NoFocusOnAppearing", ImGuiWindowFlags.NoFocusOnAppearing.toInt())
        constants.set("WindowFlags_AlwaysAutoResize", ImGuiWindowFlags.AlwaysAutoResize.toInt())
        constants.set("WindowFlags_AlwaysVerticalScrollbar", ImGuiWindowFlags.AlwaysVerticalScrollbar.toInt())
        constants.set("WindowFlags_AlwaysHorizontalScrollbar", ImGuiWindowFlags.AlwaysHorizontalScrollbar.toInt())
        constants.set("WindowFlags_NoNav", ImGuiWindowFlags.NoNav.toInt())
        constants.set("WindowFlags_NoDecoration", ImGuiWindowFlags.NoDecoration.toInt())
        constants.set("WindowFlags_NoInputs", ImGuiWindowFlags.NoInputs.toInt())

        // Флаги ColorEdit / ColorPicker
        constants.set("ColorEditFlags_AlphaBar", ImGuiColorEditFlags.AlphaBar.toInt())
        constants.set("ColorEditFlags_AlphaPreview", ImGuiColorEditFlags.AlphaPreview.toInt())
        constants.set("ColorEditFlags_AlphaPreviewHalf", ImGuiColorEditFlags.AlphaPreviewHalf.toInt())
        constants.set("ColorEditFlags_HDR", ImGuiColorEditFlags.HDR.toInt())
        constants.set("ColorEditFlags_DisplayRGB", ImGuiColorEditFlags.DisplayRGB.toInt())
        constants.set("ColorEditFlags_DisplayHSV", ImGuiColorEditFlags.DisplayHSV.toInt())
        constants.set("ColorEditFlags_DisplayHex", ImGuiColorEditFlags.DisplayHex.toInt())
        constants.set("ColorEditFlags_Uint8", ImGuiColorEditFlags.Uint8.toInt())
        constants.set("ColorEditFlags_Float", ImGuiColorEditFlags.Float.toInt())
        constants.set("ColorEditFlags_PickerHueBar", ImGuiColorEditFlags.PickerHueBar.toInt())
        constants.set("ColorEditFlags_PickerHueWheel", ImGuiColorEditFlags.PickerHueWheel.toInt())
        constants.set("ColorEditFlags_InputRGB", ImGuiColorEditFlags.InputRGB.toInt())
        constants.set("ColorEditFlags_InputHSV", ImGuiColorEditFlags.InputHSV.toInt())
        constants.set("ColorEditFlags_NoSidePreview", ImGuiColorEditFlags.NoSidePreview.toInt())
        constants.set("ColorEditFlags_NoLabel", ImGuiColorEditFlags.NoLabel.toInt())
        constants.set("ColorEditFlags_NoTooltip", ImGuiColorEditFlags.NoTooltip.toInt())
        constants.set("ColorEditFlags_NoOptions", ImGuiColorEditFlags.NoOptions.toInt())
        constants.set("ColorEditFlags_NoInputs", ImGuiColorEditFlags.NoInputs.toInt())
        constants.set("ColorEditFlags_NoDragDrop", ImGuiColorEditFlags.NoDragDrop.toInt())
        constants.set("ColorEditFlags_NoBorder", ImGuiColorEditFlags.NoBorder.toInt())
        constants.set("ColorEditFlags_NoSmallPreview", ImGuiColorEditFlags.NoSmallPreview.toInt())

        // Направления (Dir) — используется в arrowButton и т.п.
        constants.set("Dir_None", ImGuiDir.None.toInt())
        constants.set("Dir_Left", ImGuiDir.Left.toInt())
        constants.set("Dir_Right", ImGuiDir.Right.toInt())
        constants.set("Dir_Up", ImGuiDir.Up.toInt())
        constants.set("Dir_Down", ImGuiDir.Down.toInt())

        // Кнопки мыши
        constants.set("MouseButton_Left", ImGuiMouseButton.Left.toInt())
        constants.set("MouseButton_Right", ImGuiMouseButton.Right.toInt())
        constants.set("MouseButton_Middle", ImGuiMouseButton.Middle.toInt())

        // Флаги Hovered / Focused
        constants.set("HoveredFlags_None", ImGuiHoveredFlags.None.toInt())
        constants.set("HoveredFlags_ChildWindows", ImGuiHoveredFlags.ChildWindows.toInt())
        constants.set("HoveredFlags_RootWindow", ImGuiHoveredFlags.RootWindow.toInt())
        constants.set("HoveredFlags_AnyWindow", ImGuiHoveredFlags.AnyWindow.toInt())
        constants.set("HoveredFlags_AllowWhenBlockedByPopup", ImGuiHoveredFlags.AllowWhenBlockedByPopup.toInt())
        constants.set("HoveredFlags_AllowWhenBlockedByActiveItem", ImGuiHoveredFlags.AllowWhenBlockedByActiveItem.toInt())
        constants.set("HoveredFlags_AllowWhenOverlapped", ImGuiHoveredFlags.AllowWhenOverlapped.toInt())
        constants.set("HoveredFlags_AllowWhenDisabled", ImGuiHoveredFlags.AllowWhenDisabled.toInt())
        constants.set("FocusedFlags_None", ImGuiFocusedFlags.None.toInt())
        constants.set("FocusedFlags_ChildWindows", ImGuiFocusedFlags.ChildWindows.toInt())
        constants.set("FocusedFlags_RootWindow", ImGuiFocusedFlags.RootWindow.toInt())
        constants.set("FocusedFlags_AnyWindow", ImGuiFocusedFlags.AnyWindow.toInt())

        // Флаги DrawList (для polyline и т.п.)
        constants.set("DrawFlags_None", ImDrawFlags.None.toInt())
        constants.set("DrawFlags_Closed", ImDrawFlags.Closed.toInt())
        constants.set("DrawFlags_RoundCornersTopLeft", ImDrawFlags.RoundCornersTopLeft.toInt())
        constants.set("DrawFlags_RoundCornersTopRight", ImDrawFlags.RoundCornersTopRight.toInt())
        constants.set("DrawFlags_RoundCornersBottomLeft", ImDrawFlags.RoundCornersBottomLeft.toInt())
        constants.set("DrawFlags_RoundCornersBottomRight", ImDrawFlags.RoundCornersBottomRight.toInt())
        constants.set("DrawFlags_RoundCornersNone", ImDrawFlags.RoundCornersNone.toInt())
        constants.set("DrawFlags_RoundCornersTop", ImDrawFlags.RoundCornersTop.toInt())
        constants.set("DrawFlags_RoundCornersBottom", ImDrawFlags.RoundCornersBottom.toInt())
        constants.set("DrawFlags_RoundCornersLeft", ImDrawFlags.RoundCornersLeft.toInt())
        constants.set("DrawFlags_RoundCornersRight", ImDrawFlags.RoundCornersRight.toInt())
        constants.set("DrawFlags_RoundCornersAll", ImDrawFlags.RoundCornersAll.toInt())

        // Флаги TabBar / TabItem
        constants.set("TabBarFlags_None", ImGuiTabBarFlags.None.toInt())
        constants.set("TabBarFlags_Reorderable", ImGuiTabBarFlags.Reorderable.toInt())
        constants.set("TabBarFlags_AutoSelectNewTabs", ImGuiTabBarFlags.AutoSelectNewTabs.toInt())
        constants.set("TabBarFlags_TabListPopupButton", ImGuiTabBarFlags.TabListPopupButton.toInt())
        constants.set("TabBarFlags_NoCloseWithMiddleMouseButton", ImGuiTabBarFlags.NoCloseWithMiddleMouseButton.toInt())
        constants.set("TabBarFlags_NoTabListScrollingButtons", ImGuiTabBarFlags.NoTabListScrollingButtons.toInt())
        constants.set("TabBarFlags_NoTooltip", ImGuiTabBarFlags.NoTooltip.toInt())
        constants.set("TabBarFlags_FittingPolicyResizeDown", ImGuiTabBarFlags.FittingPolicyResizeDown.toInt())
        constants.set("TabBarFlags_FittingPolicyScroll", ImGuiTabBarFlags.FittingPolicyScroll.toInt())
        constants.set("TabItemFlags_None", ImGuiTabItemFlags.None.toInt())
        constants.set("TabItemFlags_UnsavedDocument", ImGuiTabItemFlags.UnsavedDocument.toInt())
        constants.set("TabItemFlags_SetSelected", ImGuiTabItemFlags.SetSelected.toInt())
        constants.set("TabItemFlags_NoCloseWithMiddleMouseButton", ImGuiTabItemFlags.NoCloseWithMiddleMouseButton.toInt())
        constants.set("TabItemFlags_NoPushId", ImGuiTabItemFlags.NoPushId.toInt())
        constants.set("TabItemFlags_NoTooltip", ImGuiTabItemFlags.NoTooltip.toInt())

        dl.set("renderLine", RenderDLLineFunction())
        dl.set("renderPolygon", RenderDLPolygonFunction())
        dl.set("renderImage", RenderDLImageFunction())
        dl.set("renderImageQuad", RenderDLImageQuadFunction())
        dl.set("renderText", RenderDLTextFunction())
        dl.set("renderFilledRect", RenderDLFilledRectFunction())
        dl.set("renderRect", RenderDLRectFunction())
        dl.set("renderFilledRectMultiColor", RenderDLFilledRectMultiColorFunction())
        dl.set("renderQuad", RenderDLQuadFunction())
        dl.set("renderFilledQuad", RenderDLFilledQuadFunction())
        dl.set("renderTriangle", RenderDLTriangleFunction())
        dl.set("renderFilledTriangle", RenderDLFilledTriangleFunction())
        dl.set("renderCircle", RenderDLCircleFunction())
        dl.set("renderFilledCircle", RenderDLFilledCircleFunction())
        dl.set("renderNgon", RenderDLNgonFunction())
        dl.set("renderFilledNgon", RenderDLFilledNgonFunction())
        dl.set("renderEllipse", RenderDLEllipseFunction())
        dl.set("renderFilledEllipse", RenderDLFilledEllipseFunction())
        dl.set("renderBezierCubic", RenderDLBezierCubicFunction())
        dl.set("renderBezierQuadratic", RenderDLBezierQuadraticFunction())
        dl.set("renderPolyline", RenderDLPolylineFunction())
        dl.set("renderFilledConvexPolygon", RenderDLFilledConvexPolygonFunction())
        dl.set("pushClipRect", DLPushClipRectFunction())
        dl.set("pushClipRectFullScreen", DLPushClipRectFullScreenFunction())
        dl.set("popClipRect", DLPopClipRectFunction())
        dl.set("pushTextureID", DLPushTextureIDFunction())
        dl.set("popTextureID", DLPopTextureIDFunction())
    }

    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            // Window management
            "begin" -> begin()
            "endBegin" -> endFunc()
            "newFrame" -> newFrame()
            "render" -> render()

            // Text
            "text" -> text()
            "textColored" -> textColored()
            "textDisabled" -> textDisabled()
            "bulletText" -> bulletText()
            "calcTextSize" -> calcTextSize()

            // Images
            "createImageObject" -> createImageObject()
            "image" -> image()

            // Buttons
            "button" -> button()
            "smallButton" -> smallButton()
            "arrowButton" -> arrowButton()
            "checkbox" -> checkbox()

            // Input
            "inputText" -> inputText()
            "inputTextMultiline" -> inputTextMultiline()
            "inputInt" -> inputInt()
            "inputFloat" -> inputFloat()
            "inputDouble" -> inputDouble()

            // Layout
            "sameLine" -> sameLine()
            "newLine" -> newLine()
            "spacing" -> spacing()
            "separator" -> separator()

            // Groups
            "beginGroup" -> beginGroup()
            "endGroup" -> endGroup()

            // Indentation
            "indent" -> indent()
            "unindent" -> unindent()

            // Indentation
            "setCursorPos" -> setCursorPos()
            "getCursorPos" -> getCursorPos()
            "getCursorScreenPos" -> getCursorScreenPos()

            // Indentation
            "treeNode" -> treeNode()
            "treeNodeEx" -> treeNodeEx()
            "treePop" -> treePop()
            "collapsingHeader" -> collapsingHeader()

            // Selectables
            "selectable" -> selectable()

            // Lists
            "listBox" -> listBox()

            // Tooltips
            "setTooltip" -> setTooltip()
            "beginTooltip" -> beginTooltip()
            "endTooltip" -> endTooltip()

            // Popups
            "beginPopup" -> beginPopup()
            "beginPopupModal" -> beginPopupModal()
            "openPopup" -> openPopup()
            "endPopup" -> endPopup()
            "closeCurrentPopup" -> closeCurrentPopup()

            // Menus
            "beginMenuBar" -> beginMenuBar()
            "endMenuBar" -> endMenuBar()
            "beginMainMenuBar" -> beginMainMenuBar()
            "endMainMenuBar" -> endMainMenuBar()
            "beginMenu" -> beginMenu()
            "endMenu" -> endMenu()
            "menuItem" -> menuItem()

            // Tabs
            "beginTabBar" -> beginTabBar()
            "endTabBar" -> endTabBar()
            "beginTabItem" -> beginTabItem()
            "endTabItem" -> endTabItem()

            // Child windows
            "beginChild" -> beginChild()
            "endChild" -> endChild()

            // Style
            "pushStyleColor" -> pushStyleColor()
            "popStyleColor" -> popStyleColor()
            "pushStyleVar" -> pushStyleVar()
            "popStyleVar" -> popStyleVar()

            // Font
            "createFontObject" -> LoadFont()
            "pushFont" -> pushFont()
            "popFont" -> popFont()

            // ID stack
            "pushID" -> pushID()
            "popID" -> popID()

            // Utilities
            "setNextItemWidth" -> setNextItemWidth()
            "isItemHovered" -> isItemHovered()
            "isItemClicked" -> isItemClicked()
            "isItemActive" -> isItemActive()
            "isWindowAppearing" -> isWindowAppearing()
            "isWindowCollapsed" -> isWindowCollapsed()
            "isWindowFocused" -> isWindowFocused()
            "isWindowHovered" -> isWindowHovered()

            // Window manipulation
            "setNextWindowSize" -> setNextWindowSize()
            "setNextWindowPos" -> setNextWindowPos()
            "setNextWindowCollapsed" -> setNextWindowCollapsed()
            "setNextWindowFocus" -> setNextWindowFocus()

            // State queries
            "getWindowSize" -> getWindowSize()
            "getWindowPos" -> getWindowPos()
            "getWindowWidth" -> getWindowWidth()
            "getWindowHeight" -> getWindowHeight()

            // Table
            "beginTable" -> beginTable()
            "tableSetupColumn" -> tableSetupColumn()
            "tableHeadersRow" -> tableHeadersRow()
            "tableNextRow" -> tableNextRow()
            "tableSetColumnIndex" -> tableSetColumnIndex()
            "endTable" -> endTable()

            // Sliders
            "sliderFloat" -> sliderFloat()
            "sliderInt" -> sliderInt()
            "sliderAngle" -> sliderAngle()
            "vSliderFloat" -> vSliderFloat()
            "vSliderInt" -> vSliderInt()

            "dragFloat" -> dragFloat()
            "dragInt" -> dragInt()

            "colorEdit3" -> colorEdit3()
            "colorEdit4" -> colorEdit4()
            "colorPicker3" -> colorPicker3()
            "colorPicker4" -> colorPicker4()
            "colorButton" -> colorButton()

            "combo" -> combo()
            "beginCombo" -> beginCombo()
            "endCombo" -> endCombo()

            "radioButton" -> radioButton()
            "progressBar" -> progressBar()

            "textWrapped" -> textWrapped()
            "labelText" -> labelText()
            "separatorText" -> separatorText()

            "plotLines" -> plotLines()
            "plotHistogram" -> plotHistogram()

            "setScrollHereY" -> setScrollHereY()
            "setScrollHereX" -> setScrollHereX()
            "getScrollY" -> getScrollY()
            "getScrollX" -> getScrollX()
            "getScrollMaxY" -> getScrollMaxY()
            "getScrollMaxX" -> getScrollMaxX()
            "setScrollY" -> setScrollY()
            "setScrollX" -> setScrollX()

            "isKeyDown" -> isKeyDown()
            "isKeyPressed" -> isKeyPressed()
            "isKeyReleased" -> isKeyReleased()
            "isMouseDown" -> isMouseDown()
            "isMouseClicked" -> isMouseClicked()
            "isMouseReleased" -> isMouseReleased()
            "isMouseDoubleClicked" -> isMouseDoubleClicked()
            "isMouseDragging" -> isMouseDragging()
            "getMousePos" -> getMousePos()
            "getMouseDragDelta" -> getMouseDragDelta()
            "isAnyMouseDown" -> isAnyMouseDown()

            "isItemVisible" -> isItemVisible()
            "isItemEdited" -> isItemEdited()
            "isItemDeactivated" -> isItemDeactivated()
            "isItemDeactivatedAfterEdit" -> isItemDeactivatedAfterEdit()
            "isItemFocused" -> isItemFocused()
            "getItemRectMin" -> getItemRectMin()
            "getItemRectMax" -> getItemRectMax()
            "getItemRectSize" -> getItemRectSize()

            "dummy" -> dummy()
            "alignTextToFramePadding" -> alignTextToFramePadding()
            "getContentRegionAvail" -> getContentRegionAvail()
            "getDisplaySize" -> getDisplaySize()
            "getFrameCount" -> getFrameCount()
            "getTime" -> getTime()
            "getFontSize" -> getFontSize()
            "getTextLineHeight" -> getTextLineHeight()
            "getTextLineHeightWithSpacing" -> getTextLineHeightWithSpacing()
            "getFrameHeight" -> getFrameHeight()
            "getFrameHeightWithSpacing" -> getFrameHeightWithSpacing()

            "setNextWindowBgAlpha" -> setNextWindowBgAlpha()
            "setNextWindowContentSize" -> setNextWindowContentSize()
            "setWindowFocus" -> setWindowFocus()
            "setWindowSize" -> setWindowSize()
            "setWindowPos" -> setWindowPos()
            "setWindowCollapsed" -> setWindowCollapsed()
            "isPopupOpen" -> isPopupOpen()
            "openPopupOnItemClick" -> openPopupOnItemClick()

            
            // ну тут прям ниже только через ffi imgui вызивать
            "pathClear" -> pathClear()
            "pathLineTo" -> pathLineTo()
            "pathStroke" -> pathStroke()

            // Constants
            "constants" -> {
                constants
            }

            "dl", "DL" -> {
                dl
            }
            else -> super.get(key)
        }
    }

    private val imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private val imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()

    var windowHandle: Long = -1

    fun onGlfwInit() {
        if (windowHandle == -1L) {
            ImGui.createContext()
            val io = ImGui.getIO()
            io.setIniFilename(null)
            io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard)
            io.addConfigFlags(ImGuiConfigFlags.DockingEnable)
            io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors)
            script.onImGuiInitEvent()
            imGuiGlfw.init(Main.mc.getWindow().handle(), true)
            imGuiGl3.init()
            windowHandle = Main.mc.getWindow().handle()
        }
    }

    fun onFrameRender() {
        if (windowHandle != -1L) {
            imGuiGlfw.newFrame()
            imGuiGl3.newFrame()
            ImGui.newFrame()

            script.onImGuiRenderEvent()
                //queue.executeAndClear()
            ImGui.render()
            imGuiGl3.renderDrawData(ImGui.getDrawData())
        }
    }

    fun cleanup() {
        if (windowHandle != -1L) {
            ImGui.getIO().fonts.clear()
        }
    }


    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        return this
    }

    private inner class RenderDLTextFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.optdouble(1, 0.0).toFloat()
            val y = args.optdouble(2, 0.0).toFloat()
            val text = args.optjstring(3, "")
            val red = args.optint(4, 255)
            val green = args.optint(5, 255)
            val blue = args.optint(6, 255)
            val alpha = args.optint(7, 255)

            // Вызов напрямую
            queue.renderText(x, y, text, red, green, blue, alpha)
            return TRUE
        }
    }

    private inner class RenderDLImageFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val textureID = args.optlong(1, 0)
            val x = args.optdouble(2, 0.0).toFloat()
            val y = args.optdouble(3, 0.0).toFloat()
            val width = args.optdouble(4, 0.0).toFloat()
            val height = args.optdouble(5, 0.0).toFloat()
            val uvMinX = args.optdouble(6, 0.0).toFloat()
            val uvMinY = args.optdouble(7, 0.0).toFloat()
            val uvMaxX = args.optdouble(8, 1.0).toFloat()
            val uvMaxY = args.optdouble(9, 1.0).toFloat()

            queue.renderImage(textureID, x, y, width, height, uvMinX, uvMinY, uvMaxX, uvMaxY)
            return TRUE
        }
    }

    private inner class RenderDLImageQuadFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val textureID = args.optlong(1, 0)
            
            // 4 corner points (table with {x, y})
            val p1 = args.arg(2)
            val p2 = args.arg(3)
            val p3 = args.arg(4)
            val p4 = args.arg(5)
            
            val p1x = p1.get("x").optdouble(0.0).toFloat()
            val p1y = p1.get("y").optdouble(0.0).toFloat()
            val p2x = p2.get("x").optdouble(0.0).toFloat()
            val p2y = p2.get("y").optdouble(0.0).toFloat()
            val p3x = p3.get("x").optdouble(0.0).toFloat()
            val p3y = p3.get("y").optdouble(0.0).toFloat()
            val p4x = p4.get("x").optdouble(0.0).toFloat()
            val p4y = p4.get("y").optdouble(0.0).toFloat()
            
            // UV coordinates (optional, default to full texture)
            val uvMinX = args.optdouble(6, 0.0).toFloat()
            val uvMinY = args.optdouble(7, 0.0).toFloat()
            val uvMaxX = args.optdouble(8, 1.0).toFloat()
            val uvMaxY = args.optdouble(9, 1.0).toFloat()
            
            // Color (optional, default to white)
            val red = args.optint(10, 255)
            val green = args.optint(11, 255)
            val blue = args.optint(12, 255)
            val alpha = args.optint(13, 255)
            val color = queue.makeImGuiColor(red, green, blue, alpha)

            queue.renderImageQuad(textureID, p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y, uvMinX, uvMinY, uvMaxX, uvMaxY, color)
            return TRUE
        }
    }

    private inner class RenderDLLineFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.optdouble(1, 0.0).toFloat()
            val y1 = args.optdouble(2, 0.0).toFloat()
            val x2 = args.optdouble(3, 0.0).toFloat()
            val y2 = args.optdouble(4, 0.0).toFloat()
            val red = args.optint(5, 255)
            val green = args.optint(6, 255)
            val blue = args.optint(7, 255)
            val alpha = args.optint(8, 255)
            val thickness = args.optdouble(9, 1.0).toFloat()

            queue.renderLine(x1, y1, x2, y2, red, green, blue, alpha, thickness)
            return TRUE
        }
    }

    private inner class RenderDLRectFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.optdouble(1, 0.0).toFloat()
            val y1 = args.optdouble(2, 0.0).toFloat()
            val x2 = args.optdouble(3, 0.0).toFloat()
            val y2 = args.optdouble(4, 0.0).toFloat()
            val red = args.optint(5, 255)
            val green = args.optint(6, 255)
            val blue = args.optint(7, 255)
            val alpha = args.optint(8, 255)
            val roudning = args.optdouble(9, 0.0).toFloat()

            queue.renderRect(x1, y1, x2, y2, red, green, blue, alpha, roudning)
            return TRUE
        }
    }

    private inner class RenderDLFilledRectFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.optdouble(1, 0.0).toFloat()
            val y1 = args.optdouble(2, 0.0).toFloat()
            val x2 = args.optdouble(3, 0.0).toFloat()
            val y2 = args.optdouble(4, 0.0).toFloat()
            val red = args.optint(5, 255)
            val green = args.optint(6, 255)
            val blue = args.optint(7, 255)
            val alpha = args.optint(8, 255)
            val roudning = args.optdouble(9, 0.0).toFloat()

            queue.renderFilledRect(x1, y1, x2, y2, red, green, blue, alpha, roudning)
            return TRUE
        }
    }

    private inner class RenderDLPolygonFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val pointsTable = args.arg(1)
            if (!pointsTable.istable()) return NIL

            val red = args.optint(2, 255)
            val green = args.optint(3, 255)
            val blue = args.optint(4, 255)
            val alpha = args.optint(5, 255)

            val points = mutableListOf<Pair<Float, Float>>()
            var i = 1
            while (true) {
                val pointTable = pointsTable.get(i)
                if (pointTable.istable()) {
                    val px = pointTable.get("x").optdouble(0.0).toFloat()
                    val py = pointTable.get("y").optdouble(0.0).toFloat()
                    points.add(px to py)
                    i++
                } else break
            }

            if (points.size >= 3) {
                queue.renderPolygon(points, red, green, blue, alpha)
                return TRUE
            }
            return FALSE
        }
    }

    inner class calcTextSize : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val text = args.checkjstring(1)
            // Дополнительные параметры (опционально)
            val hideTextAfterDoubleHash = if (args.narg() > 1) args.checkboolean(2) else false
            val wrapWidth = if (args.narg() > 2) args.checkdouble(3).toFloat() else -1.0f

            val size = ImGui.calcTextSize(text, hideTextAfterDoubleHash, wrapWidth)

            return varargsOf(
                valueOf(size.x.toDouble()),
                valueOf(size.y.toDouble())
            )
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
            return varargsOf(valueOf(changed), valueOf(value[0].toDouble()))
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
            return varargsOf(valueOf(changed), valueOf(value[0]))
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
            return varargsOf(valueOf(changed), valueOf(value[0].toDouble()))
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
            return varargsOf(valueOf(changed), valueOf(value[0]))
        }
    }

    inner class beginTable : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val name = args.checkjstring(1)
            val column = args.checkint(2)
            val flags = args.optint(3, 0)
            val opened = ImGui.beginTable(name, column, flags)
            return valueOf(opened)
        }
    }

    inner class endTable : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            ImGui.endTable()
            return NIL
        }
    }

    inner class tableSetColumnIndex : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val index = args.checkint(1)
            ImGui.tableSetColumnIndex(index)
            return NIL
        }
    }

    inner class tableNextRow : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val flags = args.optint(1, 0)
            val minRowHeight = args.optdouble(2, 0.0).toFloat()
            ImGui.tableNextRow(flags, minRowHeight)
            return NIL
        }
    }

    inner class tableHeadersRow : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            ImGui.tableHeadersRow()
            return NIL
        }
    }

    inner class tableSetupColumn : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val name = args.checkjstring(1)
            val flags = args.optint(2, 0)
            val initWidthOrWeight = args.optdouble(3, 0.0).toFloat()
            ImGui.tableSetupColumn(name, flags, initWidthOrWeight)
            return NIL
        }
    }

    inner class setNextItemWidth : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val width = args.checkdouble(1).toFloat()

            return NIL
        }
    }

    private inner class LoadFont : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs? {
            // === Аргументы ===
            val filename = args.checkjstring(1)
            val mergeMode = args.optboolean(2, false)
            val pixelSnapH = args.optboolean(3, true)
            val fontSize = args.checkdouble(4).toFloat()
            val customRangesTable = args.arg(5) // Опциональная таблица

            val mainConfig = ImFontConfig()
            mainConfig.setMergeMode(mergeMode)
            mainConfig.setPixelSnapH(pixelSnapH)

            val builder = ImFontGlyphRangesBuilder()

            // Базовые диапазоны
            builder.addRanges(ImGui.getIO().fonts.glyphRangesDefault)
            builder.addRanges(ImGui.getIO().fonts.glyphRangesCyrillic)
            builder.addRanges(ImGui.getIO().fonts.glyphRangesJapanese)
            builder.addRanges(ImGui.getIO().fonts.glyphRangesJapanese)
            builder.addRanges(ImGui.getIO().fonts.glyphRangesChineseFull)
            builder.addRanges(ImGui.getIO().fonts.glyphRangesThai)
            builder.addRanges(ImGui.getIO().fonts.glyphRangesVietnamese)

            // Логика выбора диапазонов
            val rangesToUse = if (customRangesTable.istable()) {
                // Если передана таблица, конвертируем её в ShortArray
                val table = customRangesTable.checktable()
                val len = table.length()
                val result = ShortArray(len + 1) // +1 для завершающего нуля
                for (i in 1..len) {
                    result[i - 1] = table.get(i).toint().toShort()
                }
                result[len] = 0 // ImGui требует, чтобы массив заканчивался нулем
                result
            } else {
                // Стандартные расширенные диапазоны (если таблица не передана)
                shortArrayOf(
                    0x2500.toShort(), 0x25FF.toShort(),  // Box + Geometric
                    0x2600.toShort(), 0x26FF.toShort(),  // Misc Symbols
                    0x2700.toShort(), 0x27BF.toShort(),  // Dingbats
                    0x2190.toShort(), 0x21FF.toShort(),  // Arrows
                    0x2000.toShort(), 0x206F.toShort(),  // Punctuation
                    0xE0A0.toShort(), 0xE0A3.toShort(),  // Nerd Font
                    0xE0B0.toShort(), 0xE0B3.toShort(),
                    0xF000.toShort(), 0xF2E0.toShort(),
                    0xE700.toShort(), 0xE7C5.toShort(),
                    0.toShort()
                )
            }

            builder.addRanges(rangesToUse)
            val glyphRanges = builder.buildRanges()

            return ImGuiFont(
                ImGui.getIO().fonts.addFontFromFileTTF(
                    filename,
                    fontSize,
                    mainConfig,
                    glyphRanges
                )
            )
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
                        ImGui.image(image, args.arg(2).tofloat(), args.arg(3).tofloat())
                    }
                    return TRUE
                }
            }
            return FALSE
        }
    }

    inner class pathClear : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            queue.pathClear()
            return TRUE
        }
    }

    inner class pathLineTo : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.checkdouble(1).toFloat()
            val y = args.checkdouble(2).toFloat()
            queue.pathLineTo(x, y)
            return TRUE
        }
    }

    inner class pathStroke : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val color = args.checkint(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val thickness = if (args.narg() > 2) args.checkdouble(3).toFloat() else 1.0f
            queue.pathStroke(color, flags, thickness)
            return TRUE
        }
    }

    inner class newFrame : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.newFrame()
            return TRUE
        }
    }

    inner class render : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.render()
            return TRUE
        }
    }

    inner class begin : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val name = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.begin(name, flags)
            return valueOf(opened)
        }
    }

    inner class endFunc : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.end()
            return TRUE
        }
    }

    inner class text : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.text(args.checkjstring(1))
            return NIL
        }
    }

    inner class textColored : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val r = args.checkdouble(1).toFloat()
            val g = args.checkdouble(2).toFloat()
            val b = args.checkdouble(3).toFloat()
            val a = if (args.narg() > 3) args.checkdouble(4).toFloat() else 1.0f
            ImGui.textColored(r, g, b, a, args.checkjstring(5))
            return NIL
        }
    }

    inner class textDisabled : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.textDisabled(args.checkjstring(1))
            return NIL
        }
    }

    inner class bulletText : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.bulletText(args.checkjstring(1))
            return NIL
        }
    }

    inner class button : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val width = if (args.narg() > 1) args.checkdouble(2).toFloat() else 0f
            val height = if (args.narg() > 2) args.checkdouble(3).toFloat() else 0f
            val clicked = ImGui.button(label, width, height)
            return valueOf(clicked)
        }
    }

    inner class smallButton : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val clicked = ImGui.smallButton(args.checkjstring(1))
            return valueOf(clicked)
        }
    }

    inner class arrowButton : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            val dir = args.checkint(2)
            val clicked = ImGui.arrowButton(strId, dir)
            return valueOf(clicked)
        }
    }

    inner class checkbox : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val value = ImBoolean(args.checkboolean(2))
            val changed = ImGui.checkbox(label, value)
            return varargsOf(valueOf(changed), valueOf(value.get()))
        }
    }

    inner class inputText : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val text = ImString(args.checkjstring(2), 256)
            val flags = if (args.narg() > 2) args.checkint(3) else 0
            val changed = ImGui.inputText(label, text, flags)
            return varargsOf(valueOf(changed), valueOf(text.get()))
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
            return varargsOf(valueOf(changed), valueOf(text.get()))
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
            return varargsOf(valueOf(changed), valueOf(value.get()))
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
            return varargsOf(valueOf(changed), valueOf(value.get().toDouble()))
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
            return varargsOf(valueOf(changed), valueOf(value.get()))
        }
    }

    inner class sameLine : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val offsetFromStartX = if (args.narg() > 0) args.checkdouble(1).toFloat() else 0.0f
            val spacing = if (args.narg() > 1) args.checkdouble(2).toFloat() else -1.0f
            ImGui.sameLine(offsetFromStartX, spacing)
            return NIL
        }
    }

    inner class newLine : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.newLine()
            return NIL
        }
    }

    inner class spacing : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.spacing()
            return NIL
        }
    }

    inner class separator : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.separator()
            return NIL
        }
    }

    inner class beginGroup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.beginGroup()
            return NIL
        }
    }

    inner class endGroup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endGroup()
            return NIL
        }
    }

    inner class indent : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val indentWidth = if (args.narg() > 0) args.checkdouble(1).toFloat() else 0.0f
            ImGui.indent(indentWidth)
            return NIL
        }
    }

    inner class unindent : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val indentWidth = if (args.narg() > 0) args.checkdouble(1).toFloat() else 0.0f
            ImGui.unindent(indentWidth)
            return NIL
        }
    }

    inner class setCursorPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.checkdouble(1).toFloat()
            val y = args.checkdouble(2).toFloat()
            ImGui.setCursorPos(x, y)
            return NIL
        }
    }

    inner class getCursorPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val pos = ImGui.getCursorPos()
            return varargsOf(valueOf(pos.x.toDouble()), valueOf(pos.y.toDouble()))
        }
    }

    inner class getCursorScreenPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val pos = ImGui.getCursorScreenPos()
            return varargsOf(valueOf(pos.x.toDouble()), valueOf(pos.y.toDouble()))
        }
    }

    inner class treeNode : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val opened = ImGui.treeNode(args.checkjstring(1))
            return valueOf(opened)
        }
    }

    inner class treeNodeEx : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.treeNodeEx(label, flags)
            return valueOf(opened)
        }
    }

    inner class treePop : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.treePop()
            return NIL
        }
    }

    inner class collapsingHeader : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.collapsingHeader(label, flags)
            return valueOf(opened)
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
            return varargsOf(valueOf(clicked), valueOf(selected))
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
            return valueOf(currentItemRef.get())
        }
    }

    inner class setTooltip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.setTooltip(args.checkjstring(1))
            return NIL
        }
    }

    inner class beginTooltip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.beginTooltip()
            return NIL
        }
    }

    inner class endTooltip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endTooltip()
            return NIL
        }
    }

    inner class beginPopup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.beginPopup(strId, flags)
            return valueOf(opened)
        }
    }

    inner class beginPopupModal : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val name = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.beginPopupModal(name, flags)
            return valueOf(opened)
        }
    }

    inner class endPopup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endPopup()
            return NIL
        }
    }

    inner class openPopup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            ImGui.openPopup(strId, flags)
            return NIL
        }
    }

    inner class closeCurrentPopup : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.closeCurrentPopup()
            return NIL
        }
    }

    inner class beginMenuBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val opened = ImGui.beginMenuBar()
            return valueOf(opened)
        }
    }

    inner class endMenuBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endMenuBar()
            return NIL
        }
    }

    inner class beginMainMenuBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val opened = ImGui.beginMainMenuBar()
            return valueOf(opened)
        }
    }

    inner class endMainMenuBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endMainMenuBar()
            return NIL
        }
    }

    inner class beginMenu : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val enabled = if (args.narg() > 1) args.checkboolean(2) else true
            val opened = ImGui.beginMenu(label, enabled)
            return valueOf(opened)
        }
    }

    inner class endMenu : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endMenu()
            return NIL
        }
    }

    inner class menuItem : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val shortcut = if (args.narg() > 1) args.optjstring(2, "") else ""
            val selected = if (args.narg() > 2) args.checkboolean(3) else false
            val enabled = if (args.narg() > 3) args.checkboolean(4) else true
            val clicked = ImGui.menuItem(label, shortcut, selected, enabled)
            return varargsOf(valueOf(clicked), valueOf(selected))
        }
    }

    inner class beginTabBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.beginTabBar(strId, flags)
            return valueOf(opened)
        }
    }

    inner class endTabBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endTabBar()
            return NIL
        }
    }

    inner class beginTabItem : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            val opened = ImGui.beginTabItem(label, flags)
            return valueOf(opened)
        }
    }

    inner class endTabItem : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endTabItem()
            return NIL
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
            return valueOf(opened)
        }
    }

    inner class endChild : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.endChild()
            return NIL
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
            return NIL
        }
    }

    inner class popStyleColor : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val count = if (args.narg() > 0) args.checkint(1) else 1
            ImGui.popStyleColor(count)
            return NIL
        }
    }

    inner class pushStyleVar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val idx = args.checkint(1)
            val x = args.checkdouble(2).toFloat()
            val y = if (args.narg() > 2) args.checkdouble(3).toFloat() else 0.0f
            ImGui.pushStyleVar(idx, x, y)
            return NIL
        }
    }

    inner class popStyleVar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val count = if (args.narg() > 0) args.checkint(1) else 1
            ImGui.popStyleVar(count)
            return NIL
        }
    }

    inner class pushFont : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val arg1 = args.arg(1)
            val font = when {
                arg1?.isuserdata() == true && arg1.touserdata() is ImGuiFont -> {
                    (arg1.touserdata() as ImGuiFont).font
                }

                arg1?.isuserdata() == true && arg1.touserdata() is ImFont -> {
                    arg1.touserdata() as ImFont
                }
                else -> null
            }

            if (font != null) {
                ImGui.pushFont(font)
            }

            return NIL
        }
    }

    inner class popFont : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.popFont()
            return NIL
        }
    }

    inner class pushID : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = args.checkjstring(1)
            ImGui.pushID(strId)
            return NIL
        }
    }

    inner class popID : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.popID()
            return NIL
        }
    }

    inner class isItemHovered : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val flags = if (args.narg() > 0) args.checkint(1) else 0
            val hovered = ImGui.isItemHovered(flags)
            return valueOf(hovered)
        }
    }

    inner class isItemClicked : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val mouseButton = if (args.narg() > 0) args.checkint(1) else 0
            val clicked = ImGui.isItemClicked(mouseButton)
            return valueOf(clicked)
        }
    }

    inner class isItemActive : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val active = ImGui.isItemActive()
            return valueOf(active)
        }
    }

    inner class isWindowAppearing : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val appearing = ImGui.isWindowAppearing()
            return valueOf(appearing)
        }
    }

    inner class isWindowCollapsed : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val collapsed = ImGui.isWindowCollapsed()
            return valueOf(collapsed)
        }
    }

    inner class isWindowFocused : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val flags = if (args.narg() > 0) args.checkint(1) else 0
            val focused = ImGui.isWindowFocused(flags)
            return valueOf(focused)
        }
    }

    inner class isWindowHovered : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val flags = if (args.narg() > 0) args.checkint(1) else 0
            val hovered = ImGui.isWindowHovered(flags)
            return valueOf(hovered)
        }
    }

    inner class setNextWindowSize : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val width = args.checkdouble(1).toFloat()
            val height = args.checkdouble(2).toFloat()
            val cond = if (args.narg() > 2) args.checkint(3) else 0
            ImGui.setNextWindowSize(width, height, cond)
            return NIL
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
            return NIL
        }
    }

    inner class setNextWindowCollapsed : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val collapsed = args.checkboolean(1)
            val cond = if (args.narg() > 1) args.checkint(2) else 0
            ImGui.setNextWindowCollapsed(collapsed, cond)
            return NIL
        }
    }

    inner class setNextWindowFocus : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.setNextWindowFocus()
            return NIL
        }
    }

    inner class getWindowSize : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val size = ImGui.getWindowSize()
            return varargsOf(valueOf(size.x.toDouble()), valueOf(size.y.toDouble()))
        }
    }

    inner class getWindowPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val pos = ImGui.getWindowPos()
            return varargsOf(valueOf(pos.x.toDouble()), valueOf(pos.y.toDouble()))
        }
    }

    inner class getWindowWidth : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val width = ImGui.getWindowWidth()
            return valueOf(width.toDouble())
        }
    }

    inner class getWindowHeight : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val height = ImGui.getWindowHeight()
            return valueOf(height.toDouble())
        }
    }

    inner class sliderAngle : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val vRad = floatArrayOf(args.checkdouble(2).toFloat())
            val degreesMin = if (args.narg() > 2) args.checkdouble(3).toFloat() else -360f
            val degreesMax = if (args.narg() > 3) args.checkdouble(4).toFloat() else 360f
            val changed = ImGui.sliderAngle(label, vRad, degreesMin, degreesMax)
            return varargsOf(valueOf(changed), valueOf(vRad[0].toDouble()))
        }
    }

    inner class dragFloat : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val v = floatArrayOf(args.checkdouble(2).toFloat())
            val speed = if (args.narg() > 2) args.checkdouble(3).toFloat() else 1f
            val min = if (args.narg() > 3) args.checkdouble(4).toFloat() else 0f
            val max = if (args.narg() > 4) args.checkdouble(5).toFloat() else 0f
            val format = if (args.narg() > 5) args.checkjstring(6) else "%.3f"
            val changed = ImGui.dragFloat(label, v, speed, min, max, format)
            return varargsOf(valueOf(changed), valueOf(v[0].toDouble()))
        }
    }

    inner class dragInt : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val v = intArrayOf(args.checkint(2))
            val speed = if (args.narg() > 2) args.checkdouble(3).toFloat() else 1f
            val min = if (args.narg() > 3) args.checkint(4) else 0
            val max = if (args.narg() > 4) args.checkint(5) else 0
            val changed = ImGui.dragInt(label, v, speed, min, max)
            return varargsOf(valueOf(changed), valueOf(v[0]))
        }
    }

    /** Редактор RGB-цвета. Возвращает (changed, r, g, b) в диапазоне 0.0–1.0 */
    inner class colorEdit3 : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val col = floatArrayOf(args.checkdouble(2).toFloat(), args.checkdouble(3).toFloat(), args.checkdouble(4).toFloat())
            val flags = if (args.narg() > 4) args.checkint(5) else 0
            val changed = ImGui.colorEdit3(label, col, flags)
            return varargsOf(arrayOf(valueOf(changed), valueOf(col[0].toDouble()), valueOf(col[1].toDouble()), valueOf(col[2].toDouble())))
        }
    }

    /** Редактор RGBA-цвета. Возвращает (changed, r, g, b, a) в диапазоне 0.0–1.0 */
    inner class colorEdit4 : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val col = floatArrayOf(args.checkdouble(2).toFloat(), args.checkdouble(3).toFloat(), args.checkdouble(4).toFloat(), args.checkdouble(5).toFloat())
            val flags = if (args.narg() > 5) args.checkint(6) else 0
            val changed = ImGui.colorEdit4(label, col, flags)
            return varargsOf(arrayOf(valueOf(changed), valueOf(col[0].toDouble()), valueOf(col[1].toDouble()), valueOf(col[2].toDouble()), valueOf(col[3].toDouble())))
        }
    }

    /** Полноразмерный пикер RGB. Возвращает (changed, r, g, b) в диапазоне 0.0–1.0 */
    inner class colorPicker3 : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val col = floatArrayOf(args.checkdouble(2).toFloat(), args.checkdouble(3).toFloat(), args.checkdouble(4).toFloat())
            val flags = if (args.narg() > 4) args.checkint(5) else 0
            val changed = ImGui.colorPicker3(label, col, flags)
            return varargsOf(arrayOf(valueOf(changed), valueOf(col[0].toDouble()), valueOf(col[1].toDouble()), valueOf(col[2].toDouble())))
        }
    }

    /** Полноразмерный пикер RGBA. Возвращает (changed, r, g, b, a) в диапазоне 0.0–1.0 */
    inner class colorPicker4 : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val col = floatArrayOf(args.checkdouble(2).toFloat(), args.checkdouble(3).toFloat(), args.checkdouble(4).toFloat(), args.checkdouble(5).toFloat())
            val flags = if (args.narg() > 5) args.checkint(6) else 0
            val changed = ImGui.colorPicker4(label, col, flags)
            return varargsOf(arrayOf(valueOf(changed), valueOf(col[0].toDouble()), valueOf(col[1].toDouble()), valueOf(col[2].toDouble()), valueOf(col[3].toDouble())))
        }
    }

    /** Кнопка-квадрат с цветом. Открывает пикер при клике. Возвращает true при нажатии */
    inner class colorButton : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val descId = args.checkjstring(1)
            val r = args.checkdouble(2).toFloat(); val g = args.checkdouble(3).toFloat()
            val b = args.checkdouble(4).toFloat()
            val a = if (args.narg() > 4) args.checkdouble(5).toFloat() else 1f
            val flags = if (args.narg() > 5) args.checkint(6) else 0
            return valueOf(ImGui.colorButton(descId, r, g, b, a, flags))
        }
    }

    /**
     * Простой Combo-виджет с массивом строк.
     * Возвращает (changed, currentIndex).
     */
    inner class combo : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val currentItem = ImInt(args.checkint(2))
            val items = args.checktable(3)
            val count = items.length()
            val itemsArray = Array(count) { i -> items.get(i + 1).checkjstring() }
            val heightInItems = if (args.narg() > 3) args.checkint(4) else -1
            val changed = ImGui.combo(label, currentItem, itemsArray, heightInItems)
            return varargsOf(valueOf(changed), valueOf(currentItem.get()))
        }
    }

    /**
     * Начало расширяемого Combo-блока (ручной контент внутри).
     * Возвращает true, если блок раскрыт — нужно вызвать endCombo().
     */
    inner class beginCombo : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val previewValue = args.checkjstring(2)
            val flags = if (args.narg() > 2) args.checkint(3) else 0
            return valueOf(ImGui.beginCombo(label, previewValue, flags))
        }
    }

    inner class endCombo : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { ImGui.endCombo(); return NIL }
    }

    /**
     * Радиокнопка. Возвращает true при нажатии.
     * Использование: radioButton("label", activeValue == thisValue)
     */
    inner class radioButton : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val active = args.checkboolean(2)
            return valueOf(ImGui.radioButton(label, active))
        }
    }

    /**
     * Полоса прогресса.
     * fraction — заполнение от 0.0 до 1.0.
     * sizeX, sizeY — размер (-1 = авто).
     * overlay — необязательная строка поверх полосы.
     */
    inner class progressBar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val fraction = args.checkdouble(1).toFloat()
            val sizeX = if (args.narg() > 1) args.checkdouble(2).toFloat() else -1f
            val sizeY = if (args.narg() > 2) args.checkdouble(3).toFloat() else 0f
            val overlay = if (args.narg() > 3) args.checkjstring(4) else ""
            ImGui.progressBar(fraction, sizeX, sizeY, overlay)
            return NIL
        }
    }

    /** Текст с автоматическим переносом по ширине окна */
    inner class textWrapped : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { ImGui.textWrapped(args.checkjstring(1)); return NIL }
    }

    /** Метка слева + значение справа в одну строку */
    inner class labelText : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.labelText(args.checkjstring(1), args.checkjstring(2)); return NIL
        }
    }

    /** Горизонтальный разделитель с текстовой подписью */
    inner class separatorText : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { ImGui.separatorText(args.checkjstring(1)); return NIL }
    }

    /**
     * Линейный график из Lua-таблицы чисел.
     * overlayText — необязательная подпись поверх графика.
     */
    inner class plotLines : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val table = args.checktable(2)
            val count = table.length()
            val values = FloatArray(count) { i -> table.get(i + 1).tofloat() }
            val overlayText = args.optjstring(3, "")
            val scaleMin = if (args.narg() > 3) args.checkdouble(4).toFloat() else Float.MAX_VALUE
            val scaleMax = if (args.narg() > 4) args.checkdouble(5).toFloat() else Float.MAX_VALUE
            val graphW = if (args.narg() > 5) args.checkdouble(6).toFloat() else 0f
            val graphH = if (args.narg() > 6) args.checkdouble(7).toFloat() else 0f
            ImGui.plotLines(label, values, count, 0, overlayText, scaleMin, scaleMax, graphW, graphH)
            return NIL
        }
    }

    /**
     * Гистограмма из Lua-таблицы чисел.
     */
    inner class plotHistogram : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val label = args.checkjstring(1)
            val table = args.checktable(2)
            val count = table.length()
            val values = FloatArray(count) { i -> table.get(i + 1).tofloat() }
            val overlayText = args.optjstring(3, "")
            val scaleMin = if (args.narg() > 3) args.checkdouble(4).toFloat() else Float.MAX_VALUE
            val scaleMax = if (args.narg() > 4) args.checkdouble(5).toFloat() else Float.MAX_VALUE
            val graphW = if (args.narg() > 5) args.checkdouble(6).toFloat() else 0f
            val graphH = if (args.narg() > 6) args.checkdouble(7).toFloat() else 0f
            ImGui.plotHistogram(label, values, count, 0, overlayText, scaleMin, scaleMax, graphW, graphH)
            return NIL
        }
    }

    /** Прокрутить до конца по Y. centerYRatio: 0 = вверх, 0.5 = середина, 1 = низ */
    inner class setScrollHereY : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.setScrollHereY(if (args.narg() > 0) args.checkdouble(1).toFloat() else 0.5f); return NIL
        }
    }

    inner class setScrollHereX : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.setScrollHereX(if (args.narg() > 0) args.checkdouble(1).toFloat() else 0.5f); return NIL
        }
    }

    inner class getScrollY : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getScrollY().toDouble())
    }
    inner class getScrollX : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getScrollX().toDouble())
    }
    inner class getScrollMaxY : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getScrollMaxY().toDouble())
    }
    inner class getScrollMaxX : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getScrollMaxX().toDouble())
    }
    inner class setScrollY : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { ImGui.setScrollY(args.checkdouble(1).toFloat()); return NIL }
    }
    inner class setScrollX : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { ImGui.setScrollX(args.checkdouble(1).toFloat()); return NIL }
    }


    /** Клавиша удерживается прямо сейчас */
    inner class isKeyDown : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isKeyDown(args.checkint(1)))
    }

    /** Клавиша только что нажата (один кадр). repeat — автоповтор при удержании */
    inner class isKeyPressed : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val key = args.checkint(1)
            val repeat = if (args.narg() > 1) args.checkboolean(2) else true
            return valueOf(ImGui.isKeyPressed(key, repeat))
        }
    }

    inner class isKeyReleased : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isKeyReleased(args.checkint(1)))
    }

    /** Кнопка мыши удерживается. button: 0=левая, 1=правая, 2=средняя */
    inner class isMouseDown : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isMouseDown(if (args.narg() > 0) args.checkint(1) else 0))
    }

    inner class isMouseClicked : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val button = if (args.narg() > 0) args.checkint(1) else 0
            val repeat = if (args.narg() > 1) args.checkboolean(2) else false
            return valueOf(ImGui.isMouseClicked(button, repeat))
        }
    }

    inner class isMouseReleased : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isMouseReleased(if (args.narg() > 0) args.checkint(1) else 0))
    }

    inner class isMouseDoubleClicked : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isMouseDoubleClicked(if (args.narg() > 0) args.checkint(1) else 0))
    }

    /** Мышь перетаскивается (зажата и сдвинута дальше lockThreshold) */
    inner class isMouseDragging : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val button = if (args.narg() > 0) args.checkint(1) else 0
            val lockThreshold = if (args.narg() > 1) args.checkdouble(2).toFloat() else -1f
            return valueOf(ImGui.isMouseDragging(button, lockThreshold))
        }
    }

    inner class isAnyMouseDown : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isAnyMouseDown())
    }

    /** Позиция курсора мыши в экранных координатах */
    inner class getMousePos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val pos = ImGui.getMousePos()
            return varargsOf(valueOf(pos.x.toDouble()), valueOf(pos.y.toDouble()))
        }
    }


    /** Суммарное смещение с момента начала перетаскивания */
    inner class getMouseDragDelta : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val button = if (args.narg() > 0) args.checkint(1) else 0
            val lockThreshold = if (args.narg() > 1) args.checkdouble(2).toFloat() else -1f
            val delta = ImGui.getMouseDragDelta(button, lockThreshold)
            return varargsOf(valueOf(delta.x.toDouble()), valueOf(delta.y.toDouble()))
        }
    }

    inner class isItemVisible : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isItemVisible())
    }
    inner class isItemEdited : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isItemEdited())
    }
    inner class isItemDeactivated : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isItemDeactivated())
    }
    inner class isItemDeactivatedAfterEdit : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isItemDeactivatedAfterEdit())
    }
    inner class isItemFocused : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.isItemFocused())
    }

    /** Верхний-левый угол последнего элемента в экранных координатах */
    inner class getItemRectMin : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val v = ImGui.getItemRectMin(); return varargsOf(valueOf(v.x.toDouble()), valueOf(v.y.toDouble()))
        }
    }

    /** Нижний-правый угол последнего элемента в экранных координатах */
    inner class getItemRectMax : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val v = ImGui.getItemRectMax(); return varargsOf(valueOf(v.x.toDouble()), valueOf(v.y.toDouble()))
        }
    }

    inner class getItemRectSize : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val v = ImGui.getItemRectSize(); return varargsOf(valueOf(v.x.toDouble()), valueOf(v.y.toDouble()))
        }
    }

    /** Пустой элемент заданного размера (занимает место в лэйауте) */
    inner class dummy : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.dummy(args.checkdouble(1).toFloat(), args.checkdouble(2).toFloat()); return NIL
        }
    }

    /** Выровнять курсор так, чтобы следующий текст был на высоте рамки виджета */
    inner class alignTextToFramePadding : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { ImGui.alignTextToFramePadding(); return NIL }
    }

    /** Доступная область содержимого текущего окна (w, h) */
    inner class getContentRegionAvail : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val v = ImGui.getContentRegionAvail(); return varargsOf(valueOf(v.x.toDouble()), valueOf(v.y.toDouble()))
        }
    }

    /** Размер дисплея (w, h) */
    inner class getDisplaySize : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val io = ImGui.getIO()
            return varargsOf(valueOf(io.displaySizeX.toDouble()), valueOf(io.displaySizeY.toDouble()))
        }
    }

    /** Номер текущего кадра с момента старта */
    inner class getFrameCount : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getFrameCount())
    }

    /** Время в секундах с момента старта */
    inner class getTime : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getTime())
    }

    inner class getFontSize : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getFontSize().toDouble())
    }
    inner class getTextLineHeight : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getTextLineHeight().toDouble())
    }
    inner class getTextLineHeightWithSpacing : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getTextLineHeightWithSpacing().toDouble())
    }
    inner class getFrameHeight : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getFrameHeight().toDouble())
    }
    inner class getFrameHeightWithSpacing : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = valueOf(ImGui.getFrameHeightWithSpacing().toDouble())
    }

    /** Установить прозрачность фона следующего окна (0 = полностью прозрачный, 1 = непрозрачный) */
    inner class setNextWindowBgAlpha : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { ImGui.setNextWindowBgAlpha(args.checkdouble(1).toFloat()); return NIL }
    }

    /** Задать размер прокручиваемой области содержимого следующего окна */
    inner class setNextWindowContentSize : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            ImGui.setNextWindowContentSize(args.checkdouble(1).toFloat(), args.checkdouble(2).toFloat()); return NIL
        }
    }

    /** Переключить фокус на окно (без аргументов — текущее, со строкой — по имени) */
    inner class setWindowFocus : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.narg() > 0) ImGui.setWindowFocus(args.checkjstring(1)) else ImGui.setWindowFocus(); return NIL
        }
    }

    /** Задать размер текущего окна (вызывать внутри Begin/End) */
    inner class setWindowSize : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cond = if (args.narg() > 2) args.checkint(3) else 0
            ImGui.setWindowSize(args.checkdouble(1).toFloat(), args.checkdouble(2).toFloat(), cond); return NIL
        }
    }

    inner class setWindowPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cond = if (args.narg() > 2) args.checkint(3) else 0
            ImGui.setWindowPos(args.checkdouble(1).toFloat(), args.checkdouble(2).toFloat(), cond); return NIL
        }
    }

    inner class setWindowCollapsed : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cond = if (args.narg() > 1) args.checkint(2) else 0
            ImGui.setWindowCollapsed(args.checkboolean(1), cond); return NIL
        }
    }

    /** Проверить, открыт ли указанный попап */
    inner class isPopupOpen : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val flags = if (args.narg() > 1) args.checkint(2) else 0
            return valueOf(ImGui.isPopupOpen(args.checkjstring(1), flags))
        }
    }

    /** Открыть попап при клике на последний элемент (по умолчанию ПКМ) */
    inner class openPopupOnItemClick : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val strId = if (args.narg() > 0) args.checkjstring(1) else ""
            val mouseButton = if (args.narg() > 1) args.checkint(2) else 1
            ImGui.openPopupOnItemClick(strId, mouseButton); return NIL
        }
    }

    /** Закрашенный прямоугольник с разным цветом в каждом углу (градиент) */
    private inner class RenderDLFilledRectMultiColorFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.optdouble(1, 0.0).toFloat(); val y1 = args.optdouble(2, 0.0).toFloat()
            val x2 = args.optdouble(3, 0.0).toFloat(); val y2 = args.optdouble(4, 0.0).toFloat()
            val rUL = args.optint(5,  255); val gUL = args.optint(6,  255); val bUL = args.optint(7,  255); val aUL = args.optint(8,  255)
            val rUR = args.optint(9,  255); val gUR = args.optint(10, 255); val bUR = args.optint(11, 255); val aUR = args.optint(12, 255)
            val rBR = args.optint(13, 255); val gBR = args.optint(14, 255); val bBR = args.optint(15, 255); val aBR = args.optint(16, 255)
            val rBL = args.optint(17, 255); val gBL = args.optint(18, 255); val bBL = args.optint(19, 255); val aBL = args.optint(20, 255)
            queue.renderFilledRectMultiColor(x1, y1, x2, y2, rUL, gUL, bUL, aUL, rUR, gUR, bUR, aUR, rBR, gBR, bBR, aBR, rBL, gBL, bBL, aBL)
            return TRUE
        }
    }

    /** Четырёхугольник (контур) по 4 произвольным точкам */
    private inner class RenderDLQuadFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val p1x = args.optdouble(1, 0.0).toFloat(); val p1y = args.optdouble(2, 0.0).toFloat()
            val p2x = args.optdouble(3, 0.0).toFloat(); val p2y = args.optdouble(4, 0.0).toFloat()
            val p3x = args.optdouble(5, 0.0).toFloat(); val p3y = args.optdouble(6, 0.0).toFloat()
            val p4x = args.optdouble(7, 0.0).toFloat(); val p4y = args.optdouble(8, 0.0).toFloat()
            val red = args.optint(9, 255); val green = args.optint(10, 255); val blue = args.optint(11, 255); val alpha = args.optint(12, 255)
            val thickness = args.optdouble(13, 1.0).toFloat()
            queue.renderQuad(p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y, red, green, blue, alpha, thickness)
            return TRUE
        }
    }

    /** Закрашенный четырёхугольник по 4 произвольным точкам */
    private inner class RenderDLFilledQuadFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val p1x = args.optdouble(1, 0.0).toFloat(); val p1y = args.optdouble(2, 0.0).toFloat()
            val p2x = args.optdouble(3, 0.0).toFloat(); val p2y = args.optdouble(4, 0.0).toFloat()
            val p3x = args.optdouble(5, 0.0).toFloat(); val p3y = args.optdouble(6, 0.0).toFloat()
            val p4x = args.optdouble(7, 0.0).toFloat(); val p4y = args.optdouble(8, 0.0).toFloat()
            val red = args.optint(9, 255); val green = args.optint(10, 255); val blue = args.optint(11, 255); val alpha = args.optint(12, 255)
            queue.renderFilledQuad(p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y, red, green, blue, alpha)
            return TRUE
        }
    }

    /** Треугольник (контур) */
    private inner class RenderDLTriangleFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val p1x = args.optdouble(1, 0.0).toFloat(); val p1y = args.optdouble(2, 0.0).toFloat()
            val p2x = args.optdouble(3, 0.0).toFloat(); val p2y = args.optdouble(4, 0.0).toFloat()
            val p3x = args.optdouble(5, 0.0).toFloat(); val p3y = args.optdouble(6, 0.0).toFloat()
            val red = args.optint(7, 255); val green = args.optint(8, 255); val blue = args.optint(9, 255); val alpha = args.optint(10, 255)
            val thickness = args.optdouble(11, 1.0).toFloat()
            queue.renderTriangle(p1x, p1y, p2x, p2y, p3x, p3y, red, green, blue, alpha, thickness)
            return TRUE
        }
    }

    /** Закрашенный треугольник */
    private inner class RenderDLFilledTriangleFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val p1x = args.optdouble(1, 0.0).toFloat(); val p1y = args.optdouble(2, 0.0).toFloat()
            val p2x = args.optdouble(3, 0.0).toFloat(); val p2y = args.optdouble(4, 0.0).toFloat()
            val p3x = args.optdouble(5, 0.0).toFloat(); val p3y = args.optdouble(6, 0.0).toFloat()
            val red = args.optint(7, 255); val green = args.optint(8, 255); val blue = args.optint(9, 255); val alpha = args.optint(10, 255)
            queue.renderFilledTriangle(p1x, p1y, p2x, p2y, p3x, p3y, red, green, blue, alpha)
            return TRUE
        }
    }

    /**
     * Окружность (контур).
     * numSegments = 0 — автоматическая тесселяция (рекомендуется).
     */
    private inner class RenderDLCircleFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cx = args.optdouble(1, 0.0).toFloat(); val cy = args.optdouble(2, 0.0).toFloat()
            val radius = args.optdouble(3, 1.0).toFloat()
            val red = args.optint(4, 255); val green = args.optint(5, 255); val blue = args.optint(6, 255); val alpha = args.optint(7, 255)
            val numSegments = args.optint(8, 0); val thickness = args.optdouble(9, 1.0).toFloat()
            queue.renderCircle(cx, cy, radius, red, green, blue, alpha, numSegments, thickness)
            return TRUE
        }
    }

    /** Закрашенная окружность */
    private inner class RenderDLFilledCircleFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cx = args.optdouble(1, 0.0).toFloat(); val cy = args.optdouble(2, 0.0).toFloat()
            val radius = args.optdouble(3, 1.0).toFloat()
            val red = args.optint(4, 255); val green = args.optint(5, 255); val blue = args.optint(6, 255); val alpha = args.optint(7, 255)
            val numSegments = args.optint(8, 0)
            queue.renderFilledCircle(cx, cy, radius, red, green, blue, alpha, numSegments)
            return TRUE
        }
    }

    /** Правильный n-угольник (контур) с гарантированным числом сторон */
    private inner class RenderDLNgonFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cx = args.optdouble(1, 0.0).toFloat(); val cy = args.optdouble(2, 0.0).toFloat()
            val radius = args.optdouble(3, 1.0).toFloat(); val numSegments = args.checkint(4)
            val red = args.optint(5, 255); val green = args.optint(6, 255); val blue = args.optint(7, 255); val alpha = args.optint(8, 255)
            val thickness = args.optdouble(9, 1.0).toFloat()
            queue.renderNgon(cx, cy, radius, numSegments, red, green, blue, alpha, thickness)
            return TRUE
        }
    }

    /** Закрашенный правильный n-угольник */
    private inner class RenderDLFilledNgonFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cx = args.optdouble(1, 0.0).toFloat(); val cy = args.optdouble(2, 0.0).toFloat()
            val radius = args.optdouble(3, 1.0).toFloat(); val numSegments = args.checkint(4)
            val red = args.optint(5, 255); val green = args.optint(6, 255); val blue = args.optint(7, 255); val alpha = args.optint(8, 255)
            queue.renderFilledNgon(cx, cy, radius, numSegments, red, green, blue, alpha)
            return TRUE
        }
    }

    /** Эллипс (контур). rot — поворот в радианах */
    private inner class RenderDLEllipseFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cx = args.optdouble(1, 0.0).toFloat(); val cy = args.optdouble(2, 0.0).toFloat()
            val rx = args.optdouble(3, 1.0).toFloat(); val ry = args.optdouble(4, 1.0).toFloat()
            val red = args.optint(5, 255); val green = args.optint(6, 255); val blue = args.optint(7, 255); val alpha = args.optint(8, 255)
            val rot = args.optdouble(9, 0.0).toFloat(); val numSegments = args.optint(10, 0); val thickness = args.optdouble(11, 1.0).toFloat()
            queue.renderEllipse(cx, cy, rx, ry, red, green, blue, alpha, rot, numSegments, thickness)
            return TRUE
        }
    }

    /** Закрашенный эллипс */
    private inner class RenderDLFilledEllipseFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cx = args.optdouble(1, 0.0).toFloat(); val cy = args.optdouble(2, 0.0).toFloat()
            val rx = args.optdouble(3, 1.0).toFloat(); val ry = args.optdouble(4, 1.0).toFloat()
            val red = args.optint(5, 255); val green = args.optint(6, 255); val blue = args.optint(7, 255); val alpha = args.optint(8, 255)
            val rot = args.optdouble(9, 0.0).toFloat(); val numSegments = args.optint(10, 0)
            queue.renderFilledEllipse(cx, cy, rx, ry, red, green, blue, alpha, rot, numSegments)
            return TRUE
        }
    }

    /**
     * Кубическая кривая Безье (4 точки управления).
     * numSegments = 0 — автоматически.
     */
    private inner class RenderDLBezierCubicFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val p1x = args.optdouble(1, 0.0).toFloat(); val p1y = args.optdouble(2, 0.0).toFloat()
            val p2x = args.optdouble(3, 0.0).toFloat(); val p2y = args.optdouble(4, 0.0).toFloat()
            val p3x = args.optdouble(5, 0.0).toFloat(); val p3y = args.optdouble(6, 0.0).toFloat()
            val p4x = args.optdouble(7, 0.0).toFloat(); val p4y = args.optdouble(8, 0.0).toFloat()
            val red = args.optint(9, 255); val green = args.optint(10, 255); val blue = args.optint(11, 255); val alpha = args.optint(12, 255)
            val thickness = args.optdouble(13, 1.0).toFloat(); val numSegments = args.optint(14, 0)
            queue.renderBezierCubic(p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y, red, green, blue, alpha, thickness, numSegments)
            return TRUE
        }
    }

    /**
     * Квадратичная кривая Безье (3 точки управления).
     * numSegments = 0 — автоматически.
     */
    private inner class RenderDLBezierQuadraticFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val p1x = args.optdouble(1, 0.0).toFloat(); val p1y = args.optdouble(2, 0.0).toFloat()
            val p2x = args.optdouble(3, 0.0).toFloat(); val p2y = args.optdouble(4, 0.0).toFloat()
            val p3x = args.optdouble(5, 0.0).toFloat(); val p3y = args.optdouble(6, 0.0).toFloat()
            val red = args.optint(7, 255); val green = args.optint(8, 255); val blue = args.optint(9, 255); val alpha = args.optint(10, 255)
            val thickness = args.optdouble(11, 1.0).toFloat(); val numSegments = args.optint(12, 0)
            queue.renderBezierQuadratic(p1x, p1y, p2x, p2y, p3x, p3y, red, green, blue, alpha, thickness, numSegments)
            return TRUE
        }
    }

    /**
     * Ломаная линия через массив точек.
     * flags — DrawFlags (например DrawFlags_Closed для замкнутого контура).
     */
    private inner class RenderDLPolylineFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val pointsTable = args.arg(1)
            if (!pointsTable.istable()) return NIL
            val red = args.optint(2, 255); val green = args.optint(3, 255); val blue = args.optint(4, 255); val alpha = args.optint(5, 255)
            val flags = args.optint(6, 0); val thickness = args.optdouble(7, 1.0).toFloat()
            val points = mutableListOf<Pair<Float, Float>>()
            var i = 1
            while (true) {
                val pt = pointsTable.get(i)
                if (pt.istable()) { points.add(pt.get("x").optdouble(0.0).toFloat() to pt.get("y").optdouble(0.0).toFloat()); i++ }
                else break
            }
            if (points.size >= 2) { queue.renderPolyline(points, red, green, blue, alpha, flags, thickness); return TRUE }
            return FALSE
        }
    }

    /** Закрашенный выпуклый многоугольник (точки по часовой стрелке) */
    private inner class RenderDLFilledConvexPolygonFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val pointsTable = args.arg(1)
            if (!pointsTable.istable()) return NIL
            val red = args.optint(2, 255); val green = args.optint(3, 255); val blue = args.optint(4, 255); val alpha = args.optint(5, 255)
            val points = mutableListOf<Pair<Float, Float>>()
            var i = 1
            while (true) {
                val pt = pointsTable.get(i)
                if (pt.istable()) { points.add(pt.get("x").optdouble(0.0).toFloat() to pt.get("y").optdouble(0.0).toFloat()); i++ }
                else break
            }
            if (points.size >= 3) { queue.renderFilledConvexPolygon(points, red, green, blue, alpha); return TRUE }
            return FALSE
        }
    }

    /** Установить прямоугольник отсечения (scissor). intersect = true — пересечение с текущим */
    private inner class DLPushClipRectFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            queue.pushClipRect(
                args.optdouble(1, 0.0).toFloat(), args.optdouble(2, 0.0).toFloat(),
                args.optdouble(3, 0.0).toFloat(), args.optdouble(4, 0.0).toFloat(),
                args.optboolean(5, false)
            ); return NIL
        }
    }

    /** Установить прямоугольник отсечения на весь экран */
    private inner class DLPushClipRectFullScreenFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { queue.pushClipRectFullScreen(); return NIL }
    }

    private inner class DLPopClipRectFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { queue.popClipRect(); return NIL }
    }

    /** Принудительно использовать указанную текстуру для следующих примитивов DrawList */
    private inner class DLPushTextureIDFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { queue.pushTextureID(args.checklong(1)); return NIL }
    }

    private inner class DLPopTextureIDFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs { queue.popTextureID(); return NIL }
    }
}