package com.nekiplay.neoscripts.common.features.lua.objects.misc

import com.google.gson.GsonBuilder
import com.nekiplay.neoscripts.ServerMain
import com.nekiplay.neoscripts.common.mixins.minecraft.ServerPlayerAccessor
import com.nekiplay.neoscripts.common.network.NeoLuaC2SPayload
import com.nekiplay.neoscripts.common.network.NeoLuaS2CPayload
import com.nekiplay.neoscripts.common.network.NeoPacketSenders
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerPlayer
import org.luaj.vm2.*
import org.luaj.vm2.lib.VarArgFunction

/**
 * Lua library `packets` for sending custom packets between logical client/server.
 *
 * Usage (Lua):
 *   local packets = require("packets")
 *   -- client -> server
 *   packets.sendToServer("balance", 10)
 *   packets.sendToServer("balance", {10, 20})
 *   packets["balance"]:sendToServer(10)  -- sugar: channel object
 *   packets.balance.sendToServer({val=1}) -- sugar
 *
 *   -- server -> client
 *   packets.sendToClient(player, "balance", {value=10}) -- targeted
 *   packets.sendToClient("balance", data) -- broadcast to all
 *   packets.balance:sendToClient(player, data)
 *   packets.broadcast("balance", data) -- alias for broadcast
 *
 * Receiving:
 *   -- client (packets from server)
 *   registerPacket("balance", function(data) print(data) end)
 *   registerCustomPacket("balance", function(data) end) -- alias
 *   -- server (packets from client)
 *   registerPacket("balance", function(player, data) print(player:getName(), data) end)
 */
class PacketsLib : LuaValue() {
    override fun typename(): String = "packets"
    override fun tojstring(): String = "PacketsLib"
    override fun isnil(): Boolean = false
    override fun type(): Int = TUSERDATA
    override fun call(): LuaValue = this

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "sendToServer", "send_to_server", "sendToServerSide", "c2s" -> SendToServerFunction()
            "sendToClient", "send_to_client", "sendToClients", "s2c" -> SendToClientFunction()
            "broadcast", "sendToAll", "send_to_all" -> BroadcastFunction()
            "send" -> SendGenericFunction()
            else -> {
                // syntactic sugar: packets["balance"] returns channel proxy
                val name = key.tojstring()
                if (name.isNotBlank() && name != "nil") {
                    ChannelProxy(name)
                } else {
                    NIL
                }
            }
        }
    }

    // channel-bound proxy: packets.balance:sendToServer(data)
    inner class ChannelProxy(private val channel: String) : LuaValue() {
        override fun typename(): String = "packet_channel"
        override fun tojstring(): String = "PacketChannel($channel)"
        override fun isnil(): Boolean = false
        override fun type(): Int = TUSERDATA
        override fun call(): LuaValue = this

        override fun get(key: LuaValue): LuaValue {
            return when (key.tojstring()) {
                "sendToServer", "send_to_server", "send", "c2s" -> object : VarArgFunction() {
                    override fun invoke(args: Varargs): Varargs {
                        val data = if (args.narg() >= 1) args.arg(1) else NIL
                        return doSendToServer(channel, data)
                    }
                }
                "sendToClient", "send_to_client", "s2c" -> object : VarArgFunction() {
                    override fun invoke(args: Varargs): Varargs {
                        // supports: (player, data) or (data) broadcast
                        if (args.narg() == 1) {
                            return doSendToClient(null, channel, args.arg(1))
                        } else if (args.narg() >= 2) {
                            return doSendToClient(args.arg(1), channel, args.arg(2))
                        }
                        return doSendToClient(null, channel, NIL)
                    }
                }
                "broadcast", "sendToAll" -> object : VarArgFunction() {
                    override fun invoke(args: Varargs): Varargs {
                        val data = if (args.narg() >= 1) args.arg(1) else NIL
                        return doSendToClient(null, channel, data)
                    }
                }
                "name", "channel" -> valueOf(channel)
                else -> NIL
            }
        }
    }

    inner class SendToServerFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.narg() == 0) return valueOf(false)
            // (channel, data) or (channel) with NIL
            val channel = args.arg(1).checkjstring()
            val data = if (args.narg() >= 2) args.arg(2) else NIL
            return doSendToServer(channel, data)
        }
    }

    inner class SendToClientFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.narg() == 0) return valueOf(false)
            // Overloads:
            // sendToClient(channel, data) -> broadcast
            // sendToClient(player, channel, data) -> targeted
            return if (args.narg() == 2 && args.arg(1).isstring()) {
                val channel = args.arg(1).checkjstring()
                val data = args.arg(2)
                doSendToClient(null, channel, data)
            } else if (args.narg() >= 3) {
                // first arg is player
                val playerArg = args.arg(1)
                val channel = args.arg(2).checkjstring()
                val data = args.arg(3)
                doSendToClient(playerArg, channel, data)
            } else {
                valueOf(false)
            }
        }
    }

    inner class BroadcastFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.narg() == 0) return valueOf(false)
            val channel = args.arg(1).checkjstring()
            val data = if (args.narg() >= 2) args.arg(2) else NIL
            return doSendToClient(null, channel, data)
        }
    }

    inner class SendGenericFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            // auto-detect side: if first arg is ServerPlayer-like try client send else server send
            // For generic, expect (channel, data)
            if (args.narg() == 0) return valueOf(false)
            val channel = args.arg(1).checkjstring()
            val data = if (args.narg() >= 2) args.arg(2) else NIL
            // Try both, prefer server->client if SERVER exists + has players, otherwise client->server
            val toServer = doSendToServer(channel, data)
            if (toServer.toboolean()) return toServer
            return doSendToClient(null, channel, data)
        }
    }

    private fun doSendToServer(channel: String, data: LuaValue): LuaValue {
        return try {
            val json = luaToJson(data)
            val payload = NeoLuaC2SPayload(channel, json)
            val ok = NeoPacketSenders.sendToServer(payload)
            if (ok) TRUE else FALSE
        } catch (e: Exception) {
            FALSE
        }
    }

    private fun doSendToClient(playerArg: LuaValue?, channel: String, data: LuaValue): LuaValue {
        return try {
            val json = luaToJson(data)
            val payload = NeoLuaS2CPayload(channel, json)

            if (playerArg != null && !playerArg.isnil() && !playerArg.isstring()) {
                val serverPlayer = extractServerPlayer(playerArg)
                if (serverPlayer != null) {
                    ServerPlayNetworking.send(serverPlayer, payload)
                    return TRUE
                } else {
                    return FALSE
                }
            } else {
                // broadcast to all players
                val server = ServerMain.SERVER
                if (server == null) return FALSE
                var sent = false
                for (p in server.playerList.players) {
                    try {
                        ServerPlayNetworking.send(p, payload)
                        sent = true
                    } catch (_: Throwable) {}
                }
                return valueOf(sent)
            }
        } catch (e: Exception) {
            FALSE
        }
    }

    // Mixin accessor example: obtain MinecraftServer from ServerPlayer without reflection
    private fun getServerViaMixin(player: ServerPlayer) = (player as ServerPlayerAccessor).getServerField()

    private fun extractServerPlayer(arg: LuaValue): ServerPlayer? {
        return when (arg) {
            is com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity -> {
                arg.entity as? ServerPlayer
            }
            else -> {
                // try luajava userdata
                try {
                    val obj = arg.touserdata()
                    if (obj is ServerPlayer) obj else null
                } catch (_: Exception) { null }
            }
        }
    }

    companion object {
        private val gsonCompact = ServerMain.GSON_COMPACT
        // fallback gson if SERVER not initialized early (common init)
        private val fallbackGson by lazy { GsonBuilder().create() }
        private fun gson() = try { ServerMain.GSON_COMPACT } catch (_: Exception) { fallbackGson }

        fun luaToJson(value: LuaValue): String {
            return try {
                val javaObj = luaToJava(value)
                gson().toJson(javaObj)
            } catch (e: Exception) {
                "\"\""
            }
        }

        fun jsonToLua(json: String): LuaValue {
            return try {
                if (json.isBlank()) return NIL
                val parsed = gson().fromJson(json, Any::class.java)
                anyToLua(parsed)
            } catch (e: Exception) {
                // fallback: treat as raw string
                valueOf(json)
            }
        }

        private fun luaToJava(v: LuaValue): Any? {
            return when {
                v.isnil() -> null
                v.isboolean() -> v.toboolean()
                v.isstring() -> v.tojstring()
                v.isnumber() -> {
                    val d = v.todouble()
                    if (d == kotlin.math.floor(d) && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                        val l = d.toLong()
                        if (l in Int.MIN_VALUE..Int.MAX_VALUE) l.toInt() else l
                    } else d
                }
                v.istable() -> {
                    val t = v.checktable()
                    if (isArray(t)) {
                        val list = mutableListOf<Any?>()
                        var i = 1
                        while (true) {
                            val el = t.get(i)
                            if (el.isnil()) break
                            list.add(luaToJava(el))
                            i++
                        }
                        list
                    } else {
                        val map = mutableMapOf<String, Any?>()
                        val keys = t.keys()
                        for (k in keys) {
                            map[k.tojstring()] = luaToJava(t.get(k))
                        }
                        map
                    }
                }
                else -> v.tojstring()
            }
        }

        private fun isArray(table: LuaTable): Boolean {
            val keys = table.keys()
            if (keys.isEmpty()) return false
            var idx = 1
            for (k in keys) {
                if (!k.isnumber()) return false
                val n = k.todouble()
                if (n != idx.toDouble() || n != kotlin.math.floor(n)) return false
                idx++
            }
            return true
        }

        private fun anyToLua(obj: Any?): LuaValue {
            return when (obj) {
                null -> NIL
                is String -> valueOf(obj)
                is Number -> valueOf(obj.toDouble())
                is Boolean -> valueOf(obj)
                is Map<*, *> -> {
                    val t = LuaValue.tableOf()
                    obj.forEach { (k, v) ->
                        t.set(valueOf(k.toString()), anyToLua(v))
                    }
                    t
                }
                is List<*> -> {
                    val t = LuaValue.tableOf()
                    obj.forEachIndexed { i, v -> t.set(i + 1, anyToLua(v)) }
                    t
                }
                else -> valueOf(obj.toString())
            }
        }
    }
}
