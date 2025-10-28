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
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.protocol.HttpContext
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.LayeredConnectionSocketFactory
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLContext
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.luaj.vm2.lib.ZeroArgFunction
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.Proxy
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

        // Универсальные GET функции с прокси
        http.set("get_with_proxy", getWithProxyFunction())
        http.set("get_with_headers_and_proxy", getWithHeadersAndProxyFunction())
        http.set("get_async_with_proxy", getAsyncWithProxyFunction())
        http.set("get_async_with_headers_and_proxy", getAsyncWithHeadersAndProxyFunction())

        // Универсальные POST функции с прокси
        http.set("post_with_proxy", postWithProxyFunction())
        http.set("post_with_headers_and_proxy", postWithHeadersAndProxyFunction())
        http.set("post_async_with_proxy", postAsyncWithProxyFunction())
        http.set("post_async_with_headers_and_proxy", postAsyncWithHeadersAndProxyFunction())

        env.set("http", http)
        return http
    }

    // Вспомогательные методы для работы с прокси

    private fun createHttpClientWithProxy(
        proxyHost: String,
        proxyPort: Int,
        proxyType: String = "http",
        username: String? = null,
        password: String? = null
    ): org.apache.http.impl.client.CloseableHttpClient {

        // Настраиваем аутентификацию для SOCKS прокси
        if (proxyType.lowercase().startsWith("socks") && username != null && password != null) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password.toCharArray())
                }
            })
        }

        val proxy = HttpHost(proxyHost, proxyPort)

        // Создаем socket factory для SOCKS прокси
        val socketFactory: LayeredConnectionSocketFactory? = when (proxyType.lowercase()) {
            "socks", "socks4", "socks5" -> {
                object : LayeredConnectionSocketFactory {
                    private val plainFactory = PlainConnectionSocketFactory()

                    override fun createSocket(context: HttpContext): Socket {
                        val proxyAddress = InetSocketAddress(proxyHost, proxyPort)
                        val proxy = Proxy(Proxy.Type.SOCKS, proxyAddress)
                        return Socket(proxy)
                    }

                    override fun connectSocket(
                        connectTimeout: Int,
                        socket: Socket,
                        host: HttpHost,
                        remoteAddress: InetSocketAddress,
                        localAddress: InetSocketAddress?,
                        context: HttpContext
                    ): Socket {
                        socket.connect(remoteAddress, connectTimeout)
                        return socket
                    }

                    override fun createLayeredSocket(
                        socket: Socket,
                        target: String,
                        port: Int,
                        context: HttpContext
                    ): Socket {
                        return SSLContext.getDefault().socketFactory.createSocket(socket, target, port, true)
                    }
                }
            }
            else -> null
        }

        val requestConfig = RequestConfig.custom()
            .setConnectTimeout(5000)
            .setSocketTimeout(5000)
            .setProxy(proxy)
            .build()

        val builder = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)

        // Если указаны логин и пароль для HTTP прокси
        if (username != null && password != null && !proxyType.lowercase().startsWith("socks")) {
            val credentialsProvider = org.apache.http.impl.client.BasicCredentialsProvider()
            credentialsProvider.setCredentials(
                org.apache.http.auth.AuthScope(proxyHost, proxyPort),
                org.apache.http.auth.UsernamePasswordCredentials(username, password)
            )
            builder.setDefaultCredentialsProvider(credentialsProvider)
        }

        // Если это SOCKS прокси, настраиваем специальный connection manager
        if (socketFactory != null && proxyType.lowercase().startsWith("socks")) {
            val registry = RegistryBuilder.create<ConnectionSocketFactory>()
                .register("http", PlainConnectionSocketFactory())
                .register("https", socketFactory)
                .build()

            val connectionManager = PoolingHttpClientConnectionManager(registry)
            builder.setConnectionManager(connectionManager)
        }

        return builder.build()
    }

    // Вспомогательные методы для парсинга параметров прокси
    private fun parseProxyArgs(args: Varargs, startIndex: Int): ProxyConfig {
        val proxyHost = args.arg(startIndex).checkjstring()
        val proxyPort = args.arg(startIndex + 1).checkint()
        val proxyType = args.arg(startIndex + 2).optjstring("http")
        val username = args.arg(startIndex + 3).optjstring(null)
        val password = args.arg(startIndex + 4).optjstring(null)

        return ProxyConfig(proxyHost, proxyPort, proxyType, username, password)
    }

    data class ProxyConfig(
        val host: String,
        val port: Int,
        val type: String = "http",
        val username: String? = null,
        val password: String? = null
    )

    // Универсальные GET методы с прокси

    private fun getWithProxyFunction(): LuaValue {
        return object : LibFunction() {
            override fun invoke(args: Varargs): Varargs {
                return try {
                    val url = args.arg(1).checkjstring()

                    when (args.narg()) {
                        3 -> { // url, proxyHost, proxyPort
                            val proxyConfig = parseProxyArgs(args, 2)
                            val response = executeGetRequestWithProxy(
                                url, proxyConfig, emptyMap()
                            )
                            LuaValue.valueOf(response)
                        }
                        4 -> { // url, proxyHost, proxyPort, proxyType
                            val proxyConfig = parseProxyArgs(args, 2)
                            val response = executeGetRequestWithProxy(
                                url, proxyConfig, emptyMap()
                            )
                            LuaValue.valueOf(response)
                        }
                        6 -> { // url, proxyHost, proxyPort, proxyType, username, password
                            val proxyConfig = parseProxyArgs(args, 2)
                            val response = executeGetRequestWithProxy(
                                url, proxyConfig, emptyMap()
                            )
                            LuaValue.valueOf(response)
                        }
                        else -> throw LuaError("Invalid number of arguments for get_with_proxy")
                    }
                } catch (e: Exception) {
                    throw LuaError("HTTP GET with proxy error: ${e.message}")
                }
            }
        }
    }

    private fun getWithHeadersAndProxyFunction(): LuaValue {
        return object : LibFunction() {
            override fun invoke(args: Varargs): Varargs {
                return try {
                    val url = args.arg(1).checkjstring()
                    val headersTable = args.arg(2)
                    val headers = parseHeaders(headersTable)

                    when (args.narg()) {
                        4 -> { // url, headers, proxyHost, proxyPort
                            val proxyConfig = parseProxyArgs(args, 3)
                            val response = executeGetRequestWithProxy(
                                url, proxyConfig, headers
                            )
                            LuaValue.valueOf(response)
                        }
                        5 -> { // url, headers, proxyHost, proxyPort, proxyType
                            val proxyConfig = parseProxyArgs(args, 3)
                            val response = executeGetRequestWithProxy(
                                url, proxyConfig, headers
                            )
                            LuaValue.valueOf(response)
                        }
                        7 -> { // url, headers, proxyHost, proxyPort, proxyType, username, password
                            val proxyConfig = parseProxyArgs(args, 3)
                            val response = executeGetRequestWithProxy(
                                url, proxyConfig, headers
                            )
                            LuaValue.valueOf(response)
                        }
                        else -> throw LuaError("Invalid number of arguments for get_with_headers_and_proxy")
                    }
                } catch (e: Exception) {
                    throw LuaError("HTTP GET with headers and proxy error: ${e.message}")
                }
            }
        }
    }

    private fun getAsyncWithProxyFunction(): LuaValue {
        return object : LibFunction() {
            override fun invoke(args: Varargs): Varargs {
                val url = args.arg(1).checkjstring()

                // Проверяем, есть ли callback функция в конце
                val lastArg = args.arg(args.narg())
                val hasCallback = lastArg.isfunction()

                val proxyConfig = when {
                    hasCallback && args.narg() == 4 -> parseProxyArgs(args, 2) // url, proxyHost, proxyPort, callback
                    hasCallback && args.narg() == 5 -> parseProxyArgs(args, 2) // url, proxyHost, proxyPort, proxyType, callback
                    hasCallback && args.narg() == 7 -> parseProxyArgs(args, 2) // url, proxyHost, proxyPort, proxyType, username, password, callback
                    !hasCallback && args.narg() == 3 -> parseProxyArgs(args, 2) // url, proxyHost, proxyPort
                    !hasCallback && args.narg() == 4 -> parseProxyArgs(args, 2) // url, proxyHost, proxyPort, proxyType
                    !hasCallback && args.narg() == 6 -> parseProxyArgs(args, 2) // url, proxyHost, proxyPort, proxyType, username, password
                    else -> throw LuaError("Invalid number of arguments for get_async_with_proxy")
                }

                return if (hasCallback) {
                    // Режим с callback
                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                executeGetRequestWithProxy(url, proxyConfig, emptyMap())
                            }
                            lastArg.call(LuaValue.valueOf(response), LuaValue.NIL)
                        } catch (e: Exception) {
                            lastArg.call(LuaValue.NIL, LuaValue.valueOf("Error: ${e.message}"))
                        }
                    }
                    LuaValue.TRUE
                } else {
                    // Режим с await
                    AsyncResult { callback ->
                        coroutineScope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    executeGetRequestWithProxy(url, proxyConfig, emptyMap())
                                }
                                callback.onSuccess(LuaValue.valueOf(response))
                            } catch (e: Exception) {
                                callback.onError(LuaValue.valueOf("HTTP async GET with proxy error: ${e.message}"))
                            }
                        }
                    }.asLuaValue()
                }
            }
        }
    }

    private fun getAsyncWithHeadersAndProxyFunction(): LuaValue {
        return object : LibFunction() {
            override fun invoke(args: Varargs): Varargs {
                val url = args.arg(1).checkjstring()
                val headersTable = args.arg(2)
                val headers = parseHeaders(headersTable)

                // Проверяем, есть ли callback функция в конце
                val lastArg = args.arg(args.narg())
                val hasCallback = lastArg.isfunction()

                val proxyConfig = when {
                    hasCallback && args.narg() == 5 -> parseProxyArgs(args, 3) // url, headers, proxyHost, proxyPort, callback
                    hasCallback && args.narg() == 6 -> parseProxyArgs(args, 3) // url, headers, proxyHost, proxyPort, proxyType, callback
                    hasCallback && args.narg() == 8 -> parseProxyArgs(args, 3) // url, headers, proxyHost, proxyPort, proxyType, username, password, callback
                    !hasCallback && args.narg() == 4 -> parseProxyArgs(args, 3) // url, headers, proxyHost, proxyPort
                    !hasCallback && args.narg() == 5 -> parseProxyArgs(args, 3) // url, headers, proxyHost, proxyPort, proxyType
                    !hasCallback && args.narg() == 7 -> parseProxyArgs(args, 3) // url, headers, proxyHost, proxyPort, proxyType, username, password
                    else -> throw LuaError("Invalid number of arguments for get_async_with_headers_and_proxy")
                }

                return if (hasCallback) {
                    // Режим с callback
                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                executeGetRequestWithProxy(url, proxyConfig, headers)
                            }
                            lastArg.call(LuaValue.valueOf(response), LuaValue.NIL)
                        } catch (e: Exception) {
                            lastArg.call(LuaValue.NIL, LuaValue.valueOf("Error: ${e.message}"))
                        }
                    }
                    LuaValue.TRUE
                } else {
                    // Режим с await
                    AsyncResult { callback ->
                        coroutineScope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    executeGetRequestWithProxy(url, proxyConfig, headers)
                                }
                                callback.onSuccess(LuaValue.valueOf(response))
                            } catch (e: Exception) {
                                callback.onError(LuaValue.valueOf("HTTP async GET with headers and proxy error: ${e.message}"))
                            }
                        }
                    }.asLuaValue()
                }
            }
        }
    }

    // Универсальные POST методы с прокси

    private fun postWithProxyFunction(): LuaValue {
        return object : LibFunction() {
            override fun invoke(args: Varargs): Varargs {
                return try {
                    val url = args.arg(1).checkjstring()
                    val body = args.arg(2).checkjstring()

                    when (args.narg()) {
                        4 -> { // url, body, proxyHost, proxyPort
                            val proxyConfig = parseProxyArgs(args, 3)
                            val response = executePostRequestWithProxy(
                                url, proxyConfig, emptyMap(), body
                            )
                            LuaValue.valueOf(response)
                        }
                        5 -> { // url, body, proxyHost, proxyPort, proxyType
                            val proxyConfig = parseProxyArgs(args, 3)
                            val response = executePostRequestWithProxy(
                                url, proxyConfig, emptyMap(), body
                            )
                            LuaValue.valueOf(response)
                        }
                        7 -> { // url, body, proxyHost, proxyPort, proxyType, username, password
                            val proxyConfig = parseProxyArgs(args, 3)
                            val response = executePostRequestWithProxy(
                                url, proxyConfig, emptyMap(), body
                            )
                            LuaValue.valueOf(response)
                        }
                        else -> throw LuaError("Invalid number of arguments for post_with_proxy")
                    }
                } catch (e: Exception) {
                    throw LuaError("HTTP POST with proxy error: ${e.message}")
                }
            }
        }
    }

    private fun postWithHeadersAndProxyFunction(): LuaValue {
        return object : LibFunction() {
            override fun invoke(args: Varargs): Varargs {
                return try {
                    val url = args.arg(1).checkjstring()
                    val headersTable = args.arg(2)
                    val body = args.arg(3).checkjstring()
                    val headers = parseHeaders(headersTable)

                    when (args.narg()) {
                        5 -> { // url, headers, body, proxyHost, proxyPort
                            val proxyConfig = parseProxyArgs(args, 4)
                            val response = executePostRequestWithProxy(
                                url, proxyConfig, headers, body
                            )
                            LuaValue.valueOf(response)
                        }
                        6 -> { // url, headers, body, proxyHost, proxyPort, proxyType
                            val proxyConfig = parseProxyArgs(args, 4)
                            val response = executePostRequestWithProxy(
                                url, proxyConfig, headers, body
                            )
                            LuaValue.valueOf(response)
                        }
                        8 -> { // url, headers, body, proxyHost, proxyPort, proxyType, username, password
                            val proxyConfig = parseProxyArgs(args, 4)
                            val response = executePostRequestWithProxy(
                                url, proxyConfig, headers, body
                            )
                            LuaValue.valueOf(response)
                        }
                        else -> throw LuaError("Invalid number of arguments for post_with_headers_and_proxy")
                    }
                } catch (e: Exception) {
                    throw LuaError("HTTP POST with headers and proxy error: ${e.message}")
                }
            }
        }
    }

    private fun postAsyncWithProxyFunction(): LuaValue {
        return object : LibFunction() {
            override fun invoke(args: Varargs): Varargs {
                val url = args.arg(1).checkjstring()
                val body = args.arg(2).checkjstring()

                // Проверяем, есть ли callback функция в конце
                val lastArg = args.arg(args.narg())
                val hasCallback = lastArg.isfunction()

                val proxyConfig = when {
                    hasCallback && args.narg() == 5 -> parseProxyArgs(args, 3) // url, body, proxyHost, proxyPort, callback
                    hasCallback && args.narg() == 6 -> parseProxyArgs(args, 3) // url, body, proxyHost, proxyPort, proxyType, callback
                    hasCallback && args.narg() == 8 -> parseProxyArgs(args, 3) // url, body, proxyHost, proxyPort, proxyType, username, password, callback
                    !hasCallback && args.narg() == 4 -> parseProxyArgs(args, 3) // url, body, proxyHost, proxyPort
                    !hasCallback && args.narg() == 5 -> parseProxyArgs(args, 3) // url, body, proxyHost, proxyPort, proxyType
                    !hasCallback && args.narg() == 7 -> parseProxyArgs(args, 3) // url, body, proxyHost, proxyPort, proxyType, username, password
                    else -> throw LuaError("Invalid number of arguments for post_async_with_proxy")
                }

                return if (hasCallback) {
                    // Режим с callback
                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                executePostRequestWithProxy(url, proxyConfig, emptyMap(), body)
                            }
                            lastArg.call(LuaValue.valueOf(response), LuaValue.NIL)
                        } catch (e: Exception) {
                            lastArg.call(LuaValue.NIL, LuaValue.valueOf("Error: ${e.message}"))
                        }
                    }
                    LuaValue.TRUE
                } else {
                    // Режим с await
                    AsyncResult { callback ->
                        coroutineScope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    executePostRequestWithProxy(url, proxyConfig, emptyMap(), body)
                                }
                                callback.onSuccess(LuaValue.valueOf(response))
                            } catch (e: Exception) {
                                callback.onError(LuaValue.valueOf("HTTP async POST with proxy error: ${e.message}"))
                            }
                        }
                    }.asLuaValue()
                }
            }
        }
    }

    private fun postAsyncWithHeadersAndProxyFunction(): LuaValue {
        return object : LibFunction() {
            override fun invoke(args: Varargs): Varargs {
                val url = args.arg(1).checkjstring()
                val headersTable = args.arg(2)
                val body = args.arg(3).checkjstring()
                val headers = parseHeaders(headersTable)

                // Проверяем, есть ли callback функция в конце
                val lastArg = args.arg(args.narg())
                val hasCallback = lastArg.isfunction()

                val proxyConfig = when {
                    hasCallback && args.narg() == 6 -> parseProxyArgs(args, 4) // url, headers, body, proxyHost, proxyPort, callback
                    hasCallback && args.narg() == 7 -> parseProxyArgs(args, 4) // url, headers, body, proxyHost, proxyPort, proxyType, callback
                    hasCallback && args.narg() == 9 -> parseProxyArgs(args, 4) // url, headers, body, proxyHost, proxyPort, proxyType, username, password, callback
                    !hasCallback && args.narg() == 5 -> parseProxyArgs(args, 4) // url, headers, body, proxyHost, proxyPort
                    !hasCallback && args.narg() == 6 -> parseProxyArgs(args, 4) // url, headers, body, proxyHost, proxyPort, proxyType
                    !hasCallback && args.narg() == 8 -> parseProxyArgs(args, 4) // url, headers, body, proxyHost, proxyPort, proxyType, username, password
                    else -> throw LuaError("Invalid number of arguments for post_async_with_headers_and_proxy")
                }

                return if (hasCallback) {
                    // Режим с callback
                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                executePostRequestWithProxy(url, proxyConfig, headers, body)
                            }
                            lastArg.call(LuaValue.valueOf(response), LuaValue.NIL)
                        } catch (e: Exception) {
                            lastArg.call(LuaValue.NIL, LuaValue.valueOf("Error: ${e.message}"))
                        }
                    }
                    LuaValue.TRUE
                } else {
                    // Режим с await
                    AsyncResult { callback ->
                        coroutineScope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    executePostRequestWithProxy(url, proxyConfig, headers, body)
                                }
                                callback.onSuccess(LuaValue.valueOf(response))
                            } catch (e: Exception) {
                                callback.onError(LuaValue.valueOf("HTTP async POST with headers and proxy error: ${e.message}"))
                            }
                        }
                    }.asLuaValue()
                }
            }
        }
    }

    // Методы выполнения запросов с прокси

    private fun executeGetRequestWithProxy(
        url: String,
        proxyConfig: ProxyConfig,
        headers: Map<String, String>
    ): String {
        val httpGet = HttpGet(URI.create(url))

        val requestConfig = RequestConfig.custom()
            .setConnectTimeout(5000)
            .setSocketTimeout(5000)
            .setProxy(HttpHost(proxyConfig.host, proxyConfig.port))
            .build()
        httpGet.config = requestConfig

        headers.forEach { (key, value) ->
            httpGet.addHeader(key, value)
        }

        return createHttpClientWithProxy(
            proxyConfig.host,
            proxyConfig.port,
            proxyConfig.type,
            proxyConfig.username,
            proxyConfig.password
        ).use { client ->
            client.execute(httpGet).use { response ->
                val entity = response.entity
                if (entity != null) {
                    EntityUtils.toString(entity)
                } else {
                    throw RuntimeException("Empty response")
                }
            }
        }
    }

    private fun executePostRequestWithProxy(
        url: String,
        proxyConfig: ProxyConfig,
        headers: Map<String, String>,
        body: String
    ): String {
        val httpPost = HttpPost(URI.create(url))

        val requestConfig = RequestConfig.custom()
            .setConnectTimeout(5000)
            .setSocketTimeout(5000)
            .setProxy(HttpHost(proxyConfig.host, proxyConfig.port))
            .build()
        httpPost.config = requestConfig

        headers.forEach { (key, value) ->
            httpPost.addHeader(key, value)
        }

        if (body.isNotEmpty()) {
            httpPost.entity = StringEntity(body, "UTF-8")
            httpPost.setHeader("Content-type", "application/json")
        }

        return createHttpClientWithProxy(
            proxyConfig.host,
            proxyConfig.port,
            proxyConfig.type,
            proxyConfig.username,
            proxyConfig.password
        ).use { client ->
            client.execute(httpPost).use { response ->
                val entity = response.entity
                if (entity != null) {
                    EntityUtils.toString(entity)
                } else {
                    throw RuntimeException("Empty response")
                }
            }
        }
    }

    private fun executePostRequest(url: String, timeout: Int, headers: Map<String, String>, body: String): String {
        val httpPost = HttpPost(URI.create(url))

        val requestConfig = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build()
        httpPost.config = requestConfig

        headers.forEach { (key, value) ->
            httpPost.addHeader(key, value)
        }

        if (body.isNotEmpty()) {
            httpPost.entity = StringEntity(body, "UTF-8")
            httpPost.setHeader("Content-type", "application/json")
        }

        return HttpClients.createDefault().use { client ->
            client.execute(httpPost).use { response ->
                val entity = response.entity
                if (entity != null) {
                    EntityUtils.toString(entity)
                } else {
                    throw RuntimeException("Empty response")
                }
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

    private fun executeGetRequest(url: String, timeout: Int, headers: Map<String, String>): String {
        val httpGet = HttpGet(URI.create(url))

        val requestConfig = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build()
        httpGet.config = requestConfig

        headers.forEach { (key, value) ->
            httpGet.addHeader(key, value)
        }

        return HttpClients.createDefault().use { client ->
            client.execute(httpGet).use { response ->
                val entity = response.entity
                if (entity != null) {
                    EntityUtils.toString(entity)
                } else {
                    throw RuntimeException("Empty response")
                }
            }
        }
    }

    // Классы для поддержки дополнительных аргументов

    abstract class FourArgFunction : LibFunction() {
        abstract override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue, arg4: LuaValue): LuaValue

        override fun invoke(args: Varargs): Varargs {
            return call(args.arg1(), args.arg(2), args.arg(3), args.arg(4))
        }
    }

    abstract class FiveArgFunction : LibFunction() {
        abstract fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue, arg4: LuaValue, arg5: LuaValue): LuaValue

        override fun invoke(args: Varargs): Varargs {
            return call(args.arg1(), args.arg(2), args.arg(3), args.arg(4), args.arg(5))
        }
    }

    // GET функции без прокси

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

    private fun getAsyncCallbackFunction(): LuaValue {
        return object : TwoArgFunction() {
            override fun call(url: LuaValue, callback: LuaValue): LuaValue {
                return if (callback.isfunction()) {
                    coroutineScope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                executeGetRequest(url.checkjstring(), 5000, emptyMap())
                            }
                            callback.call(LuaValue.valueOf(response), LuaValue.NIL)
                        } catch (e: Exception) {
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

    // POST функции без прокси

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
}