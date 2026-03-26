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
    private val nextId = AtomicInteger(1)
    private val scriptConnections = ArrayList<Int>()

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
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

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
            else -> super.get(key)
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
            val connection = connections[connectionId] ?: return FALSE

            return try {
                connection.reader.close()
                connection.writer.close()
                connection.socket.close()
                connections.remove(connectionId)
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
                    if (byteValue < 0 || byteValue > 255) {
                        return varargsOf(arrayOf(FALSE, valueOf("Invalid byte value: $byteValue")))
                    }
                    byteArray[i - 1] = byteValue.toByte()
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
                if (timeout > 0) {
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
                if (timeout > 0) {
                    connection.socket.soTimeout = timeout
                }

                val buffer = ByteArray(maxBytes)
                val bytesRead = connection.inputStream.read(buffer)
                
                if (bytesRead == -1) {
                    varargsOf(arrayOf(NIL, valueOf("Connection closed")))
                } else {
                    val bytesTable = tableOf()
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

            return valueOf(connection.socket.isConnected && !connection.socket.isClosed)
        }
    }

    inner class GetLocalAddress : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val connectionId = arg.checkint()
            val connection = connections[connectionId] ?: return NIL

            val localAddress = connection.socket.localAddress
            val localPort = connection.socket.localPort

            val table = tableOf()
            table.set("address", valueOf(localAddress.hostAddress))
            table.set("port", valueOf(localPort))
            table.set("hostname", valueOf(localAddress.hostName))

            return table
        }
    }

    inner class GetRemoteAddress : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val connectionId = arg.checkint()
            val connection = connections[connectionId] ?: return NIL

            val remoteAddress = connection.socket.inetAddress
            val remotePort = connection.socket.port

            val table = tableOf()
            table.set("address", valueOf(remoteAddress.hostAddress))
            table.set("port", valueOf(remotePort))
            table.set("hostname", valueOf(remoteAddress.hostName))

            return table
        }
    }

    inner class SetBlocking : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val blocking = args.arg(2).toboolean()

            val connection = connections[connectionId] ?: return varargsOf(
                arrayOf(FALSE, valueOf("Connection not found"))
            )

            return try {
                connection.socket.soTimeout = if (blocking) 0 else 1000
                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(FALSE, valueOf(e.message ?: "Set blocking failed")))
            }
        }
    }

    inner class SetTimeout : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val timeout = args.arg(2).checkint()

            val connection = connections[connectionId] ?: return varargsOf(
                arrayOf(FALSE, valueOf("Connection not found"))
            )

            return try {
                connection.socket.soTimeout = timeout
                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(FALSE, valueOf(e.message ?: "Set timeout failed")))
            }
        }
    }

    inner class GetSocketCount : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(connections.size)
        }
    }

    fun cleanup() {
        // Закрываем все соединения при остановке
        connections.values.forEach { connection ->
            try {
                connection.reader.close()
                connection.writer.close()
                connection.socket.close()
            } catch (e: Exception) {
                // Игнорируем ошибки при закрытии
            }
        }
        connections.clear()
        scriptConnections.clear()
    }
}