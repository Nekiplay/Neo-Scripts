package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import com.nekiplay.hypixelcry.HypixelCry.mc
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ItemFrameEntity
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaEntity(val entity: Entity): LuaUserdata(entity) {
    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            // Основная информация о сущности
            "id" -> valueOf(entity.id.toDouble())
            "uuid" -> valueOf(entity.uuidAsString)
            "name" -> valueOf(entity.name.string)
            "display_name" -> valueOf(entity.displayName?.string ?: "")
            "type" -> valueOf(entity.type.toString())

            // Позиция и движение
            "x" -> {
                val pos = entity.getLerpedPos(1f)
                valueOf(pos.x)
            }
            "y" -> {
                val pos = entity.getLerpedPos(1f)
                valueOf(pos.y)
            }
            "z" -> {
                val pos = entity.getLerpedPos(1f)
                valueOf(pos.z)
            }
            "velocity_x" -> valueOf(entity.velocity.x)
            "velocity_y" -> valueOf(entity.velocity.y)
            "velocity_z" -> valueOf(entity.velocity.z)
            "velocity" -> {
                val t = tableOf()
                t.set("x", valueOf(entity.velocity.x))
                t.set("y", valueOf(entity.velocity.y))
                t.set("z", valueOf(entity.velocity.z))
                t
            }

            // Размеры и вращение
            "width" -> valueOf(entity.width.toDouble())
            "height" -> valueOf(entity.height.toDouble())
            "yaw" -> valueOf(entity.yaw.toDouble())
            "pitch" -> valueOf(entity.pitch.toDouble())

            // Состояния
            "is_on_ground" -> valueOf(entity.isOnGround)
            "is_touching_water" -> valueOf(entity.isTouchingWater)
            "is_in_lava" -> valueOf(entity.isInLava)
            "is_sneaking" -> valueOf(entity.isSneaking)
            "is_sprinting" -> valueOf(entity.isSprinting)

            // Дополнительные свойства
            "age" -> valueOf(entity.age.toDouble())
            "distance_to_player" -> {
                val player = mc.player
                if (player != null) {
                    valueOf(entity.squaredDistanceTo(player).toDouble())
                } else {
                    valueOf(0.0)
                }
            }

            // Специфичные для ItemFrameEntity
            "item" -> {
                when (entity) {
                    is ItemFrameEntity -> {
                        LuaItemStack(entity.heldItemStack)
                    }

                    is ItemEntity -> {
                        LuaItemStack(entity.stack)
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
                    val mainHandStack = entity.mainHandStack
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
                    val offHandStack = entity.offHandStack
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
                    val head = entity.getEquippedStack(EquipmentSlot.HEAD)
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
                    val chest = entity.getEquippedStack(EquipmentSlot.CHEST)
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
                    val legs = entity.getEquippedStack(EquipmentSlot.LEGS)
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
                    val feet = entity.getEquippedStack(EquipmentSlot.FEET)
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
                    entity.activeStatusEffects.forEach { (effect, instance) ->
                        val effectTable = tableOf()
                        effectTable.set("type", valueOf(effect.type.name))
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

    fun getEntity(): Entity = entity
}