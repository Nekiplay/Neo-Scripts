package com.nekiplay.hypixelcry.features.lua.objects.misc.http

import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class AsyncResult(private val executor: (AsyncCallback) -> Unit) {
    fun asLuaValue(): LuaValue {
        val resultTable = LuaValue.tableOf()
        resultTable.set("await", awaitFunction())
        resultTable.set("then", thenFunction())
        return resultTable
    }

    private fun awaitFunction(): LuaValue {
        return object : ZeroArgFunction() {
            override fun call(): LuaValue {
                var result: LuaValue? = null
                var error: LuaValue? = null
                val latch = java.util.concurrent.CountDownLatch(1)

                executor(object : AsyncCallback {
                    override fun onSuccess(value: LuaValue) {
                        result = value
                        latch.countDown()
                    }

                    override fun onError(errorValue: LuaValue) {
                        error = errorValue
                        latch.countDown()
                    }
                })

                latch.await()

                return if (error != null) {
                    throw LuaError(error!!.tojstring())
                } else {
                    result ?: LuaValue.NIL
                }
            }
        }
    }

    private fun thenFunction(): LuaValue {
        return object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                if (callback.isfunction()) {
                    executor(object : AsyncCallback {
                        override fun onSuccess(value: LuaValue) {
                            callback.call(value)
                        }

                        override fun onError(errorValue: LuaValue) {
                            callback.call(LuaValue.NIL, errorValue)
                        }
                    })
                }
                return LuaValue.NIL
            }
        }
    }
}

interface AsyncCallback {
    fun onSuccess(value: LuaValue)
    fun onError(errorValue: LuaValue)
}