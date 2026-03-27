package com.nekiplay.hypixelcry.features.lua.objects.misc

import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class TCPLib : LuaValue() {
    private val connections = ConcurrentHashMap<Int, TCPConnection>()
    private val servers = ConcurrentHashMap<Int, ServerSocket>() // Карта серверов
    private val nextId = AtomicInteger(1)
    private val nextServerId = AtomicInteger(1) // ID для серверов

    data class TCPConnection(
        val socket: Socket,
        val inputStream: InputStream,
        val outputStream: OutputStream,
        val reader: BufferedReader,
        val writer: BufferedWriter
    )

    override fun typename(): String = "tcp"
    override fun tojstring(): String = "TCPObject"
    override fun isnil(): Boolean = false
    override fun type(): Int = TUSERDATA

    override fun call(): LuaValue = this

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "connect" -> Connect()
            "disconnect" -> Disconnect()
            "send" -> Send()
            "receive" -> Receive()
            "sendBytes" -> SendBytes()
            "receiveBytes" -> ReceiveBytes()
            "isConnected" -> IsConnected()
            "getLocalAddress" -> GetLocalAddress()
            "getRemoteAddress" -> GetRemoteAddress()
            "setBlocking" -> SetBlocking()
            "setTimeout" -> SetTimeout()
            "getSocketCount" -> GetSocketCount()

            "listen" -> Listen()
            "accept" -> Accept()
            "closeServer" -> CloseServer()
            else -> super.get(key)
        }
    }

    // --- СЕРВЕРНЫЕ МЕТОДЫ ---

    inner class Listen : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val port = args.arg(1).checkint()
            val host = args.arg(2).optjstring("0.0.0.0")
            val backlog = args.arg(3).optint(50)

            return try {
                val serverSocket = ServerSocket(port, backlog, InetAddress.getByName(host))
                val serverId = nextServerId.getAndIncrement()
                servers[serverId] = serverSocket

                varargsOf(arrayOf(valueOf(serverId), NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Failed to start listener")))
            }
        }
    }

    inner class Accept : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val serverId = args.arg(1).checkint()
            val timeout = args.arg(2).optint(0) // 0 = бесконечное ожидание

            val server = servers[serverId] ?: return varargsOf(arrayOf(NIL, valueOf("Server not found")))

            return try {
                server.soTimeout = timeout
                val socket = server.accept() // Блокирует поток до подключения

                // Создаем стандартное соединение из принятого сокета
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

                val connectionId = nextId.getAndIncrement()
                val connection = TCPConnection(
                    socket,
                    socket.getInputStream(),
                    socket.getOutputStream(),
                    reader,
                    writer
                )
                connections[connectionId] = connection

                varargsOf(arrayOf(valueOf(connectionId), NIL))
            } catch (e: SocketTimeoutException) {
                varargsOf(arrayOf(NIL, valueOf("Timeout")))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Accept failed")))
            }
        }
    }

    inner class CloseServer : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val serverId = arg.checkint()
            val server = servers.remove(serverId) ?: return FALSE
            return try {
                server.close()
                TRUE
            } catch (e: Exception) {
                FALSE
            }
        }
    }

    inner class Connect : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val host = args.arg(1).checkjstring()
            val port = args.arg(2).checkint()
            val timeout = if (args.narg() > 2) args.arg(3).checkint() else 5000

            return try {
                val socket = Socket()
                socket.soTimeout = timeout
                socket.connect(InetSocketAddress(host, port), timeout)

                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

                val connectionId = nextId.getAndIncrement()
                val connection = TCPConnection(
                    socket,
                    socket.getInputStream(),
                    socket.getOutputStream(),
                    reader,
                    writer
                )
                connections[connectionId] = connection

                varargsOf(arrayOf(valueOf(connectionId), NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Connection failed")))
            }
        }
    }

    inner class Disconnect : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val connectionId = arg.checkint()
            val connection = connections.remove(connectionId) ?: return FALSE
            return try {
                connection.reader.close()
                connection.writer.close()
                connection.socket.close()
                TRUE
            } catch (e: Exception) {
                FALSE
            }
        }
    }

    inner class Send : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val message = args.arg(2).checkjstring()
            val newline = if (args.narg() > 2) args.arg(3).toboolean() else true

            val connection = connections[connectionId] ?: return varargsOf(
                arrayOf(FALSE, valueOf("Connection not found"))
            )

            return try {
                if (newline) {
                    connection.writer.write(message)
                    connection.writer.newLine()
                } else {
                    connection.writer.write(message)
                }
                connection.writer.flush()
                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(FALSE, valueOf(e.message ?: "Send failed")))
            }
        }
    }

    inner class SendBytes : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val bytesTable = args.arg(2).checktable()
            val length = bytesTable.length()

            val connection = connections[connectionId] ?: return varargsOf(
                arrayOf(FALSE, valueOf("Connection not found"))
            )

            return try {
                val byteArray = ByteArray(length)
                for (i in 1..length) {
                    val byteValue = bytesTable.get(i).checkint()
                    byteArray[i - 1] = (byteValue and 0xFF).toByte()
                }

                connection.outputStream.write(byteArray)
                connection.outputStream.flush()
                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(FALSE, valueOf(e.message ?: "Send bytes failed")))
            }
        }
    }

    inner class Receive : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val timeout = if (args.narg() > 1) args.arg(2).checkint() else -1

            val connection = connections[connectionId] ?: return varargsOf(
                arrayOf(NIL, valueOf("Connection not found"))
            )

            return try {
                if (timeout >= 0) {
                    connection.socket.soTimeout = timeout
                }

                val line = connection.reader.readLine()
                if (line == null) {
                    varargsOf(arrayOf(NIL, valueOf("Connection closed")))
                } else {
                    varargsOf(arrayOf(valueOf(line), NIL))
                }
            } catch (e: SocketTimeoutException) {
                varargsOf(arrayOf(NIL, valueOf("Timeout")))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Receive failed")))
            }
        }
    }

    inner class ReceiveBytes : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val timeout = if (args.narg() > 1) args.arg(2).checkint() else -1
            val maxBytes = if (args.narg() > 2) args.arg(3).checkint() else 1024

            val connection = connections[connectionId] ?: return varargsOf(
                arrayOf(NIL, valueOf("Connection not found"))
            )

            return try {
                if (timeout >= 0) {
                    connection.socket.soTimeout = timeout
                }

                val buffer = ByteArray(maxBytes)
                val bytesRead = connection.inputStream.read(buffer)

                if (bytesRead == -1) {
                    varargsOf(arrayOf(NIL, valueOf("Connection closed")))
                } else {
                    val bytesTable = LuaTable()
                    for (i in 0 until bytesRead) {
                        bytesTable.set(i + 1, valueOf(buffer[i].toInt() and 0xFF))
                    }
                    varargsOf(arrayOf(bytesTable, NIL))
                }
            } catch (e: SocketTimeoutException) {
                varargsOf(arrayOf(NIL, valueOf("Timeout")))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Receive bytes failed")))
            }
        }
    }

    inner class IsConnected : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val connectionId = arg.checkint()
            val connection = connections[connectionId] ?: return FALSE
            return valueOf(!connection.socket.isClosed && connection.socket.isConnected)
        }
    }

    inner class GetLocalAddress : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val connectionId = arg.checkint()
            val connection = connections[connectionId] ?: return NIL
            val addr = connection.socket.localAddress
            val table = LuaTable()
            table.set("address", addr.hostAddress)
            table.set("port", connection.socket.localPort)
            return table
        }
    }

    inner class GetRemoteAddress : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val connectionId = arg.checkint()
            val connection = connections[connectionId] ?: return NIL
            val addr = connection.socket.inetAddress
            val table = LuaTable()
            table.set("address", addr.hostAddress)
            table.set("port", connection.socket.port)
            return table
        }
    }

    inner class SetBlocking : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val blocking = args.arg(2).toboolean()
            val connection = connections[connectionId] ?: return varargsOf(arrayOf(FALSE, valueOf("Not found")))
            connection.socket.soTimeout = if (blocking) 0 else 500
            return TRUE
        }
    }

    inner class SetTimeout : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val timeout = args.arg(2).checkint()
            val connection = connections[connectionId] ?: return FALSE
            connection.socket.soTimeout = timeout
            return TRUE
        }
    }

    inner class GetSocketCount : ZeroArgFunction() {
        override fun call(): LuaValue = valueOf(connections.size)
    }

    fun cleanup() {
        // Закрываем клиентские соединения
        connections.values.forEach {
            try { it.socket.close() } catch (e: Exception) {}
        }
        connections.clear()

        // Закрываем серверные сокеты
        servers.values.forEach {
            try { it.close() } catch (e: Exception) {}
        }
        servers.clear()
    }
}