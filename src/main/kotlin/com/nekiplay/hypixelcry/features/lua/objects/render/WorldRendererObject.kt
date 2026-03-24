package com.nekiplay.hypixelcry.features.lua.objects.render

import com.logisticscraft.occlusionculling.util.Vec3d
import com.mojang.blaze3d.platform.NativeImage
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.CommonColors
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

class WorldRendererObject(val lua: Lua, private val context: PrimitiveCollector?): SimpleLuaWrapper(lua) {
    override fun getFieldValue(l: Lua, key: String): Any? {
        if (context == null) return null
        return when (key) {
            "renderFilled" -> JFunction { renderFilled(it) }
            "renderFilledCircle" -> JFunction { renderFilled(it) }
            "renderOutline" -> JFunction { renderOutline(it) }
            "renderOutlineCircle" -> JFunction { renderOutlineCircle(it) }
            "renderCylinder" -> JFunction { renderCylinder(it) }
            "renderSphere" -> JFunction { renderSphere(it) }
            "renderText" -> JFunction { renderText(it) }
            "renderLinesFromPoints" -> JFunction { renderLinesFromPoints(it) }
            "renderLineFromCursor" -> JFunction { renderLineFromCursor(it) }
            "renderImage" -> JFunction { renderImage(it) }
            "renderBeaconBeam" -> JFunction { renderBeaconBeam(it) }
            "renderQuad" -> JFunction { renderQuad(it) }
            "renderHologramBlock" -> JFunction { renderHologramBlock(it) }
            "renderBlock" -> JFunction { renderBlock(it) }
            "renderItem" -> JFunction { renderItem(it) }
            else -> null
        }
    }

    private fun getArgsIdx(l: Lua): Int {
        if (l.isTable(1) && l.isTable(2)) return 2
        if (l.isTable(1)) return 1
        return 0
    }

    // --- Исправленные методы извлечения (используем rawGet для безопасности) ---
    private fun Lua.optD(idx: Int, key: String, def: Double): Double {
        if (idx == 0) return def
        this.push(key)
        this.rawGet(idx)
        val res = if (this.isNumber(-1)) this.toNumber(-1) else def
        this.pop(1)
        return res
    }

    private fun Lua.optS(idx: Int, key: String, def: String): String {
        if (idx == 0) return def
        this.push(key)
        this.rawGet(idx)
        val res = this.toString(-1) ?: def
        this.pop(1)
        return res
    }

    private fun Lua.optF(idx: Int, key: String, def: Float): Float = optD(idx, key, def.toDouble()).toFloat()
    private fun Lua.optI(idx: Int, key: String, def: Int): Int = optD(idx, key, def.toDouble()).toInt()
    private fun Lua.optB(idx: Int, key: String, def: Boolean): Boolean {
        if (idx == 0) return def
        this.push(key)
        this.rawGet(idx)
        val res = if (this.isBoolean(-1)) this.toBoolean(-1) else def
        this.pop(1)
        return res
    }

    private fun renderBlock(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение координат и ID блока через вспомогательный метод optI
        // optI гарантирует извлечение Int и очистку стека (pop(1))
        val x = l.optI(idx, "x", 0)
        val y = l.optI(idx, "y", 0)
        val z = l.optI(idx, "z", 0)
        val id = l.optI(idx, "id", 1)

        // 3. Получаем BlockState по числовому ID
        val blockState = Block.stateById(id)

        // 4. Выполнение рендеринга блока
        try {
            if (blockState != null) {
                context.submitBlock(BlockPos(x, y, z), blockState)
                l.push(true) // Сообщаем об успехе
                return 1
            }
        } catch (e: Exception) {
            // Обработка возможных ошибок рендеринга
        }

        return 0 // Возвращаем nil, если блок не найден или произошла ошибка
    }

    private fun renderHologramBlock(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение координат (x, y, z) и ID блока
        // Используем optI, так как BlockPos и ID — целые числа
        val x = l.optI(idx, "x", 0)
        val y = l.optI(idx, "y", 0)
        val z = l.optI(idx, "z", 0)
        val id = l.optI(idx, "id", 1) // По умолчанию ID = 1 (обычно камень)

        // 3. Получение BlockState из Minecraft по ID
        val blockState = Block.stateById(id)

        // 4. Выполнение рендеринга голограммы
        try {
            if (blockState != null) {
                context.submitBlockHologram(BlockPos(x, y, z), blockState)
                l.push(true) // Возвращаем успех
                return 1
            }
        } catch (e: Exception) {
            // Логирование ошибки, если метод не поддерживается в текущей версии PrimitiveCollector
        }

        return 0 // Если блок не найден или произошла ошибка
    }

    private fun renderQuad(l: Lua): Int {
        // 1. Определяем индекс таблицы с аргументами (1 или 2)
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        val points = Array(4) { Vec3.ZERO }

        // 2. Читаем таблицу "points" (массив точек)
        l.push("points")
        l.rawGet(idx) // Кладем результат на индекс -1
        val isPointsArray = l.isTable(-1)

        if (isPointsArray) {
            // Читаем из массива: points = { {x,y,z}, {x,y,z}, ... }
            for (i in 1..4) {
                l.rawGetI(-1, i) // points[i] на индекс -1
                if (l.isTable(-1)) {
                    // Здесь индекс -1, так как это временная таблица точки на вершине стека
                    val px = l.optD(-1, "x", 0.0)
                    val py = l.optD(-1, "y", 0.0)
                    val pz = l.optD(-1, "z", 0.0)
                    points[i - 1] = Vec3(px, py, pz)
                }
                l.pop(1)
            }
        }
        l.pop(1) // Удаляем результат rawGet("points")

        // 3. Fallback: если массива "points" не было, ищем поля "point1", "point2" ...
        if (!isPointsArray) {
            for (i in 1..4) {
                l.push("point$i")
                l.rawGet(idx)
                if (l.isTable(-1)) {
                    val px = l.optD(-1, "x", 0.0)
                    val py = l.optD(-1, "y", 0.0)
                    val pz = l.optD(-1, "z", 0.0)
                    points[i - 1] = Vec3(px, py, pz)
                }
                l.pop(1)
            }
        }

        // 4. Извлекаем цвета и настройки (используем idx!)
        val red = l.optI(idx, "red", 255)
        val green = l.optI(idx, "green", 255)
        val blue = l.optI(idx, "blue", 255)
        val alpha = l.optI(idx, "alpha", 255)
        val throughWalls = l.optB(idx, "throughWalls", false)

        // 5. Подготовка данных
        val colorComponents = floatArrayOf(
            red.toFloat() / 255.0f,
            green.toFloat() / 255.0f,
            blue.toFloat() / 255.0f
        )
        val alphaF = alpha.toFloat() / 255.0f

        // 6. Выполнение рендеринга
        return try {
            context.submitQuad(points, colorComponents, alphaF, throughWalls)
            1
        } catch (e: Exception) {
            0
        }
    }

    private fun renderBeaconBeam(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение координат блока (x, y, z)
        // Используем optI, так как BlockPos принимает целые числа
        val x = l.optI(idx, "x", 0)
        val y = l.optI(idx, "y", 0)
        val z = l.optI(idx, "z", 0)

        // 3. Извлечение компонентов цвета (0-255)
        val red = l.optI(idx, "red", 255)
        val green = l.optI(idx, "green", 255)
        val blue = l.optI(idx, "blue", 255)

        // 4. Подготовка массива цветов для Minecraft (нормализация 0.0 - 1.0)
        val colorComponents = floatArrayOf(
            red.toFloat() / 255.0f,
            green.toFloat() / 255.0f,
            blue.toFloat() / 255.0f
        )

        // 5. Выполнение рендеринга
        try {
            context.submitBeaconBeam(BlockPos(x, y, z), colorComponents)
            l.push(true) // Возвращаем успех в Lua
            return 1
        } catch (e: Exception) {
            // В случае ошибки (например, если мир не загружен)
            return 0
        }
    }

    private fun renderCylinder(l: Lua): Int {
        // 1. Проверка аргумента (таблица) и наличия контекста рендеринга
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение координат основания (x, y, z)
        val x = l.optD(idx, "x", 0.0)
        val y = l.optD(idx, "y", 0.0)
        val z = l.optD(idx, "z", 0.0)

        // 3. Извлечение параметров геометрии (радиус, высота, сегменты)
        val radius = l.optD(idx, "radius", 1.0).toFloat()
        val height = l.optD(idx, "height", 1.0).toFloat()
        val segments = l.optI(idx, "segments", 8) // В оригинале был 1, но для цилиндра лучше 8+

        // 4. Извлечение цвета (RGBA)
        val red = l.optI(idx, "red", 255)
        val green = l.optI(idx, "green", 255)
        val blue = l.optI(idx, "blue", 255)
        val alpha = l.optI(idx, "alpha", 255)

        // 5. Проверка отрисовки сквозь стены
        val throughWalls = l.optB(idx, "through_walls", true)

        // 6. Выполнение рендеринга
        try {
            context.submitCylinder(
                Vec3(x, y, z),
                radius,
                height,
                segments,
                getArgb(alpha, red, green, blue),
                throughWalls
            )
            l.push(true) // Возвращаем успех
            return 1
        } catch (e: Exception) {
            l.push(false)
            return 1
        }
    }

    private fun renderOutlineCircle(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение координат центра (x, y, z)
        val x = l.optD(idx, "x", 0.0)
        val y = l.optD(idx, "y", 0.0)
        val z = l.optD(idx, "z", 0.0)

        // 3. Извлечение параметров геометрии (радиус, сегменты, толщина линии)
        val radius = l.optD(idx, "radius", 1.0).toFloat()
        val segments = l.optI(idx, "segments", 8)
        val thickness = l.optD(idx, "line_width", 1.0).toFloat()

        // 4. Извлечение цвета (RGBA)
        val red = l.optI(idx, "red", 255)
        val green = l.optI(idx, "green", 255)
        val blue = l.optI(idx, "blue", 255)
        val alpha = l.optI(idx, "alpha", 255)

        // 5. Проверка отрисовки сквозь стены
        val throughWalls = l.optB(idx, "through_walls", true)

        // 6. Выполнение рендеринга
        try {
            context.submitOutlinedCircle(
                Vec3(x, y, z),
                radius,
                thickness,
                segments,
                getArgb(alpha, red, green, blue),
                throughWalls
            )
            l.push(true) // Возвращаем успех
            return 1
        } catch (e: Exception) {
            l.push(false)
            return 1
        }
    }

    // --- Реализации функций рендеринга ---
    private fun renderOutline(l: Lua): Int {
        // 1. Получаем правильный индекс таблицы с аргументами (1 или 2)
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение координат первой точки (используем idx!)
        val x = l.optD(idx, "x", 0.0)
        val y = l.optD(idx, "y", 0.0)
        val z = l.optD(idx, "z", 0.0)

        // 3. Извлечение второй точки (x2, y2, z2) через безопасный поиск
        l.push("x2")
        l.rawGet(idx)
        val hasX2 = !l.isNil(-1)
        val x2 = if (hasX2) l.toNumber(-1) else 0.0
        l.pop(1)

        // Используем optD для y2 и z2 (они будут 0.0 если их нет, что нормально при hasX2 = false)
        val y2 = l.optD(idx, "y2", 0.0)
        val z2 = l.optD(idx, "z2", 0.0)

        // 4. Извлечение цвета и настроек линии (используем idx!)
        val r = l.optI(idx, "red", 255)
        val g = l.optI(idx, "green", 255)
        val b = l.optI(idx, "blue", 255)
        val a = l.optI(idx, "alpha", 255)

        val lineWidth = l.optF(idx, "line_width", 1.0f)
        val throughWalls = l.optB(idx, "through_walls", true)

        val colorComponents = floatArrayOf(
            r.toFloat() / 255.0f,
            g.toFloat() / 255.0f,
            b.toFloat() / 255.0f,
            a.toFloat() / 255.0f
        )

        // 5. Извлечение объекта Box (через rawGet для безопасности)
        l.push("box")
        l.rawGet(idx)
        val boxObj = l.toJavaObject(-1)
        l.pop(1)

        val box = when (boxObj) {
            is LuaBox -> boxObj.box
            is AABB -> boxObj
            else -> null
        }

        // 6. Логика выбора геометрии и рендеринг
        return try {
            if (box != null) {
                // Приоритет 1: Передан готовый объект AABB
                context.submitOutlinedBox(box, colorComponents, lineWidth, throughWalls)
            } else if (hasX2) {
                // Приоритет 2: Переданы две точки
                context.submitOutlinedBox(AABB(Vec3(x, y, z), Vec3(x2, y2, z2)), colorComponents, lineWidth, throughWalls)
            } else {
                // Приоритет 3: Отрисовка по координатам одного блока
                context.submitOutlinedBox(BlockPos(x.toInt(), y.toInt(), z.toInt()), colorComponents, lineWidth, throughWalls)
            }
            1
        } catch (e: Exception) {
            0
        }
    }


    private fun renderSphere(l: Lua): Int {
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        val x = l.optD(idx, "x", 0.0);
        val y = l.optD(idx, "y", 0.0);
        val z = l.optD(idx, "z", 0.0)

        val radius = l.optD(idx, "radius", 1.0)
        val seg = l.optI(idx, "segments", 8);
        val rings = l.optI(idx, "rings", 4)
        val r = l.optI(idx, "red", 255);
        val g = l.optI(idx, "green", 255);
        val b = l.optI(idx, "blue", 255);
        val a = l.optI(idx, "alpha", 255)
        val tw = l.optB(idx, "through_walls", true)
        context.submitSphere(Vec3(x, y, z), radius.toFloat(), seg, rings, getArgb(a, r, g, b), tw)
        return 0
    }

    private fun renderFilled(l: Lua): Int {
        // 1. Получаем правильный индекс таблицы с аргументами
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение первой точки (x, y, z)
        val x = l.optD(idx, "x", 0.0)
        val y = l.optD(idx, "y", 0.0)
        val z = l.optD(idx, "z", 0.0)

        // 3. Проверка наличия второй точки (x2, y2, z2) через безопасный rawGet
        l.push("x2")
        l.rawGet(idx)
        val hasX2 = !l.isNil(-1)
        val x2 = if (hasX2) l.toNumber(-1) else 0.0
        l.pop(1)

        val y2 = l.optD(idx, "y2", 0.0)
        val z2 = l.optD(idx, "z2", 0.0)

        // 4. Извлечение цветов и настроек (заменили 1 на idx!)
        val r = l.optI(idx, "red", 255)
        val g = l.optI(idx, "green", 255)
        val b = l.optI(idx, "blue", 255)
        val a = l.optI(idx, "alpha", 255)
        val tw = l.optB(idx, "through_walls", true)

        val colors = floatArrayOf(r / 255f, g / 255f, b / 255f)
        val alpha = a / 255f

        // 5. Извлечение объекта Box (используем idx!)
        l.push("box")
        l.rawGet(idx)
        val boxObj = l.toJavaObject(-1)
        l.pop(1)

        val box = when (boxObj) {
            is LuaBox -> boxObj.box
            is AABB -> boxObj
            else -> null
        }

        // 6. Выполнение рендеринга
        return try {
            if (box != null) {
                context.submitFilledBox(box, colors, alpha, tw)
            } else if (hasX2) {
                context.submitFilledBox(AABB(Vec3(x, y, z), Vec3(x2, y2, z2)), colors, alpha, tw)
            } else {
                context.submitFilledBox(BlockPos(x.toInt(), y.toInt(), z.toInt()), colors, alpha, tw)
            }
            1
        } catch (e: Exception) {
            0
        }
    }
    private fun renderItem(l: Lua): Int {
        val x = l.optD(1, "x", 0.0); val y = l.optD(1, "y", 0.0); val z = l.optD(1, "z", 0.0)
        val id = l.optS(1, "id", "minecraft:stone")
        context?.submitItem(Vec3d(x, y, z), Identifier.bySeparator(id, ':'))
        return 0
    }

    // --- Вспомогательные методы (Color, Texture) ---

    private fun getArgb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun renderText(l: Lua): Int {
        // 1. Проверка аргументов
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение параметров через наши opt-методы
        val x = l.optD(idx, "x", 0.0)
        val y = l.optD(idx, "y", 0.0)
        val z = l.optD(idx, "z", 0.0)
        val pos = Vec3(x, y, z)

        val text = l.optS(idx, "text", "Empty")
        val scale = l.optF(idx, "scale", 1f)
        val throughWalls = l.optB(idx, "through_walls", true)
        val component = Component.literal(text)

        // 3. Цвет
        val red = l.optI(idx, "red", -1)
        val green = l.optI(idx, "green", -1)
        val blue = l.optI(idx, "blue", -1)

        val color: Int = if (red in 0..255 && green in 0..255 && blue in 0..255) {
            (255 shl 24) or (red shl 16) or (green shl 8) or blue
        } else {
            CommonColors.WHITE
        }

        // 4. Вращение (Quaternion)
        val qx = l.optD(idx, "qx", 0.0)
        val qy = l.optD(idx, "qy", 0.0)
        val qz = l.optD(idx, "qz", 0.0)
        val qw = l.optD(idx, "qw", 0.0)

        val hasRotation = (qx != 0.0 || qy != 0.0 || qz != 0.0 || qw != 0.0)

        // 5. Вызов контекста рендеринга
        if (hasRotation) {
            val quaternion = Quaternionf(qx.toFloat(), qy.toFloat(), qz.toFloat(), qw.toFloat())
            context.submitText(
                component,
                pos,
                color,
                scale,
                0.5f,
                quaternion,
                throughWalls
            )
        } else {
            context.submitText(
                component,
                pos,
                color,
                scale,
                0.5f,
                throughWalls
            )
        }

        // 6. Возвращаем true в Lua
        l.push(true)
        return 1
    }

    private fun renderLinesFromPoints(l: Lua): Int {
        // 1. Получаем правильный индекс таблицы с аргументами (1 или 2)
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Получаем таблицу "points" из аргументов (используем idx и rawGet!)
        l.push("points")
        l.rawGet(idx) // Кладем таблицу на вершину стека (-1)
        if (!l.isTable(-1)) {
            l.pop(1)
            return 0
        }

        val pointsList = mutableListOf<Vec3>()

        // 3. Итерация по массиву точек (0-based и 1-based)
        var i = 1
        var triedZero = false

        while (true) {
            l.rawGetI(-1, i) // Берем points[i] на индекс -1. Таблица points теперь на -2.

            if (!l.isTable(-1)) {
                l.pop(1) // Удаляем результат (не-таблицу)

                if (i == 1 && !triedZero) {
                    i = 0
                    triedZero = true
                    continue
                }
                break // Конец массива
            }

            // Извлекаем x, y, z из таблицы конкретной точки (она на индексе -1)
            val px = l.optD(-1, "x", 0.0)
            val py = l.optD(-1, "y", 0.0)
            val pz = l.optD(-1, "z", 0.0)
            pointsList.add(Vec3(px, py, pz))

            l.pop(1) // Удаляем таблицу точки со стека

            if (i == 0) i = 1 else i++
        }

        l.pop(1) // Удаляем саму таблицу "points" со стека

        // 4. Проверяем, достаточно ли точек
        if (pointsList.size < 2) return 0

        // 5. Извлекаем остальные параметры (ЗАМЕНИЛИ 1 НА idx!)
        val red = l.optI(idx, "red", 255)
        val green = l.optI(idx, "green", 255)
        val blue = l.optI(idx, "blue", 255)
        val alpha = l.optI(idx, "alpha", 255)

        val lineWidth = l.optF(idx, "line_width", 1.0f)
        val throughWalls = l.optB(idx, "through_walls", true)

        // 6. Выполнение рендеринга
        return try {
            context.submitLinesFromPoints(
                pointsList.toTypedArray(),
                floatArrayOf(red.toFloat() / 255.0f, green.toFloat() / 255.0f, blue.toFloat() / 255.0f),
                alpha.toFloat() / 255.0f,
                lineWidth,
                throughWalls
            )
            1
        } catch (e: Exception) {
            0
        }
    }

    private fun renderLineFromCursor(l: Lua): Int {
        // 1. Получаем индекс таблицы с аргументами (1 или 2)
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение координат (используем idx!)
        val x = l.optD(idx, "x", 0.0)
        val y = l.optD(idx, "y", 0.0)
        val z = l.optD(idx, "z", 0.0)

        // 3. Извлечение компонентов цвета
        val red = l.optI(idx, "red", 255)
        val green = l.optI(idx, "green", 255)
        val blue = l.optI(idx, "blue", 255)
        val alpha = l.optI(idx, "alpha", 255)

        // 4. Извлечение толщины линии
        val lineWidth = l.optF(idx, "line_width", 1.0f)

        // 5. Выполнение рендеринга
        return try {
            context.submitLineFromCursor(
                Vec3(x, y, z),
                floatArrayOf(red.toFloat() / 255.0f, green.toFloat() / 255.0f, blue.toFloat() / 255.0f),
                alpha.toFloat() / 255.0f,
                lineWidth
            )
            1
        } catch (e: Exception) {
            0
        }
    }

    private fun renderImage(l: Lua): Int {
        // 1. Получаем индекс таблицы с аргументами
        val idx = getArgsIdx(l)
        if (idx == 0 || context == null) return 0

        // 2. Извлечение обязательного пути к файлу (используем idx!)
        val path = l.optS(idx, "path", "")
        if (path.isEmpty()) return 0

        // 3. Извлечение координат и смещений
        val x = l.optD(idx, "x", 0.0)
        val y = l.optD(idx, "y", 0.0)
        val z = l.optD(idx, "z", 0.0)
        val ox = l.optD(idx, "offset_x", 0.0)
        val oy = l.optD(idx, "offset_y", 0.0)
        val oz = l.optD(idx, "offset_z", 0.0)

        // 4. Извлечение размеров изображения и UV
        val width = l.optF(idx, "width", 0f)
        val height = l.optF(idx, "height", 0f)
        val regionWidth = l.optF(idx, "region_width", 1f)
        val regionHeight = l.optF(idx, "region_height", 1f)

        // 5. Извлечение цветов
        val r = l.optI(idx, "red", 255).toFloat() / 255f
        val g = l.optI(idx, "green", 255).toFloat() / 255f
        val b = l.optI(idx, "blue", 255).toFloat() / 255f
        val alpha = l.optI(idx, "alpha", 255).toFloat() / 255f

        val throughWalls = l.optB(idx, "through_walls", true)

        // 6. Загрузка и рендеринг
        return try {
            val identifier = loadTexture(path)
            if (identifier != null) {
                context.submitTexturedQuad(
                    Vec3(x, y, z),
                    width,
                    height,
                    regionWidth,
                    regionHeight,
                    Vec3(ox, oy, oz),
                    identifier,
                    floatArrayOf(r, g, b),
                    alpha,
                    throughWalls
                )
                1
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Загружает текстуру из файла и возвращает её Identifier
     */
    private fun loadTexture(path: String): Identifier? {
        val scriptCacheId = "wd_global"

        // Проверяем кэш для текущего скрипта
        val scriptCache = TwoRenderObject.Companion.textureCache.getOrPut(scriptCacheId) { ConcurrentHashMap() }
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
                val textureName = "hypixelcry:texture_${scriptCacheId}_${TwoRenderObject.Companion.textureCounter.getAndIncrement()}"
                val texture = DynamicTexture(
                    Supplier { textureName },
                    nativeImage
                )

                // Создаем идентификатор
                val identifier = Identifier.fromNamespaceAndPath("hypixelcry", "texture_${scriptCacheId}_${TwoRenderObject.Companion.textureCounter.get()}")
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
}
