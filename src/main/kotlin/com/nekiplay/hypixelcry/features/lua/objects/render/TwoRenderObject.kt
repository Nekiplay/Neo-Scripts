package com.nekiplay.hypixelcry.features.lua.objects.render

import com.mojang.blaze3d.platform.NativeImage
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class TwoRenderObject(private val context: GuiGraphics?, private val scriptId: String? = null): LuaValue() {
    // Кэш для загруженных текстур с разделением по скриптам
    companion object {
        val textureCache = ConcurrentHashMap<String, MutableMap<String, ResourceLocation>>()
        val textureCounter = AtomicInteger(0)

        // Очистка кэша для конкретного скрипта
        fun clearScriptCache(scriptId: String) {
            textureCache[scriptId]?.values?.forEach { identifier ->
                mc.textureManager.release(identifier)
            }
            textureCache.remove(scriptId)
        }

        // Полная очистка всех кэшей
        fun clearAllCaches() {
            textureCache.values.forEach { scriptCache ->
                scriptCache.values.forEach { identifier ->
                    mc.textureManager.release(identifier)
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
            "renderItemStack" -> RenderItemStackFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class GetTextWidthFunction : OneArgFunction() {
        override fun call(text: LuaValue): LuaValue {
            if (text.isstring()) {
                val textRenderer: Font? = mc.font
                val width: Int? = textRenderer?.width(text.tojstring())
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
            val width: Int = mc.window.screenWidth
            val height: Int = mc.window.screenHeight

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


                val textRenderer: Font? = mc.font
                if (scale != 1.0f) {
                    context.pose().pushMatrix()
                    context.pose().translate(x.toFloat(), y.toFloat())
                    context.pose().scale(scale, scale)

                    context.drawString(textRenderer, Component.literal(text), 0, 0, color, isShadow)

                    context.pose().popMatrix()
                }
                else {
                    context.drawString(textRenderer, Component.literal(text), x, y, color, isShadow)
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
                        context.blit(
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
    private fun loadTexture(path: String): ResourceLocation? {
        val scriptCacheId = scriptId ?: "2d_global"

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
                val texture = DynamicTexture(
                    Supplier { textureName },
                    nativeImage
                )

                // Создаем идентификатор
                val identifier = ResourceLocation.fromNamespaceAndPath("hypixelcry", "texture_${scriptCacheId}_${textureCounter.get()}")
                mc.textureManager.register(identifier, texture)

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

                val thickness: Int = if (table.get("thickness").isnumber()) table.get("thickness").toint() else 1

                drawLine(context, x1, y1, x2, y2, color, thickness)
            }
            return NIL
        }
    }

    private fun drawLine(context: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, color: Int, width: Int = 1) {
        if (width <= 1) {
            // Обычная линия шириной 1 пиксель
            drawLineBresenham(context, x1, y1, x2, y2, color)
        } else {
            // Толстая линия
            drawThickLine(context, x1, y1, x2, y2, color, width)
        }
    }

    private fun drawLineBresenham(context: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        val dx = kotlin.math.abs(x2 - x1)
        val dy = kotlin.math.abs(y2 - y1)
        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1
        var err = dx - dy

        var x = x1
        var y = y1

        while (true) {
            context.fill(x, y, x + 1, y + 1, color)

            if (x == x2 && y == y2) break

            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
    }

    private fun drawThickLine(context: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, color: Int, width: Int) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())

        if (length == 0.0) {
            // Точка - рисуем квадрат
            val halfWidth = width / 2
            context.fill(x1 - halfWidth, y1 - halfWidth, x1 + halfWidth + 1, y1 + halfWidth + 1, color)
            return
        }

        // Нормализованный перпендикулярный вектор
        val perpX = -dy / length
        val perpY = dx / length
        val halfWidth = width / 2.0

        // Четыре угла прямоугольника линии
        val x1a = (x1 + perpX * halfWidth).toInt()
        val y1a = (y1 + perpY * halfWidth).toInt()
        val x1b = (x1 - perpX * halfWidth).toInt()
        val y1b = (y1 - perpY * halfWidth).toInt()
        val x2a = (x2 + perpX * halfWidth).toInt()
        val y2a = (y2 + perpY * halfWidth).toInt()
        val x2b = (x2 - perpX * halfWidth).toInt()
        val y2b = (y2 - perpY * halfWidth).toInt()

        // Рисуем прямоугольник как два треугольника
        fillTriangleSimple(context, x1a, y1a, x1b, y1b, x2a, y2a, color)
        fillTriangleSimple(context, x1b, y1b, x2a, y2a, x2b, y2b, color)
    }

    private fun fillTriangleSimple(context: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, color: Int) {
        val minY = minOf(y1, y2, y3)
        val maxY = maxOf(y1, y2, y3)

        for (y in minY..maxY) {
            val intersections = mutableListOf<Int>()

            addIntersectionInt(intersections, x1, y1, x2, y2, y)
            addIntersectionInt(intersections, x2, y2, x3, y3, y)
            addIntersectionInt(intersections, x3, y3, x1, y1, y)

            if (intersections.size >= 2) {
                intersections.sort()
                for (i in 0 until intersections.size step 2) {
                    if (i + 1 < intersections.size) {
                        val startX = intersections[i]
                        val endX = intersections[i + 1]
                        if (startX <= endX) {
                            context.hLine(startX, endX, y, color)
                        }
                    }
                }
            }
        }
    }

    private fun addIntersectionInt(intersections: MutableList<Int>, x1: Int, y1: Int, x2: Int, y2: Int, y: Int) {
        if ((y in y1..y2) || (y in y2..y1)) {
            if (y1 != y2) {
                val x = x1 + (x2 - x1) * (y - y1) / (y2 - y1)
                intersections.add(x)
            } else if (y == y1) {
                intersections.add(x1)
                intersections.add(x2)
            }
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
                            points.add(x to y)
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

    private fun drawPolygon(context: GuiGraphics, points: List<Pair<Float, Float>>, color: Int) {
        if (points.size < 3) return

        // Кэшируем первую вершину
        val (x0, y0) = points[0]

        // Рисуем треугольники фана без повторных обращений к списку точек
        for (i in 1 until points.size - 1) {
            val (x1, y1) = points[i]
            val (x2, y2) = points[i + 1]

            drawTriangle(context, x0, y0, x1, y1, x2, y2, color)
        }
    }

    private fun drawQuad(context: GuiGraphics, x1: Float, y1: Float, x2: Float, y2: Float,
                         x3: Float, y3: Float, x4: Float, y4: Float, color: Int) {
        // Рисуем четырехугольник как два треугольника
        drawTriangle(context, x1, y1, x2, y2, x3, y3, color)
        drawTriangle(context, x1, y1, x3, y3, x4, y4, color)
    }

    private inner class RenderItemStackFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val x = if (table.get("x").isnumber()) table.get("x").toint() else 0
                val y = if (table.get("y").isnumber()) table.get("y").toint() else 0
                val scale = if (table.get("scale").isnumber()) table.get("scale").tofloat() else 1.0f

                val itemStackObj = table.get("itemStack")
                val itemStack = when {
                    itemStackObj.isuserdata() && itemStackObj.touserdata() is LuaItemStack -> (itemStackObj.touserdata() as LuaItemStack).getItemStack()
                    itemStackObj.isuserdata() && itemStackObj.touserdata() is ItemStack -> itemStackObj.touserdata() as ItemStack
                    else -> null
                }

                if (itemStack != null) {
                    if (scale != 1.0f) {
                        context.pose().pushMatrix()
                        context.pose().translate(x.toFloat(), y.toFloat())
                        context.pose().scale(scale, scale)
                        context.renderItem(itemStack, 0, 0)
                        context.pose().popMatrix()
                    } else {
                        context.renderItem(itemStack, x, y)
                    }
                }
            }
            return NIL
        }
    }

    private fun drawTriangle(context: GuiGraphics,
                             x1: Float, y1: Float,
                             x2: Float, y2: Float,
                             x3: Float, y3: Float,
                             color: Int) {
        // Используем существующие методы DrawContext для рисования треугольника
        // через линии (контур) или заливку по пикселям оптимизированно

        // Вариант 1: Контур треугольника
        drawTriangleOutline(context, x1, y1, x2, y2, x3, y3, color)

        // Вариант 2: Заливка треугольника (оптимизированная)
        fillTriangle(context, x1, y1, x2, y2, x3, y3, color)
    }

    private fun drawTriangleOutline(context: GuiGraphics,
                                    x1: Float, y1: Float,
                                    x2: Float, y2: Float,
                                    x3: Float, y3: Float,
                                    color: Int) {
        // Рисуем три линии треугольника
        drawLine(context, x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt(), color)
        drawLine(context, x2.toInt(), y2.toInt(), x3.toInt(), y3.toInt(), color)
        drawLine(context, x3.toInt(), y3.toInt(), x1.toInt(), y1.toInt(), color)
    }

    private fun fillTriangle(context: GuiGraphics,
                             x1: Float, y1: Float,
                             x2: Float, y2: Float,
                             x3: Float, y3: Float,
                             color: Int) {
        // Оптимизированная заливка через scanline алгоритм
        val minY = minOf(y1, y2, y3).toInt()
        val maxY = maxOf(y1, y2, y3).toInt()

        for (y in minY..maxY) {
            val intersections = mutableListOf<Int>()

            // Находим пересечения с каждой стороной треугольника
            addIntersection(intersections, x1, y1, x2, y2, y.toFloat())
            addIntersection(intersections, x2, y2, x3, y3, y.toFloat())
            addIntersection(intersections, x3, y3, x1, y1, y.toFloat())

            if (intersections.size >= 2) {
                intersections.sort()
                for (i in 0 until intersections.size step 2) {
                    if (i + 1 < intersections.size) {
                        val x1 = intersections[i]
                        val x2 = intersections[i + 1]
                        context.hLine(x1, x2, y, color)
                    }
                }
            }
        }
    }

    private fun addIntersection(intersections: MutableList<Int>,
                                x1: Float, y1: Float,
                                x2: Float, y2: Float,
                                y: Float) {
        if ((y1 <= y && y <= y2) || (y2 <= y && y <= y1)) {
            if (y1 != y2) {
                val x = x1 + (x2 - x1) * (y - y1) / (y2 - y1)
                intersections.add(x.toInt())
            }
        }
    }

    private fun drawLine(context: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        // Алгоритм Брезенхема для рисования линии
        val dx = kotlin.math.abs(x2 - x1)
        val dy = kotlin.math.abs(y2 - y1)
        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1
        var err = dx - dy

        var x = x1
        var y = y1

        while (true) {
            context.fill(x, y, x + 1, y + 1, color)

            if (x == x2 && y == y2) break

            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
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