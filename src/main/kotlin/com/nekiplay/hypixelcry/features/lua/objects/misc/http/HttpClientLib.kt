package com.nekiplay.hypixelcry.features.lua.objects.misc.http

import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.*

class HttpClientLib(val L: Lua) {
    private val asyncExecutor: ExecutorService = Executors.newCachedThreadPool()

    companion object {
        private val HTTP_CLIENT: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    fun register() {
        L.newTable() // Создаем таблицу http

        // GET
        L.push(JFunction { getFunc(it) }); L.setField(-2, "get")
        L.push(JFunction { getWithHeadersFunc(it) }); L.setField(-2, "get_with_headers")
        L.push(JFunction { getAsyncFunc(it) }); L.setField(-2, "get_async")
        L.push(JFunction { getAsyncWithHeadersFunc(it) }); L.setField(-2, "get_async_with_headers")
        L.push(JFunction { getAsyncCallbackFunc(it) }); L.setField(-2, "get_async_callback")
        L.push(JFunction { getAsyncWithHeadersCallbackFunc(it) }); L.setField(-2, "get_async_with_headers_callback")

        // POST
        L.push(JFunction { postFunc(it) }); L.setField(-2, "post")
        L.push(JFunction { postWithHeadersFunc(it) }); L.setField(-2, "post_with_headers")
        L.push(JFunction { postAsyncFunc(it) }); L.setField(-2, "post_async")
        L.push(JFunction { postAsyncWithHeadersFunc(it) }); L.setField(-2, "post_async_with_headers")
        L.push(JFunction { postAsyncCallbackFunc(it) }); L.setField(-2, "post_async_callback")
        L.push(JFunction { postAsyncWithHeadersCallbackFunc(it) }); L.setField(-2, "post_async_with_headers_callback")

        L.setGlobal("http")
    }

    // --- Вспомогательные методы ---

    private fun parseHeaders(l: Lua, index: Int): Map<String, String> {
        val headers = HashMap<String, String>()
        if (l.isTable(index)) {
            l.pushNil()
            while (l.next(index) != 0) {
                val key = l.toString(-2) ?: ""
                val value = l.toString(-1) ?: ""
                headers[key] = value
                l.pop(1)
            }
        }
        return headers
    }

    private fun bytesToList(bytes: ByteArray): List<Int> {
        return bytes.map { it.toInt() and 0xFF }
    }

    private fun pushBytesAsTable(l: Lua, bytes: ByteArray) {
        l.newTable()
        for (i in bytes.indices) {
            l.push((bytes[i].toInt() and 0xFF).toDouble())
            l.rawSetI(-2, i + 1)
        }
    }

    private fun executeGetRequest(url: String, timeout: Int, headers: Map<String, String>): ByteArray {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeout.toLong()))
            .GET().apply { headers.forEach { (n, v) -> header(n, v) } }
            .build()
        val resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (resp.statusCode() >= 400) throw RuntimeException("HTTP ${resp.statusCode()}")
        return resp.body() ?: ByteArray(0)
    }

    private fun executePostRequest(url: String, timeout: Int, headers: Map<String, String>, body: String): ByteArray {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeout.toLong()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .apply { headers.forEach { (n, v) -> header(n, v) } }
            .build()
        val resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (resp.statusCode() >= 400) throw RuntimeException("HTTP ${resp.statusCode()}")
        return resp.body() ?: ByteArray(0)
    }

    // --- Реализация функций ---

    private fun getFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val timeout = if (l.isNumber(2)) l.toNumber(2).toInt() else 5
        return try {
            pushBytesAsTable(l, executeGetRequest(url, timeout, emptyMap()))
            1
        } catch (e: Exception) { l.error(e.message ?: "GET failed"); 0 }
    }

    private fun getWithHeadersFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val headers = parseHeaders(l, 2)
        return try {
            pushBytesAsTable(l, executeGetRequest(url, 5, headers))
            1
        } catch (e: Exception) { l.error(e.message ?: "GET failed"); 0 }
    }

    // Пример реализации коллбэка (остальные по аналогии)
    private fun getAsyncCallbackFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        if (!l.isFunction(2)) { l.error("Function expected"); return 0 }
        val callback = l.get() // Получаем LuaValue из стека (автоматически поп)

        asyncExecutor.submit {
            try {
                val data = executeGetRequest(url, 5, emptyMap())
                // Метод call(Object...) сам сконвертирует List в Lua table
                callback.call(bytesToList(data), null)
            } catch (e: Exception) {
                callback.call(null, e.message)
            }
        }
        l.push(true)
        return 1
    }

    private fun getAsyncWithHeadersCallbackFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val headers = parseHeaders(l, 2)
        val callback = l.get()
        asyncExecutor.submit {
            try {
                val data = executeGetRequest(url, 5, headers)
                callback.call(bytesToList(data), null)
            } catch (e: Exception) {
                callback.call(null, e.message)
            }
        }
        l.push(true); return 1
    }

    // --- POST методы ---

    private fun postFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val headers = parseHeaders(l, 2)
        val body = l.toString(3) ?: ""
        return try {
            pushBytesAsTable(l, executePostRequest(url, 5, headers, body))
            1
        } catch (e: Exception) { l.error(e.message ?: "POST failed"); 0 }
    }

    private fun postWithHeadersFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val body = l.toString(2) ?: ""
        return try {
            pushBytesAsTable(l, executePostRequest(url, 5, emptyMap(), body))
            1
        } catch (e: Exception) { l.error(e.message ?: "POST failed"); 0 }
    }

    private fun postAsyncCallbackFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val headers = parseHeaders(l, 2)
        val callback = l.get()
        asyncExecutor.submit {
            try {
                val data = executePostRequest(url, 5, headers, "")
                callback.call(bytesToList(data), null)
            } catch (e: Exception) {
                callback.call(null, e.message)
            }
        }
        l.push(true); return 1
    }

    private fun postAsyncWithHeadersCallbackFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val headers = parseHeaders(l, 2)
        val body = l.toString(3) ?: ""
        val callback = l.get()
        asyncExecutor.submit {
            try {
                val data = executePostRequest(url, 5, headers, body)
                callback.call(bytesToList(data), null)
            } catch (e: Exception) {
                callback.call(null, e.message)
            }
        }
        l.push(true); return 1
    }

    // --- Async / Await логика ---

    private fun setupAsyncResult(l: Lua, future: CompletableFuture<ByteArray>): Int {
        l.newTable()
        l.push(JFunction { lInner ->
            val timeout = if (lInner.isNumber(1)) lInner.toNumber(1).toInt() else 30
            try {
                val res = future.get(timeout.toLong(), TimeUnit.SECONDS)
                pushBytesAsTable(lInner, res)
                1
            } catch (e: Exception) {
                lInner.pushNil()
                1
            }
        })
        l.setField(-2, "await")
        return 1
    }

    private fun getAsyncFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val timeout = if (l.isNumber(2)) l.toNumber(2).toInt() else 5
        val future = CompletableFuture.supplyAsync({ executeGetRequest(url, timeout, emptyMap()) }, asyncExecutor)
        return setupAsyncResult(l, future)
    }

    private fun getAsyncWithHeadersFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val headers = parseHeaders(l, 2)
        val future = CompletableFuture.supplyAsync({ executeGetRequest(url, 5, headers) }, asyncExecutor)
        return setupAsyncResult(l, future)
    }

    private fun postAsyncFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val headers = parseHeaders(l, 2)
        val body = l.toString(3) ?: ""
        val future = CompletableFuture.supplyAsync({ executePostRequest(url, 5, headers, body) }, asyncExecutor)
        return setupAsyncResult(l, future)
    }

    private fun postAsyncWithHeadersFunc(l: Lua): Int {
        val url = l.toString(1) ?: ""
        val body = l.toString(2) ?: ""
        val future = CompletableFuture.supplyAsync({ executePostRequest(url, 5, emptyMap(), body) }, asyncExecutor)
        return setupAsyncResult(l, future)
    }

    fun shutdown() {
        asyncExecutor.shutdown()
    }
}