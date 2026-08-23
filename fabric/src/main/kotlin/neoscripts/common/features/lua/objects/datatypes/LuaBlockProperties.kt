package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction

class LuaBlockProperties(private val state: LuaBlockState) : LuaUserdata(state.blockState) {
    private val properties: Map<String, Property<*>> =
        state.blockState.getProperties().associateBy { it.name }

    private val blockState: BlockState
        get() = state.blockState

    override fun get(key: LuaValue): LuaValue {
        val name = key.tojstring()
        if (name == "getAll") {
            return object : ZeroArgFunction() {
                override fun call(): LuaValue = getAll()
            }
        }
        val property = findProperty(name) ?: return NIL
        return getValue(property)
    }

    override fun set(key: LuaValue, value: LuaValue) {
        if (blockState.`is`(Blocks.AIR)) return
        val name = key.tojstring()
        if (name == "getAll") return
        val property = findProperty(name) ?: return
        setValue(property, value)
    }

    private fun findProperty(name: String): Property<*>? = properties[name]

    @Suppress("UNCHECKED_CAST")
    private fun getValue(property: Property<*>): LuaValue {
        val prop = property as Property<Comparable<Any>>
        return when (val value = blockState.getValue(prop)) {
            is Boolean -> valueOf(value)
            is Int -> valueOf(value)
            is Long -> valueOf(value)
            is Byte -> valueOf(value.toInt())
            is Short -> valueOf(value.toInt())
            is Float -> valueOf(value.toDouble())
            is Double -> valueOf(value)
            else -> valueOf(prop.getName(value))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setValue(property: Property<*>, value: LuaValue) {
        val prop = property as Property<Comparable<Any>>
        val parsed = when {
            value.isboolean() -> if (value.toboolean()) "true" else "false"
            value.isnumber() -> if (value.isint() || value.islong()) value.tolong().toString() else value.tojstring()
            value.isstring() -> value.tojstring()
            else -> return
        }
        val converted = prop.getValue(parsed).orElse(null) ?: return
        state.blockState = blockState.setValue(prop, converted)
    }

    private fun getAll(): LuaTable {
        val table = LuaTable()
        for (property in properties.values) {
            if (property.name == "getAll") continue
            table.set(property.name, getValue(property))
        }
        return table
    }
}