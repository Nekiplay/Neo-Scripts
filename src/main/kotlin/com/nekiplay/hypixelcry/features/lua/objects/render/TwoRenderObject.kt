package com.nekiplay.hypixelcry.features.lua.objects.render

import com.mojang.blaze3d.vertex.VertexFormat
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class TwoRenderObject(private val context: DrawContext?, private val scriptId: String? = null): LuaValue() {
    // Кэш для загруженных текстур с разделением по скриптам
    companion object {
        private val textureCache = ConcurrentHashMap<String, MutableMap<String, Identifier>>()
        private val textureCounter = AtomicInteger(0)

        // Очистка кэша для конкретного скрипта
        fun clearScriptCache(scriptId: String) {
            textureCache[scriptId]?.values?.forEach { identifier ->
                mc.textureManager.destroyTexture(identifier)
            }
            textureCache.remove(scriptId)
        }

        // Полная очистка всех кэшей
        fun clearAllCaches() {
            textureCache.values.forEach { scriptCache ->
                scriptCache.values.forEach { identifier ->
                    mc.textureManager.destroyTexture(identifier)
                }
            }
            textureCache.clear()
        }
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getWindowScale" -> GetWindowScaleFunction()
            "getTextWidth" -> GetTextWidthFunction()
            "renderText" -> RenderTextFunction()
            "renderImage" -> RenderImageFunction()
            "renderRect" -> RenderRectFunction()
            "renderLine" -> RenderLineFunction()
            "renderPolygon" -> RenderPolygonFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class GetTextWidthFunction : OneArgFunction() {
        override fun call(text: LuaValue): LuaValue {
            if (text.isstring()) {
                val textRenderer: TextRenderer? = mc.textRenderer
                val width: Int? = textRenderer?.getWidth(text.tojstring())
                if (width != null) {
                    return valueOf(width)
                }
            }
            return NIL
        }
    }

    private inner class GetWindowScaleFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = tableOf()
            val width: Int = mc.window.scaledWidth
            val height: Int = mc.window.scaledHeight

            table.set("width", width)
            table.set("height", height)
            return table
        }
    }

    private inner class RenderTextFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val text = if (table.get("text").isstring()) table.get("text").tojstring() else "Empty"

                val x: Int = if (table.get("x").isnumber()) table.get("x").toint() else 0
                val y: Int = if (table.get("y").isnumber()) table.get("y").toint() else 0

                val red = if (table.get("red").isnumber()) table.get("red").toint() else 255
                val green = if (table.get("green").isnumber()) table.get("green").toint() else 255
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 255
                val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 255

                val color = (alpha and 0xFF shl 24) or
                        (red and 0xFF shl 16) or
                        (green and 0xFF shl 8) or
                        (blue and 0xFF)

                val isShadow = if (table.get("shadow").isboolean()) table.get("shadow").toboolean() else true
                val scale = if (table.get("scale").isnumber()) table.get("scale").tofloat() else 1.0f


                val textRenderer: TextRenderer? = mc.textRenderer
                if (scale != 1.0f) {
                    context.matrices.pushMatrix()
                    context.matrices.translate(x.toFloat(), y.toFloat())
                    context.matrices.scale(scale, scale)

                    context.drawText(textRenderer, Text.literal(text), 0, 0, color, isShadow)

                    context.matrices.popMatrix()
                }
                else {
                    context.drawText(textRenderer, Text.literal(text), x, y, color, isShadow)
                }
            }
            return NIL
        }
    }

    private inner class RenderImageFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val path = if (table.get("path").isstring()) table.get("path").tojstring() else return NIL
                val x: Int = if (table.get("x").isnumber()) table.get("x").toint() else 0
                val y: Int = if (table.get("y").isnumber()) table.get("y").toint() else 0
                val width: Int = if (table.get("width").isnumber()) table.get("width").toint() else 0
                val height: Int = if (table.get("height").isnumber()) table.get("height").toint() else 0

                val u: Int = if (table.get("u").isnumber()) table.get("u").toint() else 0
                val v: Int = if (table.get("v").isnumber()) table.get("v").toint() else 0
                val regionWidth: Int = if (table.get("region_width").isnumber()) table.get("region_width").toint() else width
                val regionHeight: Int = if (table.get("region_height").isnumber()) table.get("region_height").toint() else height

                try {
                    val identifier = loadTexture(path)
                    if (identifier != null) {
                        context.drawTexture(
                            RenderPipelines.GUI_TEXTURED, identifier,
                            x, y,
                            u.toFloat(), v.toFloat(),
                            width, height,
                            regionWidth, regionHeight,
                            regionWidth, regionHeight
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return NIL
        }
    }

    private inner class ClearImageCacheFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            // Очищаем кэш текстур только для текущего скрипта
            scriptId?.let { TwoRenderObject.clearScriptCache(it) }
            return NIL
        }
    }

    /**
     * Загружает текстуру из файла и возвращает её Identifier
     */
    private fun loadTexture(path: String): Identifier? {
        val scriptCacheId = scriptId ?: "global"

        // Проверяем кэш для текущего скрипта
        val scriptCache = textureCache.getOrPut(scriptCacheId) { ConcurrentHashMap() }
        if (scriptCache.containsKey(path)) {
            return scriptCache[path]
        }

        try {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                return null
            }

            FileInputStream(file).use { inputStream ->
                val nativeImage = NativeImage.read(inputStream)

                // Используем правильный конструктор NativeImageBackedTexture
                val textureName = "hypixelcry:texture_${scriptCacheId}_${textureCounter.getAndIncrement()}"
                val texture = NativeImageBackedTexture(
                    Supplier { textureName },
                    nativeImage
                )

                // Создаем идентификатор
                val identifier = Identifier.of("hypixelcry", "texture_${scriptCacheId}_${textureCounter.get()}")
                mc.textureManager.registerTexture(identifier, texture)

                // Сохраняем в кэш текущего скрипта
                scriptCache[path] = identifier

                return identifier
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private inner class RenderLineFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val x1: Int = if (table.get("x1").isnumber()) table.get("x1").toint() else 0
                val y1: Int = if (table.get("y1").isnumber()) table.get("y1").toint() else 0
                val x2: Int = if (table.get("x2").isnumber()) table.get("x2").toint() else 0
                val y2: Int = if (table.get("y2").isnumber()) table.get("y2").toint() else 0

                val red = if (table.get("red").isnumber()) table.get("red").toint() else 255
                val green = if (table.get("green").isnumber()) table.get("green").toint() else 255
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 255
                val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 255

                val color = (alpha and 0xFF shl 24) or
                        (red and 0xFF shl 16) or
                        (green and 0xFF shl 8) or
                        (blue and 0xFF)

                val thickness: Float = if (table.get("thickness").isnumber()) table.get("thickness").tofloat() else 1.0f

                drawLine(context, x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), thickness, color)
            }
            return NIL
        }
    }

    private inner class RenderPolygonFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val pointsTable = table.get("points")
                if (pointsTable.istable()) {
                    val red = if (table.get("red").isnumber()) table.get("red").toint() else 255
                    val green = if (table.get("green").isnumber()) table.get("green").toint() else 255
                    val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 255
                    val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 255

                    val color = (alpha and 0xFF shl 24) or
                            (red and 0xFF shl 16) or
                            (green and 0xFF shl 8) or
                            (blue and 0xFF)

                    val points = mutableListOf<Pair<Float, Float>>()
                    var i = 1
                    while (true) {
                        val pointTable = pointsTable.get(i)
                        if (pointTable.istable()) {
                            val x = if (pointTable.get("x").isnumber()) pointTable.get("x").tofloat() else 0f
                            val y = if (pointTable.get("y").isnumber()) pointTable.get("y").tofloat() else 0f
                            points.add(Pair(x, y))
                            i++
                        } else {
                            break
                        }
                    }

                    if (points.size >= 3) {
                        drawPolygon(context, points, color)
                    }
                }
            }
            return NIL
        }
    }

    private inner class RenderRectFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val x: Int = if (table.get("x").isnumber()) table.get("x").toint() else 0
                val y: Int = if (table.get("y").isnumber()) table.get("y").toint() else 0
                val width: Int = if (table.get("width").isnumber()) table.get("width").toint() else 0
                val height: Int = if (table.get("height").isnumber()) table.get("height").toint() else 0

                val red = if (table.get("red").isnumber()) table.get("red").toint() else 255
                val green = if (table.get("green").isnumber()) table.get("green").toint() else 255
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 255
                val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 255

                val color = (alpha and 0xFF shl 24) or
                        (red and 0xFF shl 16) or
                        (green and 0xFF shl 8) or
                        (blue and 0xFF)

                context.fill(x, y, x + width, y + height, color)
            }
            return NIL
        }
    }

    private fun drawLine(context: DrawContext, x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
        // Для Fabric 1.21.8 используем более простой подход с fill для линий
        val dx = x2 - x1
        val dy = y2 - y1
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())

        if (thickness <= 1f) {
            // Тонкая линия - используем fill
            context.fill(
                x1.toInt(), y1.toInt(),
                x2.toInt(), y2.toInt(),
                color
            )
        } else {
            // Толстая линия - используем заполненный прямоугольник
            val sin = kotlin.math.sin(angle).toFloat()
            val cos = kotlin.math.cos(angle).toFloat()
            val halfThickness = thickness / 2

            val x11 = x1 - sin * halfThickness
            val y11 = y1 + cos * halfThickness
            val x12 = x1 + sin * halfThickness
            val y12 = y1 - cos * halfThickness
            val x21 = x2 - sin * halfThickness
            val y21 = y2 + cos * halfThickness
            val x22 = x2 + sin * halfThickness
            val y22 = y2 - cos * halfThickness

            drawQuad(context, x11, y11, x12, y12, x22, y22, x21, y21, color)
        }
    }

    private fun drawPolygon(context: DrawContext, points: List<Pair<Float, Float>>, color: Int) {
        // Для полигонов используем triangulation через fill для каждой пары треугольников
        if (points.size < 3) return

        // Простой подход: рисуем треугольники от первой точки ко всем остальным
        for (i in 1 until points.size - 1) {
            drawTriangle(
                context,
                points[0].first, points[0].second,
                points[i].first, points[i].second,
                points[i + 1].first, points[i + 1].second,
                color
            )
        }
    }

    private fun drawQuad(context: DrawContext, x1: Float, y1: Float, x2: Float, y2: Float,
                         x3: Float, y3: Float, x4: Float, y4: Float, color: Int) {
        // Рисуем четырехугольник как два треугольника
        drawTriangle(context, x1, y1, x2, y2, x3, y3, color)
        drawTriangle(context, x1, y1, x3, y3, x4, y4, color)
    }

    private fun drawTriangle(context: DrawContext, x1: Float, y1: Float,
                             x2: Float, y2: Float, x3: Float, y3: Float, color: Int) {
        // Находим bounding box для треугольника
        val minX = minOf(x1, x2, x3).toInt()
        val minY = minOf(y1, y2, y3).toInt()
        val maxX = maxOf(x1, x2, x3).toInt()
        val maxY = maxOf(y1, y2, y3).toInt()

        // Простой подход: заполняем bounding box и проверяем каждую точку на принадлежность треугольнику
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                if (isPointInTriangle(x.toFloat(), y.toFloat(), x1, y1, x2, y2, x3, y3)) {
                    context.fill(x, y, x + 1, y + 1, color)
                }
            }
        }
    }

    private fun isPointInTriangle(px: Float, py: Float,
                                  x1: Float, y1: Float,
                                  x2: Float, y2: Float,
                                  x3: Float, y3: Float): Boolean {
        val area = 0.5 * (-y2 * x3 + y1 * (-x2 + x3) + x1 * (y2 - y3) + x2 * y3)
        val s = 1 / (2 * area) * (y1 * x3 - x1 * y3 + (y3 - y1) * px + (x1 - x3) * py)
        val t = 1 / (2 * area) * (x1 * y2 - y1 * x2 + (y1 - y2) * px + (x2 - x1) * py)
        return s > 0 && t > 0 && (1 - s - t) > 0
    }

    override fun typename(): String = "2d_renderer"
    override fun tojstring(): String = "2DRenderObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return LuaValue.TUSERDATA
    }
}