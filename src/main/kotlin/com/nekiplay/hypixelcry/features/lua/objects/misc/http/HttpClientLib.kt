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
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.net.URI

class HttpClientLib : TwoArgFunction() {

    private val client by lazy { HttpClients.createDefault() }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val http = LuaValue.tableOf()

        // GET функции
        http.set("get", getFunction())
        http.set("get_with_headers", getWithHeadersFunction())
        http.set("get_async", getAsyncFunction())
        http.set("get_async_with_headers", getAsyncWithHeadersFunction())
        http.set("get_async_callback", getAsyncCallbackFunction())
        http.set("get_async_with_headers_callback", getAsyncWithHeadersCallbackFunction())

        // POST функции
        http.set("post", postFunction())
        http.set("post_with_headers", postWithHeadersFunction())
        http.set("post_async", postAsyncFunction())
        http.set("post_async_with_headers", postAsyncWithHeadersFunction())
        http.set("post_async_callback", postAsyncCallbackFunction())
        http.set("post_async_with_headers_callback", postAsyncWithHeadersCallbackFunction())

        env.set("http", http)
        return http
    }

    // POST функции

    // Синхронный POST запрос
    private fun postFunction(): LuaValue {
        return object : ThreeArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue, body: LuaValue): LuaValue {
                return try {
                    val headers = parseHeaders(headersTable)
                    val response = executePostRequest(
                        url.checkjstring(),
                        5000,
                        headers,
                        body.checkjstring()
                    )
                    LuaValue.valueOf(response)
                } catch (e: Exception) {
                    throw LuaError("HTTP POST error: ${e.message}")
                }
            }
        }
    }

    // Синхронный POST с заголовками (упрощенная версия)
    private fun postWithHeadersFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(url: LuaValue, body: LuaValue): LuaValue {
                return try {
                    val response = executePostRequest(
                        url.checkjstring(),
                        5000,
                        emptyMap(),
                        body.checkjstring()
                    )
                    LuaValue.valueOf(response)
                } catch (e: Exception) {
                    throw LuaError("HTTP POST error: ${e.message}")
                }
            }
        }
    }

    // Асинхронный POST
    private fun postAsyncFunction(): LuaValue {
        return object : ThreeArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue, body: LuaValue): LuaValue {
                return AsyncResult { callback ->
                    coroutineScope.launch {
                        try {
                            val headers = parseHeaders(headersTable)
                            val response = withContext(Dispatchers.IO) {
                                executePostRequest(
                                    url.checkjstring(),
                                    5000,
                                    headers,
                                    body.checkjstring()
                                )
                            }
                            callback.onSuccess(LuaValue.valueOf(response))
                        } catch (e: Exception) {
                            callback.onError(LuaValue.valueOf("HTTP async POST error: ${e.message}"))
                        }
                    }
                }.asLuaValue()
            }
        }
    }

    // Асинхронный POST с заголовками
    private fun postAsyncWithHeadersFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(url: LuaValue, body: LuaValue): LuaValue {
                return AsyncResult { callback ->
                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                executePostRequest(
                                    url.checkjstring(),
                                    5000,
                                    emptyMap(),
                                    body.checkjstring()
                                )
                            }
                            callback.onSuccess(LuaValue.valueOf(response))
                        } catch (e: Exception) {
                            callback.onError(LuaValue.valueOf("HTTP async POST error: ${e.message}"))
                        }
                    }
                }.asLuaValue()
            }
        }
    }

    // Асинхронный POST с callback
    private fun postAsyncCallbackFunction(): LuaValue {
        return object : ThreeArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue, callback: LuaValue): LuaValue {
                return if (callback.isfunction()) {
                    coroutineScope.launch {
                        try {
                            val headers = parseHeaders(headersTable)
                            val response = withContext(Dispatchers.IO) {
                                executePostRequest(
                                    url.checkjstring(),
                                    5000,
                                    headers,
                                    ""
                                )
                            }
                            callback.call(LuaValue.valueOf(response), LuaValue.NIL)
                        } catch (e: Exception) {
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

    // Асинхронный POST с заголовками и body callback
    private fun postAsyncWithHeadersCallbackFunction(): LuaValue {
        return object : FourArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue, body: LuaValue, callback: LuaValue): LuaValue {
                return if (callback.isfunction()) {
                    coroutineScope.launch {
                        try {
                            val headers = parseHeaders(headersTable)
                            val response = withContext(Dispatchers.IO) {
                                executePostRequest(
                                    url.checkjstring(),
                                    5000,
                                    headers,
                                    body.checkjstring()
                                )
                            }
                            callback.call(LuaValue.valueOf(response), LuaValue.NIL)
                        } catch (e: Exception) {
                            callback.call(LuaValue.NIL, LuaValue.valueOf("Error: ${e.message}"))
                        }
                    }
                    LuaValue.TRUE
                } else {
                    throw LuaError("Fourth argument must be a function for callback")
                }
            }
        }
    }

    abstract class FourArgFunction : LibFunction() {
        abstract override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue, arg4: LuaValue): LuaValue

        override fun invoke(args: Varargs): Varargs {
            return call(args.arg1(), args.arg(2), args.arg(3), args.arg(4))
        }
    }

    private fun executePostRequest(url: String, timeout: Int, headers: Map<String, String>, body: String): String {
        val httpPost = HttpPost(URI.create(url))

        // Устанавливаем таймаут
        val requestConfig = org.apache.http.client.config.RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build()
        httpPost.config = requestConfig

        // Добавляем заголовки
        headers.forEach { (key, value) ->
            httpPost.addHeader(key, value)
        }

        // Устанавливаем тело запроса
        if (body.isNotEmpty()) {
            httpPost.entity = StringEntity(body, "UTF-8")
            httpPost.setHeader("Content-type", "application/json")
        }

        return client.execute(httpPost).use { response ->
            val entity = response.entity
            if (entity != null) {
                EntityUtils.toString(entity)
            } else {
                throw RuntimeException("Empty response")
            }
        }
    }

    private fun convertTableToJson(table: LuaTable): String {
        val json = StringBuilder()
        json.append("{")

        val keys = table.keys()
        var first = true

        keys.forEach { key ->
            if (!first) json.append(",")
            first = false

            val keyStr = key.checkjstring()
            val value = table.get(key)

            json.append("\"$keyStr\":")

            when {
                value.isstring() -> json.append("\"${value.checkjstring()}\"")
                value.isint() -> json.append(value.checkint())
                value.isnumber() -> json.append(value.checkdouble())
                value.istable() -> json.append(convertTableToJson(value.checktable()))
                value.isboolean() -> json.append(if (value.checkboolean()) "true" else "false")
                value.isnil() -> json.append("null")
                else -> json.append("\"${value.tojstring()}\"")
            }
        }

        json.append("}")
        return json.toString()
    }

    private fun convertJsonToTable(jsonString: String): LuaValue {
        // Простая реализация парсинга JSON (для production лучше использовать библиотеку)
        val table = LuaValue.tableOf()

        // Упрощенный парсинг - в реальности нужно использовать JSON библиотеку
        if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
            val content = jsonString.substring(1, jsonString.length - 1)
            val pairs = content.split(",")

            pairs.forEach { pair ->
                val keyValue = pair.split(":", limit = 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim().removeSurrounding("\"")
                    var value = keyValue[1].trim()

                    when {
                        value.startsWith("\"") && value.endsWith("\"") -> {
                            table.set(key, LuaValue.valueOf(value.removeSurrounding("\"")))
                        }
                        value == "true" -> table.set(key, LuaValue.TRUE)
                        value == "false" -> table.set(key, LuaValue.FALSE)
                        value == "null" -> table.set(key, LuaValue.NIL)
                        value.contains(".") -> table.set(key, LuaValue.valueOf(value.toDouble()))
                        else -> table.set(key, LuaValue.valueOf(value.toInt()))
                    }
                }
            }
        }

        return table
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
