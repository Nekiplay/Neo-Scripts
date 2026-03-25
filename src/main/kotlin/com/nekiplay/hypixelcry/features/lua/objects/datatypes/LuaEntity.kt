package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import com.nekiplay.hypixelcry.HypixelCry.mc
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.hypixelcry.sugar.getFormattedString
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaEntity(L: Lua?, val entity: Entity): SimpleLuaWrapper(L) {
    override fun push() {
        super.push()

        val lua = L ?: return
        if (lua.getMetatable(-1) != 0) {
            lua.push(JFunction { l ->
                l.push(entity.name.string)
                1
            })
            lua.setField(-2, "__tostring")
            lua.pop(1)
        }
    }

    override fun pushValue(): LuaValue {
        push()
        return L!!.get()
    }
    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            // Основная информация о сущности
            "id" -> entity.id
            "uuid" -> entity.stringUUID
            "name" -> entity.name.string
            "display_name" -> entity.displayName?.getFormattedString() ?: null
            "type" -> entity.type.toString()

            // Позиция и движение
            "x" -> {
                val pos = entity.getPosition(1f)
                pos.x
            }
            "y" -> {
                val pos = entity.getPosition(1f)
                pos.y
            }
            "z" -> {
                val pos = entity.getPosition(1f)
                pos.z
            }
            "pos", "position" -> {
                l.newTable()
                val pos = entity.getPosition(1f)

                l.push(pos.x)
                l.setField(-2, "x")
                l.push(pos.y)
                l.setField(-2, "y")
                l.push(pos.z)
                l.setField(-2, "z")

                l.get()
            }

            "box" -> LuaBox(l, entity.boundingBox)

            "velocity_x" -> entity.forward.x
            "velocity_y" -> entity.forward.y
            "velocity_z" -> entity.forward.z
            "velocity" -> {
                l.newTable()
                l.push(entity.forward.x)
                l.setField(-2, "x")
                l.push(entity.forward.y)
                l.setField(-2, "y")
                l.push(entity.forward.z)
                l.setField(-2, "z")

                l.get()
            }

            // Размеры и вращение
            "width" -> entity.bbWidth.toDouble()
            "height" -> entity.bbHeight.toDouble()
            "yaw" -> entity.xRot.toDouble()
            "pitch" -> entity.yRot.toDouble()

            // Состояния
            "is_on_ground" -> entity.onGround()
            "is_touching_water" -> entity.isInWater
            "is_in_lava" -> entity.isInLava
            "is_sneaking" -> entity.isShiftKeyDown
            "is_sprinting" -> entity.isSprinting

            // Дополнительные свойства
            "passengers" -> {
                l.newTable()

                entity.passengers.forEachIndexed { index, passenger ->
                    LuaEntity(l, passenger)
                    l.rawSetI(-2, index + 1)
                }
                l.get()
            }
            "age" -> entity.tickCount
            "distance_to_player" -> {
                val player = mc.player
                if (player != null) {
                    entity.distanceToSqr(player)
                } else {
                    0.0
                }
            }

            // Специфичные для ItemFrameEntity
            "item" -> {
                when (entity) {
                    is ItemFrame -> LuaItemStack(null, entity.item)
                    is ItemEntity -> LuaItemStack(null, entity.item)
                    else -> null
                }
            }

            // Специфичные для LivingEntity
            "health" -> {
                if (entity is LivingEntity) {
                    entity.health.toDouble()
                } else {
                    null
                }
            }
            "max_health" -> {
                if (entity is LivingEntity) {
                    entity.maxHealth.toDouble()
                } else {
                    null
                }
            }
            "is_alive" -> {
                if (entity is LivingEntity) {
                    entity.isAlive
                } else {
                    null
                }
            }
            "is_child", "is_baby" -> {
                if (entity is LivingEntity) {
                    entity.isBaby
                } else {
                    null
                }
            }
            "main_hand" -> {
                if (entity is LivingEntity) {
                    val mainHandStack = entity.mainHandItem
                    if (!mainHandStack.isEmpty) LuaItemStack(null, mainHandStack) else null
                } else {
                    null
                }
            }
            "off_hand" -> {
                if (entity is LivingEntity) {
                    val offHandStack = entity.offhandItem
                    if (!offHandStack.isEmpty) LuaItemStack(null, offHandStack) else null
                } else {
                    null
                }
            }
            "head" -> {
                if (entity is LivingEntity) {
                    val head = entity.getItemBySlot(EquipmentSlot.HEAD)
                    if (!head.isEmpty) LuaItemStack(null, head) else null
                } else {
                    null
                }
            }
            "chest" -> {
                if (entity is LivingEntity) {
                    val chest = entity.getItemBySlot(EquipmentSlot.CHEST)
                    if (!chest.isEmpty) LuaItemStack(null, chest) else null
                } else {
                    null
                }
            }
            "legs" -> {
                if (entity is LivingEntity) {
                    val legs = entity.getItemBySlot(EquipmentSlot.LEGS)
                    if (!legs.isEmpty) LuaItemStack(null, legs) else null
                } else {
                    null
                }
            }
            "feet" -> {
                if (entity is LivingEntity) {
                    val feet = entity.getItemBySlot(EquipmentSlot.FEET)
                    if (!feet.isEmpty) LuaItemStack(null, feet) else null
                } else {
                    null
                }
            }
            "active_effects" -> {
                if (entity is LivingEntity) {
                    // 1. Создаем главную таблицу (список всех эффектов)
                    l.newTable()
                    // Сейчас главная таблица на индексе -1

                    var effectIndex = 1
                    // Проходим по мапе эффектов Minecraft
                    entity.activeEffectsMap.forEach { (effect, instance) ->
                        // 2. Создаем под-таблицу для данных одного эффекта
                        l.newTable()
                        // Теперь под-таблица на -1, а главная сместилась на -2

                        // Наполняем под-таблицу данными
                        // Устанавливаем "type" (имя эффекта)
                        // Примечание: в зависимости от версии MC может быть effect.value().registeredName или просто effect.registeredName
                        l.push(effect.registeredName)
                        l.setField(-2, "type")

                        // Устанавливаем "duration"
                        l.push(instance.duration.toDouble())
                        l.setField(-2, "duration")

                        // Устанавливаем "amplifier"
                        l.push(instance.amplifier.toDouble())
                        l.setField(-2, "amplifier")

                        // 3. Кладем под-таблицу (-1) в главную таблицу (-2)
                        // Метод rawSetI заберет под-таблицу со стека и положит её под номером effectIndex
                        l.rawSetI(-2, effectIndex)

                        effectIndex++
                    }

                    // 4. Забираем готовую главную таблицу со стека и возвращаем её как LuaValue
                    l.get()
                } else {
                    // Если сущность не LivingEntity, возвращаем nil
                    null
                }
            }
            else -> null
        }
    }
}