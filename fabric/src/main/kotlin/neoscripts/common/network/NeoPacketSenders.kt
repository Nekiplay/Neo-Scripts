package com.nekiplay.neoscripts.common.network

import net.minecraft.server.level.ServerPlayer

/**
 * Holds platform senders to avoid Class.forName reflection.
 * Client sets [clientSender] during ClientMain init, server side uses direct ServerPlayNetworking.
 */
object NeoPacketSenders {
    // Set only on physical client via ClientMain – uses NeoLuaC2SPayload (client -> server)
    var clientSender: ((NeoLuaC2SPayload) -> Boolean)? = null

    fun sendToServer(payload: NeoLuaC2SPayload): Boolean {
        val sender = clientSender ?: return false
        return try {
            sender(payload)
        } catch (_: Exception) {
            false
        }
    }
}
