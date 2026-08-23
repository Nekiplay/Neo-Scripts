package com.nekiplay.neoscripts.common.features.lua.objects.misc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nekiplay.neoscripts.ServerMain
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction

class JsonLib : LuaValue() {
    override fun typename(): String = "json"
    override fun tojstring(): String = "JsonObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "parse", "totable", "decode" -> SimpleParseFunction()
            "stringify", "tojson", "encode" -> StringifyFunction()
            else -> super.get(key)
        }
    }


    class SimpleParseFunction : OneArgFunction() {
        override fun call(jsonString: LuaValue): LuaValue {
            return try {
                when (val result: Any? = ServerMain.GSON_COMPACT.fromJson(jsonString.tojstring(), Any::class.java)) {
                    null -> NIL
                    is Map<*, *> -> convertMap(result)
                    is List<*> -> convertList(result)
                    else -> convertPrimitive(result)
                }
            } catch (e: Exception) {
                NIL
            }
        }

        private fun convertMap(map: Map<*, *>): LuaValue {
            val table = tableOf()
            map.forEach { (key, value) ->
                table.set(convertKey(key), convertValue(value))
            }
            return table
        }

        private fun convertList(list: List<*>): LuaValue {
            val table = tableOf()
            list.forEachIndexed { index, value ->
                table.set(index + 1, convertValue(value))
            }
            return table
        }

        private fun convertValue(value: Any?): LuaValue {
            return when (value) {
                null -> NIL
                is String -> valueOf(value)
                is Number -> valueOf(value.toDouble())
                is Boolean -> valueOf(value)
                is Map<*, *> -> convertMap(value)
                is List<*> -> convertList(value)
                else -> valueOf(value.toString())
            }
        }

        private fun convertKey(key: Any?): LuaValue {
            return when (key) {
                is String -> valueOf(key)
                is Number -> valueOf(key.toDouble())
                else -> valueOf(key.toString())
            }
        }

        private fun convertPrimitive(value: Any): LuaValue {
            return when (value) {
                is String -> valueOf(value)
                is Number -> valueOf(value.toDouble())
                is Boolean -> valueOf(value)
                else -> valueOf(value.toString())
            }
        }
    }

    class StringifyFunction : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            return try {
                val luaValue = args.arg(1)
                val indent = if (args.narg() > 1) args.arg(2) else NIL

                val javaObj = convertToJava(luaValue)
                val jsonString = if (indent.isnil()) {
                    // По умолчанию - не форматированный JSON
                    ServerMain.GSON_COMPACT.toJson(javaObj)
                } else {
                    // Если указан indent = 0 или false - неформатированный
                    if (indent.isnumber() && indent.todouble() == 0.0 || indent.isboolean() && !indent.toboolean()) {
                        ServerMain.GSON_COMPACT.toJson(javaObj)
                    } else {
                        ServerMain.GSON.toJson(javaObj)
                    }
                }
                valueOf(jsonString)
            } catch (e: Exception) {
                NIL
            }
        }

        private fun convertToJava(luaValue: LuaValue): Any? {
            return when {
                luaValue.isnil() -> null
                luaValue is LuaDouble -> {
                    val d = luaValue.todouble()
                    if (d == d.toLong().toDouble()) d.toLong() else d
                }
                luaValue is LuaLong -> {
                    luaValue.tolong()
                }
                luaValue is LuaInteger -> {
                    luaValue.toint()
                }
                luaValue is LuaNumber -> {
                    val d = luaValue.todouble()
                    if (d == d.toLong().toDouble()) d.toLong() else d
                }
                luaValue.isboolean() -> luaValue.toboolean()
                luaValue.isstring() -> luaValue.tojstring()
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