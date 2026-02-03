package com.nekiplay.hypixelcry.features.lua.objects.misc.imgui

import imgui.ImGui
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

class ImGuiIO : TwoArgFunction() {
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val library = tableOf()
        library.set("getMousePosition", getMousePosition())
        library.set("getMousePos", getMousePosition())
        library.set("isWantCaptureMouse", valueOf(ImGui.getIO().wantCaptureMouse))
        library.set("setWantCaptureMouse", setWantCaptureMouse())

        return library
    }

    inner class getMousePosition : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg?.isboolean() == true) {
                val pos = ImGui.getIO().mousePos
                val t = tableOf()
                t.set("x", valueOf(pos.x.toDouble()))
                t.set("y", valueOf(pos.x.toDouble()))
                return t
            }
            return NIL
        }
    }

    inner class setWantCaptureMouse : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg?.isboolean() == true) {
                ImGui.getIO().wantCaptureMouse = arg.toboolean()
                return valueOf(ImGui.getIO().wantCaptureMouse)
            }
            return NIL
        }
    }
}