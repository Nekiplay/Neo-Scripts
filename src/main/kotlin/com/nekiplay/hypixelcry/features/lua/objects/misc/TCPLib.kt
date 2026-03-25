package com.nekiplay.hypixelcry.features.lua.objects.misc

import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import java.io.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class TCPLib(val L: Lua) {
    private val connections = ConcurrentHashMap<Int, TCPConnection>()
    private val nextId = AtomicInteger(1)

    data class TCPConnection(
        val socket: Socket,
        val inputStream: InputStream,
        val outputStream: OutputStream,
        val reader: BufferedReader,
        val writer: BufferedWriter
    )

    fun register() {
        L.newTable() // Создаем таблицу tcp

        L.push(JFunction { connect(it) })
        L.setField(-2, "connect")

        L.push(JFunction { disconnect(it) })
        L.setField(-2, "disconnect")

        L.push(JFunction { send(it) })
        L.setField(-2, "send")

        L.push(JFunction { receive(it) })
        L.setField(-2, "receive")

        L.push(JFunction { sendBytes(it) })
        L.setField(-2, "sendBytes")

        L.push(JFunction { receiveBytes(it) })
        L.setField(-2, "receiveBytes")

        L.push(JFunction { isConnected(it) })
        L.setField(-2, "isConnected")

        L.push(JFunction { getLocalAddress(it) })
        L.setField(-2, "getLocalAddress")

        L.push(JFunction { getRemoteAddress(it) })
        L.setField(-2, "getRemoteAddress")

        L.push(JFunction { setBlocking(it) })
        L.setField(-2, "setBlocking")

        L.push(JFunction { setTimeout(it) })
        L.setField(-2, "setTimeout")

        L.push(JFunction { getSocketCount(it) })
        L.setField(-2, "getSocketCount")

        L.setGlobal("tcp")
    }

    private fun connect(l: Lua): Int {
        val host = l.toString(1) ?: ""
        val port = l.toNumber(2).toInt()
        val timeout = if (l.isNumber(3)) l.toNumber(3).toInt() else 5000

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

            l.push(connectionId.toDouble())
            l.pushNil()
            2
        } catch (e: Exception) {
            l.pushNil()
            l.push(e.message ?: "Connection failed")
            2
        }
    }

    private fun disconnect(l: Lua): Int {
        val connectionId = l.toNumber(1).toInt()
        val connection = connections[connectionId] ?: run {
            l.push(false)
            return 1
        }

        return try {
            connection.reader.close()
            connection.writer.close()
            connection.socket.close()
            connections.remove(connectionId)
            l.push(true)
            1
        } catch (e: Exception) {
            l.push(false)
            1
        }
    }

    private fun send(l: Lua): Int {
        val connectionId = l.toNumber(1).toInt()
        val message = l.toString(2) ?: ""
        val newline = if (l.isBoolean(3)) l.toBoolean(3) else true

        val connection = connections[connectionId] ?: run {
            l.push(false)
            l.push("Connection not found")
            return 2
        }

        return try {
            if (newline) {
                connection.writer.write(message)
                connection.writer.newLine()
            } else {
                connection.writer.write(message)
            }
            connection.writer.flush()
            l.push(true)
            l.pushNil()
            2
        } catch (e: Exception) {
            l.push(false)
            l.push(e.message ?: "Send failed")
            2
        }
    }

    private fun sendBytes(l: Lua): Int {
        val connectionId = l.toNumber(1).toInt()
        if (!l.isTable(2)) {
            l.push(false); l.push("Table required"); return 2
        }

        val length = l.rawLength(2)
        val connection = connections[connectionId] ?: run {
            l.push(false); l.push("Connection not found"); return 2
        }

        return try {
            val byteArray = ByteArray(length)
            for (i in 1..length) {
                l.rawGetI(2, i)
                val byteValue = l.toNumber(-1).toInt()
                l.pop(1)
                if (byteValue < 0 || byteValue > 255) {
                    l.push(false); l.push("Invalid byte value: $byteValue"); return 2
                }
                byteArray[i - 1] = byteValue.toByte()
            }

            connection.outputStream.write(byteArray)
            connection.outputStream.flush()
            l.push(true)
            l.pushNil()
            2
        } catch (e: Exception) {
            l.push(false); l.push(e.message ?: "Send bytes failed"); 2
        }
    }

    private fun receive(l: Lua): Int {
        val connectionId = l.toNumber(1).toInt()
        val timeout = if (l.isNumber(2)) l.toNumber(2).toInt() else -1

        val connection = connections[connectionId] ?: run {
            l.pushNil(); l.push("Connection not found"); return 2
        }

        return try {
            if (timeout > 0) connection.socket.soTimeout = timeout

            val line = connection.reader.readLine()
            if (line == null) {
                l.pushNil(); l.push("Connection closed")
            } else {
                l.push(line); l.pushNil()
            }
            2
        } catch (e: SocketTimeoutException) {
            l.pushNil(); l.push("Timeout"); 2
        } catch (e: Exception) {
            l.pushNil(); l.push(e.message ?: "Receive failed"); 2
        }
    }

    private fun receiveBytes(l: Lua): Int {
        val connectionId = l.toNumber(1).toInt()
        val timeout = if (l.isNumber(2)) l.toNumber(2).toInt() else -1
        val maxBytes = if (l.isNumber(3)) l.toNumber(3).toInt() else 1024

        val connection = connections[connectionId] ?: run {
            l.pushNil(); l.push("Connection not found"); return 2
        }

        return try {
            if (timeout > 0) connection.socket.soTimeout = timeout

            val buffer = ByteArray(maxBytes)
            val bytesRead = connection.inputStream.read(buffer)

            if (bytesRead == -1) {
                l.pushNil(); l.push("Connection closed")
            } else {
                l.newTable()
                for (i in 0 until bytesRead) {
                    l.push((buffer[i].toInt() and 0xFF).toDouble())
                    l.rawSetI(-2, i + 1)
                }
                l.pushNil()
            }
            2
        } catch (e: SocketTimeoutException) {
            l.pushNil(); l.push("Timeout"); 2
        } catch (e: Exception) {
            l.pushNil(); l.push(e.message ?: "Receive bytes failed"); 2
        }
    }

    private fun isConnected(l: Lua): Int {
        val connectionId = l.toNumber(1).toInt()
        val connection = connections[connectionId]
        l.push(connection != null && connection.socket.isConnected && !connection.socket.isClosed)
        return 1
    }

    private fun getLocalAddress(l: Lua): Int {
        val connectionId = l.toNumber(1).toInt()
        val connection = connections[connectionId] ?: run { l.pushNil(); return 1 }

        val localAddress = connection.socket.localAddress
        l.newTable()
        l.push(localAddress.hostAddress); l.setField(-2, "address")
        l.push(connection.socket.localPort.toDouble()); l.setField(-2, "port")
        l.push(localAddress.hostName); l.setField(-2, "hostname")
        return 1
    }

    private fun getRemoteAddress(l: Lua): Int {
        val connectionId = l.toNumber(1).toInt()
        val connection = connections[connectionId] ?: run { l.pushNil(); return 1 }

        val remoteAddress = connection.socket.inetAddress
        l.newTable()
        l.push(remoteAddress.hostAddress); l.setField(-2, "address")
        l.push(connection.socket.port.toDouble()); l.setField(-2, "port")
        l.push(remoteAddress.hostName); l.setField(-2, "hostname")
        return 1
    }

    private fun setBlocking(l: Lua): Int {
        val connectionId = l.toNumber(1).toInt()
        val blocking = l.toBoolean(2)
        val connection = connections[connectionId] ?: run { l.push(false); l.push("Not found"); return 2 }

        return try {
            connection.socket.soTimeout = if (blocking) 0 else 1000
            l.push(true); l.pushNil(); 2
        } catch (e: Exception) {
            l.push(false); l.push(e.message ?: "Error"); 2
        }
    }

    private fun setTimeout(l: Lua): Int {
        val connectionId = l.toNumber(1).toInt()
        val timeout = l.toNumber(2).toInt()
        val connection = connections[connectionId] ?: run { l.push(false); l.push("Not found"); return 2 }

        return try {
            connection.socket.soTimeout = timeout
            l.push(true); l.pushNil(); 2
        } catch (e: Exception) {
            l.push(false); l.push(e.message ?: "Error"); 2
        }
    }

    private fun getSocketCount(l: Lua): Int {
        l.push(connections.size.toDouble())
        return 1
    }

    fun cleanup() {
        connections.values.forEach { connection ->
            try {
                connection.reader.close()
                connection.writer.close()
                connection.socket.close()
            } catch (e: Exception) {}
        }
        connections.clear()
    }
}