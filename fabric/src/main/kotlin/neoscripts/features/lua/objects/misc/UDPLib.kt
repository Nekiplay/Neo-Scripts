package com.nekiplay.neoscripts.features.lua.objects.misc

import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class UDPLib : LuaValue() {
    private val sockets = ConcurrentHashMap<Int, DatagramSocket>()
    private val nextId = AtomicInteger(1)

    override fun typename(): String = "udp"
    override fun tojstring(): String = "UDPObject"
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
            "setTimeout" -> SetTimeout()
            "getSocketCount" -> GetSocketCount()

            "listen" -> Listen()
            "closeSocket" -> CloseSocket()
            else -> super.get(key)
        }
    }

    // --- СЕРВЕРНЫЕ МЕТОДЫ (bind) ---

    inner class Listen : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val port = args.arg(1).checkint()
            val host = args.arg(2).optjstring("0.0.0.0")

            return try {
                val socket = DatagramSocket(port, InetAddress.getByName(host))
                val id = nextId.getAndIncrement()
                sockets[id] = socket
                varargsOf(arrayOf(valueOf(id), NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Failed to bind")))
            }
        }
    }

    inner class CloseSocket : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val id = arg.checkint()
            val socket = sockets.remove(id) ?: return FALSE
            return try {
                socket.close()
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
                val socket = DatagramSocket()
                socket.soTimeout = timeout
                socket.connect(InetAddress.getByName(host), port)

                val id = nextId.getAndIncrement()
                sockets[id] = socket
                varargsOf(arrayOf(valueOf(id), NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Connection failed")))
            }
        }
    }

    inner class Disconnect : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val id = arg.checkint()
            return if (sockets.remove(id) != null) {
                TRUE
            } else {
                FALSE
            }
        }
    }

    inner class Send : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val id = args.arg(1).checkint()
            val message = args.arg(2).checkjstring()

            val socket = sockets[id] ?: return varargsOf(
                arrayOf(FALSE, valueOf("Socket not found"))
            )

            return try {
                if (socket.isConnected) {
                    val bytes = message.toByteArray(Charsets.UTF_8)
                    socket.send(DatagramPacket(bytes, bytes.size))
                } else {
                    val port = args.arg(3).checkint()
                    val host = args.arg(4).optjstring("127.0.0.1")
                    val bytes = message.toByteArray(Charsets.UTF_8)
                    socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(host), port))
                }
                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(FALSE, valueOf(e.message ?: "Send failed")))
            }
        }
    }

    inner class SendBytes : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val id = args.arg(1).checkint()
            val bytesTable = args.arg(2).checktable()
            val length = bytesTable.length()

            val socket = sockets[id] ?: return varargsOf(
                arrayOf(FALSE, valueOf("Socket not found"))
            )

            return try {
                val byteArray = ByteArray(length)
                for (i in 1..length) {
                    val byteValue = bytesTable.get(i).checkint()
                    byteArray[i - 1] = (byteValue and 0xFF).toByte()
                }

                if (socket.isConnected) {
                    socket.send(DatagramPacket(byteArray, byteArray.size))
                } else {
                    val port = args.arg(3).checkint()
                    val host = args.arg(4).optjstring("127.0.0.1")
                    socket.send(DatagramPacket(byteArray, byteArray.size, InetAddress.getByName(host), port))
                }
                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(FALSE, valueOf(e.message ?: "Send bytes failed")))
            }
        }
    }

    inner class Receive : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val id = args.arg(1).checkint()
            val timeout = if (args.narg() > 1) args.arg(2).checkint() else -1
            val maxBytes = if (args.narg() > 2) args.arg(3).checkint() else 1024

            val socket = sockets[id] ?: return varargsOf(
                arrayOf(NIL, valueOf("Socket not found"))
            )

            return try {
                if (timeout >= 0) {
                    socket.soTimeout = timeout
                }

                val buffer = ByteArray(maxBytes)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                val message = String(buffer, 0, packet.length, Charsets.UTF_8)
                varargsOf(arrayOf(valueOf(message), NIL))
            } catch (e: SocketTimeoutException) {
                varargsOf(arrayOf(NIL, valueOf("Timeout")))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Receive failed")))
            }
        }
    }

    inner class ReceiveBytes : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val id = args.arg(1).checkint()
            val timeout = if (args.narg() > 1) args.arg(2).checkint() else -1
            val maxBytes = if (args.narg() > 2) args.arg(3).checkint() else 1024

            val socket = sockets[id] ?: return varargsOf(
                arrayOf(NIL, valueOf("Socket not found"))
            )

            return try {
                if (timeout >= 0) {
                    socket.soTimeout = timeout
                }

                val buffer = ByteArray(maxBytes)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                val bytesTable = LuaTable()
                for (i in 0 until packet.length) {
                    bytesTable.set(i + 1, valueOf(buffer[i].toInt() and 0xFF))
                }
                varargsOf(arrayOf(bytesTable, NIL))
            } catch (e: SocketTimeoutException) {
                varargsOf(arrayOf(NIL, valueOf("Timeout")))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Receive bytes failed")))
            }
        }
    }

    inner class IsConnected : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val id = arg.checkint()
            val socket = sockets[id] ?: return FALSE
            return valueOf(!socket.isClosed && socket.isConnected)
        }
    }

    inner class GetLocalAddress : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val id = arg.checkint()
            val socket = sockets[id] ?: return NIL
            val table = LuaTable()
            table.set("address", socket.localAddress.hostAddress)
            table.set("port", socket.localPort)
            return table
        }
    }

    inner class SetTimeout : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val id = args.arg(1).checkint()
            val timeout = args.arg(2).checkint()
            val socket = sockets[id] ?: return FALSE
            socket.soTimeout = timeout
            return TRUE
        }
    }

    inner class GetSocketCount : ZeroArgFunction() {
        override fun call(): LuaValue = valueOf(sockets.size)
    }

    fun cleanup() {
        sockets.values.forEach {
            try { it.close() } catch (e: Exception) {}
        }
        sockets.clear()
    }
}