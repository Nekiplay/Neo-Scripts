package com.nekiplay.hypixelcry.features.lua.utils

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import org.luaj.vm2.LuaValue

object EntityUtils {
    // Функция для получения всех сущностей в мире
    fun GetAllEntities(): LuaValue {
        val entitiesTable = LuaValue.tableOf()

        mc.world?.entities?.forEachIndexed { index, entity ->
            entitiesTable.set(index + 1, ToLua(entity) ?: LuaValue.NIL)
        }

        return entitiesTable
    }

    // Функция для получения всех живых сущностей в мире
     fun GetAllLivingEntities(): LuaValue {
        val entitiesTable = LuaValue.tableOf()

        mc.world?.entities?.forEachIndexed { index, entity ->
            if (entity is LivingEntity) {
                entitiesTable.set(index + 1, ToLua(entity) ?: LuaValue.NIL)
            }
        }

        return entitiesTable
    }

    // Функция для преобразования Entity в Lua таблицу
    public fun ToLua(entity: Entity?): LuaValue? {
        if (entity != null) {
            val table = LuaValue.tableOf()

            // Основная информация о сущности
            table.set("id", LuaValue.valueOf(entity.id))
            table.set("uuid", LuaValue.valueOf(entity.uuidAsString))
            table.set("name", LuaValue.valueOf(entity.name.string))
            table.set("display_name", LuaValue.valueOf(entity.displayName?.string))
            table.set("type", LuaValue.valueOf(entity.type.toString()))

            // Позиция и движение
            val pos = entity.pos
            table.set("x", LuaValue.valueOf(pos.x))
            table.set("y", LuaValue.valueOf(pos.y))
            table.set("z", LuaValue.valueOf(pos.z))

            val velocity = entity.velocity
            table.set("velocity_x", LuaValue.valueOf(velocity.x))
            table.set("velocity_y", LuaValue.valueOf(velocity.y))
            table.set("velocity_z", LuaValue.valueOf(velocity.z))

            // Размеры и вращение
            table.set("width", LuaValue.valueOf(entity.width.toDouble()))
            table.set("height", LuaValue.valueOf(entity.height.toDouble()))
            table.set("yaw", LuaValue.valueOf(entity.yaw.toDouble()))
            table.set("pitch", LuaValue.valueOf(entity.pitch.toDouble()))

            // Состояния
            table.set("is_on_ground", LuaValue.valueOf(entity.isOnGround))
            table.set("is_touching_water", LuaValue.valueOf(entity.isTouchingWater))
            table.set("is_in_lava", LuaValue.valueOf(entity.isInLava))
            table.set("is_sneaking", LuaValue.valueOf(entity.isSneaking))
            table.set("is_sprinting", LuaValue.valueOf(entity.isSprinting))

            // Дополнительные свойства
            table.set("age", LuaValue.valueOf(entity.age))
            table.set("distance_to_player", LuaValue.valueOf(entity.squaredDistanceTo(mc.player)))

            if (entity is LivingEntity) {
                table.set("health", LuaValue.valueOf(entity.health.toDouble()) ?: LuaValue.NIL)
                table.set("max_health", LuaValue.valueOf(entity.maxHealth.toDouble()))

                table.set("is_alive", LuaValue.valueOf(entity.isAlive))
                table.set("is_child", LuaValue.valueOf(entity.isBaby))

                val mainHandStack = entity.mainHandStack
                val offHandStack = entity.offHandStack

                if (mainHandStack != null && !mainHandStack.isEmpty) {
                    table.set("main_hand", LuaItemStack(mainHandStack))
                }
                if (offHandStack != null && !offHandStack.isEmpty) {
                    table.set("off_hand", LuaItemStack(offHandStack))
                }

                val head = entity.getEquippedStack(EquipmentSlot.HEAD)
                if (head != null && !head.isEmpty) {
                    table.set("head", LuaItemStack(head))
                }

                val chest = entity.getEquippedStack(EquipmentSlot.CHEST)
                if (chest != null && !chest.isEmpty) {
                    table.set("chest", LuaItemStack(chest))
                }

                val legs = entity.getEquippedStack(EquipmentSlot.LEGS)
                if (legs != null && !legs.isEmpty) {
                    table.set("legs", LuaItemStack(legs))
                }

                val feet = entity.getEquippedStack(EquipmentSlot.FEET)
                if (feet != null && !feet.isEmpty) {
                    table.set("feet", LuaItemStack(feet))
                }

                val effectsTable = LuaValue.tableOf()
                var effectIndex = 1
                entity.activeStatusEffects.forEach { (effect, instance) ->
                    val effectTable = LuaValue.tableOf()
                    effectTable.set("type", LuaValue.valueOf(effect.type.name))
                    effectTable.set("duration", LuaValue.valueOf(instance.duration))
                    effectTable.set("amplifier", LuaValue.valueOf(instance.amplifier))
                    effectsTable.set(effectIndex++, effectTable)
                }
                table.set("active_effects", effectsTable)
            }
            return table
        } else {
            return LuaValue.NIL
        }
    }
}