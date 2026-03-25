package com.nekiplay.hypixelcry.features.lua.objects.datatypes.core

import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

abstract class SimpleLuaWrapper(val L: Lua?) {
    abstract fun getFieldValue(l: Lua, key: String): Any?
    open fun setFieldValue(l: Lua, key: String, value: LuaValue): Boolean = false

    open fun push() {
        val lua = L ?: return
        val tableIdx = lua.getTop()
        lua.newTable()

        lua.pushJavaObject(this)
        lua.setField(tableIdx + 1, "__java_instance")

        lua.newTable()
        lua.push(JFunction { l ->
            l.getField(1, "__java_instance")
            val wrapper = l.toJavaObject(-1) as? SimpleLuaWrapper
            l.pop(1)
            val key = l.toString(2) ?: ""
            l.smartPush(wrapper?.getFieldValue(l, key))
            1
        })
        lua.setField(-2, "__index")

        lua.setMetatable(tableIdx + 1)
    }

    open fun pushValue(): LuaValue {
        push()
        return L?.get() ?: throw IllegalStateException("Lua instance not available")
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
        is SimpleLuaWrapper -> v.push()
        else -> this.pushJavaObject(v)
    }
}
