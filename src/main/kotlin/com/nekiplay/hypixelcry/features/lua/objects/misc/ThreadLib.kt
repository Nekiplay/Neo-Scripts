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
        library.set("stopThread", StopThread())
        library.set("sleep", Sleep())
        library.set("getThreadCount", GetThreadCount())
        env.set("threads", library)

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
                } finally {
                    threads.remove(threadId)
                }
            }.apply {
                isDaemon = true
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

    inner class StopThread : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val info = threads[threadId] ?: return LuaValue.FALSE
            
            // Прерываем поток
            info.thread.interrupt()
            
            // Удаляем поток из списка без ожидания завершения
            threads.remove(threadId)
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

    inner class GetThreadCount : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(threads.size)
        }
    }


    fun stopAllThreads() {
        threads.values.forEach { it.thread.interrupt() }

        threads.clear()
    }
}