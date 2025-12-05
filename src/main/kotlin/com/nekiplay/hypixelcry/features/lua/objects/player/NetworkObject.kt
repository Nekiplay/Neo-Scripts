package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.features.lua.customArgs.FourArgFunction
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.sugar.sendSequencedPacket
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
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
            val playerList = mc.connection?.listedOnlinePlayers ?: return NIL

            val table = tableOf()
            var index = 1
            for (player in playerList) {
                val table_player = tableOf()
                table_player.set("latency", valueOf(player.latency))

                // display_name
                val displayName = player.tabListDisplayName?.string
                table_player.set("display_name", if (displayName != null) valueOf(displayName) else NIL)

                // name
                val name = player.profile?.name
                table_player.set("name", if (name != null) valueOf(name) else NIL)

                // id
                val id = player.profile?.id?.toString()
                table_player.set("id", if (id != null) valueOf(id) else NIL)

                // gamemode
                val gamemode = player.gameMode?.toString()
                table_player.set("gamemode", if (gamemode != null) valueOf(gamemode) else NIL)

                // skin_texture
                val skinTexture = player.skin?.body?.toString()
                table_player.set("skin_texture", if (skinTexture != null) valueOf(skinTexture) else NIL)

                table.set(index, table_player)
                index++
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
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {
                if (arg4?.isuserdata() == true && arg4.touserdata() is LuaDirection) {
                    mc.gameMode?.sendSequencedPacket { sequence ->
                        ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                            BlockPos(arg1.toint(), arg2.toint(), arg3.toint()),
                            (arg4.touserdata() as LuaDirection).direction, sequence
                        )
                    }
                    return TRUE
                }
                else if (arg4?.isstring() == true) {
                    mc.gameMode?.sendSequencedPacket { sequence ->
                        arg4.tojstring()?.let {
                            ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                                BlockPos(arg1.toint(), arg2.toint(), arg3.toint()),
                                Direction.valueOf(it), sequence
                            )
                        }
                    }
                    return TRUE
                }
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
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {
                if (arg4?.isuserdata() == true && arg4.touserdata() is LuaDirection) {
                    mc.gameMode?.sendSequencedPacket { sequence ->
                        ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                            BlockPos(arg1.toint(), arg2.toint(), arg3.toint()),
                            (arg4.touserdata() as LuaDirection).direction, sequence
                        )
                    }
                    return TRUE
                }
                else if (arg4?.isstring() == true) {
                    mc.gameMode?.sendSequencedPacket { sequence ->
                        arg4.tojstring()?.let {
                            ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                                BlockPos(arg1.toint(), arg2.toint(), arg3.toint()),
                                Direction.valueOf(it), sequence
                            )
                        }
                    }
                    return TRUE
                }
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
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {
                if (arg4?.isuserdata() == true && arg4.touserdata() is LuaDirection) {
                    mc.gameMode?.sendSequencedPacket { sequence ->
                        ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                            BlockPos(arg1.toint(), arg2.toint(), arg3.toint()),
                            (arg4.touserdata() as LuaDirection).direction, sequence
                        )
                    }
                    return TRUE
                }
                else if (arg4?.isstring() == true) {
                    mc.gameMode?.sendSequencedPacket { sequence ->
                        arg4.tojstring()?.let {
                            ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                                BlockPos(arg1.toint(), arg2.toint(), arg3.toint()),
                                Direction.valueOf(it), sequence
                            )
                        }
                    }
                    return TRUE
                }
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