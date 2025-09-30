package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.features.lua.customArgs.FourArgFunction
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction

class NetworkObject : LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getPlayersList" -> GetPlayerList()
            
            "sendStartDestroyBlockPacket" -> SendStartDestroyBlockPacket()
            "sendStopDestroyBlockPacket" -> SendStopDestroyBlockPacket()
            "sendAbortDestroyBlockPacket" -> SendAbortDestroyBlockPacket()
            else -> NIL
        } as LuaValue
    }

    private inner class GetPlayerList : ZeroArgFunction() {
        override fun call(): LuaValue {
            val playerList = mc.networkHandler?.playerList ?: return NIL
            val table = tableOf()
            var index = 1
            for (player in playerList) {
                val table_player = tableOf()
                table_player.set("latency", valueOf(player.latency))
                table_player.set("display_name", valueOf(player.displayName?.string))
                table_player.set("name", valueOf(player.profile?.name))
                table_player.set("id", valueOf(player.profile?.id.toString()))
                table_player.set("gamemode", valueOf(player.gameMode.toString()))
                table_player.set("skin_texture", valueOf(player.skinTextures.texture.toString()))
                table.set(index, table_player)
                index++;
            }
            return table
        }
    }

    private inner class SendStartDestroyBlockPacket : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isstring() == true) {
                mc.networkHandler?.sendPacket(
                    PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                        BlockPos(arg1.toint(), arg2.toint(), arg3.toint()),
                        Direction.valueOf(arg4.tojstring())
                    )
                )
                return TRUE
            }
            return FALSE
        }
    }

    private inner class SendStopDestroyBlockPacket : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isstring() == true) {
                mc.networkHandler?.sendPacket(
                    PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                        BlockPos(arg1.toint(), arg2.toint(), arg3.toint()),
                        Direction.valueOf(arg4.tojstring())
                    )
                )
                return TRUE
            }
            return FALSE
        }
    }

    private inner class SendAbortDestroyBlockPacket : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isstring() == true) {
                mc.networkHandler?.sendPacket(
                    PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                        BlockPos(arg1.toint(), arg2.toint(), arg3.toint()),
                        Direction.valueOf(arg4.tojstring())
                    )
                )
                return TRUE
            }
            return FALSE
        }
    }

    override fun typename(): String = "network"
    override fun tojstring(): String = "NetworkObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}