package com.nekiplay.neoscripts.common.features.lua.objects.misc

import net.minecraft.core.registries.BuiltInRegistries
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

class Entities : LuaValue() {
    override fun typename(): String = "entities"
    override fun tojstring(): String = "EntitiesObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getAll", "getEntities" -> GetEntities()
            else -> super.get(key)
        }
    }

    class GetEntities : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val entitiesList = tableOf()
            var index = 1

            for (entityType in BuiltInRegistries.ENTITY_TYPE) {
                // Получаем идентификатор типа сущности, например "minecraft:sheep"
                val id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType)?.toString() ?: continue
                entitiesList.set(index++, valueOf(id))
            }

            return entitiesList
        }
    }
}