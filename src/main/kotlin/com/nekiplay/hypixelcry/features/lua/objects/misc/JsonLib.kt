package com.nekiplay.hypixelcry.features.lua.objects.misc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

class JsonLib : TwoArgFunction() {
    private val gson = Gson()
    private val gsonPretty = GsonBuilder().setPrettyPrinting().create()

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val library = LuaValue.tableOf().apply {
            set("parse", SimpleParseFunction())
            set("stringify", StringifyFunction())
        }
        env.set("json", library)
        return library
    }

    inner class SimpleParseFunction : OneArgFunction() {
        override fun call(jsonString: LuaValue): LuaValue {
            return try {
                val result: Any? = gson.fromJson(jsonString.tojstring(), Any::class.java)
                when (result) {
                    null -> LuaValue.NIL
                    is Map<*, *> -> convertMap(result)
                    is List<*> -> convertList(result)
                    else -> convertPrimitive(result)
                }
            } catch (e: Exception) {
                LuaValue.NIL
            }
        }

        private fun convertMap(map: Map<*, *>): LuaValue {
            val table = LuaValue.tableOf()
            map.forEach { (key, value) ->
                table.set(convertKey(key), convertValue(value))
            }
            return table
        }

        private fun convertList(list: List<*>): LuaValue {
            val table = LuaValue.tableOf()
            list.forEachIndexed { index, value ->
                table.set(index + 1, convertValue(value))
            }
            return table
        }

        private fun convertValue(value: Any?): LuaValue {
            return when (value) {
                null -> LuaValue.NIL
                is String -> LuaValue.valueOf(value)
                is Number -> LuaValue.valueOf(value.toDouble())
                is Boolean -> LuaValue.valueOf(value)
                is Map<*, *> -> convertMap(value)
                is List<*> -> convertList(value)
                else -> LuaValue.valueOf(value.toString())
            }
        }

        private fun convertKey(key: Any?): LuaValue {
            return when (key) {
                is String -> LuaValue.valueOf(key)
                is Number -> LuaValue.valueOf(key.toDouble())
                else -> LuaValue.valueOf(key.toString())
            }
        }

        private fun convertPrimitive(value: Any): LuaValue {
            return when (value) {
                is String -> LuaValue.valueOf(value)
                is Number -> LuaValue.valueOf(value.toDouble())
                is Boolean -> LuaValue.valueOf(value)
                else -> LuaValue.valueOf(value.toString())
            }
        }
    }

    inner class StringifyFunction : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            return try {
                val luaValue = args.arg(1)
                val indent = if (args.narg() > 1) args.arg(2) else LuaValue.NIL

                val javaObj = convertToJava(luaValue)
                val jsonString = if (indent.isnil()) {
                    // По умолчанию - форматированный JSON
                    gsonPretty.toJson(javaObj)
                } else {
                    // Если указан indent = 0 или false - неформатированный
                    if (indent.isnumber() && indent.todouble() == 0.0 || indent.isboolean() && !indent.toboolean()) {
                        gson.toJson(javaObj)
                    } else {
                        gsonPretty.toJson(javaObj)
                    }
                }
                LuaValue.valueOf(jsonString)
            } catch (e: Exception) {
                LuaValue.NIL
            }
        }

        private fun convertToJava(luaValue: LuaValue): Any? {
            return when {
                luaValue.isnil() -> null
                luaValue.isstring() -> luaValue.tojstring()
                luaValue.isnumber() -> luaValue.todouble()
                luaValue.isboolean() -> luaValue.toboolean()
                luaValue.istable() -> convertTable(luaValue)
                else -> luaValue.tojstring()
            }
        }

        private fun convertTable(table: LuaValue): Any {
            val tableObj = table.checktable()

            // Проверяем, является ли таблица массивом
            if (isArray(tableObj)) {
                return convertTableToArray(tableObj)
            } else {
                return convertTableToObject(tableObj)
            }
        }

        private fun isArray(table: LuaTable): Boolean {
            var arrayIndex = 1
            val keys = table.keys()

            // Проверяем все ключи таблицы
            for (key in keys) {
                // Если ключ не число или не соответствует последовательности массива - это объект
                if (!key.isnumber()) {
                    return false
                }

                val keyNum = key.todouble()
                // Если ключ не целое число или не соответствует ожидаемому индексу массива
                if (keyNum != arrayIndex.toDouble() || keyNum != Math.floor(keyNum)) {
                    return false
                }

                arrayIndex++
            }

            // Если в таблице нет элементов, считаем ее объектом
            return keys.isNotEmpty()
        }

        private fun convertTableToArray(table: LuaTable): List<Any?> {
            val list = mutableListOf<Any?>()
            var index = 1

            while (true) {
                val value = table.get(index)
                if (value.isnil()) {
                    break
                }
                list.add(convertToJava(value))
                index++
            }

            return list
        }

        private fun convertTableToObject(table: LuaTable): Map<String, Any?> {
            val map = mutableMapOf<String, Any?>()
            val keys = table.keys()

            for (key in keys) {
                map[key.tojstring()] = convertToJava(table.get(key))
            }

            return map
        }
    }
}