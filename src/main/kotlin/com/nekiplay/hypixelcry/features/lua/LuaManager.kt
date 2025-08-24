package com.nekiplay.hypixelcry.features.lua

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.functions.main.RegisterClientTickFunction
import com.nekiplay.hypixelcry.features.lua.functions.main.UnregisterClientTickFunction
import com.nekiplay.hypixelcry.features.lua.objects.player.PlayerObject
import com.nekiplay.hypixelcry.features.lua.objects.world.WorldObject
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import net.minecraft.client.MinecraftClient
import org.luaj.vm2.lib.OneArgFunction
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class LuaManager(configDir: File) {
    private val client: MinecraftClient = MinecraftClient.getInstance()
    private val globals: Globals = JsePlatform.standardGlobals()
    private val persistentGlobals = ConcurrentHashMap<String, LuaValue>()

    private val clientTickCallbacks = CopyOnWriteArrayList<LuaValue>()

    init {
        registerCustomFunctions()
        registerGlobalObjects()
    }

    private fun registerCustomFunctions() {
        globals.set("print", object : OneArgFunction() {
            override fun call(message: LuaValue): LuaValue {
                HypixelCry.LOGGER.info(message.tojstring());
                return NIL
            }
        })

        globals.set("registerClientTick", RegisterClientTickFunction(this))

        globals.set("unregisterClientTick", UnregisterClientTickFunction(this))
    }

    // Методы добавления callback'ов
    fun addClientTickCallback(callback: LuaValue): Boolean {
        if (callback.isfunction()) {
            clientTickCallbacks.add(callback)
            return true
        }
        return false
    }

    // Методы удаления callback'ов
    fun removeClientTickCallback(callback: LuaValue): Boolean {
        return clientTickCallbacks.remove(callback)
    }

    // Методы очистки всех callback'ов
    fun clearAllCallbacks() {
        clientTickCallbacks.clear()
    }

    fun clearClientTickCallbacks() = clientTickCallbacks.clear()

    // Получение количества callback'ов (для отладки)
    fun getClientTickCallbackCount(): Int = clientTickCallbacks.size

    private fun registerGlobalObjects() {
        // Регистрируем глобальные объекты
        globals.set("player", PlayerObject())
        globals.set("world", WorldObject())
    }

    private fun convertToJavaObject(luaValue: LuaValue): Any {
        return when {
            luaValue.isstring() -> luaValue.tojstring()
            luaValue.isnumber() -> luaValue.todouble()
            luaValue.isboolean() -> luaValue.toboolean()
            luaValue.istable() -> luaTableToMap(luaValue)
            else -> luaValue.tojstring()
        }
    }

    private fun luaTableToMap(table: LuaValue): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = table.checktable().keys()
        val iterator = keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val value = table.get(key)
            map[key.checkjstring()] = convertToJavaObject(value)
        }

        return map
    }

    // Callback методы
    // Callback методы для множественных обработчиков
    fun onClientTick() {
        clientTickCallbacks.forEach { callback ->
            try {
                callback.call()
            } catch (e: Exception) {
                println("Error in client tick callback: ${e.message}")
            }
        }
    }

    fun convertToLuaValue(value: Any?): LuaValue {
        return when (value) {
            null -> LuaValue.NIL
            is String -> LuaValue.valueOf(value)
            is Number -> LuaValue.valueOf(value.toDouble())
            is Boolean -> LuaValue.valueOf(value)
            is LuaValue -> value
            is Map<*, *> -> convertMapToLuaTable(value)
            is List<*> -> convertListToLuaTable(value)
            else -> LuaValue.valueOf(value.toString())
        }
    }

    private fun convertMapToLuaTable(map: Map<*, *>): LuaValue {
        val table = LuaValue.tableOf()
        map.forEach { (key, value) ->
            if (key is String) {
                table.set(key, convertToLuaValue(value))
            }
        }
        return table
    }

    private fun convertListToLuaTable(list: List<*>): LuaValue {
        val table = LuaValue.tableOf()
        list.forEachIndexed { index, value ->
            table.set(index + 1, convertToLuaValue(value)) // Lua tables are 1-indexed
        }
        return table
    }

    fun executeScript(script: String): Any? {
        return try {
            val chunk = globals.load(script)
            val result = chunk.call()
            restoreGlobals()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun restoreGlobals() {
        persistentGlobals.forEach { (name, value) ->
            globals.set(name, value)
        }
    }
}