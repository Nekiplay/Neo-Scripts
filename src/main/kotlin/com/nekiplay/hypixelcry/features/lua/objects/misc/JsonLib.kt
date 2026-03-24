package com.nekiplay.hypixelcry.features.lua.objects.misc

import com.nekiplay.hypixelcry.HypixelCry
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua

class JsonLib(val L: Lua) {

    fun register() {
        L.newTable() // Создаем таблицу json

        L.push(JFunction { parse(it) })
        L.setField(-2, "parse")

        L.push(JFunction { stringify(it) })
        L.setField(-2, "stringify")

        L.setGlobal("json")
    }

    private fun parse(l: Lua): Int {
        val jsonString = l.toString(1) ?: run {
            l.pushNil()
            return 1
        }

        return try {
            // Используем GSON из вашего проекта
            val result: Any? = HypixelCry.GSON_COMPACT.fromJson(jsonString, Any::class.java)
            pushJavaToLua(l, result)
            1
        } catch (e: Exception) {
            l.pushNil()
            1
        }
    }

    private fun stringify(l: Lua): Int {
        try {
            if (l.isNoneOrNil(1)) {
                l.pushNil()
                return 1
            }

            val javaObj = luaToJava(l, 1)

            // Логика indent (аргумент 2)
            val jsonString = if (l.isNoneOrNil(2)) {
                // По умолчанию - форматированный
                HypixelCry.GSON.toJson(javaObj)
            } else {
                // Если indent == 0 или indent == false -> компактный
                val isCompact = (l.isNumber(2) && l.toNumber(2) == 0.0) ||
                        (l.isBoolean(2) && !l.toBoolean(2))

                if (isCompact) {
                    HypixelCry.GSON_COMPACT.toJson(javaObj)
                } else {
                    HypixelCry.GSON.toJson(javaObj)
                }
            }

            l.push(jsonString)
            return 1
        } catch (e: Exception) {
            l.pushNil()
            return 1
        }
    }

    // --- Из Java (GSON) в Lua ---
    private fun pushJavaToLua(l: Lua, value: Any?) {
        when (value) {
            null -> l.pushNil()
            is String -> l.push(value)
            is Number -> l.push(value.toDouble())
            is Boolean -> l.push(value)
            is Map<*, *> -> {
                l.newTable()
                value.forEach { (k, v) ->
                    // Ключ
                    pushJavaToLua(l, k)
                    // Значение
                    pushJavaToLua(l, v)
                    // t[k] = v
                    l.setTable(-3)
                }
            }
            is List<*> -> {
                l.newTable()
                value.forEachIndexed { index, v ->
                    pushJavaToLua(l, v)
                    l.rawSetI(-2, index + 1)
                }
            }
            else -> l.push(value.toString())
        }
    }

    // --- Из Lua в Java (GSON) ---
    private fun luaToJava(l: Lua, index: Int): Any? {
        return when (l.type(index)) {
            Lua.LuaType.NIL -> null
            Lua.LuaType.BOOLEAN -> l.toBoolean(index)
            Lua.LuaType.NUMBER -> l.toNumber(index)
            Lua.LuaType.STRING -> l.toString(index)
            Lua.LuaType.TABLE -> {
                if (isTableArray(l, index)) {
                    val list = mutableListOf<Any?>()
                    val len = l.rawLength(index)
                    for (i in 1..len) {
                        l.rawGetI(index, i)
                        list.add(luaToJava(l, -1))
                        l.pop(1)
                    }
                    list
                } else {
                    val map = mutableMapOf<String, Any?>()
                    l.pushNil() // Начальный ключ для next
                    while (l.next(index) != 0) {
                        // Ключ на -2, значение на -1
                        val keyStr = if (l.isNumber(-2)) {
                            l.toNumber(-2).toString().replace(".0", "")
                        } else {
                            l.toString(-2) ?: "null"
                        }
                        map[keyStr] = luaToJava(l, -1)
                        l.pop(1) // Удаляем значение, оставляем ключ
                    }
                    map
                }
            }
            else -> l.toString(index)
        }
    }

    /**
     * Ваша логика isArray: таблица считается массивом, если в ней только
     * последовательные числовые ключи от 1 до N.
     */
    private fun isTableArray(l: Lua, index: Int): Boolean {
        val len = l.rawLength(index)
        if (len == 0) return false

        var count = 0
        l.pushNil()
        while (l.next(index) != 0) {
            // Если ключ не число - это объект
            if (!l.isNumber(-2)) {
                l.pop(2) // удаляем ключ и значение
                return false
            }

            val keyNum = l.toNumber(-2)
            // Если ключ не целое число или не в диапазоне массива
            if (keyNum != Math.floor(keyNum) || keyNum < 1.0 || keyNum > len.toDouble()) {
                l.pop(2)
                return false
            }

            count++
            l.pop(1)
        }

        // Массив, если количество элементов совпадает с длиной #table
        return count == len
    }
}