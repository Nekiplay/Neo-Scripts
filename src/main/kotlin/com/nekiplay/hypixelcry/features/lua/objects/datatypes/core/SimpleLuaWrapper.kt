package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

abstract class SimpleLuaWrapper(val L: Lua) {
    abstract fun getFieldValue(l: Lua, key: String): Any?
    open fun setFieldValue(l: Lua, key: String, value: LuaValue): Boolean = false

    open fun push(): LuaValue {
        L.newTable() 
        val tableIdx = L.getTop()

        // Store this Java object inside the table so the metatable can find it
        L.pushJavaObject(this)
        L.setField(tableIdx, "__java_instance")

        // Create the metatable
        L.newTable()

        // __index: Called when you do object.key
        L.push(JFunction { l ->
            l.getField(1, "__java_instance")
            val wrapper = l.toJavaObject(-1) as? SimpleLuaWrapper
            l.pop(1) // remove java instance from stack

            val key = l.toString(2) ?: ""
            l.smartPush(wrapper?.getFieldValue(l, key))
            1
        })
        L.setField(-2, "__index")

        // __newindex: Called when you do object.key = value
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
        return L.get() // Returns the table as a LuaValue and cleans stack
    }
}

fun Lua.smartPush(v: Any?) {
    when (v) {
        null -> this.pushNil()
        is Boolean -> this.push(v)
        is String -> this.push(v)
        is Double -> this.push(v)
        is Number -> this.push(v.toDouble())
        is JFunction -> this.push(v) // IMPORTANT: Must be before 'else'
        is LuaValue -> this.push(v)    // IMPORTANT: Allows nested objects
        else -> this.pushJavaObject(v)
    }
}
