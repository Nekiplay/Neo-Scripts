package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import com.mojang.authlib.properties.Property
import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.ClientMain.mc
import com.nekiplay.neoscripts.client.features.lua.customArgs.FourArgFunction
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponent
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponentBuilder
import com.nekiplay.neoscripts.client.sugar.getFormattedString
import com.nekiplay.neoscripts.client.sugar.getRotation
import com.nekiplay.neoscripts.client.utils.Utils
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.resources.Identifier
import net.minecraft.util.ProblemReporter
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaLong
import org.luaj.vm2.LuaNumber
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
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
                val logger = ClientMain.LOGGER
                if (logger != null) {
                    val registryLookup = Utils.getRegistryWrapperLookup()
                    val reporter = ProblemReporter.ScopedCollector(ClientMain.LOGGER)
                    val output = TagValueOutput.createWithContext(reporter, registryLookup)
                    entity.saveWithoutId(output)
                    valueOf(output.buildResult().toString())
                }
                else { NIL }
            }

            "add_effect" -> AddEffectFunction()
            "remove_effect" -> RemoveEffectFunction()

            // Инвентарь игрока (для ServerPlayer изменения синхронизируются сервером с клиентом автоматически)
            "inventory" -> {
                if (entity is Player) LuaInventory(entity.inventory) else NIL
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

    private fun parseEffect(arg: LuaValue): Holder<MobEffect>? {
        if (!arg.isstring()) return null
        val name = arg.tojstring()
        return try {
            val id = if (name.indexOf(':') >= 0) name else "minecraft:$name"
            BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(id)).orElse(null)
        } catch (_: Exception) {
            null
        }
    }

    // Выдача эффекта: add_effect("minecraft:speed"[, duration][, amplifier]) -> boolean
    // duration в тиках, -1 — бесконечно
    private inner class AddEffectFunction : VarArgFunction() {
        override fun invoke(args: Varargs?): LuaValue {
            val living = entity as? LivingEntity ?: return NIL
            if (args == null || args.narg() < 1) return NIL
            val holder = parseEffect(args.arg1()) ?: return FALSE
            val duration = if (args.narg() >= 2 && args.arg(2).isnumber()) args.arg(2).toint() else -1
            val amplifier = if (args.narg() >= 3 && args.arg(3).isnumber()) args.arg(3).toint() else 0
            return valueOf(living.addEffect(MobEffectInstance(holder, duration, amplifier)))
        }
    }

    // Убирание эффекта: remove_effect(["minecraft:speed"]) -> boolean
    // Без аргументов снимает все эффекты
    private inner class RemoveEffectFunction : VarArgFunction() {
        override fun invoke(args: Varargs?): LuaValue {
            val living = entity as? LivingEntity ?: return NIL
            if (args == null || args.narg() == 0 || args.isnil(1)) {
                return valueOf(living.removeAllEffects())
            }
            val holder = parseEffect(args.arg1()) ?: return FALSE
            return valueOf(living.removeEffect(holder))
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

    private fun toStack(value: LuaValue): ItemStack? = when {
        value.isnil() -> ItemStack.EMPTY
        value is LuaItemStack -> value.stack.copy()
        else -> null
    }

    override fun set(key: LuaValue, value: LuaValue) {
        val field = key.tojstring()
        // Пакет о перемещении отправляем только если правим локального игрока с клиента
        val isLocalPlayer = entity == mc.player

        when (field) {
            // --- Позиция и движение (любая сущность; пакет — только для локального игрока) ---
            "velocity_x" -> {
                if (value.isnumber()) {
                    entity.setDeltaMovement(value.todouble(), entity.deltaMovement.y, entity.deltaMovement.z)
                }
            }
            "velocity_y" -> {
                if (value.isnumber()) {
                    entity.setDeltaMovement(entity.deltaMovement.x, value.todouble(), entity.deltaMovement.z)
                }
            }
            "velocity_z" -> {
                if (value.isnumber()) {
                    entity.setDeltaMovement(entity.deltaMovement.x, entity.deltaMovement.y, value.todouble())
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
                    if (isLocalPlayer) {
                        val rot = entity.getRotation()
                        mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                    }
                }
            }
            "y" -> {
                if (value.isnumber()) {
                    val vector = Vec3(entity.getPosition(1f).x, value.todouble(), entity.getPosition(1f).z)
                    entity.setPos(vector.x, vector.y, vector.z)
                    if (isLocalPlayer) {
                        val rot = entity.getRotation()
                        mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                    }
                }
            }
            "z" -> {
                if (value.isnumber()) {
                    val vector = Vec3(entity.getPosition(1f).x, entity.getPosition(1f).y, value.todouble())
                    entity.setPos(vector.x, vector.y, vector.z)
                    if (isLocalPlayer) {
                        val rot = entity.getRotation()
                        mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                    }
                }
            }
            "pos", "position" -> {
                var vector: Vec3? = null
                if (value.isuserdata() && value.touserdata() is LuaVector3d) {
                    vector = (value.touserdata() as LuaVector3d).location
                } else if (value.isuserdata() && value.touserdata() is Vec3) {
                    vector = value.touserdata() as Vec3
                }
                if (vector != null) {
                    entity.setPos(vector.x, vector.y, vector.z)
                    if (isLocalPlayer) {
                        val rot = entity.getRotation()
                        mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                    }
                }
            }

            // --- Общие свойства (любая сущность) ---
            "yaw" -> {
                if (value.isnumber()) {
                    entity.setYRot(value.todouble().toFloat())
                }
            }
            "pitch" -> {
                if (value.isnumber()) {
                    entity.setXRot(value.todouble().toFloat())
                }
            }
            "custom_name" -> {
                when {
                    value.isstring() -> entity.setCustomName(Component.literal(value.tojstring()))
                    value is LuaComponentBuilder -> entity.setCustomName(value.buildComponent())
                    value is LuaComponent -> entity.setCustomName(value.component.copy())
                    value.isnil() -> entity.setCustomName(null)
                }
            }
            "custom_name_visible" -> {
                if (value.isboolean()) {
                    entity.setCustomNameVisible(value.toboolean())
                }
            }
            "is_sneaking" -> {
                if (value.isboolean()) {
                    entity.setShiftKeyDown(value.toboolean())
                }
            }
            "is_sprinting" -> {
                if (value.isboolean()) {
                    entity.setSprinting(value.toboolean())
                }
            }
            "is_swimming" -> {
                if (value.isboolean()) {
                    entity.setSwimming(value.toboolean())
                }
            }
            "no_gravity" -> {
                if (value.isboolean()) {
                    entity.setNoGravity(value.toboolean())
                }
            }
            "invulnerable" -> {
                if (value.isboolean()) {
                    entity.setInvulnerable(value.toboolean())
                }
            }
            "glowing" -> {
                if (value.isboolean()) {
                    entity.setGlowingTag(value.toboolean())
                }
            }

            // --- Специфичные для LivingEntity ---
            "health" -> {
                if (entity is LivingEntity && value.isnumber()) {
                    entity.setHealth(value.todouble().toFloat().coerceIn(0f, entity.maxHealth))
                }
            }
            "main_hand" -> {
                if (entity is LivingEntity) {
                    toStack(value)?.let { entity.setItemInHand(InteractionHand.MAIN_HAND, it) }
                }
            }
            "off_hand" -> {
                if (entity is LivingEntity) {
                    toStack(value)?.let { entity.setItemInHand(InteractionHand.OFF_HAND, it) }
                }
            }
            "head" -> {
                if (entity is LivingEntity) {
                    toStack(value)?.let { entity.setItemSlot(EquipmentSlot.HEAD, it) }
                }
            }
            "chest" -> {
                if (entity is LivingEntity) {
                    toStack(value)?.let { entity.setItemSlot(EquipmentSlot.CHEST, it) }
                }
            }
            "legs" -> {
                if (entity is LivingEntity) {
                    toStack(value)?.let { entity.setItemSlot(EquipmentSlot.LEGS, it) }
                }
            }
            "feet" -> {
                if (entity is LivingEntity) {
                    toStack(value)?.let { entity.setItemSlot(EquipmentSlot.FEET, it) }
                }
            }
            "is_baby", "is_child" -> {
                if (entity is AgeableMob && value.isboolean()) {
                    entity.setBaby(value.toboolean())
                }
            }
        }
    }

    override fun typename(): String = "entity"
}