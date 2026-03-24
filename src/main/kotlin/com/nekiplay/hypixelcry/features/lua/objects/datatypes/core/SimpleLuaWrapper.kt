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

    open fun push(): LuaValue {
        // 1. Create a table to represent our object in Lua
        L.newTable() 
        val tableIdx = L.getTop()
    
        // 2. Store the Java wrapper inside the table with a hidden key
        L.pushJavaObject(this)
        L.setField(tableIdx, "__java_instance")
    
        // 3. Create the metatable
        L.newTable()
    
        // __index logic
        L.push(JFunction { l ->
            // Get the Java instance from the table (index 1 is the table)
            l.getField(1, "__java_instance")
            val wrapper = l.toJavaObject(-1) as? SimpleLuaWrapper
            l.pop(1) // Remove java_instance from stack
    
            val key = l.toString(2) ?: ""
            l.smartPush(wrapper?.getFieldValue(l, key))
            1
        })
        L.setField(-2, "__index")
    
        // __newindex logic
        L.push(JFunction { l ->
            l.getField(1, "__java_instance")
            val wrapper = l.toJavaObject(-1) as? SimpleLuaWrapper
            l.pop(1)
    
            val key = l.toString(2) ?: ""
            l.pushValue(3)
            val value = l.get()
            wrapper?.setFieldValue(l, key, value)
            0
        })
        L.setField(-2, "__newindex")
    
        L.setMetatable(tableIdx)
        
        // Return the table as a LuaValue
        L.pushValue(tableIdx)
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
        is JFunction -> this.push(v) 
        // Для всех остальных Java-объектов используем pushJava
        else -> this.pushJavaObject(v)
    }
}
