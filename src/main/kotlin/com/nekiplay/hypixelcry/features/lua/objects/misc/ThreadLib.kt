package com.nekiplay.hypixelcry.features.lua.objects.misc

import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.Lua.LuaType
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
        if (!l.isFunction(1)) {
            l.error("startThread expects a function as first argument")
            return 0
        }

        // Сохраняем Lua инстанс для использования в потоке
        val luaState = l.luaState

        // Создаем копию функции на стеке и получаем LuaValue
        l.pushValue(1)
        val func = l.get()

        val threadId = nextId.getAndIncrement()
        val thread = Thread {
            try {
                // Создаем новый Lua контекст для этого потока
                val threadLua = Lua(LuaType.LUAJIT, luaState)
                threadLua.pushValue(1)
                val threadFunc = threadLua.get()
                threadFunc.call()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                threads.remove(threadId)
            }
        }.apply {
            isDaemon = true
        }

        threads[threadId] = ThreadInfo(thread)
        thread.start()

        l.push(threadId)
        return 1
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