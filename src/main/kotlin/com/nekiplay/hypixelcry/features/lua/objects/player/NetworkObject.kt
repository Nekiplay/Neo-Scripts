package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.sugar.sendSequencedPacket
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua

class NetworkObject(L: Lua) : SimpleLuaWrapper(L) {

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "getPlayersList" -> JFunction { getPlayersList(it) }
            "sendStartDestroyBlockPacket" -> JFunction { sendDestroyPacket(it, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) }
            "sendStopDestroyBlockPacket" -> JFunction { sendDestroyPacket(it, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) }
            "sendAbortDestroyBlockPacket" -> JFunction { sendDestroyPacket(it, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) }
            else -> null
        }
    }

    private fun getPlayersList(l: Lua): Int {
        val playerList = mc.connection?.listedOnlinePlayers ?: run {
            l.pushNil()
            return 1
        }

        l.newTable() // Основная таблица
        var index = 1
        for (player in playerList) {
            l.newTable() // Таблица конкретного игрока

            // latency
            l.push(player.latency.toDouble())
            l.setField(-2, "latency")

            // display_name
            val displayName = player.tabListDisplayName?.string
            if (displayName != null) l.push(displayName) else l.pushNil()
            l.setField(-2, "display_name")

            // name
            val name = player.profile?.name
            if (name != null) l.push(name) else l.pushNil()
            l.setField(-2, "name")

            // id
            val id = player.profile?.id?.toString()
            if (id != null) l.push(id) else l.pushNil()
            l.setField(-2, "id")

            // gamemode
            val gamemode = player.gameMode?.toString()
            if (gamemode != null) l.push(gamemode) else l.pushNil()
            l.setField(-2, "gamemode")

            // skin_texture
            val skinTexture = player.skin?.body?.toString()
            if (skinTexture != null) l.push(skinTexture) else l.pushNil()
            l.setField(-2, "skin_texture")

            // Вставляем таблицу игрока в общую таблицу под индексом
            l.rawSetI(-2, index)
            index++
        }
        return 1
    }

    private fun sendDestroyPacket(l: Lua, action: ServerboundPlayerActionPacket.Action): Int {
        val gamemode = mc.gameMode

        // Validate numeric coordinates and game mode availability
        if (!l.isNumber(1) || !l.isNumber(2) || !l.isNumber(3) || gamemode == null) {
            l.push(false)
            return 1
        }

        // Parse block position
        val blockPos = BlockPos(l.toInteger(1).toInt(), l.toInteger(2).toInt(), l.toInteger(3).toInt())

        // Parse direction from either LuaDirection userdata or string
        val direction: Direction? = when {
            l.isUserdata(4) && l.toJavaObject(4) is LuaDirection -> {
                (l.toJavaObject(4) as LuaDirection).direction
            }
            l.isString(4) -> {
                val dirStr = l.toString(4)?.uppercase()
                try {
                    dirStr?.let { Direction.valueOf(it) }
                } catch (e: IllegalArgumentException) {
                    println("Lua warning: Invalid direction '$dirStr'.")
                    null
                }
            }
            else -> null
        }

        // Fail early if direction couldn't be resolved
        if (direction == null) {
            l.push(false)
            return 1
        }

        // Safe packet sending
        gamemode.sendSequencedPacket { sequence ->
            ServerboundPlayerActionPacket(
                action,
                blockPos,
                direction,
                sequence
            )
        }

        l.push(true)
        return 1
    }
}