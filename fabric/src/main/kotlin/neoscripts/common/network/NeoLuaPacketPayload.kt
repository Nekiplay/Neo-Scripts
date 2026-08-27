package com.nekiplay.neoscripts.common.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class NeoLuaPacketPayload(val channel: String, val json: String) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Identifier.fromNamespaceAndPath("neoscripts", "lua_packet")
        val TYPE: CustomPacketPayload.Type<NeoLuaPacketPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, NeoLuaPacketPayload> = object : StreamCodec<RegistryFriendlyByteBuf, NeoLuaPacketPayload> {
            override fun encode(buf: RegistryFriendlyByteBuf, value: NeoLuaPacketPayload) {
                ByteBufCodecs.STRING_UTF8.encode(buf, value.channel)
                ByteBufCodecs.STRING_UTF8.encode(buf, value.json)
            }
            override fun decode(buf: RegistryFriendlyByteBuf): NeoLuaPacketPayload {
                val c = ByteBufCodecs.STRING_UTF8.decode(buf)
                val j = ByteBufCodecs.STRING_UTF8.decode(buf)
                return NeoLuaPacketPayload(c, j)
            }
        }
    }
}
