package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import org.luaj.vm2.LuaValue

class LuaLong(val value: Long) : LuaValue() {
    override fun type(): Int = LuaValue.TUSERDATA
    override fun typename(): String = "long"
    override fun tojstring(): String = value.toString()
    override fun toString(): String = value.toString()

    override fun todouble(): Double = value.toDouble()
    override fun toint(): Int = value.toInt()
    override fun tolong(): Long = value

    // ИСПРАВЛЕНО: Используем конструктор LuaLong вместо valueOf
    override fun add(rhs: LuaValue): LuaValue = LuaLong(value + rhs.tolong())
    override fun sub(rhs: LuaValue): LuaValue = LuaLong(value - rhs.tolong())

    override fun checklong(): Long = value
}