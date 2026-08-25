package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mojang.authlib.properties.Property
import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.ClientMain.mc
import com.nekiplay.neoscripts.client.features.lua.customArgs.FourArgFunction
import com.nekiplay.neoscripts.common.mixins.minecraft.ArmorStandAccessor
import com.nekiplay.neoscripts.common.mixins.minecraft.BlockDisplayAccessor
import com.nekiplay.neoscripts.common.mixins.minecraft.DisplayAccessor
import com.nekiplay.neoscripts.common.mixins.minecraft.InteractionAccessor
import com.nekiplay.neoscripts.common.mixins.minecraft.ItemDisplayAccessor
import com.nekiplay.neoscripts.common.mixins.minecraft.TextDisplayAccessor
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponent
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponentBuilder
import com.nekiplay.neoscripts.client.sugar.getFormattedString
import com.nekiplay.neoscripts.client.sugar.getRotation
import com.nekiplay.neoscripts.client.sugar.isBlock
import com.nekiplay.neoscripts.client.sugar.isEntity
import com.nekiplay.neoscripts.client.sugar.isTransformation
import com.nekiplay.neoscripts.client.sugar.isVector
import com.nekiplay.neoscripts.client.sugar.toBlock
import com.nekiplay.neoscripts.client.sugar.toEntity
import com.nekiplay.neoscripts.client.sugar.toTransformation
import com.nekiplay.neoscripts.client.sugar.toVector
import com.nekiplay.neoscripts.client.utils.Utils
import net.minecraft.core.Holder
import net.minecraft.core.Rotations
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import com.mojang.serialization.JsonOps
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.resources.Identifier
import net.minecraft.util.Brightness
import net.minecraft.util.ProblemReporter
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Interaction
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaLong
import org.luaj.vm2.LuaNumber
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
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

            // Текущая трансформация display-сущности (только чтение через LuaTransform)
            "transformation", "transform" -> {
                if (entity is Display) {
                    LuaTransform.fromTransformation(
                        DisplayAccessor.nsCreateTransformation(entity.entityData)
                    )
                } else {
                    NIL
                }
            }

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

            // Специфичные для ArmorStand
            "is_invisible", "invisible" -> valueOf(entity.isInvisible)
            "small" -> {
                if (entity is ArmorStand) valueOf(entity.isSmall) else NIL
            }
            "marker" -> {
                if (entity is ArmorStand) valueOf(entity.isMarker) else NIL
            }
            "show_arms" -> {
                if (entity is ArmorStand) valueOf(entity.showArms()) else NIL
            }
            "no_base_plate", "no_baseplate" -> {
                if (entity is ArmorStand) valueOf(!entity.showBasePlate()) else NIL
            }
            "head_pose" -> {
                if (entity is ArmorStand) rotationsToTable(entity.headPose) else NIL
            }
            "body_pose" -> {
                if (entity is ArmorStand) rotationsToTable(entity.bodyPose) else NIL
            }
            "left_arm_pose" -> {
                if (entity is ArmorStand) rotationsToTable(entity.leftArmPose) else NIL
            }
            "right_arm_pose" -> {
                if (entity is ArmorStand) rotationsToTable(entity.rightArmPose) else NIL
            }
            "left_leg_pose" -> {
                if (entity is ArmorStand) rotationsToTable(entity.leftLegPose) else NIL
            }
            "right_leg_pose" -> {
                if (entity is ArmorStand) rotationsToTable(entity.rightLegPose) else NIL
            }

            // Специфичные для Display (text_display, item_display, block_display)
            "billboard", "billboard_mode" -> {
                if (entity is Display) {
                    valueOf((entity as DisplayAccessor).nsGetBillboardConstraints().getSerializedName())
                } else {
                    NIL
                }
            }
            "view_range" -> {
                if (entity is Display) {
                    valueOf((entity as DisplayAccessor).nsGetViewRange().toDouble())
                } else {
                    NIL
                }
            }
            "shadow_radius" -> {
                if (entity is Display) {
                    valueOf((entity as DisplayAccessor).nsGetShadowRadius().toDouble())
                } else {
                    NIL
                }
            }
            "shadow_strength" -> {
                if (entity is Display) {
                    valueOf((entity as DisplayAccessor).nsGetShadowStrength().toDouble())
                } else {
                    NIL
                }
            }
            "brightness_override" -> {
                if (entity is Display) {
                    valueOf((entity as DisplayAccessor).nsGetPackedBrightnessOverride())
                } else {
                    NIL
                }
            }

            // Специфичные для TextDisplay
            "text" -> {
                if (entity is Display.TextDisplay) {
                    valueOf((entity as TextDisplayAccessor).nsGetText().getFormattedString())
                } else {
                    NIL
                }
            }
            "line_width" -> {
                if (entity is Display.TextDisplay) {
                    valueOf((entity as TextDisplayAccessor).nsGetLineWidth())
                } else {
                    NIL
                }
            }
            "text_opacity" -> {
                if (entity is Display.TextDisplay) {
                    val opacity = (entity as TextDisplayAccessor).nsGetTextOpacity().toInt() and 0xFF
                    valueOf(opacity)
                } else {
                    NIL
                }
            }
            "background_color" -> {
                if (entity is Display.TextDisplay) {
                    valueOf((entity as TextDisplayAccessor).nsGetBackgroundColor())
                } else {
                    NIL
                }
            }
            "text_shadow", "has_text_shadow" -> {
                if (entity is Display.TextDisplay) {
                    val flags = (entity as TextDisplayAccessor).nsGetFlags().toInt()
                    valueOf(flags and Display.TextDisplay.FLAG_SHADOW.toInt() != 0)
                } else {
                    NIL
                }
            }
            "see_through" -> {
                if (entity is Display.TextDisplay) {
                    val flags = (entity as TextDisplayAccessor).nsGetFlags().toInt()
                    valueOf(flags and Display.TextDisplay.FLAG_SEE_THROUGH.toInt() != 0)
                } else {
                    NIL
                }
            }
            "use_default_background", "default_background" -> {
                if (entity is Display.TextDisplay) {
                    val flags = (entity as TextDisplayAccessor).nsGetFlags().toInt()
                    valueOf(flags and Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND.toInt() != 0)
                } else {
                    NIL
                }
            }
            "text_align", "text_alignment" -> {
                if (entity is Display.TextDisplay) {
                    val flags = (entity as TextDisplayAccessor).nsGetFlags().toInt()
                    when {
                        flags and Display.TextDisplay.FLAG_ALIGN_LEFT.toInt() != 0 -> valueOf("left")
                        flags and Display.TextDisplay.FLAG_ALIGN_RIGHT.toInt() != 0 -> valueOf("right")
                        else -> valueOf("center")
                    }
                } else {
                    NIL
                }
            }

            // Специфичные для ItemDisplay
            "display_item", "displayed_item" -> {
                if (entity is Display.ItemDisplay) {
                    val stack = (entity as ItemDisplayAccessor).nsGetItemStack()
                    if (!stack.isEmpty) LuaItemStack(stack) else NIL
                } else {
                    NIL
                }
            }

            // Специфичные для BlockDisplay
            "display_block", "displayed_block" -> {
                if (entity is Display.BlockDisplay) {
                    LuaBlockState((entity as BlockDisplayAccessor).nsGetBlockState())
                } else {
                    NIL
                }
            }

            // Специфичные для Interaction
            "interaction_width" -> {
                if (entity is Interaction) {
                    valueOf((entity as InteractionAccessor).nsGetWidth().toDouble())
                } else {
                    NIL
                }
            }
            "interaction_height" -> {
                if (entity is Interaction) {
                    valueOf((entity as InteractionAccessor).nsGetHeight().toDouble())
                } else {
                    NIL
                }
            }
            "response" -> {
                if (entity is Interaction) {
                    valueOf((entity as InteractionAccessor).nsGetResponse())
                } else {
                    NIL
                }
            }

            // Инвентарь игрока (для ServerPlayer изменения синхронизируются сервером с клиентом автоматически)
            "inventory" -> {
                if (entity is Player) LuaInventory(entity.inventory) else NIL
            }
            "teleport" -> TeleportFunction()
            "add_passenger", "addPassenger" -> AddPassengerFunction()
            "remove_passenger", "removePassenger" -> RemovePassengerFunction()
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

    private fun parseEntityArg(arg: LuaValue?): Entity? =
        if (arg != null && arg.isEntity()) arg.toEntity() else null

    // Добавление пассажира: add_passenger(entity) -> boolean
    private inner class AddPassengerFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val passenger = parseEntityArg(arg) ?: return NIL
            if (passenger == entity) return FALSE
            passenger.stopRiding()
            return valueOf(passenger.startRiding(entity))
        }
    }

    // Снятие пассажира: remove_passenger(entity) -> boolean
    private inner class RemovePassengerFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val passenger = parseEntityArg(arg) ?: return NIL
            if (passenger == entity || !entity.hasPassenger(passenger)) return FALSE
            passenger.stopRiding()
            return TRUE
        }
    }

    private fun rotationsToTable(rotations: Rotations): LuaValue {
        val t = tableOf()
        t.set("x", valueOf(rotations.x().toDouble()))
        t.set("y", valueOf(rotations.y().toDouble()))
        t.set("z", valueOf(rotations.z().toDouble()))
        return t
    }

    private fun parseRotations(value: LuaValue): Rotations? {
        val vector = if (value.isVector()) value.toVector() else null
        if (vector != null) {
            return Rotations(vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat())
        }
        if (value.istable() != true) return null
        val x = value.get("x")
        val y = value.get("y")
        val z = value.get("z")
        if (!x.isnumber() || !y.isnumber() || !z.isnumber()) return null
        return Rotations(x.tofloat(), y.tofloat(), z.tofloat())
    }

    private inner class TeleportFunction : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue {
            var vector: Vec3? = null
            var onGround = true

            when {
                arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true -> {
                    vector = Vec3(arg1.todouble(), arg2.todouble(), arg3.todouble())
                    if (arg4?.isboolean() == true) onGround = arg4.toboolean()
                }
                arg1?.istable() == true -> {
                    val x: Double = if (arg1.get("x").isnumber()) arg1.get("x").todouble() else 0.0
                    val y: Double = if (arg1.get("y").isnumber()) arg1.get("y").todouble() else 0.0
                    val z: Double = if (arg1.get("z").isnumber()) arg1.get("z").todouble() else 0.0
                    if (arg1.get("on_ground").isboolean()) onGround = arg1.get("on_ground").toboolean()
                    vector = Vec3(x, y, z)
                }
                arg1 != null && arg1.isVector() -> {
                    vector = arg1.toVector()
                    if (arg2?.isboolean() == true) onGround = arg2.toboolean()
                }
            }

            if (vector == null) return FALSE

            // Логический сервер: телепортируем через серверное API.
            // ServerPlayer переопределяет teleportTo и синхронизирует позицию с клиентом.
            if (!entity.level().isClientSide) {
                entity.teleportTo(vector.x, vector.y, vector.z)
                return TRUE
            }

            // Клиентская сторона: пакетный путь доступен только для локального игрока
            if (entity != mc.player) return FALSE

            entity.setPos(vector.x, vector.y, vector.z)
            val rot = entity.getRotation()
            mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, onGround, entity.horizontalCollision))
            return TRUE
        }
    }

    /**
     * Конвертирует Lua-значение (таблица/строка/число/булево) в Gson JsonElement.
     * Таблицы с последовательными целыми ключами 1..n становятся массивами,
     * остальные — объектами. Используется для JSON text components.
     */
    private fun luaToJson(v: LuaValue): com.google.gson.JsonElement {
        return when {
            v.istable() -> {
                val t = v.checktable()
                var count = 0
                var isArray = true
                var k: LuaValue = LuaValue.NIL
                while (true) {
                    val e = t.next(k)
                    k = e.arg(1)
                    if (k.isnil()) break
                    if (!k.isint() || k.toint() != count + 1) isArray = false
                    count++
                }
                if (count == 0) {
                    JsonObject()
                } else if (isArray) {
                    val arr = JsonArray()
                    for (i in 1..count) arr.add(luaToJson(t.get(i)))
                    arr
                } else {
                    val obj = JsonObject()
                    var k2: LuaValue = LuaValue.NIL
                    while (true) {
                        val e = t.next(k2)
                        k2 = e.arg(1)
                        if (k2.isnil()) break
                        obj.add(k2.tojstring(), luaToJson(e.arg(2)))
                    }
                    obj
                }
            }
            v.isboolean() -> JsonPrimitive(v.toboolean())
            v.isnumber() -> JsonPrimitive(v.todouble())
            v.isstring() -> JsonPrimitive(v.tojstring())
            else -> JsonNull.INSTANCE
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
            // hurtMarked = true заставляет сервер разослать SetEntityMotion наблюдателям
            "velocity_x" -> {
                if (value.isnumber()) {
                    entity.setDeltaMovement(value.todouble(), entity.deltaMovement.y, entity.deltaMovement.z)
                    entity.hurtMarked = true
                }
            }
            "velocity_y" -> {
                if (value.isnumber()) {
                    entity.setDeltaMovement(entity.deltaMovement.x, value.todouble(), entity.deltaMovement.z)
                    entity.hurtMarked = true
                }
            }
            "velocity_z" -> {
                if (value.isnumber()) {
                    entity.setDeltaMovement(entity.deltaMovement.x, entity.deltaMovement.y, value.todouble())
                    entity.hurtMarked = true
                }
            }
            "velocity" -> {
                val vector = if (value.isVector()) value.toVector() else null
                if (vector != null) {
                    entity.setDeltaMovement(vector.x, vector.y, vector.z)
                    entity.hurtMarked = true
                }
            }
            "x" -> {
                if (value.isnumber()) {
                    val vector = Vec3(value.todouble(), entity.getPosition(1f).y, entity.getPosition(1f).z)
                    // Логический сервер: ServerPlayer переопределяет teleportTo и синхронизирует позицию с клиентом
                    if (!entity.level().isClientSide) {
                        entity.teleportTo(vector.x, vector.y, vector.z)
                    }
                    else {
                        entity.setPos(vector.x, vector.y, vector.z)
                        if (isLocalPlayer) {
                            val rot = entity.getRotation()
                            mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                        }
                    }
                }
            }
            "y" -> {
                if (value.isnumber()) {
                    val vector = Vec3(entity.getPosition(1f).x, value.todouble(), entity.getPosition(1f).z)
                    if (!entity.level().isClientSide) {
                        entity.teleportTo(vector.x, vector.y, vector.z)
                    }
                    else {
                        entity.setPos(vector.x, vector.y, vector.z)
                        if (isLocalPlayer) {
                            val rot = entity.getRotation()
                            mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                        }
                    }
                }
            }
            "z" -> {
                if (value.isnumber()) {
                    val vector = Vec3(entity.getPosition(1f).x, entity.getPosition(1f).y, value.todouble())
                    if (!entity.level().isClientSide) {
                        entity.teleportTo(vector.x, vector.y, vector.z)
                    }
                    else {
                        entity.setPos(vector.x, vector.y, vector.z)
                        if (isLocalPlayer) {
                            val rot = entity.getRotation()
                            mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                        }
                    }
                }
            }
            "pos", "position" -> {
                var vector: Vec3? = null
                if (value.isVector()) {
                    vector = value.toVector()
                }
                if (vector != null) {
                    if (!entity.level().isClientSide) {
                        entity.teleportTo(vector.x, vector.y, vector.z)
                    }
                    else {
                        entity.setPos(vector.x, vector.y, vector.z)
                        if (isLocalPlayer) {
                            val rot = entity.getRotation()
                            mc.connection?.send(ServerboundMovePlayerPacket.PosRot(vector, rot.first, rot.second, entity.onGround(), entity.horizontalCollision))
                        }
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
            "passengers" -> {
                if (value.istable()) {
                    val resolved = mutableListOf<Entity>()
                    var index = 1
                    while (true) {
                        val entry = value.get(index)
                        if (entry.isnil()) break
                        parseEntityArg(entry)?.let { resolved.add(it) }
                        index++
                    }
                    entity.ejectPassengers()
                    for (passenger in resolved) {
                        if (passenger == entity) continue
                        passenger.stopRiding()
                        passenger.startRiding(entity)
                    }
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

            // --- Специфичные для ArmorStand ---
            "invisible", "is_invisible" -> {
                if (value.isboolean()) {
                    entity.setInvisible(value.toboolean())
                }
            }
            "small" -> {
                if (entity is ArmorStand && value.isboolean()) {
                    (entity as ArmorStandAccessor).nsSetSmall(value.toboolean())
                }
            }
            "marker" -> {
                if (entity is ArmorStand && value.isboolean()) {
                    (entity as ArmorStandAccessor).nsSetMarker(value.toboolean())
                }
            }
            "show_arms" -> {
                if (entity is ArmorStand && value.isboolean()) {
                    entity.setShowArms(value.toboolean())
                }
            }
            "no_base_plate", "no_baseplate" -> {
                if (entity is ArmorStand && value.isboolean()) {
                    entity.setNoBasePlate(value.toboolean())
                }
            }
            "head_pose" -> {
                if (entity is ArmorStand) parseRotations(value)?.let { entity.setHeadPose(it) }
            }
            "body_pose" -> {
                if (entity is ArmorStand) parseRotations(value)?.let { entity.setBodyPose(it) }
            }
            "left_arm_pose" -> {
                if (entity is ArmorStand) parseRotations(value)?.let { entity.setLeftArmPose(it) }
            }
            "right_arm_pose" -> {
                if (entity is ArmorStand) parseRotations(value)?.let { entity.setRightArmPose(it) }
            }
            "left_leg_pose" -> {
                if (entity is ArmorStand) parseRotations(value)?.let { entity.setLeftLegPose(it) }
            }
            "right_leg_pose" -> {
                if (entity is ArmorStand) parseRotations(value)?.let { entity.setRightLegPose(it) }
            }

            // --- Специфичные для Display (text_display, item_display, block_display) ---
            "billboard", "billboard_mode" -> {
                if (entity is Display && value.isstring()) {
                    val mode = Display.BillboardConstraints.values().firstOrNull {
                        it.getSerializedName().equals(value.tojstring(), ignoreCase = true)
                    }
                    if (mode != null) {
                        (entity as DisplayAccessor).nsSetBillboardConstraints(mode)
                    }
                }
            }
            "view_range" -> {
                if (entity is Display && value.isnumber()) {
                    (entity as DisplayAccessor).nsSetViewRange(value.todouble().toFloat().coerceAtLeast(0f))
                }
            }
            "shadow_radius" -> {
                if (entity is Display && value.isnumber()) {
                    (entity as DisplayAccessor).nsSetShadowRadius(value.todouble().toFloat().coerceAtLeast(0f))
                }
            }
            "shadow_strength" -> {
                if (entity is Display && value.isnumber()) {
                    (entity as DisplayAccessor).nsSetShadowStrength(
                        value.todouble().toFloat().coerceIn(0f, 1f)
                    )
                }
            }
            "brightness_override" -> {
                if (entity is Display && value.isnumber()) {
                    val packed = value.toint()
                    val brightness = if (packed < 0) null else Brightness.unpack(packed)
                    (entity as DisplayAccessor).nsSetBrightnessOverride(brightness)
                }
            }
            "transformation", "transform" -> {
                if (entity is Display) {
                    when {
                        // LuaTransform / Transformation (sugar учитывает fullMatrix)
                        value.isTransformation() -> {
                            (entity as DisplayAccessor).nsSetTransformation(value.toTransformation())
                        }
                        // { matrix = {16 чисел row-major} } — точная матрица 4x4
                        value.istable() -> {
                            val mt = value.get("matrix")
                            if (mt.istable() && mt.length() >= 16) {
                                val mat = org.joml.Matrix4f()
                                for (col in 0..3) {
                                    for (row in 0..3) {
                                        mat.set(col, row, mt[row * 4 + col + 1].tofloat())
                                    }
                                }
                                (entity as DisplayAccessor).nsSetTransformation(
                                    com.mojang.math.Transformation(mat)
                                )
                            }
                        }
                    }
                }
            }

            // --- Специфичные для TextDisplay ---
            "text" -> {
                if (entity is Display.TextDisplay) {
                    when {
                        value.isstring() -> (entity as TextDisplayAccessor).nsSetText(Component.literal(value.tojstring()))
                        value is LuaComponentBuilder -> (entity as TextDisplayAccessor).nsSetText(value.buildComponent())
                        value is LuaComponent -> (entity as TextDisplayAccessor).nsSetText(value.component.copy())
                        value.istable() -> {
                            // JSON text component (как в NBT text_display): таблица -> JsonElement -> Component
                            val json = luaToJson(value)
                            val parsed = ComponentSerialization.CODEC.decode(JsonOps.INSTANCE, json).result()
                            if (parsed.isPresent) {
                                (entity as TextDisplayAccessor).nsSetText(parsed.get().first)
                            }
                        }
                    }
                }
            }
            "line_width" -> {
                if (entity is Display.TextDisplay && value.isnumber()) {
                    (entity as TextDisplayAccessor).nsSetLineWidth(value.toint())
                }
            }
            "text_opacity" -> {
                if (entity is Display.TextDisplay && value.isnumber()) {
                    val opacity = value.toint().coerceIn(0, 255).toByte()
                    (entity as TextDisplayAccessor).nsSetTextOpacity(opacity)
                }
            }
            "background_color" -> {
                if (entity is Display.TextDisplay && value.isnumber()) {
                    (entity as TextDisplayAccessor).nsSetBackgroundColor(value.toint())
                }
            }
            "text_shadow", "has_text_shadow", "see_through", "use_default_background", "default_background" -> {
                if (entity is Display.TextDisplay && value.isboolean()) {
                    val accessor = entity as TextDisplayAccessor
                    val bit = when (field) {
                        "see_through" -> Display.TextDisplay.FLAG_SEE_THROUGH.toInt()
                        "use_default_background", "default_background" -> Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND.toInt()
                        else -> Display.TextDisplay.FLAG_SHADOW.toInt()
                    }
                    var flags = accessor.nsGetFlags().toInt()
                    flags = if (value.toboolean()) flags or bit else flags and bit.inv()
                    accessor.nsSetFlags(flags.toByte())
                }
            }
            "text_align", "text_alignment" -> {
                if (entity is Display.TextDisplay && value.isstring()) {
                    val accessor = entity as TextDisplayAccessor
                    val align = value.tojstring().lowercase()
                    val leftBit = Display.TextDisplay.FLAG_ALIGN_LEFT.toInt()
                    val rightBit = Display.TextDisplay.FLAG_ALIGN_RIGHT.toInt()
                    var flags = accessor.nsGetFlags().toInt() and (leftBit or rightBit).inv()
                    flags = when (align) {
                        "left" -> flags or leftBit
                        "right" -> flags or rightBit
                        else -> flags // center: оба бита сняты
                    }
                    accessor.nsSetFlags(flags.toByte())
                }
            }

            // --- Специфичные для ItemDisplay ---
            "display_item", "displayed_item" -> {
                if (entity is Display.ItemDisplay) {
                    toStack(value)?.let { (entity as ItemDisplayAccessor).nsSetItemStack(it) }
                }
            }

            // --- Специфичные для BlockDisplay ---
            "display_block", "displayed_block" -> {
                val state = if (value != null && value.isBlock()) value.toBlock() else null
                if (entity is Display.BlockDisplay && state != null) {
                    (entity as BlockDisplayAccessor).nsSetBlockState(state)
                }
            }

            // --- Специфичные для Interaction ---
            "interaction_width" -> {
                if (entity is Interaction && value.isnumber()) {
                    (entity as InteractionAccessor).nsSetWidth(value.todouble().toFloat().coerceAtLeast(0f))
                }
            }
            "interaction_height" -> {
                if (entity is Interaction && value.isnumber()) {
                    (entity as InteractionAccessor).nsSetHeight(value.todouble().toFloat().coerceAtLeast(0f))
                }
            }
            "response" -> {
                if (entity is Interaction && value.isboolean()) {
                    (entity as InteractionAccessor).nsSetResponse(value.toboolean())
                }
            }
        }
    }

    override fun typename(): String = "entity"
}