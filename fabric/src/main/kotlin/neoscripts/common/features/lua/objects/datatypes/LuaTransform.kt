package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import com.mojang.math.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

/**
 * Трансформация для display-сущностей (text_display, item_display, block_display):
 * смещение, масштаб и поворот в градусах. Создается через creator.createTransform(...).
 */
class LuaTransform(
    var translation: Vector3f = Vector3f(0f, 0f, 0f),
    var scale: Vector3f = Vector3f(1f, 1f, 1f),
    var rotationDegrees: Vector3f = Vector3f(0f, 0f, 0f)
) : LuaUserdata(toTransformation(translation, scale, rotationDegrees)) {

    companion object {
        fun toTransformation(translation: Vector3f, scale: Vector3f, rotationDegrees: Vector3f): Transformation {
            val radX = Math.toRadians(rotationDegrees.x.toDouble()).toFloat()
            val radY = Math.toRadians(rotationDegrees.y.toDouble()).toFloat()
            val radZ = Math.toRadians(rotationDegrees.z.toDouble()).toFloat()
            val leftRot = Quaternionf().rotationZYX(radZ, radY, radX)
            return Transformation(translation, leftRot, scale, Quaternionf())
        }

        fun fromTransformation(transformation: Transformation): LuaTransform {
            val translation = Vector3f(transformation.translation())
            val scale = Vector3f(transformation.scale())
            val eulerRad = Vector3f()
            transformation.leftRotation().getEulerAnglesXYZ(eulerRad)
            val rotationDegrees = Vector3f(
                Math.toDegrees(eulerRad.x.toDouble()).toFloat(),
                Math.toDegrees(eulerRad.y.toDouble()).toFloat(),
                Math.toDegrees(eulerRad.z.toDouble()).toFloat()
            )
            return LuaTransform(translation, scale, rotationDegrees)
        }
    }

    fun toTransformation(): Transformation = toTransformation(translation, scale, rotationDegrees)

    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "translation", "offset" -> vectorToTable(translation)
            "scale" -> vectorToTable(scale)
            "rotation", "rotation_degrees" -> vectorToTable(rotationDegrees)
            else -> super.get(key)
        }
    }

    override fun set(key: LuaValue, value: LuaValue) {
        when (val field = key.tojstring()) {
            "translation", "offset" -> parseVector(value)?.let { translation = it }
            "scale" -> parseVector(value)?.let { scale = it }
            "rotation", "rotation_degrees" -> parseVector(value)?.let { rotationDegrees = it }
            else -> super.set(key, value)
        }
    }

    private fun vectorToTable(v: Vector3f): LuaValue {
        val t = tableOf()
        t.set("x", valueOf(v.x.toDouble()))
        t.set("y", valueOf(v.y.toDouble()))
        t.set("z", valueOf(v.z.toDouble()))
        return t
    }

    private fun parseVector(value: LuaValue?): Vector3f? {
        if (value == null || !value.istable()) return null
        val x = value.get("x")
        val y = value.get("y")
        val z = value.get("z")
        if (!x.isnumber() || !y.isnumber() || !z.isnumber()) return null
        return Vector3f(x.tofloat(), y.tofloat(), z.tofloat())
    }

    override fun typename(): String = "transform"
    override fun tojstring(): String =
        "LuaTransform(translation=${translation}, scale=${scale}, rotation=${rotationDegrees})"
}
