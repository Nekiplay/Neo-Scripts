package com.nekiplay.neoscripts.features.lua.objects.datatypes

import com.mojang.authlib.properties.Property
import com.nekiplay.neoscripts.Main
import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.features.lua.customArgs.FourArgFunction
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.neoscripts.sugar.getFormattedString
import com.nekiplay.neoscripts.sugar.getRotation
import com.nekiplay.neoscripts.utils.Utils
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.util.ProblemReporter
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaLong
import org.luaj.vm2.LuaNumber
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JavaInstance

class LuaEntity(val entity: Entity): LuaUserdata(entity) {
    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            // Основная информация о сущности
            "javaClass", "class" -> JavaInstance(entity);
            "id" -> valueOf(entity.id)
            "uuid" -> valueOf(entity.stringUUID)
            "name" -> valueOf(entity.name.string)
            "identifier" -> valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).toString())
            "display_name" -> valueOf(entity.displayName?.getFormattedString()) ?: NIL
            "custom_name" -> valueOf(entity.customName?.getFormattedString()) ?: NIL
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
            "gamemode" -> {
                if (entity is Player) {
                    valueOf(entity.gameMode()?.name)
                } else {
                    NIL
                }
            }
            "is_blocking" -> {
                if (entity is LivingEntity) {
                    valueOf(entity.isBlocking)
                } else {
                    NIL
                }
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
                val logger = Main.LOGGER
                if (logger != null) {
                    val registryLookup = Utils.getRegistryWrapperLookup()
                    val reporter = ProblemReporter.ScopedCollector(Main.LOGGER)
                    val output = TagValueOutput.createWithContext(reporter, registryLookup)
                    entity.saveWithoutId(output)
                    valueOf(output.buildResult().toString())
                }
                else { NIL }
            }
            "teleport" -> TeleportFunction()
            else -> super.get(key)
        }
    }

    override fun eq(other: LuaValue?): LuaValue {
        return when (other) {
            is LuaEntity if entity == other.entity -> {
                LuaValue.TRUE
            }
            is LuaInteger if entity.id == other.toint() -> {
                LuaValue.TRUE
            }
            is LuaNumber if entity.id == other.toint() -> {
                LuaValue.TRUE
            }
            is LuaLong if entity.id == other.toint() -> {
                LuaValue.TRUE
            }
            is LuaDouble if entity.id == other.toint() -> {
                LuaValue.TRUE
            }
            else -> {
                LuaValue.FALSE
            }
        }
    }

    private inner class TeleportFunction : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            if (entity != mc.player) return FALSE
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isboolean() == true) {
                val vector = Vec3(arg1.todouble(), arg2.todouble(), arg3.todouble())
                entity.setPos(vector.x, vector.y, vector.z)
                val rot = entity.getRotation()
                mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, arg4.toboolean(), entity.horizontalCollision))
                return TRUE
            }
            else if (arg1?.istable() ?: false) {
                val x: Double = if (arg1.get("x").isnumber()) arg1.get("x").todouble() else 0.0
                val y: Double = if (arg1.get("y").isnumber()) arg1.get("y").todouble() else 0.0
                val z: Double = if (arg1.get("z").isnumber()) arg1.get("z").todouble() else 0.0
                val on_ground: Boolean = if (arg1.get("on_ground").isnumber()) arg1.get("on_ground").toboolean() else true

                val vector = Vec3(x, y, z)
                entity.setPos(vector.x, vector.y, vector.z)
                val rot = entity.getRotation()
                mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, on_ground, entity.horizontalCollision))
                return TRUE
            }
            else if (arg1?.isuserdata() == true && arg1.touserdata() is LuaVector3d) {
                val vector = arg1.touserdata() as LuaVector3d
                entity.setPos(vector.location.x, vector.location.y, vector.location.z)
                val rot = entity.getRotation()
                mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector.location, rot.first, rot.second, arg2?.toboolean() ?: true, entity.horizontalCollision))
            }
            else if (arg1?.isuserdata() == true && arg1.touserdata() is Vec3) {
                val vector = arg1.touserdata() as Vec3
                entity.setPos(vector.x, vector.y, vector.z)
                val rot = entity.getRotation()
                mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, arg2?.toboolean() ?: true, entity.horizontalCollision))
            }
            return FALSE
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
            "x" -> {
                if (value.isnumber()) {
                    val vector = Vec3(value.todouble(), entity.getPosition(1f).y, entity.getPosition(1f).z)
                    entity.setPos(vector.x, vector.y, vector.z)
                    val rot = entity.getRotation()
                    mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                }
            }
            "y" -> {
                if (value.isnumber()) {
                    val vector = Vec3(entity.getPosition(1f).x, value.todouble(), entity.getPosition(1f).z)
                    entity.setPos(vector.x, vector.y, vector.z)
                    val rot = entity.getRotation()
                    mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                }
            }
            "z" -> {
                if (value.isnumber()) {
                    val vector = Vec3(entity.getPosition(1f).x, entity.getPosition(1f).y, value.todouble())
                    entity.setPos(vector.x, vector.y, vector.z)
                    val rot = entity.getRotation()
                    mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                }
            }
            "pos", "position" -> {
                if (value.isuserdata() && value.touserdata() is LuaVector3d) {
                    val vector = value.touserdata() as LuaVector3d
                    entity.setPos(vector.location.x, vector.location.y, vector.location.z)
                    val rot = entity.getRotation()
                    mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector.location, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                }
                else if (value.isuserdata() && value.touserdata() is Vec3) {
                    val vector = value.touserdata() as Vec3
                    entity.setPos(vector.x, vector.y, vector.z)
                    val rot = entity.getRotation()
                    mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                }
            }
        }
    }

    override fun typename(): String = "entity"
}