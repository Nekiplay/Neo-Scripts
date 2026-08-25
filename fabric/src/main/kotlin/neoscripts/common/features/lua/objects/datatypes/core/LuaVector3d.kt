package com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core

import com.nekiplay.neoscripts.client.sugar.isVector
import com.nekiplay.neoscripts.client.sugar.toVector
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.JavaInstance

class LuaVector3d(val location: Vec3): LuaUserdata(location) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "javaClass", "class" -> JavaInstance(location)
            "x" -> valueOf(location.x)
            "y" -> valueOf(location.y)
            "z" -> valueOf(location.z)

            "add" -> Add()
            "sub" -> Sub()
            "mul" -> Mul()
            "div" -> Div()
            "neg" -> Neg()
            "length" -> Length()
            "lengthSquared" -> LengthSquared()
            "normalize" -> Normalize()
            "dot" -> Dot()
            "cross" -> Cross()
            "distanceTo", "distTo", "dist" -> DistanceTo()
            "distanceToSqr", "distToSqr", "distSqr" -> DistanceToSqr()
            "angleTo" -> AngleTo()
            "lerp" -> Lerp()
            "toRadians" -> ToRadians()
            "toDegrees" -> ToDegrees()
            "horizontalDistanceTo" -> HorizontalDistanceTo()
            "yRot" -> YRot()
            "xRot" -> XRot()

            else -> super.get(key)
        }
    }



    override fun eq(other: LuaValue?): LuaValue {
        val user = other?.touserdata()
        if (other is LuaVector3d) {
            if (other.location == location) {
                return TRUE
            }
        }
        if (user is Vec3) {
            if (user == location) {
                return TRUE
            }
            return TRUE
        }
        return FALSE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LuaVector3d) return false
        return location == other.location
    }

    override fun typename(): String = "vector3d"

    private inner class Add : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val other = parseVec3(args) ?: return NIL
            return LuaVector3d(location.add(other))
        }
    }

    private inner class Sub : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val other = parseVec3(args) ?: return NIL
            return LuaVector3d(location.subtract(other))
        }
    }

    private inner class Mul : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val scalar = args?.arg(1)?.todouble() ?: return NIL
            return LuaVector3d(Vec3(location.x * scalar, location.y * scalar, location.z * scalar))
        }
    }

    private inner class Div : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val scalar = args?.arg(1)?.todouble() ?: return NIL
            if (scalar == 0.0) return NIL
            return LuaVector3d(Vec3(location.x / scalar, location.y / scalar, location.z / scalar))
        }
    }

    private inner class Neg : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            return LuaVector3d(Vec3(-location.x, -location.y, -location.z))
        }
    }

    private inner class Length : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            return valueOf(location.length())
        }
    }

    private inner class LengthSquared : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            return valueOf(location.lengthSqr())
        }
    }

    private inner class Normalize : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val len = location.length()
            if (len == 0.0) return NIL
            return LuaVector3d(location.normalize())
        }
    }

    private inner class Dot : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val other = parseVec3(args) ?: return NIL
            return valueOf(location.dot(other))
        }
    }

    private inner class Cross : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val other = parseVec3(args) ?: return NIL
            return LuaVector3d(location.cross(other))
        }
    }

    private inner class DistanceTo : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val other = parseVec3(args) ?: return NIL
            return valueOf(location.distanceTo(other))
        }
    }

    private inner class DistanceToSqr : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val other = parseVec3(args) ?: return NIL
            return valueOf(location.distanceToSqr(other))
        }
    }

    private inner class AngleTo : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val other = parseVec3(args) ?: return NIL
            val dot = location.dot(other)
            val divisor = location.length() * other.length()
            if (divisor == 0.0) return valueOf(0.0)
            val clamped = dot.coerceIn(-1.0, 1.0)
            return valueOf(Math.acos(clamped))
        }
    }

    private inner class Lerp : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val other = parseVec3(args) ?: return NIL
            val t = args?.arg(4)?.todouble() ?: 0.0
            return LuaVector3d(location.lerp(other, t))
        }
    }

    private inner class ToRadians : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            return LuaVector3d(Vec3(
                Math.toRadians(location.x),
                Math.toRadians(location.y),
                Math.toRadians(location.z)
            ))
        }
    }

    private inner class ToDegrees : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            return LuaVector3d(Vec3(
                Math.toDegrees(location.x),
                Math.toDegrees(location.y),
                Math.toDegrees(location.z)
            ))
        }
    }

    private inner class HorizontalDistanceTo : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val other = parseVec3(args) ?: return NIL
            val d = location.x - other.x
            val e = location.z - other.z
            return valueOf(Math.sqrt(d * d + e * e))
        }
    }

    private inner class YRot : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val degrees = args?.arg(1)?.todouble() ?: return NIL
            val radians = Math.toRadians(degrees)
            val f = Math.sin(radians)
            val g = Math.cos(radians)
            return LuaVector3d(Vec3(
                location.x * g - location.z * f,
                location.y,
                location.x * f + location.z * g
            ))
        }
    }

    private inner class XRot : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val degrees = args?.arg(1)?.todouble() ?: return NIL
            val radians = Math.toRadians(degrees)
            val f = Math.sin(radians)
            val g = Math.cos(radians)
            return LuaVector3d(Vec3(
                location.x,
                location.y * g - location.z * f,
                location.y * f + location.z * g
            ))
        }
    }

    private fun parseVec3(args: Varargs?): Vec3? {
        if (args == null) return null
        return when {
            args.arg(1).isVector() -> args.arg(1).toVector()
            args.arg(1).isnumber() && args.arg(2).isnumber() && args.arg(3).isnumber() -> {
                Vec3(args.arg(1).todouble(), args.arg(2).todouble(), args.arg(3).todouble())
            }
            else -> null
        }
    }
}
