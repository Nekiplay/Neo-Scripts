package com.nekiplay.neoscripts.features.lua.objects.misc.imgui

import imgui.ImFont
import org.luaj.vm2.LuaUserdata
class ImGuiFont(val font: ImFont) : LuaUserdata(font) {

}