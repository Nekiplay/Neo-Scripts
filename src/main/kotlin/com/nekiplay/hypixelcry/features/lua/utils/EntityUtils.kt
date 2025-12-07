package com.nekiplay.hypixelcry.features.lua.utils

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.world.entity.LivingEntity
import org.luaj.vm2.LuaValue

object EntityUtils {
    // Функция для получения всех сущностей в мире
    fun GetAllEntities(): LuaValue {
        val entitiesTable = LuaValue.tableOf()

        mc.level?.entitiesForRendering()?.forEachIndexed { index, entity ->
            entitiesTable.set(index + 1, LuaEntity(entity))
        }

        return entitiesTable
    }

    // Функция для получения всех живых сущностей в мире
     fun GetAllLivingEntities(): LuaValue {
        val entitiesTable = LuaValue.tableOf()

        var index = 0
        mc.level?.entitiesForRendering()?.forEach { entity ->
            if (entity is LivingEntity) {
                entitiesTable.set(index + 1, LuaEntity(entity))
                index++
            }
        }

        return entitiesTable
    }
}