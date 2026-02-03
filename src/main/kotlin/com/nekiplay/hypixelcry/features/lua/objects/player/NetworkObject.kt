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
            val gamemode = mc.gameMode
            // Validate numeric coordinates and game mode availability
            if (arg1?.isnumber() != true || arg2?.isnumber() != true || arg3?.isnumber() != true || gamemode == null) {
                return FALSE
            }

            // Parse block position once (avoids repeated toint() calls)
            val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())

            // Parse direction from either LuaDirection userdata or string
            val direction: Direction? = when {
                arg4?.isuserdata() == true && arg4.touserdata() is LuaDirection -> {
                    (arg4.touserdata() as LuaDirection).direction
                }
                arg4?.isstring() == true -> {
                    val dirStr = arg4.tojstring()?.uppercase() // Normalize case for enum matching
                    try {
                        dirStr?.let { Direction.valueOf(it) }
                    } catch (e: IllegalArgumentException) {
                        // Invalid direction name - return FALSE instead of crashing
                        println("Lua warning: Invalid direction '$dirStr'. Valid values: ${Direction.values().joinToString { it.name }}")
                        null
                    }
                }
                else -> null
            }

            // Fail early if direction couldn't be resolved
            if (direction == null) {
                return FALSE
            }

            // Safe packet sending - lambda now guaranteed to return non-null packet
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

    private inner class SendStopDestroyBlockPacket : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            val gamemode = mc.gameMode
            // Validate numeric coordinates and game mode availability
            if (arg1?.isnumber() != true || arg2?.isnumber() != true || arg3?.isnumber() != true || gamemode == null) {
                return FALSE
            }

            // Parse block position once (avoids repeated toint() calls)
            val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())

            // Parse direction from either LuaDirection userdata or string
            val direction: Direction? = when {
                arg4?.isuserdata() == true && arg4.touserdata() is LuaDirection -> {
                    (arg4.touserdata() as LuaDirection).direction
                }
                arg4?.isstring() == true -> {
                    val dirStr = arg4.tojstring()?.uppercase() // Normalize case for enum matching
                    try {
                        dirStr?.let { Direction.valueOf(it) }
                    } catch (e: IllegalArgumentException) {
                        // Invalid direction name - return FALSE instead of crashing
                        println("Lua warning: Invalid direction '$dirStr'. Valid values: ${Direction.values().joinToString { it.name }}")
                        null
                    }
                }
                else -> null
            }

            // Fail early if direction couldn't be resolved
            if (direction == null) {
                return FALSE
            }

            // Safe packet sending - lambda now guaranteed to return non-null packet
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

    private inner class SendAbortDestroyBlockPacket : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            val gamemode = mc.gameMode
            // Validate numeric coordinates and game mode availability
            if (arg1?.isnumber() != true || arg2?.isnumber() != true || arg3?.isnumber() != true || gamemode == null) {
                return FALSE
            }

            // Parse block position once (avoids repeated toint() calls)
            val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())

            // Parse direction from either LuaDirection userdata or string
            val direction: Direction? = when {
                arg4?.isuserdata() == true && arg4.touserdata() is LuaDirection -> {
                    (arg4.touserdata() as LuaDirection).direction
                }
                arg4?.isstring() == true -> {
                    val dirStr = arg4.tojstring()?.uppercase() // Normalize case for enum matching
                    try {
                        dirStr?.let { Direction.valueOf(it) }
                    } catch (e: IllegalArgumentException) {
                        // Invalid direction name - return FALSE instead of crashing
                        println("Lua warning: Invalid direction '$dirStr'. Valid values: ${Direction.values().joinToString { it.name }}")
                        null
                    }
                }
                else -> null
            }

            // Fail early if direction couldn't be resolved
            if (direction == null) {
                return FALSE
            }

            // Safe packet sending - lambda now guaranteed to return non-null packet
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