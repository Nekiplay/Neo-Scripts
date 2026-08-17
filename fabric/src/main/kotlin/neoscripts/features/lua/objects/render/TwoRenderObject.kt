package com.nekiplay.neoscripts.features.lua.objects.render

import com.mojang.blaze3d.platform.NativeImage
import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaItemStack
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier
import kotlin.math.abs
import kotlin.math.sqrt


class TwoRenderObject(private val gui: GuiGraphicsExtractor, private val scriptId: String? = null): LuaValue() {
    // Кэш для загруженных текстур с разделением по скриптам
    companion object {
        val textureCache = ConcurrentHashMap<String, MutableMap<String, Identifier>>()
        val textureCounter = AtomicInteger(0)

        var context: GuiGraphicsExtractor? = null
        var deltaTracker: DeltaTracker? = null

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

        fun extractBeforeMiscOverlay(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker?) {
            Companion.context = graphics
            Companion.deltaTracker = deltaTracker
        }
    }

    override fun call(): LuaValue {
        return this
    }

    private val functions: Map<LuaValue, LuaValue> by lazy {
        hashMapOf(
            LuaValue.valueOf("getWindowScale") to GetWindowScaleFunction(),
            LuaValue.valueOf("getTextWidth") to GetTextWidthFunction(),
            LuaValue.valueOf("renderText") to RenderTextFunction(),
            LuaValue.valueOf("renderImage") to RenderImageFunction(),
            LuaValue.valueOf("renderRect") to RenderRectFunction(),
            LuaValue.valueOf("renderLine") to RenderLineFunction(),
            LuaValue.valueOf("renderPolygon") to RenderPolygonFunction(),
            LuaValue.valueOf("renderItemStack") to RenderItemStackFunction()
        )
    }

    override fun get(key: LuaValue): LuaValue {
        if (key.type() != TSTRING) return NIL
        return functions[key] ?: NIL
    }

    private inner class GetTextWidthFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val text = args.arg1()
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

    private inner class GetWindowScaleFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val table = tableOf()
            val width: Int = mc.window.guiScaledWidth
            val height: Int = mc.window.guiScaledHeight

            table.set("width", width)
            table.set("height", height)
            return table
        }
    }

    private inner class RenderTextFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val ctx = context ?: return NIL

            val x = args.optint(1, 0)
            val y = args.optint(2, 0)
            val text = args.optjstring(3, "Empty")
            val red = args.optint(4, 255)
            val green = args.optint(5, 255)
            val blue = args.optint(6, 255)
            val alpha = args.optint(7, 255)
            val shadow = args.optboolean(8, true)
            val scale = args.optdouble(9, 1.0).toFloat()

            val color = (alpha and 0xFF shl 24) or
                    (red and 0xFF shl 16) or
                    (green and 0xFF shl 8) or
                    (blue and 0xFF)

            val textRenderer: Font? = mc.font
            if (textRenderer != null) {
                if (scale != 1.0f) {
                    ctx.pose().pushMatrix()
                    ctx.pose().translate(x.toFloat(), y.toFloat())
                    ctx.pose().scale(scale, scale)

                    ctx.text(textRenderer, text, 0, 0, color, shadow)

                    ctx.pose().popMatrix()
                } else {
                    ctx.text(textRenderer, text, x, y, color, shadow)
                }
            }
            return TRUE
        }
    }

    private inner class RenderImageFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val ctx = context ?: return NIL

            val path = args.optjstring(1, "") ?: return NIL
            if (path.isEmpty()) return NIL

            val x = args.optint(2, 0)
            val y = args.optint(3, 0)
            val width = args.optint(4, 0)
            val height = args.optint(5, 0)
            val u = args.optint(6, 0)
            val v = args.optint(7, 0)
            val regionWidth = args.optint(8, width)
            val regionHeight = args.optint(9, height)

            try {
                val identifier = loadTexture(path)
                if (identifier != null) {
                    ctx.blit(
                        RenderPipelines.GUI_TEXTURED, identifier,
                        x, y,
                        u.toFloat(), v.toFloat(),
                        width, height,
                        regionWidth, regionHeight
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return TRUE
        }
    }

    private inner class ClearImageCacheFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            // Очищаем кэш текстур только для текущего скрипта
            scriptId?.let { clearScriptCache(it) }
            return NIL
        }
    }

    /**
     * Загружает текстуру из файла и возвращает её Identifier
     */
    private fun loadTexture(path: String): Identifier? {
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
                val textureName = "neoscripts:texture_${scriptCacheId}_${textureCounter.getAndIncrement()}"
                val texture = DynamicTexture(
                    Supplier { textureName },
                    nativeImage
                )

                // Создаем идентификатор
                val identifier = Identifier.fromNamespaceAndPath("neoscripts", "texture_${scriptCacheId}_${textureCounter.get()}")
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

    private inner class RenderLineFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val ctx = context ?: return NIL

            val x1 = args.optint(1, 0)
            val y1 = args.optint(2, 0)
            val x2 = args.optint(3, 0)
            val y2 = args.optint(4, 0)
            val red = args.optint(5, 255)
            val green = args.optint(6, 255)
            val blue = args.optint(7, 255)
            val alpha = args.optint(8, 255)
            val thickness = args.optint(9, 1)

            val color = (alpha and 0xFF shl 24) or
                    (red and 0xFF shl 16) or
                    (green and 0xFF shl 8) or
                    (blue and 0xFF)

            drawLine(ctx, x1, y1, x2, y2, color, thickness)
            return TRUE
        }
    }

    private fun drawLine(context: GuiGraphicsExtractor, x1: Int, y1: Int, x2: Int, y2: Int, color: Int, width: Int = 1) {
        if (width <= 1) {
            // Обычная линия шириной 1 пиксель
            drawLineBresenham(context, x1, y1, x2, y2, color)
        } else {
            // Толстая линия
            drawThickLine(context, x1, y1, x2, y2, color, width)
        }
    }

    private fun drawLineBresenham(context: GuiGraphicsExtractor, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
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

    private fun drawThickLine(context: GuiGraphicsExtractor, x1: Int, y1: Int, x2: Int, y2: Int, color: Int, width: Int) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = sqrt((dx * dx + dy * dy).toDouble())

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

    private fun fillTriangleSimple(context: GuiGraphicsExtractor, x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, color: Int) {
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
                            context.verticalLine(startX, endX, y, color)
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

    private inner class RenderPolygonFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val ctx = context ?: return NIL

            val table = args.arg1()
            if (!table.istable()) return NIL

            val pointsTable = table.get("points")
            if (!pointsTable.istable()) return NIL

            val red = table.get("red").optint(255)
            val green = table.get("green").optint(255)
            val blue = table.get("blue").optint(255)
            val alpha = table.get("alpha").optint(255)

            val color = (alpha and 0xFF shl 24) or
                    (red and 0xFF shl 16) or
                    (green and 0xFF shl 8) or
                    (blue and 0xFF)

            val points = mutableListOf<Pair<Float, Float>>()
            var i = 1
            while (true) {
                val pointTable = pointsTable.get(i)
                if (pointTable.istable()) {
                    val x = pointTable.get("x").optdouble(0.0).toFloat()
                    val y = pointTable.get("y").optdouble(0.0).toFloat()
                    points.add(x to y)
                    i++
                } else {
                    break
                }
            }

            if (points.size >= 3) {
                drawPolygon(ctx, points, color)
            }
            return TRUE
        }
    }

    private inner class RenderRectFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val ctx = context ?: return NIL

            val x = args.optint(1, 0)
            val y = args.optint(2, 0)
            val width = args.optint(3, 0)
            val height = args.optint(4, 0)
            val red = args.optint(5, 255)
            val green = args.optint(6, 255)
            val blue = args.optint(7, 255)
            val alpha = args.optint(8, 255)

            val color = (alpha and 0xFF shl 24) or
                    (red and 0xFF shl 16) or
                    (green and 0xFF shl 8) or
                    (blue and 0xFF)

            ctx.fill(x, y, x + width, y + height, color)
            return TRUE
        }
    }

    private fun drawPolygon(context: GuiGraphicsExtractor, points: List<Pair<Float, Float>>, color: Int) {
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

    private inner class RenderItemStackFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val ctx = context ?: return NIL

            if (args.narg() < 3) return NIL

            val x = args.optint(1, 0)
            val y = args.optint(2, 0)
            val itemStackArg = args.arg(3)
            val scale = args.optdouble(4, 1.0).toFloat()

            val itemStack = when {
                itemStackArg.isuserdata() && itemStackArg.touserdata() is LuaItemStack -> (itemStackArg.touserdata() as LuaItemStack).stack
                itemStackArg.isuserdata() && itemStackArg.touserdata() is ItemStack -> itemStackArg.touserdata() as ItemStack
                else -> null
            }

            if (itemStack != null) {
                if (scale != 1.0f) {
                    ctx.pose().pushMatrix()
                    ctx.pose().translate(x.toFloat(), y.toFloat())
                    ctx.pose().scale(scale, scale)
                    ctx.fakeItem(itemStack, 0, 0)
                    ctx.pose().popMatrix()
                } else {
                    ctx.fakeItem(itemStack, x, y)
                }
                return TRUE
            }
            return NIL
        }
    }

    private fun drawTriangle(context: GuiGraphicsExtractor,
                             x1: Float, y1: Float,
                             x2: Float, y2: Float,
                             x3: Float, y3: Float,
                             color: Int) {
        // Рисуем контур треугольника
        drawTriangleOutline(context, x1, y1, x2, y2, x3, y3, color)

        // Заливка треугольника
        fillTriangle(context, x1, y1, x2, y2, x3, y3, color)
    }

    private fun drawTriangleOutline(context: GuiGraphicsExtractor,
                                    x1: Float, y1: Float,
                                    x2: Float, y2: Float,
                                    x3: Float, y3: Float,
                                    color: Int) {
        // Рисуем три линии треугольника
        drawLine(context, x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt(), color)
        drawLine(context, x2.toInt(), y2.toInt(), x3.toInt(), y3.toInt(), color)
        drawLine(context, x3.toInt(), y3.toInt(), x1.toInt(), y1.toInt(), color)
    }

    private fun fillTriangle(context: GuiGraphicsExtractor,
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
                        val ix1 = intersections[i]
                        val ix2 = intersections[i + 1]
                        context.verticalLine(ix1, ix2, y, color)
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

    private fun drawLine(context: GuiGraphicsExtractor, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        // Алгоритм Брезенхема для рисования линии
        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
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

    override fun typename(): String = "2d_renderer"
    override fun tojstring(): String = "2DRenderObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}