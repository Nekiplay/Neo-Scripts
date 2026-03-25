package com.nekiplay.hypixelcry.features.lua.objects.misc

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.smartPush
import com.nekiplay.hypixelcry.features.lua.objects.misc.http.HttpClientLib
import com.nekiplay.hypixelcry.features.lua.objects.modules.ModulesLib
import com.nekiplay.hypixelcry.features.lua.objects.player.PlayerObject
import com.nekiplay.hypixelcry.features.lua.objects.world.WorldObject
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ThreadLib(val L: Lua) {

    private val threads = ConcurrentHashMap<Int, ThreadInfo>()
    private val nextId = AtomicInteger(1)

    data class ThreadInfo(
        val thread: Thread,
        val startTime: Long = System.currentTimeMillis()
    )

    fun register() {
        L.newTable() // Создаем таблицу threads

        L.push(JFunction { startThread(it) })
        L.setField(-2, "startThread")

        L.push(JFunction { joinThread(it) })
        L.setField(-2, "joinThread")

        L.push(JFunction { isAlive(it) })
        L.setField(-2, "isAlive")

        L.push(JFunction { interruptThread(it) })
        L.setField(-2, "interruptThread")

        L.push(JFunction { stopThread(it) })
        L.setField(-2, "stopThread")

        L.push(JFunction { sleep(it) })
        L.setField(-2, "sleep")

        L.push(JFunction { getThreadCount(it) })
        L.setField(-2, "getThreadCount")

        L.setGlobal("threads")
    }

    private fun startThread(l: Lua): Int {
        val script: String?
        val data: Map<String, Any?>?

        when {
            l.isString(1) -> {
                script = l.toString(1)
                data = if (l.isTable(2)) luaTableToMap(l, 2) else null
            }
            l.isFunction(1) -> {
                l.pushValue(1)
                l.push("dump")
                l.pCall(1, 1)
                script = l.toString(-1)
                l.pop(1)
                if (script == null) {
                    l.pushNil()
                    return 1
                }
                data = if (l.isTable(2)) luaTableToMap(l, 2) else null
            }
            else -> {
                l.pushNil()
                return 1
            }
        }

        if (script != null) {
            val threadId = nextId.getAndIncrement()
            val thread = Thread {
                val newL = LuaJit() // Ваша функция инициализации нового стейта
                try {
                    if (data != null) {
                        // Используем ваш smartPush для загрузки данных в новый поток
                        newL.smartPush(data)
                        newL.setGlobal("args") // Таблица будет доступна как глобальная переменная args
                    }


                    DJLLib(newL).register()
                    EncodingLib(newL).register()
                    Creator(newL).register()
                    JsonLib(newL).register()
                    TCPLib(newL).register()
                    ThreadLib(newL).register()
                    HttpClientLib(newL).register()
                    ImGuiLib(newL).register()
                    ModulesLib(newL).register()
                    WorldObject(newL).register()
                    PlayerObject(newL).register()

                    // Выполняем скрипт
                    try {
                        newL.load(script)
                        newL.pCall(0, 0)
                    } catch (e: Exception) {
                        println("Lua Load Error: " + e)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    newL.close()
                    threads.remove(threadId)
                }
            }

            thread.isDaemon = true
            threads[threadId] = ThreadInfo(thread)
            thread.start()

            l.push(threadId.toDouble()) // Возвращаем ID потока
        }
        return 1
    }


    fun luaTableToMap(l: Lua, index: Int): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        // Преобразуем относительный индекс (например, -1, -2) в абсолютный,
        // так как внутри цикла стек будет меняться (l.next пушит новые элементы),
        // и относительный индекс станет указывать не на ту таблицу.
        val absIndex = if (index < 0) l.getTop() + index + 1 else index

        l.pushNil() // Кладем nil на стек, чтобы lua_next начал итерацию с начала

        // ИСПРАВЛЕНИЕ: Добавляем != 0, так как l.next(index) возвращает Int (1 или 0)
        while (l.next(absIndex) != 0) {
            // Теперь на стеке: ключ (key) под индексом -2, значение (value) под индексом -1

            // 1. Получаем ключ (обычно строка или число)
            val key = if (l.isNumber(-2)) {
                l.toNumber(-2).toInt().toString()
            } else {
                l.toString(-2) ?: "unknown_key"
            }

            // 2. Получаем значение с помощью вашей логики (рекурсивно)
            map[key] = luaToValue(l, -1)

            // 3. Удаляем значение (-1), но ОСТАВЛЯЕМ ключ (-2) для следующей итерации l.next
            l.pop(1)
        }

        return map
    }

    fun luaToValue(l: Lua, index: Int): Any? {
        return when {
            l.isNil(index) -> null
            l.isBoolean(index) -> l.toBoolean(index)
            l.isNumber(index) -> l.toNumber(index)
            l.isString(index) -> l.toString(index)
            l.isTable(index) -> luaTableToMap(l, index) // Рекурсия для таблиц
            l.isUserdata(index) -> l.toJavaObject(index) // Java-объекты
            l.isFunction(index) -> l.toObject(index) // LuaValue (функция)
            else -> l.toObject(index) // Все остальное
        }
    }

    private fun joinThread(l: Lua): Int {
        val threadId = l.toNumber(1).toInt()
        val info = threads[threadId] ?: run {
            l.pushNil()
            return 1
        }

        try {
            info.thread.join()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            threads.remove(threadId)
        }

        l.pushNil()
        return 1
    }

    private fun isAlive(l: Lua): Int {
        val threadId = l.toNumber(1).toInt()
        val info = threads[threadId] ?: run {
            l.push(false)
            return 1
        }

        if (info.thread.isAlive) {
            l.push(true)
        } else {
            threads.remove(threadId)
            l.push(false)
        }
        return 1
    }

    private fun interruptThread(l: Lua): Int {
        val threadId = l.toNumber(1).toInt()
        val info = threads[threadId] ?: run {
            l.push(false)
            return 1
        }
        info.thread.interrupt()
        l.push(true)
        return 1
    }

    private fun stopThread(l: Lua): Int {
        val threadId = l.toNumber(1).toInt()
        val info = threads[threadId] ?: run {
            l.push(false)
            return 1
        }

        info.thread.interrupt()
        threads.remove(threadId)
        l.push(true)
        return 1
    }

    private fun sleep(l: Lua): Int {
        if (l.isNumber(1)) {
            val ms = l.toNumber(1).toLong()
            try {
                Thread.sleep(ms)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        return 0
    }

    private fun getThreadCount(l: Lua): Int {
        l.push(threads.size.toDouble())
        return 1
    }

    fun stopAllThreads() {
        threads.values.forEach { it.thread.interrupt() }
        threads.clear()
    }
}