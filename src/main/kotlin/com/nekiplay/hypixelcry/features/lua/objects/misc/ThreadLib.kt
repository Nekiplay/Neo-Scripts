package com.nekiplay.hypixelcry.features.lua.objects.misc

import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ThreadLib : TwoArgFunction() {

    private val threads = ConcurrentHashMap<Int, ThreadInfo>()
    private val nextId = AtomicInteger(1)

    data class ThreadInfo(
        val thread: Thread,
        val startTime: Long = System.currentTimeMillis()
    )

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val library = LuaTable()
        library.set("startThread", StartThread())
        library.set("joinThread", JoinThread())
        library.set("isAlive", IsAlive())
        library.set("interruptThread", InterruptThread())
        library.set("sleep", Sleep())
        library.set("cleanupFinishedThreads", CleanupFinishedThreads())
        library.set("getThreadCount", GetThreadCount())
        env.set("threads", library)

        // Запускаем фоновую задачу для очистки завершенных потоков
        startCleanupTask()

        return library
    }

    inner class StartThread : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val luaFunc = args.arg(1)
            if (!luaFunc.isfunction()) error("startThread expects a function as first argument")
            val func = luaFunc.checkfunction()

            val threadId = nextId.getAndIncrement()
            val thread = Thread {
                try {
                    func.call()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Завершение потока обрабатывается в cleanupFinishedThreads()
            }.apply {
                isDaemon = true // Потоки-демоны не препятствуют завершению JVM
            }

            threads[threadId] = ThreadInfo(thread)
            thread.start()
            return LuaValue.valueOf(threadId)
        }
    }

    inner class JoinThread : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val info = threads[threadId] ?: return LuaValue.NIL
            try {
                info.thread.join()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                // Всегда удаляем поток после join
                threads.remove(threadId)
            }
            return LuaValue.NIL
        }
    }

    inner class IsAlive : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val info = threads[threadId] ?: return LuaValue.FALSE

            // Проверяем жив ли поток
            return if (info.thread.isAlive) {
                LuaValue.TRUE
            } else {
                // Если поток не жив, удаляем его из списка
                threads.remove(threadId)
                LuaValue.FALSE
            }
        }
    }

    inner class InterruptThread : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val info = threads[threadId] ?: return LuaValue.FALSE
            info.thread.interrupt()
            return LuaValue.TRUE
        }
    }

    inner class Sleep : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val ms = arg.checklong()
            try {
                Thread.sleep(ms)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt() // Восстанавливаем флаг прерывания
            }
            return LuaValue.NIL
        }
    }

    inner class CleanupFinishedThreads : ZeroArgFunction() {
        override fun call(): LuaValue {
            cleanupFinishedThreads()
            return LuaValue.NIL
        }
    }

    inner class GetThreadCount : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(threads.size)
        }
    }

    private fun cleanupFinishedThreads() {
        val iterator = threads.entries.iterator()
        var removedCount = 0
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value.thread.isAlive) {
                iterator.remove()
                removedCount++
            }
        }
        println("ThreadLib: Удалено $removedCount завершенных потоков")
    }

    private fun startCleanupTask() {
        val cleanupThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(30000) // Проверяем каждые 30 секунд
                    cleanupFinishedThreads()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.apply {
            isDaemon = true
            name = "ThreadLib-Cleanup"
        }
        cleanupThread.start()
    }

    fun stopAllThreads() {
        // Прерываем все потоки
        threads.values.forEach { it.thread.interrupt() }

        // Даем время на завершение
        threads.values.forEach { info ->
            try {
                info.thread.join(2000) // Ждем до 2 секунд
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        // Очищаем все записи
        threads.clear()
        println("ThreadLib: Все потоки остановлены")
    }

    // Метод для ручной очистки при уничтожении модуля
    fun cleanup() {
        stopAllThreads()
    }
}