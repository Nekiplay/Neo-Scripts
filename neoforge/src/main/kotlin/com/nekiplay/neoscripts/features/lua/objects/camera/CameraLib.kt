package com.nekiplay.neoscripts.features.lua.objects.camera

import com.nekiplay.neoscripts.utils.render.CameraState
import org.joml.Matrix4f
import org.joml.Vector4f
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class CameraLib : LuaValue() {
    override fun typename() = "camera"

    override fun tojstring() = "CameraObject"

    override fun isnil() = false

    override fun type() = TUSERDATA

    override fun call() = this

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getViewMatrix" -> GetViewMatrixFunction()
            "getProjectionMatrix" -> GetProjectionMatrixFunction()
            "world2screen" -> World2ScreenFunction()
            else -> NIL
        }
    }

    private inner class GetViewMatrixFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val matrix = CameraState.getViewMatrix() ?: return NIL
            return matrixToTable(matrix)
        }
    }

    private inner class GetProjectionMatrixFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val matrix = CameraState.getProjectionMatrix() ?: return NIL
            return matrixToTable(matrix)
        }
    }

    private inner class World2ScreenFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!CameraState.isValid()) return NIL

            val x = args.arg(1).todouble()
            val y = args.arg(2).todouble()
            val z = args.arg(3).todouble()

            val view = CameraState.getViewMatrix() ?: return NIL
            val proj = CameraState.getProjectionMatrix() ?: return NIL

            val mvp = proj.mul(view, Matrix4f())

            val inPos = Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), 1.0f)
            val outPos = mvp.transform(inPos)

            if (outPos.w <= 0.0f) {
                return varargsOf(NIL, NIL, FALSE)
            }

            val nx = outPos.x / outPos.w
            val ny = outPos.y / outPos.w

            val vpW = CameraState.getViewportWidth()
            val vpH = CameraState.getViewportHeight()

            val screenX = (nx + 1.0f) * 0.5f * vpW
            val screenY = (1.0f - ny) * 0.5f * vpH

            return varargsOf(valueOf(screenX.toDouble()), valueOf(screenY.toDouble()), TRUE)
        }
    }

    companion object {
        fun matrixToTable(matrix: Matrix4f): LuaValue {
            val table = LuaValue.tableOf()
            // JOML is column-major: m[row][col] = elements[col * 4 + row]
            // Return as Lua table in row-major order for Lua convenience
            // Index 1-16 in row-major (row 0: 1-4, row 1: 5-8, etc.)
            for (row in 0..3) {
                for (col in 0..3) {
                    val value = matrix.get(row, col)
                    val index = row * 4 + col + 1
                    table.set(index, LuaValue.valueOf(value.toDouble()))
                }
            }
            // Named access: m00, m01, ..., m33
            table.set("m00", LuaValue.valueOf(matrix.get(0, 0).toDouble()))
            table.set("m01", LuaValue.valueOf(matrix.get(0, 1).toDouble()))
            table.set("m02", LuaValue.valueOf(matrix.get(0, 2).toDouble()))
            table.set("m03", LuaValue.valueOf(matrix.get(0, 3).toDouble()))
            table.set("m10", LuaValue.valueOf(matrix.get(1, 0).toDouble()))
            table.set("m11", LuaValue.valueOf(matrix.get(1, 1).toDouble()))
            table.set("m12", LuaValue.valueOf(matrix.get(1, 2).toDouble()))
            table.set("m13", LuaValue.valueOf(matrix.get(1, 3).toDouble()))
            table.set("m20", LuaValue.valueOf(matrix.get(2, 0).toDouble()))
            table.set("m21", LuaValue.valueOf(matrix.get(2, 1).toDouble()))
            table.set("m22", LuaValue.valueOf(matrix.get(2, 2).toDouble()))
            table.set("m23", LuaValue.valueOf(matrix.get(2, 3).toDouble()))
            table.set("m30", LuaValue.valueOf(matrix.get(3, 0).toDouble()))
            table.set("m31", LuaValue.valueOf(matrix.get(3, 1).toDouble()))
            table.set("m32", LuaValue.valueOf(matrix.get(3, 2).toDouble()))
            table.set("m33", LuaValue.valueOf(matrix.get(3, 3).toDouble()))
            return table
        }
    }
}
