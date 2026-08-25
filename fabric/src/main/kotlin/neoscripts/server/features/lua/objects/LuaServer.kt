package com.nekiplay.neoscripts.server.features.lua.objects

import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class LuaServer(val server: MinecraftServer?) : LuaValue() {
    override fun typename(): String = "luaserver"
    override fun tojstring(): String = "LuaServer"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getLevel", "getWorld" -> GetLevel()
            "getLevels", "getWorlds" -> GetLevels()
            "getOnlinePlayers" -> GetOnlinePlayers()
            else -> super.get(key)
        }
    }

    inner class GetOnlinePlayers : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val table = tableOf()
            server?.playerList?.players?.forEachIndexed { index, player ->
                table.set(index + 1, LuaEntity(player))
            }
            return table
        }
    }

    inner class GetLevel : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val world = arg?.optjstring("minecraft:overworld") ?: return NIL
            val dim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(world));

            return ServerWorldObject(server?.getLevel(dim))
        }
    }

    // Все загруженные измерения сервера
    inner class GetLevels : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = tableOf()
            server?.allLevels?.forEachIndexed { index, level ->
                table.set(index + 1, ServerWorldObject(level))
            }
            return table
        }
    }
}