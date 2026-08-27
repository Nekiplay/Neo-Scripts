package com.nekiplay.neoscripts.common.network

import net.minecraft.server.level.ServerPlayer

/**
 * Holds platform senders to avoid Class.forName reflection.
 * Client sets [clientSender] during ClientMain init, server side uses direct ServerPlayNetworking.
 */
object NeoPacketSenders {
    // Set only on physical client via ClientMain
    var clientSender: ((NeoLuaPacketPayload) -> Boolean)? = null

    fun sendToServer(payload: NeoLuaPacketPayload): Boolean {
        val sender = clientSender ?: return false
        return try {
            sender(payload)
        } catch (_: Exception) {
            false
        }
    }
}
