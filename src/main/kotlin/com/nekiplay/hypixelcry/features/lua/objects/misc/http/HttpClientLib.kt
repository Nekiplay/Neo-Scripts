package com.nekiplay.hypixelcry.features.lua.objects.misc.http

import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import java.net.URI
import java.net.URISyntaxException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.*
import java.util.function.Consumer
import java.util.function.Function


class HttpClientLib : TwoArgFunction() {
    private val asyncExecutor: ExecutorService = Executors.newCachedThreadPool()

    override fun call(modname: LuaValue?, env: LuaValue): LuaValue {
        val http: LuaValue = tableOf()

        // GET функции
        http.set("get", this.function)
        http.set("get_with_headers", this.withHeadersFunction)
        http.set("get_async", this.asyncFunction)
        http.set("get_async_with_headers", this.asyncWithHeadersFunction)
        http.set("get_async_callback", this.asyncCallbackFunction)
        http.set("get_async_with_headers_callback", this.asyncWithHeadersCallbackFunction)

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

    private fun parseHeaders(headersTable: LuaValue): MutableMap<String?, String?> {
        val headers: MutableMap<String?, String?> = HashMap<String?, String?>()
        if (headersTable.istable()) {
            var key = NIL
            while (true) {
                val n = headersTable.next(key)
                key = n.arg1()
                if (key.isnil()) {
                    break // конец итерации
                }
                val value = n.arg(2)
                headers[key.checkjstring()] = value.checkjstring()
            }
        }
        return headers
    }

    private fun executeGetRequest(url: String, timeoutSeconds: Int, headers: MutableMap<String?, String?>): String {
        try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI(url))
                .timeout(Duration.ofSeconds(timeoutSeconds.toLong()))
                .GET()

            headers.forEach { (name: String?, value: String?) -> requestBuilder.header(name, value) }

            val response = HTTP_CLIENT.send<String?>(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            )

            if (response.statusCode() >= 400) {
                throw RuntimeException("HTTP error " + response.statusCode())
            }

            return response.body()
        } catch (e: URISyntaxException) {
            throw RuntimeException("Invalid URL: " + url, e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Request interrupted", e)
        } catch (e: Exception) {
            throw RuntimeException("GET request failed: " + e.message, e)
        }
    }

    private fun executePostRequest(
        url: String,
        timeoutSeconds: Int,
        headers: MutableMap<String?, String?>,
        body: String
    ): String {
        try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI(url))
                .timeout(Duration.ofSeconds(timeoutSeconds.toLong()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))

            headers.forEach { (name: String?, value: String?) -> requestBuilder.header(name, value) }

            val response = HTTP_CLIENT.send<String?>(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            )

            if (response.statusCode() >= 400) {
                throw RuntimeException("HTTP error " + response.statusCode())
            }

            return response.body()
        } catch (e: URISyntaxException) {
            throw RuntimeException("Invalid URL: " + url, e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Request interrupted", e)
        } catch (e: Exception) {
            throw RuntimeException("POST request failed: " + e.message, e)
        }
    }

    private val function: TwoArgFunction
        get() = object : TwoArgFunction() {
            override fun call(url: LuaValue, timeout: LuaValue): LuaValue? {
                try {
                    val timeoutSec = if (timeout.isnil()) 5 else timeout.toint()
                    val response = executeGetRequest(url.checkjstring(), timeoutSec, mutableMapOf<String?, String?>())
                    return valueOf(response)
                } catch (e: Exception) {
                    throw LuaError("HTTP GET error: " + e.message)
                }
            }
        }

    private val withHeadersFunction: TwoArgFunction
        get() = object : TwoArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue): LuaValue? {
                try {
                    val headers = parseHeaders(headersTable)
                    val response = executeGetRequest(url.checkjstring(), 5, headers)
                    return valueOf(response)
                } catch (e: Exception) {
                    throw LuaError("HTTP GET with headers error: " + e.message)
                }
            }
        }

    private fun postFunction(): ThreeArgFunction {
        return object : ThreeArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue, body: LuaValue): LuaValue? {
                try {
                    val headers = parseHeaders(headersTable)
                    val response = executePostRequest(
                        url.checkjstring(),
                        5,
                        headers,
                        body.checkjstring()
                    )
                    return valueOf(response)
                } catch (e: Exception) {
                    throw LuaError("HTTP POST error: " + e.message)
                }
            }
        }
    }

    private fun postWithHeadersFunction(): TwoArgFunction {
        return object : TwoArgFunction() {
            override fun call(url: LuaValue, body: LuaValue): LuaValue? {
                try {
                    val response = executePostRequest(
                        url.checkjstring(),
                        5,
                        mutableMapOf<String?, String?>(),
                        body.checkjstring()
                    )
                    return valueOf(response)
                } catch (e: Exception) {
                    throw LuaError("HTTP POST error: " + e.message)
                }
            }
        }
    }

    private val asyncCallbackFunction: TwoArgFunction
        get() = object : TwoArgFunction() {
            override fun call(url: LuaValue, callback: LuaValue): LuaValue? {
                if (!callback.isfunction()) {
                    throw LuaError("Second argument must be a callback function")
                }

                asyncExecutor.submit(Runnable {
                    try {
                        val response = executeGetRequest(url.checkjstring(), 5, mutableMapOf<String?, String?>())
                        callback.call(valueOf(response), NIL)
                    } catch (e: Exception) {
                        callback.call(NIL, valueOf("Error: " + e.message))
                    }
                })

                return TRUE
            }
        }

    private val asyncWithHeadersCallbackFunction: ThreeArgFunction
        get() = object : ThreeArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue, callback: LuaValue): LuaValue? {
                if (!callback.isfunction()) {
                    throw LuaError("Third argument must be a callback function")
                }

                asyncExecutor.submit(Runnable {
                    try {
                        val headers = parseHeaders(headersTable)
                        val response = executeGetRequest(url.checkjstring(), 5, headers)
                        callback.call(valueOf(response), NIL)
                    } catch (e: Exception) {
                        callback.call(NIL, valueOf("Error: " + e.message))
                    }
                })

                return TRUE
            }
        }

    private fun postAsyncCallbackFunction(): ThreeArgFunction {
        return object : ThreeArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue, callback: LuaValue): LuaValue? {
                if (!callback.isfunction()) {
                    throw LuaError("Third argument must be a callback function")
                }

                asyncExecutor.submit(Runnable {
                    try {
                        val headers = parseHeaders(headersTable)
                        val response = executePostRequest(
                            url.checkjstring(),
                            5,
                            headers,
                            ""
                        )
                        callback.call(valueOf(response), NIL)
                    } catch (e: Exception) {
                        callback.call(NIL, valueOf("Error: " + e.message))
                    }
                })

                return TRUE
            }
        }
    }

    private fun postAsyncWithHeadersCallbackFunction(): VarArgFunction {
        return object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs? {
                val url = args.arg1()
                val headersTable = args.arg(2)
                val body = args.arg(3)
                val callback = args.arg(4)

                if (!callback.isfunction()) {
                    throw LuaError("Fourth argument must be a callback function")
                }

                asyncExecutor.submit(Runnable {
                    try {
                        val headers = parseHeaders(headersTable)
                        val response = executePostRequest(
                            url.checkjstring(),
                            5,
                            headers,
                            body.checkjstring()
                        )
                        callback.call(valueOf(response), NIL)
                    } catch (e: Exception) {
                        callback.call(NIL, valueOf("Error: " + e.message))
                    }
                })

                return TRUE
            }
        }
    }

    private class AsyncResult {
        private val future = CompletableFuture<LuaValue?>()

        fun onSuccess(value: LuaValue?) {
            future.complete(value)
        }

        fun onError(error: LuaValue) {
            future.completeExceptionally(LuaError(error.checkjstring()))
        }

        fun asLuaValue(): LuaValue {
            val table: LuaValue = tableOf()
            table.set("await", object : TwoArgFunction() {
                override fun call(timeoutVal: LuaValue, defaultVal: LuaValue): LuaValue? {
                    try {
                        val timeoutSec = if (timeoutVal.isnil()) 30 else timeoutVal.toint()
                        return future.get(timeoutSec.toLong(), TimeUnit.SECONDS)
                    } catch (e: TimeoutException) {
                        return if (defaultVal.isnil()) NIL else defaultVal
                    } catch (e: Exception) {
                        throw LuaError("Async operation failed: " + e.message)
                    }
                }
            })
            return table
        }
    }

    private val asyncFunction: TwoArgFunction
        get() = object : TwoArgFunction() {
            override fun call(url: LuaValue, timeout: LuaValue): LuaValue {
                val result = AsyncResult()

                val timeoutSec = if (timeout.isnil()) 5 else timeout.toint()
                val urlString = url.checkjstring()

                HTTP_CLIENT.sendAsync<String?>(
                    HttpRequest.newBuilder()
                        .uri(URI.create(urlString))
                        .timeout(Duration.ofSeconds(timeoutSec.toLong()))
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                ).thenAccept(Consumer { response: HttpResponse<String?>? ->
                    if (response!!.statusCode() >= 400) {
                        result.onError(valueOf("HTTP error " + response.statusCode()))
                    } else {
                        result.onSuccess(valueOf(response.body()))
                    }
                }).exceptionally(Function { ex: Throwable? ->
                    result.onError(valueOf("Request failed: " + ex!!.message))
                    null
                })

                return result.asLuaValue()
            }
        }

    private val asyncWithHeadersFunction: TwoArgFunction
        get() = object : TwoArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue): LuaValue {
                val result = AsyncResult()
                val urlString = url.checkjstring()
                val headers = parseHeaders(headersTable)

                val requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .timeout(Duration.ofSeconds(5))
                    .GET()

                headers.forEach { (name: String?, value: String?) -> requestBuilder.header(name, value) }

                HTTP_CLIENT.sendAsync<String?>(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                ).thenAccept(Consumer { response: HttpResponse<String?>? ->
                    if (response!!.statusCode() >= 400) {
                        result.onError(valueOf("HTTP error " + response.statusCode()))
                    } else {
                        result.onSuccess(valueOf(response.body()))
                    }
                }).exceptionally(Function { ex: Throwable? ->
                    result.onError(valueOf("Request failed: " + ex!!.message))
                    null
                })

                return result.asLuaValue()
            }
        }

    private fun postAsyncFunction(): ThreeArgFunction {
        return object : ThreeArgFunction() {
            override fun call(url: LuaValue, headersTable: LuaValue, body: LuaValue): LuaValue {
                val result = AsyncResult()
                val urlString = url.checkjstring()
                val headers = parseHeaders(headersTable)
                val bodyStr = body.checkjstring()

                val requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyStr, StandardCharsets.UTF_8))

                headers.forEach { (name: String?, value: String?) -> requestBuilder.header(name, value) }

                HTTP_CLIENT.sendAsync<String?>(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                ).thenAccept(Consumer { response: HttpResponse<String?>? ->
                    if (response!!.statusCode() >= 400) {
                        result.onError(valueOf("HTTP error " + response.statusCode()))
                    } else {
                        result.onSuccess(valueOf(response.body()))
                    }
                }).exceptionally(Function { ex: Throwable? ->
                    result.onError(valueOf("Request failed: " + ex!!.message))
                    null
                })

                return result.asLuaValue()
            }
        }
    }

    private fun postAsyncWithHeadersFunction(): TwoArgFunction {
        return object : TwoArgFunction() {
            override fun call(url: LuaValue, body: LuaValue): LuaValue {
                val result = AsyncResult()
                val urlString = url.checkjstring()
                val bodyStr = body.checkjstring()

                HTTP_CLIENT.sendAsync<String?>(
                    HttpRequest.newBuilder()
                        .uri(URI.create(urlString))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bodyStr, StandardCharsets.UTF_8))
                        .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                ).thenAccept(Consumer { response: HttpResponse<String?>? ->
                    if (response!!.statusCode() >= 400) {
                        result.onError(valueOf("HTTP error " + response.statusCode()))
                    } else {
                        result.onSuccess(valueOf(response.body()))
                    }
                }).exceptionally(Function { ex: Throwable? ->
                    result.onError(valueOf("Request failed: " + ex!!.message))
                    null
                })

                return result.asLuaValue()
            }
        }
    }

    fun shutdown() {
        asyncExecutor.shutdown()
    }

    companion object {
        private val HTTP_CLIENT: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }
}