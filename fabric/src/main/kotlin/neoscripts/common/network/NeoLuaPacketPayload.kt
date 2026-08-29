package com.nekiplay.neoscripts.common.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

// Server -> Client (uses clientboundPlay)
data class NeoLuaS2CPayload(val channel: String, val json: String) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
    companion object {
        val ID: Identifier = Identifier.fromNamespaceAndPath("neoscripts", "lua_s2c")
        val TYPE: CustomPacketPayload.Type<NeoLuaS2CPayload> = CustomPacketPayload.Type(ID)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, NeoLuaS2CPayload> = object : StreamCodec<RegistryFriendlyByteBuf, NeoLuaS2CPayload> {
            override fun encode(buf: RegistryFriendlyByteBuf, value: NeoLuaS2CPayload) {
                ByteBufCodecs.STRING_UTF8.encode(buf, value.channel)
                ByteBufCodecs.STRING_UTF8.encode(buf, value.json)
            }
            override fun decode(buf: RegistryFriendlyByteBuf): NeoLuaS2CPayload {
                return NeoLuaS2CPayload(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf))
            }
        }
    }
}

// Client -> Server (uses serverboundPlay)
data class NeoLuaC2SPayload(val channel: String, val json: String) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
    companion object {
        val ID: Identifier = Identifier.fromNamespaceAndPath("neoscripts", "lua_c2s")
        val TYPE: CustomPacketPayload.Type<NeoLuaC2SPayload> = CustomPacketPayload.Type(ID)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, NeoLuaC2SPayload> = object : StreamCodec<RegistryFriendlyByteBuf, NeoLuaC2SPayload> {
            override fun encode(buf: RegistryFriendlyByteBuf, value: NeoLuaC2SPayload) {
                ByteBufCodecs.STRING_UTF8.encode(buf, value.channel)
                ByteBufCodecs.STRING_UTF8.encode(buf, value.json)
            }
            override fun decode(buf: RegistryFriendlyByteBuf): NeoLuaC2SPayload {
                return NeoLuaC2SPayload(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf))
            }
        }
    }
}
