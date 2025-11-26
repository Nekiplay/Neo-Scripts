package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import com.nekiplay.hypixelcry.HypixelCry.mc
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaEntity(val entity: Entity): LuaUserdata(entity) {
    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            // Основная информация о сущности
            "id" -> valueOf(entity.id.toDouble())
            "uuid" -> valueOf(entity.stringUUID)
            "name" -> valueOf(entity.name.string)
            "display_name" -> valueOf(entity.displayName?.string ?: "")
            "type" -> valueOf(entity.type.toString())

            // Позиция и движение
            "x" -> {
                val pos = entity.getPosition(1f)
                valueOf(pos.x)
            }
            "y" -> {
                val pos = entity.getPosition(1f)
                valueOf(pos.y)
            }
            "z" -> {
                val pos = entity.getPosition(1f)
                valueOf(pos.z)
            }
            "velocity_x" -> valueOf(entity.forward.x)
            "velocity_y" -> valueOf(entity.forward.y)
            "velocity_z" -> valueOf(entity.forward.z)
            "velocity" -> {
                val t = tableOf()
                t.set("x", valueOf(entity.forward.x))
                t.set("y", valueOf(entity.forward.y))
                t.set("z", valueOf(entity.forward.z))
                t
            }

            // Размеры и вращение
            "width" -> valueOf(entity.bbWidth.toDouble())
            "height" -> valueOf(entity.bbHeight.toDouble())
            "yaw" -> valueOf(entity.xRot.toDouble())
            "pitch" -> valueOf(entity.yRot.toDouble())

            // Состояния
            "is_on_ground" -> valueOf(entity.onGround())
            "is_touching_water" -> valueOf(entity.isInWater)
            "is_in_lava" -> valueOf(entity.isInLava)
            "is_sneaking" -> valueOf(entity.isShiftKeyDown)
            "is_sprinting" -> valueOf(entity.isSprinting)

            // Дополнительные свойства
            "age" -> valueOf(entity.tickCount)
            "distance_to_player" -> {
                val player = mc.player
                if (player != null) {
                    valueOf(entity.distanceToSqr(player).toDouble())
                } else {
                    valueOf(0.0)
                }
            }

            // Специфичные для ItemFrameEntity
            "item" -> {
                when (entity) {
                    is ItemFrame -> {
                        LuaItemStack(entity.item)
                    }

                    is ItemEntity -> {
                        LuaItemStack(entity.item)
                    }

                    else -> {
                        LuaValue.NIL
                    }
                }
            }

            // Специфичные для LivingEntity
            "health" -> {
                if (entity is LivingEntity) {
                    valueOf(entity.health.toDouble())
                } else {
                    LuaValue.NIL
                }
            }
            "max_health" -> {
                if (entity is LivingEntity) {
                    valueOf(entity.maxHealth.toDouble())
                } else {
                    LuaValue.NIL
                }
            }
            "is_alive" -> {
                if (entity is LivingEntity) {
                    valueOf(entity.isAlive)
                } else {
                    LuaValue.NIL
                }
            }
            "is_child" -> {
                if (entity is LivingEntity) {
                    valueOf(entity.isBaby)
                } else {
                    LuaValue.NIL
                }
            }
            "main_hand" -> {
                if (entity is LivingEntity) {
                    val mainHandStack = entity.mainHandItem
                    if (!mainHandStack.isEmpty) {
                        LuaItemStack(mainHandStack)
                    } else {
                        LuaValue.NIL
                    }
                } else {
                    LuaValue.NIL
                }
            }
            "off_hand" -> {
                if (entity is LivingEntity) {
                    val offHandStack = entity.offhandItem
                    if (!offHandStack.isEmpty) {
                        LuaItemStack(offHandStack)
                    } else {
                        LuaValue.NIL
                    }
                } else {
                    LuaValue.NIL
                }
            }
            "head" -> {
                if (entity is LivingEntity) {
                    val head = entity.getItemBySlot(EquipmentSlot.HEAD)
                    if (!head.isEmpty) {
                        LuaItemStack(head)
                    } else {
                        LuaValue.NIL
                    }
                } else {
                    LuaValue.NIL
                }
            }
            "chest" -> {
                if (entity is LivingEntity) {
                    val chest = entity.getItemBySlot(EquipmentSlot.CHEST)
                    if (!chest.isEmpty) {
                        LuaItemStack(chest)
                    } else {
                        LuaValue.NIL
                    }
                } else {
                    LuaValue.NIL
                }
            }
            "legs" -> {
                if (entity is LivingEntity) {
                    val legs = entity.getItemBySlot(EquipmentSlot.LEGS)
                    if (!legs.isEmpty) {
                        LuaItemStack(legs)
                    } else {
                        LuaValue.NIL
                    }
                } else {
                    LuaValue.NIL
                }
            }
            "feet" -> {
                if (entity is LivingEntity) {
                    val feet = entity.getItemBySlot(EquipmentSlot.FEET)
                    if (!feet.isEmpty) {
                        LuaItemStack(feet)
                    } else {
                        LuaValue.NIL
                    }
                } else {
                    LuaValue.NIL
                }
            }
            "active_effects" -> {
                if (entity is LivingEntity) {
                    val effectsTable = tableOf()
                    var effectIndex = 1
                    entity.activeEffectsMap.forEach { (effect, instance) ->
                        val effectTable = tableOf()
                        effectTable.set("type", valueOf(effect.registeredName))
                        effectTable.set("duration", valueOf(instance.duration.toDouble()))
                        effectTable.set("amplifier", valueOf(instance.amplifier.toDouble()))
                        effectsTable.set(effectIndex++, effectTable)
                    }
                    effectsTable
                } else {
                    LuaValue.NIL
                }
            }
            else -> super.get(key)
        }
    }

    fun getEnt(): Entity = entity
}