package com.nekiplay.neoscripts.features.lua.objects.player

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.features.lua.customArgs.FourArgFunction
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.sugar.sendSequencedPacket
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction


class NetworkObject : LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    companion object {
        fun parseDirection(arg: LuaValue?): Direction? {
            return when {
                arg?.isuserdata() == true && arg.touserdata() is LuaDirection -> {
                    (arg.touserdata() as LuaDirection).direction
                }

                arg?.isuserdata() == true && arg.touserdata() is Direction -> {
                    arg.touserdata() as Direction
                }

                arg?.isstring() == true -> {
                    val dirStr = arg.tojstring()?.uppercase()
                    try {
                        dirStr?.let { Direction.valueOf(it) }
                    } catch (e: IllegalArgumentException) {
                        println(
                            "Lua warning: Invalid direction '$dirStr'. Valid values: ${
                                Direction.values().joinToString { it.name }
                            }"
                        )
                        null
                    }
                }

                else -> null
            }
        }

        fun disconnectFromServer(reason: String) {
            val minecraft = Minecraft.getInstance()

            if (minecraft.connection != null) {
                val disconnectReason: Component = Component.literal(reason)

                val disconnectPacket =
                    ClientboundDisconnectPacket(disconnectReason)

                minecraft.connection?.handleDisconnect(disconnectPacket)
            }
        }
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getPlayersList" -> GetPlayerList()
            "connectToServer" -> ConnectToServer()
            "disconnectFromServer" -> DisconnectFromServer()
            "getCurrentServer" -> getCurrentServer()
            
            "sendStartDestroyBlockPacket" -> SendStartDestroyBlockPacket()
            "sendStopDestroyBlockPacket" -> SendStopDestroyBlockPacket()
            "sendAbortDestroyBlockPacket" -> SendAbortDestroyBlockPacket()
            else -> NIL
        } as LuaValue
    }

    private class getCurrentServer : ZeroArgFunction() {
        override fun call(): LuaValue {
            val mc = Minecraft.getInstance()
            val server = mc.currentServer

            if (server != null) {
                val address = server.ip
                // Разбор IP и порта
                if (address.contains(":")) {
                    val ip = address.substring(0, address.lastIndexOf(':'))
                    val port = address.substring(address.lastIndexOf(':'.code.toChar()) + 1).toInt()
                    return valueOf("$ip:$port")
                } else {
                    val ip = address // порт по умолчанию 25565
                    val port = 25565

                    return valueOf("$ip:$port")
                }
            }
            return NIL
        }
    }

    private class DisconnectFromServer : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            disconnectFromServer(arg.tojstring())
            return TRUE
        }
    }

    private class ConnectToServer : TwoArgFunction() {
        override fun call(host: LuaValue, port: LuaValue): LuaValue {
            val minecraft = Minecraft.getInstance()

            if (minecraft.gui.screen() != null) {
                val serverAddress = ServerAddress(host.tojstring(), port.toint())

                val serverData = ServerData("My Server", host.tojstring() + ":" + port.toint(), ServerData.Type.OTHER)

                disconnectFromServer("Connecting to another server")

                ConnectScreen.startConnecting(
                    minecraft.gui.screen()!!,
                    minecraft,
                    serverAddress,
                    serverData,
                    false,
                    null
                )
            }
            return TRUE
        }
    }

    private class GetPlayerList : ZeroArgFunction() {
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

    private class SendStartDestroyBlockPacket : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            val gamemode = mc.gameMode

            val (blockPos, direction) = when {
                arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos -> {
                    val pos = (arg1.touserdata() as LuaBlockPos).pos
                    val dir = parseDirection(arg2)
                    pos to dir
                }
                arg1?.isuserdata() == true && arg1.touserdata() is BlockPos -> {
                    val pos = arg1.touserdata() as BlockPos
                    val dir = parseDirection(arg2)
                    pos to dir
                }
                arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true -> {
                    val pos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                    val dir = parseDirection(arg4)
                    pos to dir
                }
                else -> return FALSE
            }

            if (blockPos == null || direction == null || gamemode == null) {
                return FALSE
            }

            gamemode.sendSequencedPacket { sequence ->
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    blockPos,
                    direction,
                    sequence
                )
            }

            return TRUE
        }
    }

    private class SendStopDestroyBlockPacket : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            val gamemode = mc.gameMode

            val (blockPos, direction) = when {
                arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos -> {
                    val pos = (arg1.touserdata() as LuaBlockPos).pos
                    val dir = parseDirection(arg2)
                    pos to dir
                }
                arg1?.isuserdata() == true && arg1.touserdata() is BlockPos -> {
                    val pos = arg1.touserdata() as BlockPos
                    val dir = parseDirection(arg2)
                    pos to dir
                }
                arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true -> {
                    val pos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                    val dir = parseDirection(arg4)
                    pos to dir
                }
                else -> return FALSE
            }

            if (blockPos == null || direction == null || gamemode == null) {
                return FALSE
            }

            gamemode.sendSequencedPacket { sequence ->
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    blockPos,
                    direction,
                    sequence
                )
            }

            return TRUE
        }
    }

    private class SendAbortDestroyBlockPacket : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            val gamemode = mc.gameMode

            val (blockPos, direction) = when {
                arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos -> {
                    val pos = (arg1.touserdata() as LuaBlockPos).pos
                    val dir = parseDirection(arg2)
                    pos to dir
                }
                arg1?.isuserdata() == true && arg1.touserdata() is BlockPos -> {
                    val pos = arg1.touserdata() as BlockPos
                    val dir = parseDirection(arg2)
                    pos to dir
                }
                arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true -> {
                    val pos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                    val dir = parseDirection(arg4)
                    pos to dir
                }
                else -> return FALSE
            }

            if (blockPos == null || direction == null || gamemode == null) {
                return FALSE
            }

            gamemode.sendSequencedPacket { sequence ->
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    blockPos,
                    direction,
                    sequence
                )
            }

            return TRUE
        }
    }

    override fun typename(): String = "network"
    override fun tojstring(): String = "NetworkObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}