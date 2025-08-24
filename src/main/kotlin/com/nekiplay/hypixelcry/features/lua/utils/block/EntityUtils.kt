package com.nekiplay.hypixelcry.features.lua.utils.block

import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import org.luaj.vm2.LuaValue

object EntityUtils {
    // Функция для преобразования Entity в Lua таблицу
    public fun ToLua(entity: Entity?): LuaValue? {
        if (entity != null) {
            val table = LuaValue.tableOf()

            // Основная информация о сущности
            table.set("id", LuaValue.valueOf(entity.id))
            table.set("uuid", LuaValue.valueOf(entity.uuidAsString))
            table.set("name", LuaValue.valueOf(entity.name.string))
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
            table.set("distance_to_player", LuaValue.valueOf(entity.distanceTo(mc.player).toDouble()))

            return table
        } else {
            return LuaValue.NIL
        }
    }

    // Функция для преобразования LivingEntity в Lua таблицу
    public fun ToLua(livingEntity: ClientPlayerEntity?): LuaValue? {
        if (livingEntity != null) {
            // Сначала получаем базовые данные Entity
            val table = ToLua(livingEntity as Entity) ?: LuaValue.tableOf()

            // Добавляем специфичные для LivingEntity свойства
            table.set("health", LuaValue.valueOf(livingEntity.health.toDouble()))
            table.set("max_health", LuaValue.valueOf(livingEntity.maxHealth.toDouble()))
            table.set("is_alive", LuaValue.valueOf(livingEntity.isAlive))
            // Информация об экипировке и состоянии
            table.set("is_child", LuaValue.valueOf(livingEntity.isBaby))

            // Активные эффекты
            val effectsTable = LuaValue.tableOf()
            var effectIndex = 1
            livingEntity.activeStatusEffects.forEach { (effect, instance) ->
                val effectTable = LuaValue.tableOf()
                effectTable.set("type", LuaValue.valueOf(effect.type.name))
                effectTable.set("duration", LuaValue.valueOf(instance.duration))
                effectTable.set("amplifier", LuaValue.valueOf(instance.amplifier))
                effectsTable.set(effectIndex++, effectTable)
            }
            table.set("active_effects", effectsTable)

            return table
        } else {
            return LuaValue.NIL

        }
    }
}