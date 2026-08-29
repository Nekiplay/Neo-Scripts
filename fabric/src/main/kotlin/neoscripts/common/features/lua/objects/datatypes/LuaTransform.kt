package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import com.mojang.math.Transformation
import com.nekiplay.neoscripts.client.sugar.isVector
import com.nekiplay.neoscripts.client.sugar.toVector
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

    // Полная матрица 4x4 (если трансформация была получена из Transformation,
    // где возможен ненулевой rightRotation — euler-поля её не покрывают)
    var fullMatrix: org.joml.Matrix4f? = null

    companion object {
        fun toTransformation(translation: Vector3f, scale: Vector3f, rotationDegrees: Vector3f): Transformation {
            val radX = Math.toRadians(rotationDegrees.x.toDouble()).toFloat()
            val radY = Math.toRadians(rotationDegrees.y.toDouble()).toFloat()
            val radZ = Math.toRadians(rotationDegrees.z.toDouble()).toFloat()
            // Порядок XYZ (Rx * Ry * Rz) — согласован с fromTransformation (getEulerAnglesXYZ)
            val leftRot = Quaternionf().rotationXYZ(radX, radY, radZ)
            return Transformation(translation, leftRot, scale, Quaternionf())
        }

        fun fromTransformation(transformation: Transformation): LuaTransform {
            val translation = Vector3f(transformation.translation())
            val scale = Vector3f(transformation.scale())
            // Декодируем в том же порядке, что и кодируем в toTransformation (rotationXYZ),
            // иначе обратная конвертация не совпадает при мультиосевых поворотах
            val eulerRad = Vector3f()
            transformation.leftRotation().getEulerAnglesXYZ(eulerRad)
            val rotationDegrees = Vector3f(
                Math.toDegrees(eulerRad.x.toDouble()).toFloat(),
                Math.toDegrees(eulerRad.y.toDouble()).toFloat(),
                Math.toDegrees(eulerRad.z.toDouble()).toFloat()
            )
            val result = LuaTransform(translation, scale, rotationDegrees)
            result.fullMatrix = transformation.getMatrixCopy()
            return result
        }
    }

    fun toTransformation(): Transformation = toTransformation(translation, scale, rotationDegrees)

    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "translation", "offset" -> vectorToTable(translation)
            "scale" -> vectorToTable(scale)
            "rotation", "rotation_degrees", "rotationDegrees" -> vectorToTable(rotationDegrees)
            "matrix" -> matrixToTable()
            else -> super.get(key)
        }
    }

    /**
     * Полная матрица 4x4 в row-major порядке (1..16).
     * Возвращает nil, если трансформация создана через createTransform
     * (там достаточно translation/scale/rotation).
     */
    private fun matrixToTable(): LuaValue {
        val m = fullMatrix ?: return NIL
        val t = tableOf()
        for (row in 0..3) {
            for (col in 0..3) {
                t.set(row * 4 + col + 1, valueOf(m.get(col, row).toDouble()))
            }
        }
        return t
    }

    override fun set(key: LuaValue, value: LuaValue) {
        when (val field = key.tojstring()) {
            "translation", "offset" -> parseVector(value)?.let { translation = it }
            "scale" -> parseVector(value)?.let { scale = it }
            "rotation", "rotation_degrees", "rotationDegrees" -> parseVector(value)?.let { rotationDegrees = it }
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
        if (value == null) return null
        if (value.isVector()) {
            val vec = value.toVector()
            if (vec != null) {
                return Vector3f(vec.x.toFloat(), vec.y.toFloat(), vec.z.toFloat())
            }
        }
        if (!value.istable()) return null
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
