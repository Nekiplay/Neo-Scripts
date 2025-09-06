package com.nekiplay.hypixelcry.features.lua.objects.misc.http

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.net.URI

class HttpClientLib : TwoArgFunction() {

    private val client by lazy { HttpClients.createDefault() }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val http = LuaValue.tableOf()

        // Синхронные функции
        http.set("get", getFunction())
        http.set("get_with_headers", getWithHeadersFunction())

        // Асинхронные функции
        http.set("get_async", getAsyncFunction())
        http.set("get_async_with_headers", getAsyncWithHeadersFunction())
        http.set("get_async_callback", getAsyncCallbackFunction())
        http.set("get_async_with_headers_callback", getAsyncWithHeadersCallbackFunction())

        env.set("http", http)
        return http
    }

    // Синхронный GET запрос
    private fun getFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(url: LuaValue, timeout: LuaValue): LuaValue {
                return try {
                    val response = executeGetRequest(
                        url.checkjstring(),
                        timeout.optint(5000),
                        emptyMap()
                    )
                    LuaValue.valueOf(response)
                } catch (e: Exception) {
                    throw LuaError("HTTP GET error: ${e.message}")
                }
            }
        }
    }

    // Синхронный GET с заголовками
    private fun getWithHeadersFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue): LuaValue {
                return try {
                    val headers = parseHeaders(headersTable)
                    val response = executeGetRequest(
                        url.checkjstring(),
                        5000,
                        headers
                    )
                    LuaValue.valueOf(response)
                } catch (e: Exception) {
                    throw LuaError("HTTP GET with headers error: ${e.message}")
                }
            }
        }
    }

    // Асинхронный GET (возвращает Deferred)
    private fun getAsyncFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(url: LuaValue, timeout: LuaValue): LuaValue {
                return AsyncResult { callback ->
                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                executeGetRequest(
                                    url.checkjstring(),
                                    timeout.optint(5000),
                                    emptyMap()
                                )
                            }
                            callback.onSuccess(LuaValue.valueOf(response))
                        } catch (e: Exception) {
                            callback.onError(LuaValue.valueOf("HTTP async GET error: ${e.message}"))
                        }
                    }
                }.asLuaValue()
            }
        }
    }

    // Асинхронный GET с заголовками
    private fun getAsyncWithHeadersFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue): LuaValue {
                return AsyncResult { callback ->
                    coroutineScope.launch {
                        try {
                            val headers = parseHeaders(headersTable)
                            val response = withContext(Dispatchers.IO) {
                                executeGetRequest(
                                    url.checkjstring(),
                                    5000,
                                    headers
                                )
                            }
                            callback.onSuccess(LuaValue.valueOf(response))
                        } catch (e: Exception) {
                            callback.onError(LuaValue.valueOf("HTTP async GET with headers error: ${e.message}"))
                        }
                    }
                }.asLuaValue()
            }
        }
    }

    // Асинхронный GET с callback-функцией
    private fun getAsyncCallbackFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(url: LuaValue, callback: LuaValue): LuaValue {
                return if (callback.isfunction()) {
                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                executeGetRequest(url.checkjstring(), 5000, emptyMap())
                            }
                            // Вызываем callback с результатом (первый аргумент) и nil вместо ошибки (второй аргумент)
                            callback.call(LuaValue.valueOf(response), LuaValue.NIL)
                        } catch (e: Exception) {
                            // Вызываем callback с nil вместо результата и ошибкой
                            callback.call(LuaValue.NIL, LuaValue.valueOf("Error: ${e.message}"))
                        }
                    }
                    LuaValue.TRUE
                } else {
                    throw LuaError("Second argument must be a function for callback")
                }
            }
        }
    }

    // Асинхронный GET с заголовками и callback-функцией
    private fun getAsyncWithHeadersCallbackFunction(): LuaValue {
        return object : ThreeArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue, callback: LuaValue): LuaValue {
                return if (callback.isfunction()) {
                    coroutineScope.launch {
                        try {
                            val headers = parseHeaders(headersTable)
                            val response = withContext(Dispatchers.IO) {
                                executeGetRequest(url.checkjstring(), 5000, headers)
                            }
                            // Вызываем callback с результатом и nil вместо ошибки
                            callback.call(LuaValue.valueOf(response), LuaValue.NIL)
                        } catch (e: Exception) {
                            // Вызываем callback с nil вместо результата и ошибкой
                            callback.call(LuaValue.NIL, LuaValue.valueOf("Error: ${e.message}"))
                        }
                    }
                    LuaValue.TRUE
                } else {
                    throw LuaError("Third argument must be a function for callback")
                }
            }
        }
    }

    private fun executeGetRequest(url: String, timeout: Int, headers: Map<String, String>): String {
        val httpGet = HttpGet(URI.create(url))

        // Устанавливаем таймаут
        val requestConfig = org.apache.http.client.config.RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build()
        httpGet.config = requestConfig

        // Добавляем заголовки
        headers.forEach { (key, value) ->
            httpGet.addHeader(key, value)
        }

        return client.execute(httpGet).use { response ->
            val entity = response.entity
            if (entity != null) {
                EntityUtils.toString(entity)
            } else {
                throw RuntimeException("Empty response")
            }
        }
    }

    private fun parseHeaders(headersTable: LuaValue): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        if (headersTable.istable()) {
            headersTable.checktable().keys().forEach { key ->
                val headerName = key.checkjstring()
                val headerValue = headersTable.get(key).checkjstring()
                headers[headerName] = headerValue
            }
        }
        return headers
    }
}

// Класс для асинхронных результатов
