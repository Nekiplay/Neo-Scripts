package com.nekiplay.hypixelcry.features.lua.objects.misc

import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import java.io.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class TCPLib : TwoArgFunction() {
    private val connections = ConcurrentHashMap<Int, TCPConnection>()
    private val nextId = AtomicInteger(1)
    private val scriptConnections = ConcurrentHashMap<String, MutableSet<Int>>()

    data class TCPConnection(
        val socket: Socket,
        val inputStream: InputStream,
        val outputStream: OutputStream,
        val reader: BufferedReader,
        val writer: BufferedWriter
    )

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val library = LuaTable()
        library.set("connect", Connect())
        library.set("disconnect", Disconnect())
        library.set("send", Send())
        library.set("receive", Receive())
        library.set("sendBytes", SendBytes())
        library.set("receiveBytes", ReceiveBytes())
        library.set("isConnected", IsConnected())
        library.set("getLocalAddress", GetLocalAddress())
        library.set("getRemoteAddress", GetRemoteAddress())
        library.set("setBlocking", SetBlocking())
        library.set("setTimeout", SetTimeout())
        library.set("getSocketCount", GetSocketCount())
        env.set("tcp", library)

        // Store script identifier for connection tracking
        val scriptId = modname.tojstring()
        scriptConnections.putIfAbsent(scriptId, mutableSetOf())

        return library
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
                val connection = TCPConnection(socket, reader, writer)
                connections[connectionId] = connection
                
                // Associate connection with current script
                val scriptId = modname.tojstring()
                val scriptConnectionsSet = scriptConnections.getOrDefault(scriptId, mutableSetOf())
                scriptConnectionsSet.add(connectionId)
                scriptConnections[scriptId] = scriptConnectionsSet
                
                LuaValue.varargsOf(arrayOf(LuaValue.valueOf(connectionId), LuaValue.NIL))
            } catch (e: Exception) {
                LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "Connection failed")))
            }
        }
    }

    inner class Disconnect : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val connectionId = arg.checkint()
            val connection = connections[connectionId] ?: return LuaValue.FALSE

            return try {
                connection.reader.close()
                connection.writer.close()
                connection.socket.close()
                connections.remove(connectionId)
                LuaValue.TRUE
            } catch (e: Exception) {
                LuaValue.FALSE
            }
        }
    }

    inner class Send : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val message = args.arg(2).checkjstring()
            val newline = if (args.narg() > 2) args.arg(3).toboolean() else true

            val connection = connections[connectionId] ?: return LuaValue.varargsOf(
                arrayOf(LuaValue.FALSE, LuaValue.valueOf("Connection not found"))
            )

            return try {
                if (newline) {
                    connection.writer.write(message)
                    connection.writer.newLine()
                } else {
                    connection.writer.write(message)
                }
                connection.writer.flush()
                LuaValue.varargsOf(arrayOf(LuaValue.TRUE, LuaValue.NIL))
            } catch (e: Exception) {
                LuaValue.varargsOf(arrayOf(LuaValue.FALSE, LuaValue.valueOf(e.message ?: "Send failed")))
            }
        }
    }

    inner class SendBytes : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val bytesTable = args.arg(2).checktable()
            val length = bytesTable.length()

            val connection = connections[connectionId] ?: return LuaValue.varargsOf(
                arrayOf(LuaValue.FALSE, LuaValue.valueOf("Connection not found"))
            )

            return try {
                val byteArray = ByteArray(length)
                for (i in 1..length) {
                    val byteValue = bytesTable.get(i).checkint()
                    if (byteValue < 0 || byteValue > 255) {
                        return LuaValue.varargsOf(arrayOf(LuaValue.FALSE, LuaValue.valueOf("Invalid byte value: $byteValue")))
                    }
                    byteArray[i - 1] = byteValue.toByte()
                }
                
                connection.outputStream.write(byteArray)
                connection.outputStream.flush()
                LuaValue.varargsOf(arrayOf(LuaValue.TRUE, LuaValue.NIL))
            } catch (e: Exception) {
                LuaValue.varargsOf(arrayOf(LuaValue.FALSE, LuaValue.valueOf(e.message ?: "Send bytes failed")))
            }
        }
    }

    inner class Receive : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val timeout = if (args.narg() > 1) args.arg(2).checkint() else -1

            val connection = connections[connectionId] ?: return LuaValue.varargsOf(
                arrayOf(LuaValue.NIL, LuaValue.valueOf("Connection not found"))
            )

            return try {
                if (timeout > 0) {
                    connection.socket.soTimeout = timeout
                }

                val line = connection.reader.readLine()
                if (line == null) {
                    LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("Connection closed")))
                } else {
                    LuaValue.varargsOf(arrayOf(LuaValue.valueOf(line), LuaValue.NIL))
                }
            } catch (e: SocketTimeoutException) {
                LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("Timeout")))
            } catch (e: Exception) {
                LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "Receive failed")))
            }
        }
    }

    inner class ReceiveBytes : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val timeout = if (args.narg() > 1) args.arg(2).checkint() else -1
            val maxBytes = if (args.narg() > 2) args.arg(3).checkint() else 1024

            val connection = connections[connectionId] ?: return LuaValue.varargsOf(
                arrayOf(LuaValue.NIL, LuaValue.valueOf("Connection not found"))
            )

            return try {
                if (timeout > 0) {
                    connection.socket.soTimeout = timeout
                }

                val buffer = ByteArray(maxBytes)
                val bytesRead = connection.inputStream.read(buffer)
                
                if (bytesRead == -1) {
                    LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("Connection closed")))
                } else {
                    val bytesTable = LuaValue.tableOf()
                    for (i in 0 until bytesRead) {
                        bytesTable.set(i + 1, LuaValue.valueOf(buffer[i].toInt() and 0xFF))
                    }
                    LuaValue.varargsOf(arrayOf(bytesTable, LuaValue.NIL))
                }
            } catch (e: SocketTimeoutException) {
                LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("Timeout")))
            } catch (e: Exception) {
                LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "Receive bytes failed")))
            }
        }
    }

    inner class IsConnected : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val connectionId = arg.checkint()
            val connection = connections[connectionId] ?: return LuaValue.FALSE

            return LuaValue.valueOf(connection.socket.isConnected && !connection.socket.isClosed)
        }
    }

    inner class GetLocalAddress : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val connectionId = arg.checkint()
            val connection = connections[connectionId] ?: return LuaValue.NIL

            val localAddress = connection.socket.localAddress
            val localPort = connection.socket.localPort

            val table = LuaValue.tableOf()
            table.set("address", LuaValue.valueOf(localAddress.hostAddress))
            table.set("port", LuaValue.valueOf(localPort))
            table.set("hostname", LuaValue.valueOf(localAddress.hostName))

            return table
        }
    }

    inner class GetRemoteAddress : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val connectionId = arg.checkint()
            val connection = connections[connectionId] ?: return LuaValue.NIL

            val remoteAddress = connection.socket.inetAddress
            val remotePort = connection.socket.port

            val table = LuaValue.tableOf()
            table.set("address", LuaValue.valueOf(remoteAddress.hostAddress))
            table.set("port", LuaValue.valueOf(remotePort))
            table.set("hostname", LuaValue.valueOf(remoteAddress.hostName))

            return table
        }
    }

    inner class SetBlocking : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val blocking = args.arg(2).toboolean()

            val connection = connections[connectionId] ?: return LuaValue.varargsOf(
                arrayOf(LuaValue.FALSE, LuaValue.valueOf("Connection not found"))
            )

            return try {
                connection.socket.soTimeout = if (blocking) 0 else 1000
                LuaValue.varargsOf(arrayOf(LuaValue.TRUE, LuaValue.NIL))
            } catch (e: Exception) {
                LuaValue.varargsOf(arrayOf(LuaValue.FALSE, LuaValue.valueOf(e.message ?: "Set blocking failed")))
            }
        }
    }

    inner class SetTimeout : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val connectionId = args.arg(1).checkint()
            val timeout = args.arg(2).checkint()

            val connection = connections[connectionId] ?: return LuaValue.varargsOf(
                arrayOf(LuaValue.FALSE, LuaValue.valueOf("Connection not found"))
            )

            return try {
                connection.socket.soTimeout = timeout
                LuaValue.varargsOf(arrayOf(LuaValue.TRUE, LuaValue.NIL))
            } catch (e: Exception) {
                LuaValue.varargsOf(arrayOf(LuaValue.FALSE, LuaValue.valueOf(e.message ?: "Set timeout failed")))
            }
        }
    }

    inner class GetSocketCount : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(connections.size)
        }
    }

    fun cleanup(scriptId: String) {
        // Закрываем только соединения, связанные с этим скриптом
        val connectionsToRemove = scriptConnections[scriptId] ?: return
        
        connectionsToRemove.forEach { connectionId ->
            val connection = connections[connectionId] ?: return@forEach
            try {
                connection.reader.close()
                connection.writer.close()
                connection.socket.close()
                connections.remove(connectionId)
            } catch (e: Exception) {
                // Игнорируем ошибки при закрытии
            }
        }
        
        scriptConnections.remove(scriptId)
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