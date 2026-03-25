package com.nekiplay.hypixelcry.features.lua.objects.modules

import com.nekiplay.hypixelcry.features.esp.pathfinder.PathFinderWorker
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import net.minecraft.core.BlockPos
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua

class PathFinderRendererObject(L: Lua?) : SimpleLuaWrapper(L) {

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "isHasPath" -> JFunction { isHasPath(it) }
            "removePath" -> JFunction { removePath(it) }
            "addOrUpdatePath" -> JFunction { addOrUpdatePath(it) }
            "getPathBlocks" -> JFunction { getPathBlocks(it) }
            else -> null
        }
    }

    private fun isHasPath(l: Lua): Int {
        if (l.isString(1)) {
            val id = l.toString(1) ?: ""
            l.push(PathFinderWorker.hasPath(id))
            return 1
        }
        l.pushNil()
        return 1
    }

    private fun removePath(l: Lua): Int {
        if (l.isString(1)) {
            val id = l.toString(1) ?: ""
            if (PathFinderWorker.hasPath(id)) {
                PathFinderWorker.removePath(id)
                l.push(true)
            } else {
                l.push(false)
            }
            return 1
        }
        l.pushNil()
        return 1
    }

    private fun addOrUpdatePath(l: Lua): Int {
        if (l.isTable(1)) {
            val t = 1 // Индекс таблицы-конфига в стеке

            // Читаем ID
            l.getField(t, "id")
            val id = if (l.isString(-1)) l.toString(-1)!! else "empty"
            l.pop(1)

            // Читаем координаты
            l.getField(t, "x"); val x = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 0; l.pop(1)
            l.getField(t, "y"); val y = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 0; l.pop(1)
            l.getField(t, "z"); val z = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 0; l.pop(1)

            // Читаем цвета
            l.getField(t, "red"); val r = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 0; l.pop(1)
            l.getField(t, "green"); val g = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 0; l.pop(1)
            l.getField(t, "blue"); val b = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 0; l.pop(1)
            l.getField(t, "alpha"); val a = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 255; l.pop(1)

            // Читаем настройки
            l.getField(t, "smooth"); val smooth = if (l.isBoolean(-1)) l.toBoolean(-1) else false; l.pop(1)
            l.getField(t, "updater"); val updater = if (l.isBoolean(-1)) l.toBoolean(-1) else true; l.pop(1)
            l.getField(t, "end_text"); val endText = if (l.isString(-1)) l.toString(-1)!! else "empty"; l.pop(1)

            val colorComponents = floatArrayOf(
                r.toFloat() / 255.0f,
                g.toFloat() / 255.0f,
                b.toFloat() / 255.0f,
                a.toFloat() / 255.0f
            )

            PathFinderWorker.addOrUpdatePath(
                id, BlockPos(x, y, z),
                colorComponents,
                endText,
                smooth,
                updater
            )
            l.push(true)
            return 1
        }
        l.pushNil()
        return 1
    }

    private fun getPathBlocks(l: Lua): Int {
        if (l.isString(1)) {
            val id = l.toString(1) ?: ""
            val list = PathFinderWorker.getPathBlocks(id)

            l.newTable() // Создаем основную таблицу
            list.forEachIndexed { index, item ->
                l.newTable() // Создаем таблицу-элемент {x, y, z}

                l.push(item.x.toDouble()); l.setField(-2, "x")
                l.push(item.y.toDouble()); l.setField(-2, "y")
                l.push(item.z.toDouble()); l.setField(-2, "z")

                // Сохраняем логику: в оригинале index начинался с 0
                l.rawSetI(-2, index)
            }
            return 1
        }
        l.pushNil()
        return 1
    }
}