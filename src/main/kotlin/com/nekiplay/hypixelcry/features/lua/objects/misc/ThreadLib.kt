package com.nekiplay.hypixelcry.features.lua.objects.misc

import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import java.util.concurrent.ConcurrentHashMap

class ThreadLib(val scriptName: String) : TwoArgFunction() {

    // Хранение потоков с меткой скрипта
    private val threads = ConcurrentHashMap<Int, Pair<String, Thread>>() // id -> (scriptName, thread)
    private var nextId = 1

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val library = LuaTable()
        library.set("startThread", StartThread())
        library.set("joinThread", JoinThread())
        library.set("isAlive", IsAlive())
        library.set("interruptThread", InterruptThread())
        library.set("sleep", Sleep())
        env.set("threads", library)
        return library
    }

    inner class StartThread : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val luaFunc = args.arg(1)
            if (!luaFunc.isfunction()) error("startThread expects a function as second argument")
            val func = luaFunc.checkfunction()

            val threadId = nextId++
            val thread = Thread {
                try {
                    func.call()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    threads.remove(threadId)
                }
            }
            threads[threadId] = Pair(scriptName, thread)
            thread.start()
            return LuaValue.valueOf(threadId)
        }
    }

    inner class JoinThread : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val thread = threads[threadId]?.second ?: return LuaValue.NIL
            try {
                thread.join()
            } catch (_: InterruptedException) {}
            return LuaValue.NIL
        }
    }

    inner class IsAlive : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val thread = threads[threadId]?.second ?: return LuaValue.FALSE
            return LuaValue.valueOf(thread.isAlive)
        }
    }

    inner class InterruptThread : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val threadId = arg.checkint()
            val thread = threads[threadId]?.second ?: return LuaValue.FALSE
            thread.interrupt()
            return LuaValue.TRUE
        }
    }

    public fun stopThreads(scriptName: String) {
        // Останавливаем и удаляем все потоки для данного скрипта
        threads.entries.removeIf { entry ->
            if (entry.value.first == scriptName) {
                entry.value.second.interrupt()
                true
            } else false
        }
    }

    inner class Sleep : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val ms = arg.checklong()
            try {
                Thread.sleep(ms)
            } catch (_: InterruptedException) {}
            return LuaValue.NIL
        }
    }
}