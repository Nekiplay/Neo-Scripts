package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

abstract class SimpleLuaWrapper(val L: Lua) {
    /**
     * Здесь возвращайте: String, Number, Boolean, null или другой SimpleLuaWrapper
     */
    abstract fun getFieldValue(l: Lua, key: String): Any?

    open fun setFieldValue(l: Lua, key: String, value: LuaValue): Boolean = false

    fun push(): LuaValue {
        L.pushJavaObject(this)
        L.newTable()

        // __index (Чтение)
        L.push(JFunction { l ->
            val wrapper = l.toJavaObject(1) as? SimpleLuaWrapper
            val key = l.toString(2) ?: ""
            l.smartPush(wrapper?.getFieldValue(l, key))
            1
        })
        L.setField(-2, "__index")

        // __newindex (Запись) - ИСПРАВЛЕНО
        L.push(JFunction { l ->
            val wrapper = l.toJavaObject(1) as? SimpleLuaWrapper
            val key = l.toString(2) ?: ""

            // В Lua __newindex(table, key, value) аргументы: 1=table, 2=key, 3=value
            // Чтобы получить LuaValue из индекса 3:
            l.pushValue(3) // Копируем значение из индекса 3 на вершину стека
            val value = l.get() // Забираем вершину как LuaValue (стек очищается)

            wrapper?.setFieldValue(l, key, value)
            0
        })
        L.setField(-2, "__newindex")

        L.setMetatable(-2)
        return L.get()
    }
}

/**
 * Расширение для корректного пуша в Iroiro LuaJava 4.1.0
 */
fun Lua.smartPush(v: Any?) {
    when (v) {
        null -> this.pushNil()
        is Boolean -> this.push(v)
        is String -> this.push(v)
        is Double -> this.push(v)
        is Float -> this.push(v.toDouble())
        is Int -> this.push(v.toDouble())
        is Long -> this.push(v.toDouble())
        is LuaValue -> this.push(v)
        // Для всех остальных Java-объектов используем pushJava
        else -> this.pushJavaObject(v)
    }
}