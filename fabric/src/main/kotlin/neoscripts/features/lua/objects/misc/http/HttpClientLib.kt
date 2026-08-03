package com.nekiplay.neoscripts.features.lua.objects.misc.http

import com.sun.net.httpserver.HttpServer
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.net.*
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.*
import kotlin.collections.component1
import kotlin.collections.component2

class HttpClientLib : LuaValue() {
    private val asyncExecutor: ExecutorService = Executors.newCachedThreadPool()

    override fun typename(): String = "http"
    override fun tojstring(): String = "HttpObject"
    override fun isnil(): Boolean = false
    override fun type(): Int = TUSERDATA
    override fun call(): LuaValue = this

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "create_server" -> createServerFunction()

            "get" -> this.function
            "get_with_headers" -> this.withHeadersFunction
            "get_async_with_headers_callback" -> this.asyncWithHeadersCallbackFunction
            "post" -> postFunction()
            "post_with_headers" -> postWithHeadersFunction()
            "post_async_with_headers_callback" -> postAsyncWithHeadersCallbackFunction()

            // Методы с прокси
            "get_with_proxy" -> getWithProxyFunction()
            "get_with_headers_with_proxy" -> getWithHeadersWithProxyFunction()
            "get_async_with_headers_with_proxy_callback" -> getAsyncWithHeadersWithProxyCallbackFunction()
            "post_with_proxy" -> postWithProxyFunction()
            "post_with_headers_with_proxy" -> postWithHeadersWithProxyFunction()
            "post_async_with_headers_with_proxy_callback" -> postAsyncWithHeadersWithProxyCallbackFunction()

            else -> super.get(key)
        }
    }

    public var servers = ArrayList<HttpServer>();

    private fun createServerFunction() = object : ThreeArgFunction() {
        override fun call(hostVal: LuaValue, portVal: LuaValue, callbackVal: LuaValue): LuaValue {
            val host = hostVal.checkjstring()
            val port = portVal.checkint()
            val callback = callbackVal.checkfunction()

            return try {
                // Создаем привязку к конкретному IP (хосту) и порту
                val server = HttpServer.create(InetSocketAddress(host, port), 0)

                server.createContext("/") { exchange ->
                    try {
                        val method = exchange.requestMethod
                        val path = exchange.requestURI.path ?: "/"

                        // Парсинг заголовков запроса
                        val reqHeaders = tableOf()
                        exchange.requestHeaders.forEach { (k, v) ->
                            if (k != null) {
                                reqHeaders.set(k, v.joinToString(", "))
                            }
                        }

                        // Чтение тела запроса
                        val bodyBytes = exchange.requestBody.readBytes()
                        val body = String(bodyBytes, StandardCharsets.UTF_8)

                        val reqTable = tableOf()
                        reqTable.set("method", valueOf(method))
                        reqTable.set("path", valueOf(path))
                        reqTable.set("headers", reqHeaders)
                        reqTable.set("body", valueOf(body))

                        // Вызываем Lua callback и получаем ответ
                        val responseVal = callback.call(reqTable)

                        var status = 200
                        var respBody: LuaTable? = null
                        val respHeaders = mutableMapOf<String, String>()

                        if (responseVal.istable()) {
                            status = responseVal.get("status").optint(200)
                            respBody = responseVal.get("body").checktable()

                            val headersVal = responseVal.get("headers")
                            if (headersVal.istable()) {
                                var key = NIL
                                while (true) {
                                    val n = headersVal.next(key)
                                    key = n.arg1()
                                    if (key.isnil()) break
                                    respHeaders[key.checkjstring()] = n.arg(2).checkjstring()
                                }
                            }
                        } else if (responseVal.istable()) {
                            respBody = responseVal.checktable()
                        }

                        if (respBody != null) {
                            val length = respBody.length() ?: 0
                            val byteArray = ByteArray(length)
                            for (i in 1..length) {
                                val byteValue = respBody.get(i).checkint()
                                byteArray[i - 1] = (byteValue and 0xFF).toByte()
                            }
                            // Запись заголовков ответа
                            respHeaders.forEach { (k, v) -> exchange.responseHeaders.add(k, v) }

                            exchange.sendResponseHeaders(status, byteArray.size.toLong())
                            exchange.responseBody.write(byteArray)
                        }
                    } catch (e: Exception) {
                        val errMsg = "Internal Server Error: ${e.message}"
                        val bytes = errMsg.toByteArray(StandardCharsets.UTF_8)
                        exchange.sendResponseHeaders(500, bytes.size.toLong())
                        exchange.responseBody.write(bytes)
                    } finally {
                        exchange.close()
                    }
                }

                server.executor = asyncExecutor
                server.start()
                servers.add(server)

                object : LuaValue() {
                    override fun typename(): String = "HttpServer"
                    override fun type(): Int = TUSERDATA
                    override fun get(key: LuaValue): LuaValue {
                        return when (key.tojstring()) {
                            "stop" -> object : ZeroArgFunction() {
                                override fun call(): LuaValue {
                                    server.stop(0)
                                    return NIL
                                }
                            }
                            else -> super.get(key)
                        }
                    }
                }
            } catch (e: Exception) {
                throw LuaError("Failed to start HTTP server: ${e.message}")
            }
        }
    }

    /**
     * Создает клиент с настройками прокси
     */
    private fun getClient(host: String?, port: Int, user: String?, pass: String?): HttpClient {
        if (host.isNullOrEmpty()) return HTTP_CLIENT

        return try {
            val builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.of(InetSocketAddress(host, port)))

            if (!user.isNullOrEmpty() && !pass.isNullOrEmpty()) {
                builder.authenticator(object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(user, pass.toCharArray())
                    }
                })
            }

            builder.build()
        } catch (e: Exception) {
            throw RuntimeException("Proxy configuration failed: ${e.message}")
        }
    }

    private fun toLuaTable(bytes: ByteArray): LuaValue {
        val bytesTable = tableOf()
        for (i in bytes.indices) {
            bytesTable.set(i + 1, valueOf(bytes[i].toInt() and 0xFF))
        }
        return bytesTable
    }

    private fun parseHeaders(headersTable: LuaValue): MutableMap<String?, String?> {
        val headers = mutableMapOf<String?, String?>()
        if (headersTable.istable()) {
            var key = NIL
            while (true) {
                val n = headersTable.next(key)
                key = n.arg1()
                if (key.isnil()) break
                headers[key.checkjstring()] = n.arg(2).checkjstring()
            }
        }
        return headers
    }

    // --- Реализация логики запросов ---

    private fun executeGetRequest(
        url: String,
        timeout: Int,
        headers: MutableMap<String?, String?>,
        pHost: String?, pPort: Int, pUser: String?, pPass: String?
    ): ByteArray {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI(url))
            .timeout(Duration.ofSeconds(timeout.toLong()))
            .GET()

        headers.forEach { (name, value) -> if (name != null && value != null) requestBuilder.header(name, value) }

        val client = getClient(pHost, pPort, pUser, pPass)
        val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() >= 400) throw RuntimeException("HTTP error ${response.statusCode()}")
        return response.body() ?: ByteArray(0)
    }

    private fun executePostRequest(
        url: String,
        timeout: Int,
        headers: MutableMap<String?, String?>,
        body: String,
        pHost: String?, pPort: Int, pUser: String?, pPass: String?
    ): ByteArray {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI(url))
            .timeout(Duration.ofSeconds(timeout.toLong()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))

        headers.forEach { (name, value) -> if (name != null && value != null) requestBuilder.header(name, value) }

        val client = getClient(pHost, pPort, pUser, pPass)
        val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() >= 400) throw RuntimeException("HTTP error ${response.statusCode()}")
        return response.body() ?: ByteArray(0)
    }

    // --- Методы с прокси (Lua API) ---

    // get_with_proxy(url, host, port, user, pass, timeout)
    private fun getWithProxyFunction() = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val url = args.arg1().checkjstring()
            val host = args.arg(2).checkjstring()
            val port = args.arg(3).checkint()
            val user = if (args.arg(4).isnil()) null else args.arg(4).tojstring()
            val pass = if (args.arg(5).isnil()) null else args.arg(5).tojstring()
            val timeout = if (args.arg(6).isnil()) 5 else args.arg(6).toint()

            return try {
                val res = executeGetRequest(url, timeout, mutableMapOf(), host, port, user, pass)
                toLuaTable(res)
            } catch (e: Exception) { throw LuaError("HTTP GET Proxy Error: ${e.message}") }
        }
    }

    // get_with_headers_with_proxy(url, headers, host, port, user, pass)
    private fun getWithHeadersWithProxyFunction() = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val url = args.arg1().checkjstring()
            val headers = parseHeaders(args.arg(2))
            val host = args.arg(3).checkjstring()
            val port = args.arg(4).checkint()
            val user = if (args.arg(5).isnil()) null else args.arg(5).tojstring()
            val pass = if (args.arg(6).isnil()) null else args.arg(6).tojstring()

            return try {
                val res = executeGetRequest(url, 5, headers, host, port, user, pass)
                toLuaTable(res)
            } catch (e: Exception) { throw LuaError("HTTP GET Headers Proxy Error: ${e.message}") }
        }
    }

    // get_async_with_headers_with_proxy_callback(url, headers, host, port, user, pass, callback)
    private fun getAsyncWithHeadersWithProxyCallbackFunction() = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val url = args.arg1().checkjstring()
            val headers = parseHeaders(args.arg(2))
            val host = args.arg(3).checkjstring()
            val port = args.arg(4).checkint()
            val user = if (args.arg(5).isnil()) null else args.arg(5).tojstring()
            val pass = if (args.arg(6).isnil()) null else args.arg(6).tojstring()
            val callback = args.arg(7).checkfunction()

            asyncExecutor.submit {
                try {
                    val res = executeGetRequest(url, 5, headers, host, port, user, pass)
                    callback.call(toLuaTable(res), NIL)
                } catch (e: Exception) {
                    callback.call(NIL, valueOf("Error: ${e.message}"))
                }
            }
            return TRUE
        }
    }

    // post_with_proxy(url, body, host, port, user, pass)
    private fun postWithProxyFunction() = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val url = args.arg1().checkjstring()
            val body = args.arg(2).checkjstring()
            val host = args.arg(3).checkjstring()
            val port = args.arg(4).checkint()
            val user = if (args.arg(5).isnil()) null else args.arg(5).tojstring()
            val pass = if (args.arg(6).isnil()) null else args.arg(6).tojstring()

            return try {
                val res = executePostRequest(url, 5, mutableMapOf(), body, host, port, user, pass)
                toLuaTable(res)
            } catch (e: Exception) { throw LuaError("HTTP POST Proxy Error: ${e.message}") }
        }
    }

    // post_with_headers_with_proxy(url, body, headers, host, port, user, pass)
    private fun postWithHeadersWithProxyFunction() = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val url = args.arg1().checkjstring()
            val body = args.arg(2).checkjstring()
            val headers = parseHeaders(args.arg(3))
            val host = args.arg(4).checkjstring()
            val port = args.arg(5).checkint()
            val user = if (args.arg(6).isnil()) null else args.arg(6).tojstring()
            val pass = if (args.arg(7).isnil()) null else args.arg(7).tojstring()

            return try {
                val res = executePostRequest(url, 5, headers, body, host, port, user, pass)
                toLuaTable(res)
            } catch (e: Exception) { throw LuaError("HTTP POST Headers Proxy Error: ${e.message}") }
        }
    }

    // post_async_with_headers_with_proxy_callback(url, body, headers, host, port, user, pass, callback)
    private fun postAsyncWithHeadersWithProxyCallbackFunction() = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val url = args.arg1().checkjstring()
            val body = args.arg(2).checkjstring()
            val headers = parseHeaders(args.arg(3))
            val host = args.arg(4).checkjstring()
            val port = args.arg(5).checkint()
            val user = if (args.arg(6).isnil()) null else args.arg(6).tojstring()
            val pass = if (args.arg(7).isnil()) null else args.arg(7).tojstring()
            val callback = args.arg(8).checkfunction()

            asyncExecutor.submit {
                try {
                    val res = executePostRequest(url, 5, headers, body, host, port, user, pass)
                    callback.call(toLuaTable(res), NIL)
                } catch (e: Exception) {
                    callback.call(NIL, valueOf("Error: ${e.message}"))
                }
            }
            return TRUE
        }
    }

    // --- Оригинальные методы --- (остались без изменений)

    private val function = object : TwoArgFunction() {
        override fun call(url: LuaValue, timeout: LuaValue): LuaValue {
            val res = executeGetRequest(url.checkjstring(), if (timeout.isnil()) 5 else timeout.toint(), mutableMapOf(), null, 0, null, null)
            return toLuaTable(res)
        }
    }

    private val withHeadersFunction = object : TwoArgFunction() {
        override fun call(url: LuaValue, headersTable: LuaValue): LuaValue {
            val res = executeGetRequest(url.checkjstring(), 5, parseHeaders(headersTable), null, 0, null, null)
            return toLuaTable(res)
        }
    }

    private fun postFunction() = object : ThreeArgFunction() {
        override fun call(url: LuaValue, headersTable: LuaValue, body: LuaValue): LuaValue {
            val res = executePostRequest(url.checkjstring(), 5, parseHeaders(headersTable), body.checkjstring(), null, 0, null, null)
            return toLuaTable(res)
        }
    }

    private fun postWithHeadersFunction() = object : TwoArgFunction() {
        override fun call(url: LuaValue, body: LuaValue): LuaValue {
            val res = executePostRequest(url.checkjstring(), 5, mutableMapOf(), body.checkjstring(), null, 0, null, null)
            return toLuaTable(res)
        }
    }

    private val asyncWithHeadersCallbackFunction = object : ThreeArgFunction() {
        override fun call(url: LuaValue, headersTable: LuaValue, callback: LuaValue): LuaValue {
            val cb = callback.checkfunction()
            asyncExecutor.submit {
                try {
                    val res = executeGetRequest(url.checkjstring(), 5, parseHeaders(headersTable), null, 0, null, null)
                    cb.call(toLuaTable(res), NIL)
                } catch (e: Exception) {
                    cb.call(NIL, valueOf("Error: ${e.message}"))
                }
            }
            return TRUE
        }
    }

    private fun postAsyncWithHeadersCallbackFunction() = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val url = args.arg1().checkjstring()
            val headers = parseHeaders(args.arg(2))
            val body = args.arg(3).checkjstring()
            val callback = args.arg(4).checkfunction()

            asyncExecutor.submit {
                try {
                    val res = executePostRequest(url, 5, headers, body, null, 0, null, null)
                    callback.call(toLuaTable(res), NIL)
                } catch (e: Exception) {
                    callback.call(NIL, valueOf("Error: ${e.message}"))
                }
            }
            return TRUE
        }
    }

    fun shutdown() {
        asyncExecutor.shutdown()
    }

    companion object {
        private val HTTP_CLIENT: HttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }
}