package com.nekiplay.hypixelcry.features.lua.objects.misc.imgui

import imgui.ImGui
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class ImGuiDrawListLib : TwoArgFunction() {
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val library = LuaValue.tableOf()

        // Basic shapes
        library.set("addLine", AddLine())
        library.set("addRect", AddRect())
        library.set("addRectFilled", AddRectFilled())
        library.set("addCircle", AddCircle())
        library.set("addCircleFilled", AddCircleFilled())
        library.set("addTriangle", AddTriangle())
        library.set("addTriangleFilled", AddTriangleFilled())
        library.set("addQuad", AddQuad())
        library.set("addQuadFilled", AddQuadFilled())

        // Curves
        library.set("addBezierCubic", AddBezierCubic())
        library.set("addBezierQuadratic", AddBezierQuadratic())

        // Text
        library.set("addText", AddText())

        // Path API
        library.set("pathClear", PathClear())
        library.set("pathLineTo", PathLineTo())
        library.set("pathStroke", PathStroke())
        library.set("pathFillConvex", PathFillConvex())

        // Image rendering
        library.set("addImage", AddImage())
        library.set("addImageQuad", AddImageQuad())
        library.set("addImageRounded", AddImageRounded())

        return library
    }

    // ============ Basic Shapes ============

    inner class AddLine : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.checknumber(1).tofloat()
            val y1 = args.checknumber(2).tofloat()
            val x2 = args.checknumber(3).tofloat()
            val y2 = args.checknumber(4).tofloat()
            val col = args.checkint(5)
            val thickness = if (args.narg() >= 6) args.checknumber(6).tofloat() else 1f
            ImGui.getBackgroundDrawList().addLine(x1, y1, x2, y2, col, thickness)
            return LuaValue.TRUE
        }
    }

    inner class AddRect : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.checknumber(1).tofloat()
            val y1 = args.checknumber(2).tofloat()
            val x2 = args.checknumber(3).tofloat()
            val y2 = args.checknumber(4).tofloat()
            val col = args.checkint(5)
            val rounding = if (args.narg() >= 6) args.checknumber(6).tofloat() else 0f
            val flags = if (args.narg() >= 7) args.checkint(7) else 0
            val thickness = if (args.narg() >= 8) args.checknumber(8).tofloat() else 1f
            ImGui.getBackgroundDrawList().addRect(x1, y1, x2, y2, col, rounding, flags, thickness)
            return LuaValue.TRUE
        }
    }

    inner class AddRectFilled : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.checknumber(1).tofloat()
            val y1 = args.checknumber(2).tofloat()
            val x2 = args.checknumber(3).tofloat()
            val y2 = args.checknumber(4).tofloat()
            val col = args.checkint(5)
            val rounding = if (args.narg() >= 6) args.checknumber(6).tofloat() else 0f
            val flags = if (args.narg() >= 7) args.checkint(7) else 0
            ImGui.getBackgroundDrawList().addRectFilled(x1, y1, x2, y2, col, rounding, flags)
            return LuaValue.TRUE
        }
    }

    inner class AddCircle : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cx = args.checknumber(1).tofloat()
            val cy = args.checknumber(2).tofloat()
            val radius = args.checknumber(3).tofloat()
            val col = args.checkint(4)
            val numSegments = if (args.narg() >= 5) args.checkint(5) else 0
            val thickness = if (args.narg() >= 6) args.checknumber(6).tofloat() else 1f
            ImGui.getBackgroundDrawList().addCircle(cx, cy, radius, col, numSegments, thickness)
            return LuaValue.TRUE
        }
    }

    inner class AddCircleFilled : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val cx = args.checknumber(1).tofloat()
            val cy = args.checknumber(2).tofloat()
            val radius = args.checknumber(3).tofloat()
            val col = args.checkint(4)
            val numSegments = if (args.narg() >= 5) args.checkint(5) else 0
            ImGui.getBackgroundDrawList().addCircleFilled(cx, cy, radius, col, numSegments)
            return LuaValue.TRUE
        }
    }

    inner class AddTriangle : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.checknumber(1).tofloat()
            val y1 = args.checknumber(2).tofloat()
            val x2 = args.checknumber(3).tofloat()
            val y2 = args.checknumber(4).tofloat()
            val x3 = args.checknumber(5).tofloat()
            val y3 = args.checknumber(6).tofloat()
            val col = args.checkint(7)
            val thickness = if (args.narg() >= 8) args.checknumber(8).tofloat() else 1f
            ImGui.getBackgroundDrawList().addTriangle(x1, y1, x2, y2, x3, y3, col, thickness)
            return LuaValue.TRUE
        }
    }

    inner class AddTriangleFilled : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.checknumber(1).tofloat()
            val y1 = args.checknumber(2).tofloat()
            val x2 = args.checknumber(3).tofloat()
            val y2 = args.checknumber(4).tofloat()
            val x3 = args.checknumber(5).tofloat()
            val y3 = args.checknumber(6).tofloat()
            val col = args.checkint(7)
            ImGui.getBackgroundDrawList().addTriangleFilled(x1, y1, x2, y2, x3, y3, col)
            return LuaValue.TRUE
        }
    }

    inner class AddQuad : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.checknumber(1).tofloat()
            val y1 = args.checknumber(2).tofloat()
            val x2 = args.checknumber(3).tofloat()
            val y2 = args.checknumber(4).tofloat()
            val x3 = args.checknumber(5).tofloat()
            val y3 = args.checknumber(6).tofloat()
            val x4 = args.checknumber(7).tofloat()
            val y4 = args.checknumber(8).tofloat()
            val col = args.checkint(9)
            val thickness = if (args.narg() >= 10) args.checknumber(10).tofloat() else 1f
            ImGui.getBackgroundDrawList().addQuad(x1, y1, x2, y2, x3, y3, x4, y4, col, thickness)
            return LuaValue.TRUE
        }
    }

    inner class AddQuadFilled : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.checknumber(1).tofloat()
            val y1 = args.checknumber(2).tofloat()
            val x2 = args.checknumber(3).tofloat()
            val y2 = args.checknumber(4).tofloat()
            val x3 = args.checknumber(5).tofloat()
            val y3 = args.checknumber(6).tofloat()
            val x4 = args.checknumber(7).tofloat()
            val y4 = args.checknumber(8).tofloat()
            val col = args.checkint(9)
            ImGui.getBackgroundDrawList().addQuadFilled(x1, y1, x2, y2, x3, y3, x4, y4, col)
            return LuaValue.TRUE
        }
    }

    // ============ Curves ============

    inner class AddBezierCubic : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.checknumber(1).tofloat()
            val y1 = args.checknumber(2).tofloat()
            val x2 = args.checknumber(3).tofloat()
            val y2 = args.checknumber(4).tofloat()
            val x3 = args.checknumber(5).tofloat()
            val y3 = args.checknumber(6).tofloat()
            val x4 = args.checknumber(7).tofloat()
            val y4 = args.checknumber(8).tofloat()
            val col = args.checkint(9)
            val thickness = if (args.narg() >= 10) args.checknumber(10).tofloat() else 1f
            val numSegments = if (args.narg() >= 11) args.checkint(11) else 0
            ImGui.getBackgroundDrawList().addBezierCubic(x1, y1, x2, y2, x3, y3, x4, y4, col, thickness, numSegments)
            return LuaValue.TRUE
        }
    }

    inner class AddBezierQuadratic : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x1 = args.checknumber(1).tofloat()
            val y1 = args.checknumber(2).tofloat()
            val x2 = args.checknumber(3).tofloat()
            val y2 = args.checknumber(4).tofloat()
            val x3 = args.checknumber(5).tofloat()
            val y3 = args.checknumber(6).tofloat()
            val col = args.checkint(7)
            val thickness = if (args.narg() >= 8) args.checknumber(8).tofloat() else 1f
            val numSegments = if (args.narg() >= 9) args.checkint(9) else 0
            ImGui.getBackgroundDrawList().addBezierQuadratic(x1, y1, x2, y2, x3, y3, col, thickness, numSegments)
            return LuaValue.TRUE
        }
    }

    // ============ Text ============

    inner class AddText : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.narg() == 4) {
                // addText(x, y, col, text)
                val x = args.checknumber(1).tofloat()
                val y = args.checknumber(2).tofloat()
                val col = args.checkint(3)
                val text = args.checkjstring(4)
                ImGui.getBackgroundDrawList().addText(x, y, col, text)
            } else if (args.narg() == 5) {
                // addText(font, size, pos, col, text) - simplified version
                val x = args.checknumber(2).tofloat()
                val y = args.checknumber(3).tofloat()
                val col = args.checkint(4)
                val text = args.checkjstring(5)
                ImGui.getBackgroundDrawList().addText(x, y, col, text)
            }
            return LuaValue.TRUE
        }
    }

    // ============ Path API ============

    inner class PathClear : ZeroArgFunction() {
        override fun call(): LuaValue {
            ImGui.getBackgroundDrawList().pathClear()
            return LuaValue.TRUE
        }
    }

    inner class PathLineTo : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.checknumber(1).tofloat()
            val y = args.checknumber(2).tofloat()
            ImGui.getBackgroundDrawList().pathLineTo(x, y)
            return LuaValue.TRUE
        }
    }

    inner class PathStroke : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val col = args.checkint(1)
            val closed = if (args.narg() >= 2) args.checkboolean(2) else false
            val thickness = if (args.narg() >= 3) args.checknumber(3).tofloat() else 1f
            ImGui.getBackgroundDrawList().pathStroke(col, if (closed) 1 else 0, thickness)
            return LuaValue.TRUE
        }
    }

    inner class PathFillConvex : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val col = args.checkint(1)
            ImGui.getBackgroundDrawList().pathFillConvex(col)
            return LuaValue.TRUE
        }
    }

    // ============ Image Rendering ============

    inner class AddImage : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val textureId = resolveTextureId(args.arg(1))
            if (textureId == null || textureId <= 0) return LuaValue.FALSE

            if (args.narg() == 5) {
                // addImage(texture, x, y, w, h)
                val x = args.checknumber(2).tofloat()
                val y = args.checknumber(3).tofloat()
                val w = args.checknumber(4).tofloat()
                val h = args.checknumber(5).tofloat()
                ImGui.getBackgroundDrawList().addImage(textureId, x, y, x + w, y + h)
            } else if (args.narg() == 9) {
                // addImage(texture, x, y, w, h, uv0x, uv0y, uv1x, uv1y)
                val x = args.checknumber(2).tofloat()
                val y = args.checknumber(3).tofloat()
                val w = args.checknumber(4).tofloat()
                val h = args.checknumber(5).tofloat()
                val uv0x = args.checknumber(6).tofloat()
                val uv0y = args.checknumber(7).tofloat()
                val uv1x = args.checknumber(8).tofloat()
                val uv1y = args.checknumber(9).tofloat()
                ImGui.getBackgroundDrawList().addImage(textureId, x, y, x + w, y + h, uv0x, uv0y, uv1x, uv1y)
            } else if (args.narg() == 10) {
                // addImage(texture, x, y, w, h, uv0x, uv0y, uv1x, uv1y, col)
                val x = args.checknumber(2).tofloat()
                val y = args.checknumber(3).tofloat()
                val w = args.checknumber(4).tofloat()
                val h = args.checknumber(5).tofloat()
                val uv0x = args.checknumber(6).tofloat()
                val uv0y = args.checknumber(7).tofloat()
                val uv1x = args.checknumber(8).tofloat()
                val uv1y = args.checknumber(9).tofloat()
                val col = args.checkint(10)
                ImGui.getBackgroundDrawList().addImage(textureId, x, y, x + w, y + h, uv0x, uv0y, uv1x, uv1y, col)
            } else {
                return LuaValue.FALSE
            }
            return LuaValue.TRUE
        }
    }

    inner class AddImageQuad : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val textureId = resolveTextureId(args.arg(1))
            if (textureId == null || textureId <= 0) return LuaValue.FALSE

            val x1 = args.checknumber(2).tofloat()
            val y1 = args.checknumber(3).tofloat()
            val x2 = args.checknumber(4).tofloat()
            val y2 = args.checknumber(5).tofloat()
            val x3 = args.checknumber(6).tofloat()
            val y3 = args.checknumber(7).tofloat()
            val x4 = args.checknumber(8).tofloat()
            val y4 = args.checknumber(9).tofloat()
            val uv0x = if (args.narg() >= 10) args.checknumber(10).tofloat() else 0f
            val uv0y = if (args.narg() >= 11) args.checknumber(11).tofloat() else 0f
            val uv1x = if (args.narg() >= 12) args.checknumber(12).tofloat() else 1f
            val uv1y = if (args.narg() >= 13) args.checknumber(13).tofloat() else 1f
            ImGui.getBackgroundDrawList().addImageQuad(
                textureId, x1, y1, x2, y2, x3, y3, x4, y4,
                uv0x, uv0y, uv1x, uv1y
            )
            return LuaValue.TRUE
        }
    }

    inner class AddImageRounded : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val textureId = resolveTextureId(args.arg(1))
            if (textureId == null || textureId <= 0) return LuaValue.FALSE

            val x = args.checknumber(2).tofloat()
            val y = args.checknumber(3).tofloat()
            val w = args.checknumber(4).tofloat()
            val h = args.checknumber(5).tofloat()
            val uv0x = args.checknumber(6).tofloat()
            val uv0y = args.checknumber(7).tofloat()
            val uv1x = args.checknumber(8).tofloat()
            val uv1y = args.checknumber(9).tofloat()
            val col = args.checkint(10)
            val rounding = args.checknumber(11).tofloat()
            val flags = if (args.narg() >= 12) args.checkint(12) else 0

            ImGui.getBackgroundDrawList().addImageRounded(
                textureId, x, y, x + w, y + h,
                uv0x, uv0y, uv1x, uv1y, col, rounding, flags
            )
            return LuaValue.TRUE
        }
    }

    // ============ Helper Methods ============

    private fun resolveTextureId(arg: LuaValue): Int? {
        return when {
            arg.isuserdata() && arg.touserdata() is ImGuiTexture ->
                (arg.touserdata() as ImGuiTexture).texture.get()
            arg.isuserdata() && arg.touserdata() is java.util.concurrent.atomic.AtomicInteger ->
                (arg.touserdata() as java.util.concurrent.atomic.AtomicInteger).get()
            arg.isnumber() -> arg.toint()
            else -> null
        }
    }
}