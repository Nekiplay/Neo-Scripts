package com.nekiplay.neoscripts.features.lua.objects.misc

import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ThreadLib : LuaValue() {

    private val threads = ConcurrentHashMap<Int, ThreadInfo>()
    private val nextId = AtomicInteger(1)

    data class ThreadInfo(
        val thread: Thread,
        val startTime: Long = System.currentTimeMillis()
    )

    override fun call(): LuaValue {
        return this
    }

    override fun typename(): String = "threads"
    override fun tojstring(): String = "ThreadsObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "startThread" -> StartThread()
            "joinThread" -> JoinThread()
            "isAlive" -> IsAlive()
            "interruptThread" -> InterruptThread()
            "stopThread" -> StopThread()
            "sleep" -> Sleep()
            "getThreadCount" -> GetThreadCount()
            else -> super.get(key)
        }
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
            return valueOf(threadId)
        }
    }

    inner class JoinThread : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val info = threads[threadId] ?: return NIL
            try {
                info.thread.join()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                // Всегда удаляем поток после join
                threads.remove(threadId)
            }
            return NIL
        }
    }

    inner class IsAlive : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val info = threads[threadId] ?: return FALSE

            // Проверяем жив ли поток
            return if (info.thread.isAlive) {
                TRUE
            } else {
                // Если поток не жив, удаляем его из списка
                threads.remove(threadId)
                FALSE
            }
        }
    }

    inner class InterruptThread : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val info = threads[threadId] ?: return FALSE
            info.thread.interrupt()
            return TRUE
        }
    }

    inner class StopThread : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val info = threads[threadId] ?: return FALSE
            
            // Прерываем поток
            info.thread.interrupt()
            
            // Удаляем поток из списка без ожидания завершения
            threads.remove(threadId)
            return TRUE
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
            return NIL
        }
    }

    inner class GetThreadCount : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(threads.size)
        }
    }


    fun stopAllThreads() {
        threads.values.forEach { it.thread.interrupt() }

        threads.clear()
    }
}