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

class WorldRendererObject(val l: Lua, private val context: PrimitiveCollector?): SimpleLuaWrapper(l) {
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

    // --- Вспомогательные методы извлечения данных из таблицы на стеке ---
    private fun Lua.optD(idx: Int, key: String, def: Double): Double {
        this.getField(idx, key)
        val res = if (this.isNumber(-1)) this.toNumber(-1) else def
        this.pop(1)
        return res
    }

    private fun Lua.optI(idx: Int, key: String, def: Int): Int {
        this.getField(idx, key)
        val res = if (this.isNumber(-1)) this.toInteger(-1).toInt() else def
        this.pop(1)
        return res
    }

    private fun Lua.optB(idx: Int, key: String, def: Boolean): Boolean {
        this.getField(idx, key)
        val res = if (this.isBoolean(-1)) this.toBoolean(-1) else def
        this.pop(1)
        return res
    }

    private fun Lua.optS(idx: Int, key: String, def: String): String {
        this.getField(idx, key)
        val res = this.toString(-1) ?: def
        this.pop(1)
        return res
    }

    private fun Lua.optF(idx: Int, key: String, def: Float): Float {
        this.getField(idx, key)
        val res = if (this.isNumber(-1)) this.toNumber(-1).toFloat() else def
        this.pop(1)
        return res
    }

    private fun renderBlock(l: Lua): Int {
        // 1. Проверка аргумента (должна быть таблица) и контекста рендеринга
        if (!l.isTable(1) || context == null) {
            return 0 // Возвращаем nil в Lua
        }

        // 2. Извлечение координат и ID блока через вспомогательный метод optI
        // optI гарантирует извлечение Int и очистку стека (pop(1))
        val x = l.optI(1, "x", 0)
        val y = l.optI(1, "y", 0)
        val z = l.optI(1, "z", 0)
        val id = l.optI(1, "id", 1)

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
        // 1. Проверка аргумента (таблица) и наличия контекста рендеринга
        if (!l.isTable(1) || context == null) {
            return 0 // Возвращаем nil в Lua
        }

        // 2. Извлечение координат (x, y, z) и ID блока
        // Используем optI, так как BlockPos и ID — целые числа
        val x = l.optI(1, "x", 0)
        val y = l.optI(1, "y", 0)
        val z = l.optI(1, "z", 0)
        val id = l.optI(1, "id", 1) // По умолчанию ID = 1 (обычно камень)

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
        // 1. Проверка аргумента (таблица) и контекста
        if (!l.isTable(1) || context == null) {
            return 0
        }

        val points = Array(4) { Vec3.ZERO }

        // 2. Пытаемся прочитать массив "points"
        l.getField(1, "points") // Кладём таблицу points на индекс -1
        val isPointsArray = l.isTable(-1)

        if (isPointsArray) {
            // Читаем из массива: points = { {x,y,z}, {x,y,z}, ... }
            for (i in 1..4) {
                l.rawGetI(-1, i) // Берем points[i] на индекс -1, сама таблица points теперь на -2
                if (l.isTable(-1)) {
                    val px = l.optD(-1, "x", 0.0)
                    val py = l.optD(-1, "y", 0.0)
                    val pz = l.optD(-1, "z", 0.0)
                    points[i - 1] = Vec3(px, py, pz)
                }
                l.pop(1) // Удаляем таблицу конкретной точки
            }
        }
        l.pop(1) // Удаляем таблицу "points" (или nil) со стека

        // 3. Если массива "points" не было, ищем поля "point1", "point2" ...
        if (!isPointsArray) {
            for (i in 1..4) {
                l.getField(1, "point$i") // Кладем таблицу pointN на индекс -1
                if (l.isTable(-1)) {
                    val px = l.optD(-1, "x", 0.0)
                    val py = l.optD(-1, "y", 0.0)
                    val pz = l.optD(-1, "z", 0.0)
                    points[i - 1] = Vec3(px, py, pz)
                }
                l.pop(1)
            }
        }

        // 4. Извлекаем цвета и настройки
        val red = l.optI(1, "red", 255)
        val green = l.optI(1, "green", 255)
        val blue = l.optI(1, "blue", 255)
        val alpha = l.optI(1, "alpha", 255)

        val throughWalls = l.optB(1, "throughWalls", false)

        // 5. Подготовка данных для Minecraft
        val colorComponents = floatArrayOf(
            red.toFloat() / 255.0f,
            green.toFloat() / 255.0f,
            blue.toFloat() / 255.0f
        )
        val alphaF = alpha.toFloat() / 255.0f

        // 6. Выполнение рендеринга
        try {
            context.submitQuad(points, colorComponents, alphaF, throughWalls)
            l.push(true)
            return 1
        } catch (e: Exception) {
            return 0
        }
    }

    private fun renderBeaconBeam(l: Lua): Int {
        // 1. Проверка аргумента (таблица) и наличия контекста рендеринга
        if (!l.isTable(1) || context == null) {
            return 0 // Возвращаем nil в Lua
        }

        // 2. Извлечение координат блока (x, y, z)
        // Используем optI, так как BlockPos принимает целые числа
        val x = l.optI(1, "x", 0)
        val y = l.optI(1, "y", 0)
        val z = l.optI(1, "z", 0)

        // 3. Извлечение компонентов цвета (0-255)
        val red = l.optI(1, "red", 255)
        val green = l.optI(1, "green", 255)
        val blue = l.optI(1, "blue", 255)

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
        if (!l.isTable(1) || context == null) {
            return 0 // Возвращаем nil в Lua
        }

        // 2. Извлечение координат основания (x, y, z)
        val x = l.optD(1, "x", 0.0)
        val y = l.optD(1, "y", 0.0)
        val z = l.optD(1, "z", 0.0)

        // 3. Извлечение параметров геометрии (радиус, высота, сегменты)
        val radius = l.optD(1, "radius", 1.0).toFloat()
        val height = l.optD(1, "height", 1.0).toFloat()
        val segments = l.optI(1, "segments", 8) // В оригинале был 1, но для цилиндра лучше 8+

        // 4. Извлечение цвета (RGBA)
        val red = l.optI(1, "red", 255)
        val green = l.optI(1, "green", 255)
        val blue = l.optI(1, "blue", 255)
        val alpha = l.optI(1, "alpha", 255)

        // 5. Проверка отрисовки сквозь стены
        val throughWalls = l.optB(1, "through_walls", true)

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
        // 1. Проверка аргумента (таблица) и наличия контекста рендеринга
        if (!l.isTable(1) || context == null) {
            return 0 // Возвращаем nil в Lua
        }

        // 2. Извлечение координат центра (x, y, z)
        val x = l.optD(1, "x", 0.0)
        val y = l.optD(1, "y", 0.0)
        val z = l.optD(1, "z", 0.0)

        // 3. Извлечение параметров геометрии (радиус, сегменты, толщина линии)
        val radius = l.optD(1, "radius", 1.0).toFloat()
        val segments = l.optI(1, "segments", 8)
        val thickness = l.optD(1, "line_width", 1.0).toFloat()

        // 4. Извлечение цвета (RGBA)
        val red = l.optI(1, "red", 255)
        val green = l.optI(1, "green", 255)
        val blue = l.optI(1, "blue", 255)
        val alpha = l.optI(1, "alpha", 255)

        // 5. Проверка отрисовки сквозь стены
        val throughWalls = l.optB(1, "through_walls", true)

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
        // 1. Проверка аргумента (должна быть таблица) и контекста
        if (!l.isTable(1) || context == null) {
            return 0
        }

        // 2. Извлечение базовых координат
        val x = l.optD(1, "x", 0.0)
        val y = l.optD(1, "y", 0.0)
        val z = l.optD(1, "z", 0.0)

        // 3. Извлечение второй точки (x2, y2, z2)
        l.getField(1, "x2")
        val hasX2 = !l.isNil(-1)
        val x2 = if (hasX2) l.toNumber(-1) else 0.0
        l.pop(1)

        l.getField(1, "y2"); val y2 = l.toNumber(-1); l.pop(1)
        l.getField(1, "z2"); val z2 = l.toNumber(-1); l.pop(1)

        // 4. Извлечение цвета и настроек линии
        val r = l.optI(1, "red", 255)
        val g = l.optI(1, "green", 255)
        val b = l.optI(1, "blue", 255)
        val a = l.optI(1, "alpha", 255)

        val lineWidth = l.optF(1, "line_width", 1.0f)
        val throughWalls = l.optB(1, "through_walls", true)

        // Подготовка массива цветов для Minecraft (от 0.0 до 1.0)
        val colorComponents = floatArrayOf(
            r.toFloat() / 255.0f,
            g.toFloat() / 255.0f,
            b.toFloat() / 255.0f,
            a.toFloat() / 255.0f
        )

        // 5. Извлечение объекта Box (если он передан)
        l.getField(1, "box")
        val boxObj = l.toJavaObject(-1)
        l.pop(1)

        val box = when (boxObj) {
            is LuaBox -> boxObj.box
            is AABB -> boxObj
            else -> null
        }

        // 6. Логика выбора геометрии для отрисовки
        try {
            if (box != null) {
                // Приоритет 1: Передан готовый объект AABB/LuaBox
                context.submitOutlinedBox(box, colorComponents, lineWidth, throughWalls)
            } else if (hasX2) {
                // Приоритет 2: Переданы две точки (x,y,z) и (x2,y2,z2)
                context.submitOutlinedBox(AABB(Vec3(x, y, z), Vec3(x2, y2, z2)), colorComponents, lineWidth, throughWalls)
            } else {
                // Приоритет 3: Отрисовка одного блока по координатам x, y, z
                context.submitOutlinedBox(BlockPos(x.toInt(), y.toInt(), z.toInt()), colorComponents, lineWidth, throughWalls)
            }
            l.push(true)
        } catch (e: Exception) {
            l.push(false)
        }

        return 1
    }


    private fun renderSphere(l: Lua): Int {
        val x = l.optD(1, "x", 0.0); val y = l.optD(1, "y", 0.0); val z = l.optD(1, "z", 0.0)
        val radius = l.optD(1, "radius", 1.0)
        val seg = l.optI(1, "segments", 8); val rings = l.optI(1, "rings", 4)
        val r = l.optI(1, "red", 255); val g = l.optI(1, "green", 255); val b = l.optI(1, "blue", 255); val a = l.optI(1, "alpha", 255)
        val tw = l.optB(1, "through_walls", true)
        context?.submitSphere(Vec3(x, y, z), radius.toFloat(), seg, rings, getArgb(a, r, g, b), tw)
        return 0
    }

    private fun renderFilled(l: Lua): Int {
        val x = l.optD(1, "x", 0.0); val y = l.optD(1, "y", 0.0); val z = l.optD(1, "z", 0.0)

        l.getField(1, "x2"); val hasX2 = !l.isNil(-1); val x2 = l.toNumber(-1); l.pop(1)
        l.getField(1, "y2"); val y2 = l.toNumber(-1); l.pop(1)
        l.getField(1, "z2"); val z2 = l.toNumber(-1); l.pop(1)

        val r = l.optI(1, "red", 255); val g = l.optI(1, "green", 255); val b = l.optI(1, "blue", 255); val a = l.optI(1, "alpha", 255)
        val tw = l.optB(1, "through_walls", true)

        val colors = floatArrayOf(r/255f, g/255f, b/255f)
        val alpha = a/255f

        l.getField(1, "box")
        val boxObj = l.toJavaObject(-1)
        l.pop(1)

        val box = when (boxObj) {
            is LuaBox -> boxObj.box
            is AABB -> boxObj
            else -> null
        }

        if (box != null) {
            context?.submitFilledBox(box, colors, alpha, tw)
        } else if (hasX2) {
            context?.submitFilledBox(AABB(Vec3(x, y, z), Vec3(x2, y2, z2)), colors, alpha, tw)
        } else {
            context?.submitFilledBox(BlockPos(x.toInt(), y.toInt(), z.toInt()), colors, alpha, tw)
        }
        return 0
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
        if (!l.isTable(1) || context == null) {
            return 0 // Возвращаем nil в Lua
        }

        // 2. Извлечение параметров через наши opt-методы
        val x = l.optD(1, "x", 0.0)
        val y = l.optD(1, "y", 0.0)
        val z = l.optD(1, "z", 0.0)
        val pos = Vec3(x, y, z)

        val text = l.optS(1, "text", "Empty")
        val scale = l.optF(1, "scale", 1f)
        val throughWalls = l.optB(1, "through_walls", true)
        val component = Component.literal(text)

        // 3. Цвет
        val red = l.optI(1, "red", -1)
        val green = l.optI(1, "green", -1)
        val blue = l.optI(1, "blue", -1)

        val color: Int = if (red in 0..255 && green in 0..255 && blue in 0..255) {
            (255 shl 24) or (red shl 16) or (green shl 8) or blue
        } else {
            CommonColors.WHITE
        }

        // 4. Вращение (Quaternion)
        val qx = l.optD(1, "qx", 0.0)
        val qy = l.optD(1, "qy", 0.0)
        val qz = l.optD(1, "qz", 0.0)
        val qw = l.optD(1, "qw", 0.0)

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
        // 1. Проверка аргументов
        if (!l.isTable(1) || context == null) {
            return 0
        }

        // 2. Получаем таблицу "points"
        l.getField(1, "points") // Кладём таблицу на индекс -1
        if (!l.isTable(-1)) {
            l.pop(1)
            return 0
        }

        val pointsList = mutableListOf<Vec3>()

        // 3. Итерация по массиву точек (поддержка 0-based и 1-based)
        var i = 1
        var triedZero = false

        while (true) {
            l.rawGetI(-1, i) // Берем points[i] на индекс -1. Таблица points смещается на -2.

            if (!l.isTable(-1)) {
                l.pop(1) // Удаляем результат (не-таблицу)

                // Если на индексе 1 ничего не нашли, пробуем индекс 0 (для 0-based массивов)
                if (i == 1 && !triedZero) {
                    i = 0
                    triedZero = true
                    continue
                }
                break // Конец массива
            }

            // Извлекаем x, y, z из таблицы точки (она на индексе -1)
            val px = l.optD(-1, "x", 0.0)
            val py = l.optD(-1, "y", 0.0)
            val pz = l.optD(-1, "z", 0.0)
            pointsList.add(Vec3(px, py, pz))

            l.pop(1) // Удаляем таблицу точки со стека

            // Логика инкремента индекса
            if (i == 0) i = 1 else i++
        }

        l.pop(1) // Удаляем таблицу "points" со стека

        // 4. Проверяем, достаточно ли точек для линии
        if (pointsList.size < 2) {
            l.push(false)
            return 1
        }

        // 5. Извлекаем остальные параметры (цвет, толщина, through_walls)
        val red = l.optI(1, "red", 255)
        val green = l.optI(1, "green", 255)
        val blue = l.optI(1, "blue", 255)
        val alpha = l.optI(1, "alpha", 255)

        val lineWidth = l.optF(1, "line_width", 1.0f)
        val throughWalls = l.optB(1, "through_walls", true)

        // 6. Подготовка компонентов цвета
        val colorComponents = floatArrayOf(
            red.toFloat() / 255.0f,
            green.toFloat() / 255.0f,
            blue.toFloat() / 255.0f
        )
        val alphaComponent = alpha.toFloat() / 255.0f

        // 7. Вызов метода рендеринга
        try {
            context.submitLinesFromPoints(
                pointsList.toTypedArray(),
                colorComponents,
                alphaComponent,
                lineWidth,
                throughWalls
            )
            l.push(true)
        } catch (e: Exception) {
            l.push(false)
        }

        return 1
    }

    private fun renderLineFromCursor(l: Lua): Int {
        // 1. Проверка аргумента (таблица) и контекста рендеринга
        if (!l.isTable(1) || context == null) {
            return 0 // Возвращаем nil в Lua
        }

        // 2. Извлечение координат (x, y, z)
        val x = l.optD(1, "x", 0.0)
        val y = l.optD(1, "y", 0.0)
        val z = l.optD(1, "z", 0.0)

        // 3. Извлечение компонентов цвета (RGBA)
        val red = l.optI(1, "red", 255)
        val green = l.optI(1, "green", 255)
        val blue = l.optI(1, "blue", 255)
        val alpha = l.optI(1, "alpha", 255)

        // 4. Извлечение толщины линии
        val lineWidth = l.optF(1, "line_width", 1.0f)

        // 5. Подготовка данных для Minecraft
        val colorComponents = floatArrayOf(
            red.toFloat() / 255.0f,
            green.toFloat() / 255.0f,
            blue.toFloat() / 255.0f
        )
        val alphaF = alpha.toFloat() / 255.0f
        val pos = Vec3(x, y, z)

        // 6. Вызов метода рендеринга в коллекторе
        try {
            context.submitLineFromCursor(
                pos,
                colorComponents,
                alphaF,
                lineWidth
            )
            l.push(true)
        } catch (e: Exception) {
            l.push(false)
        }

        return 1 // Возвращаем 1 результат на стеке (true/false)
    }

    private fun renderImage(l: Lua): Int {
        // 1. Проверка: передан ли аргумент-таблица и инициализирован ли контекст
        if (!l.isTable(1) || context == null) {
            return 0 // Возвращаем nil в Lua
        }

        // 2. Извлечение обязательного пути к файлу
        val path = l.optS(1, "path", "")
        if (path.isEmpty()) {
            return 0
        }

        // 3. Извлечение мировых координат
        val x = l.optD(1, "x", 0.0)
        val y = l.optD(1, "y", 0.0)
        val z = l.optD(1, "z", 0.0)

        // 4. Извлечение смещений (offsets)
        val ox = l.optD(1, "offset_x", 0.0)
        val oy = l.optD(1, "offset_y", 0.0)
        val oz = l.optD(1, "offset_z", 0.0)

        // 5. Извлечение размеров изображения
        val width = l.optF(1, "width", 0f)
        val height = l.optF(1, "height", 0f)

        // 6. Извлечение размеров региона текстуры (UV)
        val regionWidth = l.optF(1, "region_width", 1f)
        val regionHeight = l.optF(1, "region_height", 1f)

        // 7. Извлечение цветов (конвертируем 0-255 в 0.0-1.0)
        val r = l.optI(1, "red", 255).toFloat() / 255f
        val g = l.optI(1, "green", 255).toFloat() / 255f
        val b = l.optI(1, "blue", 255).toFloat() / 255f
        val alpha = l.optI(1, "alpha", 255).toFloat() / 255f

        // 8. Проверка отрисовки сквозь стены
        val throughWalls = l.optB(1, "through_walls", true)

        try {
            // 9. Загрузка текстуры через ваш метод (он возвращает Identifier)
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
                l.push(true) // Возвращаем успех
                return 1
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0 // Если текстура не загрузилась
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
