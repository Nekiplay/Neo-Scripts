package com.nekiplay.hypixelcry.features.lua.objects.render

import com.mojang.blaze3d.platform.NativeImage
import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.smartPush
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class TwoRenderObject(private val lua: Lua, private val context: GuiGraphics?, private val scriptId: String? = null) {
    companion object {
        val textureCache = ConcurrentHashMap<String, MutableMap<String, Identifier>>()
        val textureCounter = AtomicInteger(0)

        fun clearScriptCache(scriptId: String) {
            textureCache[scriptId]?.values?.forEach { mc.textureManager.release(it) }
            textureCache.remove(scriptId)
        }
    }

    fun push(): LuaValue {
        lua.newTable() // [table]
        val tIdx = lua.getTop()

        // Добавляем функции рендеринга напрямую (они захватывают контекст)
        registerFunction(tIdx, "getWindowScale") { getWindowScale(it) }
        registerFunction(tIdx, "getTextWidth") { getTextWidth(it) }
        registerFunction(tIdx, "renderText") { renderText(it) }
        registerFunction(tIdx, "renderImage") { renderImage(it) }
        registerFunction(tIdx, "renderRect") { renderRect(it) }
        registerFunction(tIdx, "renderLine") { renderLine(it) }
        registerFunction(tIdx, "renderPolygon") { renderPolygon(it) }
        registerFunction(tIdx, "renderItemStack") { renderItemStack(it) }
        return lua.get() // Забираем и возвращаем готовую таблицу
    }


    private fun registerFunction(tableIdx: Int, name: String, func: (Lua) -> Any?) {
        lua.push(name) // Кладем имя
        lua.push(JFunction { l ->
            val result = func(l)
            if (result is Int) {
                return@JFunction result
            }
            lua.smartPush(result)
            1 // Возвращаем 1 значение в Lua
        })
        lua.setTable(tableIdx) // table[name] = closure
    }

    fun luaFunction(block: (Lua) -> Any?): JFunction {
        return JFunction { l ->
            val result = block(l)
            l.smartPush(result)
            1 // ВАЖНО: всегда возвращаем 1
        }
    }

    // --- Вспомогательные методы чтения стека ---
    private fun getArgsIdx(l: Lua): Int {
        val top = l.getTop()
        if (top == 0) return 0
        // Если вызвано как context:renderText(args), то args на индексе 2
        // Если вызвано как context.renderText(args), то args на индексе 1
        for (i in 1..top) {
            if (l.isTable(i)) {
                // Проверяем, не является ли эта таблица самим контекстом (у него есть __java_instance)
                l.push("__java_instance")
                l.rawGet(i)
                val isContext = !l.isNil(-1)
                l.pop(1)
                if (!isContext) return i
            }
        }
        // Если нашли только одну таблицу, и это аргументы
        if (top >= 1 && l.isTable(top)) return top
        return 0
    }

    private fun Lua.optI(idx: Int, key: String, def: Int): Int {
        this.getField(idx, key); val res = if (this.isNumber(-1)) this.toInteger(-1).toInt() else def
        this.pop(1); return res
    }
    private fun Lua.optF(idx: Int, key: String, def: Float): Float {
        this.getField(idx, key); val res = if (this.isNumber(-1)) this.toNumber(-1).toFloat() else def
        this.pop(1); return res
    }
    private fun Lua.optB(idx: Int, key: String, def: Boolean): Boolean {
        this.getField(idx, key); val res = if (this.isBoolean(-1)) this.toBoolean(-1) else def
        this.pop(1); return res
    }
    private fun Lua.optS(idx: Int, key: String, def: String): String {
        this.getField(idx, key); val res = this.toString(-1) ?: def
        this.pop(1); return res
    }

    // --- Реализация функций ---

    private fun getWindowScale(l: Lua): Int {
        l.push(mc.window.guiScaledWidth.toDouble())
        l.push(mc.window.guiScaledHeight.toDouble())
        return 2
    }

    private fun getTextWidth(l: Lua): Int {
        val text = l.toString(1) ?: return 0
        l.push(mc.font.width(text).toDouble())
        return 1
    }

    private fun renderText(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0) return 0

        val text = l.optS(idx, "text", "Empty")
        val x = l.optI(idx, "x", 0)
        val y = l.optI(idx, "y", 0)
        val r = l.optI(idx, "red", 255);
        val g = l.optI(idx, "green", 255);
        val b = l.optI(idx, "blue", 255);
        val a = l.optI(idx, "alpha", 255)
        val color = (a and 0xFF shl 24) or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)
        val shadow = l.optB(idx, "shadow", true)
        val scale = l.optF(idx, "scale", 1.0f)

        if (scale != 1.0f) {
            context!!.pose().pushMatrix()
            context.pose().translate(x.toFloat(), y.toFloat())
            context.pose().scale(scale, scale)
            context.drawString(mc.font, Component.literal(text), 0, 0, color, shadow)
            context.pose().popMatrix()
        } else {
            context!!.drawString(mc.font, Component.literal(text), x, y, color, shadow)
        }
        return 0
    }

    private fun renderRect(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0) return 0

        val x = l.optI(idx, "x", 0);
        val y = l.optI(idx, "y", 0)
        val w = l.optI(idx, "width", 0);
        val h = l.optI(idx, "height", 0)
        val r = l.optI(idx, "red", 255);
        val g = l.optI(idx, "green", 255);
        val b = l.optI(idx, "blue", 255); val a = l.optI(1, "alpha", 255)
        context!!.fill(x, y, x + w, y + h, (a shl 24) or (r shl 16) or (g shl 8) or b)
        return 0
    }

    private fun renderItemStack(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0) return 0

        val x = l.optI(idx, "x", 0);
        val y = l.optI(idx, "y", 0)
        val scale = l.optF(idx, "scale", 1.0f)

        l.getField(idx, "itemStack")
        val itemObj = l.toJavaObject(-1)
        l.pop(1)

        val stack: ItemStack? = when (itemObj) {
            is LuaItemStack -> itemObj.stack
            is ItemStack -> itemObj
            else -> null
        }

        if (stack == null || stack.isEmpty) {
            HypixelCry.LOGGER.info("renderItemStack: stack is null or empty")
            return 0
        }

        HypixelCry.LOGGER.info("renderItemStack: x=$x, y=$y, scale=$scale, item=${stack.item.name.string}, count=${stack.count}")

        stack.let {
            if (scale != 1.0f) {
                context!!.pose().pushMatrix()
                context.pose().translate(x.toFloat(), y.toFloat())
                context.pose().scale(scale, scale)
                context.renderItem(it, 0, 0)
                context.pose().popMatrix()
            } else {
                context!!.renderItem(it, x, y)
            }
        }
        return 0
    }

    private fun renderPolygon(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0) return 0

        l.getField(idx, "points")
        if (!l.isTable(-1)) { l.pop(1); return 0 }

        val r = l.optI(idx, "red", 255);
        val g = l.optI(idx, "green", 255);
        val b = l.optI(idx, "blue", 255);
        val a = l.optI(idx, "alpha", 255)
        val color = (a shl 24) or (r shl 16) or (g shl 8) or b

        val points = mutableListOf<Pair<Float, Float>>()
        var i = 1
        while (true) {
            l.rawGetI(-1, i)
            if (!l.isTable(-1)) { l.pop(1); break }
            val px = l.optF(-1, "x", 0f); val py = l.optF(-1, "y", 0f)
            points.add(px to py)
            l.pop(1); i++
        }
        l.pop(1) // pop points table

        if (points.size >= 3) {
            val (x0, y0) = points[0]
            for (j in 1 until points.size - 1) {
                fillTriangle(context!!, x0, y0, points[j].first, points[j].second, points[j+1].first, points[j+1].second, color)
            }
        }
        return 0
    }

    private fun renderImage(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение пути к файлу (обязательно)
        val path = l.optS(idx, "path", "")
        if (path.isEmpty()) return 0

        // 3. Извлечение координат и размеров на экране
        val x = l.optI(idx, "x", 0)
        val y = l.optI(idx, "y", 0)
        val width = l.optI(idx, "width", 0)
        val height = l.optI(idx, "height", 0)

        // 4. Извлечение параметров UV (текстурные координаты)
        val u = l.optI(idx, "u", 0)
        val v = l.optI(idx, "v", 0)

        // 5. Извлечение размеров региона (если не указаны, используем ширину/высоту картинки)
        val regionWidth = l.optI(idx, "region_width", width)
        val regionHeight = l.optI(1, "region_height", height)

        try {
            // 6. Загрузка текстуры через ваш метод (кэширование уже внутри него)
            val identifier = loadTexture(path)

            if (identifier != null) {
                // 7. Вызов blit с использованием RenderPipelines (как в вашем оригинале)
                context.blit(
                    RenderPipelines.GUI_TEXTURED,
                    identifier,
                    x, y,
                    u.toFloat(), v.toFloat(),
                    width, height,
                    regionWidth, regionHeight, // Texture size (total)
                    regionWidth, regionHeight  // Region size (drawn)
                )
                l.push(true)
                return 1
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0
    }

    private fun renderLine(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        val x1 = l.optI(idx, "x1", 0);
        val y1 = l.optI(idx, "y1", 0)
        val x2 = l.optI(idx, "x2", 0);
        val y2 = l.optI(idx, "y2", 0)
        val r = l.optI(idx, "red", 255);
        val g = l.optI(idx, "green", 255);
        val b = l.optI(idx, "blue", 255);
        val a = l.optI(idx, "alpha", 255)
        val color = (a shl 24) or (r shl 16) or (g shl 8) or b
        val thick = l.optI(idx, "thickness", 1)

        if (thick <= 1) drawLineBresenham(context!!, x1, y1, x2, y2, color)
        else drawThickLine(context!!, x1, y1, x2, y2, color, thick)
        return 0
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
                val textureName = "hypixelcry:texture_${scriptCacheId}_${textureCounter.getAndIncrement()}"
                val texture = DynamicTexture(
                    Supplier { textureName },
                    nativeImage
                )

                // Создаем идентификатор
                val identifier = Identifier.fromNamespaceAndPath("hypixelcry", "texture_${scriptCacheId}_${textureCounter.get()}")
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
}