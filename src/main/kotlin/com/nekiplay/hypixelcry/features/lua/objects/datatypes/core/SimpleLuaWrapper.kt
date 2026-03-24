package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

abstract class SimpleLuaWrapper(val L: Lua) {
    abstract fun getFieldValue(l: Lua, key: String): Any?
    open fun setFieldValue(l: Lua, key: String, value: LuaValue): Boolean = false

    open fun push() {
        L.newTable()
        val tableIdx = L.getTop()

        L.pushJavaObject(this)
        L.setField(tableIdx, "__java_instance")

        L.newTable()
        L.push(JFunction { l ->
            l.getField(1, "__java_instance")
            val wrapper = l.toJavaObject(-1) as? SimpleLuaWrapper
            l.pop(1)
            val key = l.toString(2) ?: ""
            l.smartPush(wrapper?.getFieldValue(l, key))
            1
        })
        L.setField(-2, "__index")

        L.setMetatable(tableIdx)
    }
}

fun Lua.smartPush(v: Any?) {
    when (v) {
        null -> this.pushNil()
        is Boolean -> this.push(v)
        is String -> this.push(v)
        is Double -> this.push(v)
        is Number -> this.push(v.toDouble())
        is JFunction -> this.push(v)
        is LuaValue -> this.push(v)
        is SimpleLuaWrapper -> this.push(v.push()) 
        else -> this.pushJavaObject(v)
    }
}
