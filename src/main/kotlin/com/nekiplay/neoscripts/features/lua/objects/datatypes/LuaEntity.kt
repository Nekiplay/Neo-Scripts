package com.nekiplay.neoscripts.features.lua.objects.datatypes

import com.mojang.authlib.properties.Property
import com.nekiplay.neoscripts.Main
import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.neoscripts.sugar.getFormattedString
import com.nekiplay.neoscripts.utils.Utils
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.util.ProblemReporter
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaEntity(val entity: Entity): LuaUserdata(entity) {
    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            // Основная информация о сущности
            "id" -> valueOf(entity.id)
            "uuid" -> valueOf(entity.stringUUID)
            "name" -> valueOf(entity.name.string)
            "display_name" -> valueOf(entity.displayName?.getFormattedString()) ?: NIL
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
            "pos", "position" -> LuaVector3d(entity.getPosition(1f))
            "blockpos" -> LuaBlockPos(entity.blockPosition())

            "box" -> LuaBox(entity.boundingBox)

            "velocity_x" -> valueOf(entity.deltaMovement.x)
            "velocity_y" -> valueOf(entity.deltaMovement.y)
            "velocity_z" -> valueOf(entity.deltaMovement.z)
            "velocity" -> LuaVector3d(entity.deltaMovement)

            "gravity" -> valueOf(entity.gravity)
            "horizontal_collision" -> valueOf(entity.horizontalCollision)
            "vertical_collision" -> valueOf(entity.verticalCollision)
            "hurt_marked" -> valueOf(entity.hurtMarked)
            "controlled_venicle" -> {
                val venicle = entity.controlledVehicle
                if (venicle != null) {
                    LuaEntity(venicle)
                }
                else {
                    NIL
                }
            }
            "nearest_view_direction" -> LuaDirection(entity.nearestViewDirection)
            "direction" -> LuaDirection(entity.direction)
            "touching_unloaded_chunk" -> valueOf(entity.touchingUnloadedChunk())

            // Размеры и вращение
            "width" -> valueOf(entity.bbWidth.toDouble())
            "height" -> valueOf(entity.bbHeight.toDouble())
            "yaw" -> valueOf(entity.xRot.toDouble())
            "pitch" -> valueOf(entity.yRot.toDouble())

            // Состояния
            "is_swimming" -> valueOf(entity.isSwimming)
            "is_on_ground" -> valueOf(entity.onGround())
            "is_touching_water" -> valueOf(entity.isInWater)
            "is_in_lava" -> valueOf(entity.isInLava)
            "is_sneaking" -> valueOf(entity.isShiftKeyDown)
            "is_sprinting" -> valueOf(entity.isSprinting)
            "is_in_powder_snow" -> valueOf(entity.isInPowderSnow)
            "is_crouching" -> valueOf(entity.isCrouching)

            // Дополнительные свойства
            "passengers" -> {
                val t = tableOf()
                entity.passengers.forEachIndexed { index, entity ->
                    t.set(index + 1, LuaEntity(entity))
                }
                t
            }
            "age" -> valueOf(entity.tickCount)
            "distance_to_player" -> {
                val player = mc.player
                if (player != null) {
                    valueOf(entity.distanceToSqr(player))
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
                        NIL
                    }
                }
            }

            // Специфичные для LivingEntity
            "skin" -> {
                if (entity is Player) {
                    val profile = entity.gameProfile
                    if (profile != null) {
                        val textures: Collection<Property> = profile.properties().get("textures")
                        for (entry in textures) {
                            if (entry.value() != null) {
                                return valueOf(entry.value())
                            }
                        }
                        
                    }
                }
                return NIL
            }
            "health" -> {
                if (entity is LivingEntity) {
                    valueOf(entity.health.toDouble())
                } else {
                    NIL
                }
            }
            "max_health" -> {
                if (entity is LivingEntity) {
                    valueOf(entity.maxHealth.toDouble())
                } else {
                    NIL
                }
            }
            "is_alive" -> {
                if (entity is LivingEntity) {
                    valueOf(entity.isAlive)
                } else {
                    NIL
                }
            }
            "is_child", "is_baby" -> {
                if (entity is LivingEntity) {
                    valueOf(entity.isBaby)
                } else {
                    NIL
                }
            }
            "main_hand" -> {
                if (entity is LivingEntity) {
                    val mainHandStack = entity.mainHandItem
                    if (!mainHandStack.isEmpty) {
                        LuaItemStack(mainHandStack)
                    } else {
                        NIL
                    }
                } else {
                    NIL
                }
            }
            "off_hand" -> {
                if (entity is LivingEntity) {
                    val offHandStack = entity.offhandItem
                    if (!offHandStack.isEmpty) {
                        LuaItemStack(offHandStack)
                    } else {
                        NIL
                    }
                } else {
                    NIL
                }
            }
            "head" -> {
                if (entity is LivingEntity) {
                    val head = entity.getItemBySlot(EquipmentSlot.HEAD)
                    if (!head.isEmpty) {
                        LuaItemStack(head)
                    } else {
                        NIL
                    }
                } else {
                    NIL
                }
            }
            "chest" -> {
                if (entity is LivingEntity) {
                    val chest = entity.getItemBySlot(EquipmentSlot.CHEST)
                    if (!chest.isEmpty) {
                        LuaItemStack(chest)
                    } else {
                        NIL
                    }
                } else {
                    NIL
                }
            }
            "legs" -> {
                if (entity is LivingEntity) {
                    val legs = entity.getItemBySlot(EquipmentSlot.LEGS)
                    if (!legs.isEmpty) {
                        LuaItemStack(legs)
                    } else {
                        NIL
                    }
                } else {
                    NIL
                }
            }
            "feet" -> {
                if (entity is LivingEntity) {
                    val feet = entity.getItemBySlot(EquipmentSlot.FEET)
                    if (!feet.isEmpty) {
                        LuaItemStack(feet)
                    } else {
                        NIL
                    }
                } else {
                    NIL
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
                    NIL
                }
            }
            "nbt" -> {
                val registryLookup = Utils.getRegistryWrapperLookup()
                val reporter = ProblemReporter.ScopedCollector(Main.LOGGER)
                val output = TagValueOutput.createWithContext(reporter, registryLookup)
                entity.saveWithoutId(output)
                valueOf(output.buildResult().toString())
            }
            else -> super.get(key)
        }
    }

    override fun set(key: LuaValue, value: LuaValue) {
        if (entity != mc.player) return

        when (val field = key.tojstring()) {
            "velocity_x" -> {
                if (value.isnumber()) {
                    entity.setDeltaMovement(value.todouble(), entity.deltaMovement.y, entity.deltaMovement.z)
                }
            }
            "velocity_y" -> {
                if (value.isnumber()) {
                    entity.setDeltaMovement(entity.deltaMovement.z, value.todouble(), entity.deltaMovement.z)
                }
            }
            "velocity_z" -> {
                if (value.isnumber()) {
                    entity.setDeltaMovement(entity.deltaMovement.z, entity.deltaMovement.y, value.todouble())
                }
            }
            "velocity" -> {
                if (value.isuserdata() && value.touserdata() is LuaVector3d) {
                    val vector = value.touserdata() as LuaVector3d
                    entity.setDeltaMovement(vector.location.x, vector.location.y, vector.location.z)
                }
                else if (value.isuserdata() && value.touserdata() is Vec3) {
                    val vector = value.touserdata() as Vec3
                    entity.setDeltaMovement(vector.x, vector.y, vector.z)
                }
            }
            "pos", "position" -> {
                if (value.isuserdata() && value.touserdata() is LuaVector3d) {
                    val vector = value.touserdata() as LuaVector3d
                    entity.setPos(vector.location.x, vector.location.y, vector.location.z)
                    mc.connection?.send(ServerboundMovePlayerPacket.Pos(vector.location, entity.onGround(), entity.horizontalCollision))
                }
                else if (value.isuserdata() && value.touserdata() is Vec3) {
                    val vector = value.touserdata() as Vec3
                    entity.setPos(vector.x, vector.y, vector.z)
                    mc.connection?.send(ServerboundMovePlayerPacket.Pos(vector, entity.onGround(), entity.horizontalCollision))
                }
            }
        }
    }

    override fun typename(): String = "entity"
}