package com.nekiplay.neoscripts.server.features.lua.objects

import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.client.features.lua.customArgs.FourArgFunction
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.neoscripts.client.sugar.isBlock
import com.nekiplay.neoscripts.client.sugar.isBlockPos
import com.nekiplay.neoscripts.client.sugar.isBox
import com.nekiplay.neoscripts.client.sugar.isEntity
import com.nekiplay.neoscripts.client.sugar.isEntityType
import com.nekiplay.neoscripts.client.sugar.isVector
import com.nekiplay.neoscripts.client.sugar.toBlock
import com.nekiplay.neoscripts.client.sugar.toBlockPos
import com.nekiplay.neoscripts.client.sugar.toBox
import com.nekiplay.neoscripts.client.sugar.toEntity
import com.nekiplay.neoscripts.client.sugar.toEntityType
import com.nekiplay.neoscripts.client.sugar.toVector
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaMutableBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaRaycast
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.nekiplay.neoscripts.common.features.lua.objects.misc.LuaEntityType
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JavaInstance
import net.minecraft.world.entity.Relative
import java.util.EnumSet
import java.util.UUID

class ServerWorldObject(val level: ServerLevel?) : LuaUserdata(level) {
    override fun call(): LuaValue {
        return this
    }

    companion object {
        // --- Статические хелперы парсинга ---

        // Precomputed dynamic keys
        private val javaClassKey = valueOf("javaClass")
        private val classKey = valueOf("class")

        private fun parseBlockPos(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): BlockPos? {
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {
                return BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
            }
            return when {
                arg1 is LuaMutableBlockPos -> arg1.pos
                arg1 != null && arg1.isBlockPos() -> arg1.toBlockPos()
                arg1?.istable() == true -> {
                    val x = arg1.get("x").toint()
                    val y = arg1.get("y").toint()
                    val z = arg1.get("z").toint()
                    BlockPos(x, y, z)
                }
                else -> null
            }
        }

        private fun parseVec3(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): Pair<Vec3?, Int> {
            return when {
                arg1 != null && arg1.isVector() -> Pair(arg1.toVector(), 1)
                arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true -> {
                    Pair(Vec3(arg1.todouble(), arg2.todouble(), arg3.todouble()), 3)
                }
                arg1?.istable() == true -> {
                    val xVal = arg1.get("x")
                    val yVal = arg1.get("y")
                    val zVal = arg1.get("z")
                    if (xVal.isnumber() && yVal.isnumber() && zVal.isnumber()) {
                        Pair(Vec3(xVal.todouble(), yVal.todouble(), zVal.todouble()), 1)
                    } else {
                        Pair(null, 0)
                    }
                }
                else -> Pair(null, 0)
            }
        }

        private fun parseBlockPosWithBlockState(
            arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?, arg4: LuaValue?
        ): Pair<BlockPos, BlockState>? {
            return when {
                arg1 != null && arg1.isBlockPos() -> {
                    val pos = arg1.toBlockPos()
                    val state = parseBlockState(arg2)
                    if (pos != null && state != null) pos to state else null
                }
                arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true -> {
                    val pos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                    val state = parseBlockState(arg4)
                    if (state != null) pos to state else null
                }
                else -> null
            }
        }

        private fun parseBlockState(arg: LuaValue?): BlockState? {
            return if (arg != null && arg.isBlock()) arg.toBlock() else null
        }

        /**
         * Разрешает аргумент в EntityType: LuaEntityType, сырой userdata EntityType
         * или строковый идентификатор ("minecraft:pig").
         */
        private fun resolveEntityType(arg: LuaValue?): EntityType<*>? {
            return when {
                arg?.isstring() == true -> try {
                    val holder = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(arg.tojstring()))
                    if (holder.isPresent) holder.get().value() else null
                } catch (e: Exception) {
                    null
                }
                else -> arg?.toEntityType()
            }
        }

        /**
         * Разрешает аргумент в ServerPlayer: LuaEntity (обёртка над игроком),
         * числовой id сущности или строка (ник / UUID).
         */
        private fun resolveServerPlayer(lvl: ServerLevel, arg: LuaValue?): ServerPlayer? {
            return when {
                arg != null && arg.isEntity() -> arg.toEntity() as? ServerPlayer
                arg?.isnumber() == true -> lvl.getEntity(arg.toint()) as? ServerPlayer
                arg?.isstring() == true -> {
                    val server = lvl.server ?: return null
                    val query = arg.tojstring()
                    runCatching { server.playerList.getPlayer(UUID.fromString(query)) }.getOrNull()
                        ?: server.playerList.getPlayer(query)
                        ?: server.playerList.players.firstOrNull { it.gameProfile.name.equals(query, ignoreCase = true) }
                }
                else -> null
            }
        }

        // --- Серверные хелперы трассировки ---

        /**
         * Серверный аналог клиентского RaycastUtils.findCrosshairTarget для сущностей:
         * ищет ближайшее пересечение луча с хитбоксами сущностей уровня.
         */
        private fun findEntityHit(
            level: ServerLevel,
            startVec: Vec3,
            endVec: Vec3,
            maxDistanceSqr: Double
        ): EntityHitResult? {
            val searchBox = AABB(startVec, endVec).inflate(1.0)
            val candidates = level.getEntitiesOfClass(Entity::class.java, searchBox) { entity ->
                !entity.isSpectator && entity.isPickable
            }

            var closest: EntityHitResult? = null
            var closestDistSqr = maxDistanceSqr
            for (entity in candidates) {
                val hitPos = entity.boundingBox.clip(startVec, endVec).orElse(null) ?: continue
                val distSqr = hitPos.distanceToSqr(startVec)
                if (distSqr <= closestDistSqr) {
                    closestDistSqr = distSqr
                    closest = EntityHitResult(entity, hitPos)
                }
            }
            return closest
        }

        /**
         * Серверный аналог клиентского RaycastUtils.rayTraceToBlocks:
         * DDA-обход блоков с проверкой collision shape.
         */
        private fun rayTraceToBlocks(
            level: ServerLevel,
            startVec: Vec3,
            endVec: Vec3,
            targetBlocks: List<Block>
        ): BlockHitResult? {
            val currPos = BlockPos.MutableBlockPos(Mth.floor(startVec.x), Mth.floor(startVec.y), Mth.floor(startVec.z))
            val endX = Mth.floor(endVec.x)
            val endY = Mth.floor(endVec.y)
            val endZ = Mth.floor(endVec.z)

            fun checkCurrent(): BlockHitResult? {
                val state = level.getBlockState(currPos)
                val shouldCheck =
                    (targetBlocks.isEmpty() && state.isRedstoneConductor(level, currPos)) ||
                            targetBlocks.contains(state.block)
                if (!shouldCheck) return null
                return state.getCollisionShape(level, currPos).clip(startVec, endVec, currPos)
            }

            // Проверка стартового блока
            checkCurrent()?.let { return it }

            var x = currPos.x
            var y = currPos.y
            var z = currPos.z

            val dx = endVec.x - startVec.x
            val dy = endVec.y - startVec.y
            val dz = endVec.z - startVec.z

            val stepX = when { dx > 0 -> 1; dx < 0 -> -1; else -> 0 }
            val stepY = when { dy > 0 -> 1; dy < 0 -> -1; else -> 0 }
            val stepZ = when { dz > 0 -> 1; dz < 0 -> -1; else -> 0 }

            val tDeltaX = if (dx != 0.0) Math.abs(1.0 / dx) else Double.MAX_VALUE
            val tDeltaY = if (dy != 0.0) Math.abs(1.0 / dy) else Double.MAX_VALUE
            val tDeltaZ = if (dz != 0.0) Math.abs(1.0 / dz) else Double.MAX_VALUE

            var tMaxX = when {
                dx > 0 -> (x + 1.0 - startVec.x) / dx
                dx < 0 -> (x - startVec.x) / dx
                else -> Double.MAX_VALUE
            }
            var tMaxY = when {
                dy > 0 -> (y + 1.0 - startVec.y) / dy
                dy < 0 -> (y - startVec.y) / dy
                else -> Double.MAX_VALUE
            }
            var tMaxZ = when {
                dz > 0 -> (z + 1.0 - startVec.z) / dz
                dz < 0 -> (z - startVec.z) / dz
                else -> Double.MAX_VALUE
            }

            var steps = 200
            while (steps-- > 0) {
                if (x == endX && y == endY && z == endZ) return null

                if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                    if (stepX == 0 || tMaxX > 1.0) return null
                    x += stepX
                    tMaxX += tDeltaX
                } else if (tMaxY <= tMaxZ) {
                    if (stepY == 0 || tMaxY > 1.0) return null
                    y += stepY
                    tMaxY += tDeltaY
                } else {
                    if (stepZ == 0 || tMaxZ > 1.0) return null
                    z += stepZ
                    tMaxZ += tDeltaZ
                }

                currPos.set(x, y, z)
                checkCurrent()?.let { return it }
            }

            return null
        }
    }

    override fun get(key: LuaValue): LuaValue {
        return when {
            key == javaClassKey || key == classKey -> JavaInstance(level)
            key.type() == TSTRING -> functions[key] ?: NIL
            else -> NIL
        }
    }

    private val functions: Map<LuaValue, LuaValue> by lazy {
        buildMap {
            put(valueOf("getBlock"), GetBlockFunction())
            put(valueOf("getBlockState"), GetBlockFunction())
            put(valueOf("getBlockEntity"), GetBlockEntityFunction())
            put(valueOf("getLight"), GetLightFunction())
            put(valueOf("getBrightness"), GetLightFunction())
            put(valueOf("getLightSky"), GetLightSkyFunction())
            put(valueOf("getBrightnessSky"), GetLightSkyFunction())
            put(valueOf("getDimension"), GetDimension())
            put(valueOf("setBlock"), SetBlockFunction())
            put(valueOf("isBlockLoaded"), IsBlockLoadedFunction())
            put(valueOf("getEntities"), GetEntitiesFunction())
            put(valueOf("getLivingEntities"), GetLivingEntitiesFunction())
            put(valueOf("getArmorStandEntities"), GetArmorStandEntitiesFunction())
            put(valueOf("getEntitiesInBox"), GetEntitiesInBoxFunction())
            put(valueOf("getArmorStandEntitiesInBox"), GetArmorStandEntitiesInBoxFunction())
            put(valueOf("getEntityById"), GetEntityByIdFunction())
            put(valueOf("spawnEntity"), SpawnEntityFunction())
            put(valueOf("spawn"), SpawnEntityFunction())
            put(valueOf("removeEntity"), RemoveEntityFunction())
            put(valueOf("despawnEntity"), RemoveEntityFunction())
            put(valueOf("despawn"), RemoveEntityFunction())
            put(valueOf("getCollisionBoxes"), GetCollisionBoxesFunction())
            put(valueOf("getOutlineBoxes"), GetOutlineBoxesFunction())
            put(valueOf("getBlocksInBox"), GetBlocksInBoxFunction())
            put(valueOf("getBlocksFromList"), GetBlocksFromListFunction())
            put(valueOf("raycast"), RaycastFunction())
            put(valueOf("raycastFromRotation"), RaycastFromRotationFunction())
            put(valueOf("raycastToBlocksFromId"), RaycastToBlocksFunction())
            put(valueOf("raycastToBlocksFromIdentifier"), RaycastToBlocksFromIdentifierFunction())
            put(valueOf("playSound"), PlaySoundFunction())
            put(valueOf("executeCommand"), ExecuteCommandFunction())
            put(valueOf("runCommand"), ExecuteCommandFunction())
            put(valueOf("spawnEntityFor"), SpawnPrivateEntityFunction())
            put(valueOf("spawnPrivateEntity"), SpawnPrivateEntityFunction())
            put(valueOf("removeEntityFor"), RemovePrivateEntityFunction())
            put(valueOf("removePrivateEntity"), RemovePrivateEntityFunction())
            put(valueOf("updateEntityFor"), UpdatePrivateEntityFunction())
            put(valueOf("updatePrivateEntity"), UpdatePrivateEntityFunction())
            put(valueOf("setBlockFor"), SetBlockForPlayerFunction())
            put(valueOf("setPrivateBlock"), SetBlockForPlayerFunction())
            put(valueOf("resetBlockFor"), ResetBlockForPlayerFunction())
            put(valueOf("resetPrivateBlock"), ResetBlockForPlayerFunction())
        }
    }

    private inner class GetBlockFunction : ThreeArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?
        ): LuaValue? {
            val blockPos = parseBlockPos(arg1, arg2, arg3)
            if (blockPos != null) {
                val state = level?.getBlockState(blockPos)
                if (state != null) {
                    return LuaBlockState(state);
                }
            }
            return NIL
        }
    }

    private inner class GetBlockEntityFunction : ThreeArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?
        ): LuaValue? {
            val blockPos = parseBlockPos(arg1, arg2, arg3)
            if (blockPos != null) {
                val blockEntity = level?.getBlockEntity(blockPos)
                if (blockEntity != null) {
                    return LuaBlockEntity(blockEntity);
                }
            }
            return NIL
        }
    }

    private inner class GetLightFunction : ThreeArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?
        ): LuaValue? {
            val blockPos = parseBlockPos(arg1, arg2, arg3)
            if (blockPos != null) {
                val lighth = level?.getBrightness(LightLayer.BLOCK, blockPos)
                if (lighth != null) {
                    return valueOf(lighth)
                }
            }
            return NIL
        }
    }

    private inner class GetLightSkyFunction : ThreeArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?
        ): LuaValue? {
            val blockPos = parseBlockPos(arg1, arg2, arg3)
            if (blockPos != null) {
                val lighth = level?.getBrightness(LightLayer.SKY, blockPos)
                if (lighth != null) {
                    return valueOf(lighth)
                }
            }
            return NIL
        }
    }

    private inner class IsBlockLoadedFunction : ThreeArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?
        ): LuaValue? {
            val blockPos = parseBlockPos(arg1, arg2, arg3)
            if (blockPos != null) {
                return valueOf(level?.isLoaded(blockPos) ?: false);
            }
            return NIL
        }
    }

    private inner class GetDimension : ZeroArgFunction() {
        override fun call(): LuaValue {
            val lvl = level ?: return NIL

            // Получаем ключ измерения (ResourceKey<Level>)
            val dimensionKey = lvl.dimension();

            // Получаем уникальный текстовый идентификатор (ResourceLocation), например, "minecraft:overworld"
            val dimensionLocation = dimensionKey.identifier();
            val dimensionString = dimensionLocation.toString();
            return valueOf(dimensionString)
        }
    }

    private inner class SetBlockFunction : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            val lvl = level ?: return NIL

            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4 != null && arg4.isBlock()) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val blockState = arg4.toBlock() ?: return NIL

                lvl.setBlockAndUpdate(blockPos, blockState)
                lvl.updateNeighborsAt(blockPos, blockState.block)
                return TRUE
            }
            else if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isnumber() == true) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val blockId = arg4.toint()
                val blockState = Block.stateById(blockId)

                lvl.setBlockAndUpdate(blockPos, blockState)
                lvl.updateNeighborsAt(blockPos, blockState.block)
                return TRUE
            }
            else if (arg1?.istable() ?: false) {
                val x: Int = if (arg1.get("x").isnumber()) arg1.get("x").toint() else 0
                val y: Int = if (arg1.get("y").isnumber()) arg1.get("y").toint() else 0
                val z: Int = if (arg1.get("z").isnumber()) arg1.get("z").toint() else 0
                val id: Int = if (arg1.get("id").isnumber()) arg1.get("id").toint() else 0

                val blockPos = BlockPos(x, y, z)
                val blockState = Block.stateById(id)

                lvl.setBlockAndUpdate(blockPos, blockState)
                lvl.updateNeighborsAt(blockPos, blockState.block)
                return TRUE
            }
            else if (arg1 != null && arg1.isBlockPos() && arg2 != null && arg2.isBlock()) {
                val pos = arg1.toBlockPos() ?: return NIL
                val state = arg2.toBlock() ?: return NIL
                lvl.setBlockAndUpdate(pos, state)
                lvl.updateNeighborsAt(pos, state.block)
                return TRUE
            }
            else if (arg1 != null && arg1.isBlockPos()) {
                val pos = arg1.toBlockPos() ?: return NIL
                val blockId = arg2?.optint(1) ?: 1
                val blockState = Block.stateById(blockId)
                lvl.setBlockAndUpdate(pos, blockState)
                lvl.updateNeighborsAt(pos, blockState.block)
                return TRUE
            }
            return NIL
        }
    }

    private inner class GetEntitiesFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val entitiesTable = tableOf()

            var index = 1
            level?.allEntities?.forEach { entity ->
                entitiesTable.set(index++, LuaEntity(entity))
            }

            return entitiesTable
        }
    }

    private inner class GetLivingEntitiesFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val entitiesTable = tableOf()

            var index = 1
            level?.allEntities?.forEach { entity ->
                if (entity is LivingEntity) {
                    entitiesTable.set(index++, LuaEntity(entity))
                }
            }

            return entitiesTable
        }
    }

    private inner class GetArmorStandEntitiesFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val entitiesTable = tableOf()

            var index = 1
            level?.allEntities?.forEach { entity ->
                if (entity is ArmorStand) {
                    entitiesTable.set(index++, LuaEntity(entity))
                }
            }

            return entitiesTable
        }
    }

    private inner class GetEntitiesInBoxFunction() : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val box = if (arg.isBox()) arg.toBox() else null
            val entitiesTable = tableOf()
            if (box != null) {
                var index = 1
                level?.getEntitiesOfClass(Entity::class.java, box)?.forEach { entity ->
                    entitiesTable.set(index++, LuaEntity(entity))
                }
            }
            return entitiesTable
        }
    }

    private inner class GetArmorStandEntitiesInBoxFunction() : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val box = if (arg.isBox()) arg.toBox() else null
            val entitiesTable = tableOf()
            if (box != null) {
                var index = 1
                level?.getEntitiesOfClass(ArmorStand::class.java, box)?.forEach { entity ->
                    entitiesTable.set(index++, LuaEntity(entity))
                }
            }
            return entitiesTable
        }
    }

    private inner class GetEntityByIdFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            if (arg?.isnumber() == true) {
                val entityId = arg.toint()
                val entity = level?.getEntity(entityId)
                if (entity != null) {
                    return LuaEntity(entity)
                }
            }
            return NIL
        }
    }

    /**
     * world.spawnEntity(type, x, y, z [, yaw, pitch])
     * type: строка "minecraft:sheep", creator.createEntity(...) (готовый LuaEntity —
     * будет заспавнен именно этот инстанс), creator.createEntityType(...) или EntityType.
     * Возвращает заспавненную LuaEntity или NIL.
     */
    private inner class SpawnEntityFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val lvl = level ?: return NIL

            // Готовый инстанс сущности (LuaEntity или сырой Entity) — спавним его же
            val preCreated = if (args.arg(1).isEntity()) args.arg(1).toEntity() else null

            val entity = preCreated ?: run {
                val type = resolveEntityType(args.arg(1))
                    ?: return error("Unknown entity type: expected identifier string, entitytype userdata or EntityType")
                type.create(lvl, EntitySpawnReason.COMMAND)
            } ?: return NIL

            val (posVec, offset) = parseVec3(args.arg(2), args.arg(3), args.arg(4))
                .let { if (it.second > 0) it else Pair<Vec3?, Int>(null, 0) }
            if (posVec == null) {
                return error("Invalid position: expected x, y, z numbers or vector")
            }

            val nextArgIndex = 2 + offset
            val yaw = args.arg(nextArgIndex)?.optdouble(0.0) ?: 0.0
            val pitch = args.arg(nextArgIndex + 1)?.optdouble(0.0) ?: 0.0

            entity.snapTo(posVec.x, posVec.y, posVec.z, yaw.toFloat(), pitch.toFloat())
            lvl.addFreshEntity(entity)

            return LuaEntity(entity)
        }
    }

    /**
     * world.removeEntity(entity|entityId) — удаляет сущность с сервера (без дропа лута).
     * Принимает LuaEntity, сырой Entity userdata или числовой ID сущности.
     */
    private inner class RemoveEntityFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val lvl = level ?: return NIL
            val entity = when {
                arg?.isnumber() == true -> lvl.getEntity(arg.toint())
                arg != null && arg.isEntity() -> arg.toEntity()
                else -> null
            } ?: return FALSE
            if (!entity.isAlive) return FALSE
            entity.discard()
            return TRUE
        }
    }

    private inner class GetOutlineBoxesFunction : FourArgFunction() {
        override fun invoke(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?, arg4: LuaValue?): LuaValue {
            val lvl = level ?: return error("No world loaded")

            val (blockPos, blockState) = parseBlockPosWithBlockState(arg1, arg2, arg3, arg4)
                ?: return error("Invalid arguments: expected BlockPos + BlockState or x, y, z + BlockState")

            val shape = try {
                blockState.getShape(lvl, blockPos)
            } catch (e: Exception) {
                return error("Error getting shape: ${e.message}")
            }

            if (shape.isEmpty) {
                return tableOf()
            }

            val result = tableOf()
            var index = 1
            shape.toAabbs().forEach { voxel ->
                result.set(index++, LuaBox(voxel))
            }

            return result
        }
    }

    private inner class GetCollisionBoxesFunction : FourArgFunction() {
        override fun invoke(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?, arg4: LuaValue?): LuaValue {
            val lvl = level ?: return error("No world loaded")

            val (blockPos, blockState) = parseBlockPosWithBlockState(arg1, arg2, arg3, arg4)
                ?: return error("Invalid arguments: expected BlockPos + BlockState or x, y, z + BlockState")

            val collisionShape = try {
                blockState.getCollisionShape(lvl, blockPos)
            } catch (e: Exception) {
                return error("Error getting collision shape: ${e.message}")
            }

            // Если shape пустой, возвращаем пустую таблицу
            if (collisionShape.isEmpty) {
                return tableOf()
            }

            // Конвертируем VoxelShape в Lua таблицу с bounding boxes
            val result = tableOf()
            var index = 1
            collisionShape.toAabbs().forEach { voxel ->
                result.set(index++, LuaBox(voxel))
            }

            return result
        }
    }

    private inner class GetBlocksInBoxFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val lvl = level ?: return NIL

            val minPos: BlockPos
            val maxPos: BlockPos

            val arg1 = args.arg(1)

            // Вариант 1: world.getBlocksInBox(luaBox)
            if (arg1.isBox()) {
                val aabb = arg1.toBox() ?: return error("Invalid arguments: expected (LuaBox) or (BlockPos, BlockPos)")
                minPos = BlockPos(
                    Math.floor(aabb.minX).toInt(),
                    Math.floor(aabb.minY).toInt(),
                    Math.floor(aabb.minZ).toInt()
                )
                maxPos = BlockPos(
                    Math.floor(aabb.maxX).toInt(),
                    Math.floor(aabb.maxY).toInt(),
                    Math.floor(aabb.maxZ).toInt()
                )
            }
            // Вариант 2: world.getBlocksInBox(pos1, pos2)
            else {
                val p1 = parseBlockPos(args.arg(1), null, null)
                val p2 = parseBlockPos(args.arg(2), null, null)

                if (p1 == null || p2 == null) {
                    return error("Invalid arguments: expected (LuaBox) or (BlockPos, BlockPos)")
                }

                minPos = BlockPos(minOf(p1.x, p2.x), minOf(p1.y, p2.y), minOf(p1.z, p2.z))
                maxPos = BlockPos(maxOf(p1.x, p2.x), maxOf(p1.y, p2.y), maxOf(p1.z, p2.z))
            }

            val resultTable = tableOf()
            var index = 1

            // Итерируемся по области
            for (pos in BlockPos.betweenClosed(minPos, maxPos)) {
                // Важно: проверяем, загружен ли чанк
                if (lvl.hasChunkAt(pos)) {
                    val state = lvl.getBlockState(pos)

                    val entry = tableOf()
                    entry.set("pos", LuaBlockPos(pos.immutable()))
                    entry.set("state", LuaBlockState(state))

                    resultTable.set(index++, entry)
                }
            }

            return resultTable
        }
    }

    private inner class GetBlocksFromListFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val lvl = level ?: return NIL
            if (arg?.istable() != true) return NIL

            val resultTable = tableOf()
            var index = 1
            val len = arg.length()
            val pos = BlockPos.MutableBlockPos()

            for (i in 1..len) {
                val entry = arg.get(i)
                if (!entry.istable()) continue

                val ex = entry.get(1)
                val ey = entry.get(2)
                val ez = entry.get(3)

                if (ex.isnumber() && ey.isnumber() && ez.isnumber()) {
                    pos.set(ex.toint(), ey.toint(), ez.toint())
                } else {
                    val nx = entry.get("x")
                    val ny = entry.get("y")
                    val nz = entry.get("z")
                    if (!nx.isnumber() || !ny.isnumber() || !nz.isnumber()) continue
                    pos.set(nx.toint(), ny.toint(), nz.toint())
                }

                if (lvl.hasChunkAt(pos)) {
                    val state = lvl.getBlockState(pos)
                    val row = tableOf()
                    row.set("pos", LuaBlockPos(pos.immutable()))
                    row.set("state", LuaBlockState(state))
                    resultTable.set(index++, row)
                }
            }

            return resultTable
        }
    }

    private inner class RaycastFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            if (arg?.istable() != true) return NIL
            val lvl = level ?: return NIL

            val startVec = Vec3(
                arg.get("startX").optdouble(0.0),
                arg.get("startY").optdouble(0.0),
                arg.get("startZ").optdouble(0.0)
            )
            val endVec = Vec3(
                arg.get("endX").optdouble(0.0),
                arg.get("endY").optdouble(0.0),
                arg.get("endZ").optdouble(0.0)
            )

            val includeFluid = arg.get("include_fluid").optboolean(false)
            val includeEntity = arg.get("include_entity").optboolean(false)

            val fluidMode = if (includeFluid) ClipContext.Fluid.ANY else ClipContext.Fluid.NONE
            var hitResult: HitResult = lvl.clip(
                ClipContext(startVec, endVec, ClipContext.Block.OUTLINE, fluidMode, CollisionContext.empty())
            )

            if (includeEntity) {
                val entityHit = findEntityHit(lvl, startVec, endVec, startVec.distanceToSqr(endVec))
                if (entityHit != null &&
                    (hitResult.type == HitResult.Type.MISS ||
                            entityHit.location.distanceToSqr(startVec) <= hitResult.location.distanceToSqr(startVec))
                ) {
                    hitResult = entityHit
                }
            }

            return if (hitResult.type != HitResult.Type.MISS) {
                LuaRaycast(hitResult)
            } else {
                NIL
            }
        }
    }

    private inner class RaycastFromRotationFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            if (arg == null || !arg.istable()) return NIL
            val lvl = level ?: return NIL

            val startVec = Vec3(
                arg.get("startX").optdouble(0.0),
                arg.get("startY").optdouble(0.0),
                arg.get("startZ").optdouble(0.0)
            )
            val yaw = arg.get("yaw").optdouble(0.0).toFloat()
            val pitch = arg.get("pitch").optdouble(0.0).toFloat()
            val range = arg.get("range").optdouble(0.0)

            val includeFluid = arg.get("include_fluid").optboolean(false)
            val includeEntity = arg.get("include_entity").optboolean(false)

            // 1. Вычисляем вектор направления из вращения (Rotation to Vector)
            val f = Math.cos(-yaw * 0.017453292 - Math.PI).toFloat()
            val f1 = Math.sin(-yaw * 0.017453292 - Math.PI).toFloat()
            val f2 = -Math.cos(-pitch * 0.017453292).toFloat()
            val f3 = Math.sin(-pitch * 0.017453292).toFloat()
            val dirVec = Vec3((f1 * f2).toDouble(), f3.toDouble(), (f * f2).toDouble())

            // 2. Вычисляем конечную точку луча
            val endVec = startVec.add(dirVec.scale(range))

            val fluidMode = if (includeFluid) ClipContext.Fluid.ANY else ClipContext.Fluid.NONE
            var hitResult: HitResult = lvl.clip(
                ClipContext(startVec, endVec, ClipContext.Block.OUTLINE, fluidMode, CollisionContext.empty())
            )

            if (includeEntity) {
                val entityHit = findEntityHit(lvl, startVec, endVec, range * range)
                if (entityHit != null &&
                    (hitResult.type == HitResult.Type.MISS ||
                            entityHit.location.distanceToSqr(startVec) <= hitResult.location.distanceToSqr(startVec))
                ) {
                    hitResult = entityHit
                }
            }

            return if (hitResult.type != HitResult.Type.MISS) {
                LuaRaycast(hitResult)
            } else {
                NIL
            }
        }
    }

    private inner class RaycastToBlocksFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            if (arg?.istable() != true) return NIL
            val lvl = level ?: return NIL

            val startVec = Vec3(
                arg.get("startX").optdouble(0.0),
                arg.get("startY").optdouble(0.0),
                arg.get("startZ").optdouble(0.0)
            )
            val endVec = Vec3(
                arg.get("endX").optdouble(0.0),
                arg.get("endY").optdouble(0.0),
                arg.get("endZ").optdouble(0.0)
            )

            // Получаем список блоков для проверки (если указаны)
            val blocksTable = arg.get("blocks")
            val targetBlocks = mutableListOf<Block>()
            if (blocksTable.istable()) {
                val len = blocksTable.length()
                for (i in 1..len) {
                    val value = blocksTable.get(i)
                    if (value.isint()) {
                        val id = value.toint()
                        val state = Block.stateById(id)
                        targetBlocks.add(state.block)
                    }
                }
            }

            val hitResult = rayTraceToBlocks(lvl, startVec, endVec, targetBlocks)
            return if (hitResult != null) {
                LuaRaycast(hitResult)
            } else {
                NIL
            }
        }
    }

    private inner class RaycastToBlocksFromIdentifierFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            if (arg?.istable() != true) return NIL
            val lvl = level ?: return NIL

            val startVec = Vec3(
                arg.get("startX").optdouble(0.0),
                arg.get("startY").optdouble(0.0),
                arg.get("startZ").optdouble(0.0)
            )
            val endVec = Vec3(
                arg.get("endX").optdouble(0.0),
                arg.get("endY").optdouble(0.0),
                arg.get("endZ").optdouble(0.0)
            )

            // Получаем список блоков для проверки (если указаны)
            val blocksTable = arg.get("blocks")
            val targetBlocks = mutableListOf<Block>()
            if (blocksTable.istable()) {
                val len = blocksTable.length()
                for (i in 1..len) {
                    val value = blocksTable.get(i)
                    if (value.isint()) {
                        val id = value.tojstring()
                        val state = BuiltInRegistries.BLOCK.get(Identifier.parse(id))

                        if (state.isPresent) {
                            targetBlocks.add(state.get().value())
                        } else {
                            ClientMain.LOGGER?.warn("No block found for ID: $id")
                        }
                    }
                }
            }

            val hitResult = rayTraceToBlocks(lvl, startVec, endVec, targetBlocks)
            return if (hitResult != null) {
                LuaRaycast(hitResult)
            } else {
                NIL
            }
        }
    }

    /**
     * world.executeCommand(command [, x, y, z]) — выполняет серверную команду от имени
     * виртуального источника с полными правами (PermissionSet.ALL_PERMISSIONS).
     *
     * Возвращает несколько значений:
     *   1) результат команды:
     *        - LuaEntity — команда заспавнила ровно одну сущность (например, /summon);
     *        - таблица LuaEntity — заспавнено несколько сущностей;
     *        - TRUE — новых сущностей нет;
     *        - FALSE при ошибке выполнения;
     *   2) числовой результат команды (int) или NIL;
     *   3) сообщение об ошибке (string) или NIL.
     *
     * Необязательная позиция задаёт точку для относительных координат (~) в команде.
     */
    private inner class ExecuteCommandFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val lvl = level ?: return NIL
            val server = lvl.server ?: return NIL

            if (args.narg() < 1 || !args.arg1().isstring()) {
                return error("Invalid arguments: expected command string")
            }

            // Brigadier ожидает команду без ведущего слэша
            val command = args.arg1().tojstring().removePrefix("/")

            val parsedPos = parseVec3(args.arg(2), args.arg(3), args.arg(4))
            val position = if (parsedPos.second > 0) parsedPos.first ?: Vec3.ZERO else Vec3.ZERO

            // Снимок сущностей до выполнения — по разнице находим заспавненные командой
            val entitiesBefore = lvl.allEntities.toHashSet()

            val source = CommandSourceStack(
                CommandSource.NULL,
                position,
                Vec2.ZERO,
                lvl,
                PermissionSet.ALL_PERMISSIONS,
                "NeoScripts",
                Component.literal("NeoScripts"),
                server,
                null
            ).withSuppressedOutput()

            val dispatcher = server.commands.dispatcher
            return try {
                val parseResults = dispatcher.parse(command, source)
                val result = dispatcher.execute(parseResults)

                // Разница снимков: новые сущности
                val spawned = lvl.allEntities.filter { !entitiesBefore.contains(it) }

                when {
                    spawned.size == 1 ->
                        LuaValue.varargsOf(LuaEntity(spawned[0]), valueOf(result))

                    spawned.isNotEmpty() -> {
                        val entitiesTable = tableOf()
                        spawned.forEachIndexed { index, entity ->
                            entitiesTable.set(index + 1, LuaEntity(entity))
                        }
                        LuaValue.varargsOf(entitiesTable, valueOf(result))
                    }

                    else ->
                        LuaValue.varargsOf(TRUE, valueOf(result))
                }
            } catch (e: CommandSyntaxException) {
                LuaValue.varargsOf(FALSE, NIL, valueOf(e.message ?: "Unknown command error"))
            }
        }
    }

    /**
     * world.spawnEntityFor(player, type, x, y, z [, yaw, pitch]) — создаёт сущность,
     * видимую ТОЛЬКО переданному игроку. Сущность не добавляется в мир: она не тикает
     * и не видна другим игрокам. Возвращает LuaEntity (для последующего removeEntityFor).
     */
    private inner class SpawnPrivateEntityFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val lvl = level ?: return NIL

            val player = resolveServerPlayer(lvl, args.arg(1))
                ?: return error("Invalid arguments: expected player (entity, id or name)")
            val entity = run {
                val type = resolveEntityType(args.arg(2))
                    ?: return error("Unknown entity type: expected identifier string, entitytype userdata or EntityType")
                type.create(lvl, EntitySpawnReason.COMMAND)
            } ?: return NIL

            val (posVec, offset) = parseVec3(args.arg(3), args.arg(4), args.arg(5))
                .let { if (it.second > 0) it else Pair<Vec3?, Int>(null, 0) }
            if (posVec == null) {
                return error("Invalid position: expected x, y, z numbers or vector")
            }

            val nextArgIndex = 3 + offset
            val yaw = args.arg(nextArgIndex)?.optdouble(0.0) ?: 0.0
            val pitch = args.arg(nextArgIndex + 1)?.optdouble(0.0) ?: 0.0

            entity.snapTo(posVec.x, posVec.y, posVec.z, yaw.toFloat(), pitch.toFloat())
            entity.setId(lvl.nextEntityId)

            // Пакет спавна отправляем только целевому игроку; сущность НЕ добавляется в мир
            player.connection.send(ClientboundAddEntityPacket(entity, 0, BlockPos.containing(entity.position())))

            // Метаданные (имя, невидимость и т.п.), если отличаются от дефолта
            val dataValues = entity.entityData.nonDefaultValues
            if (!dataValues.isNullOrEmpty()) {
                player.connection.send(ClientboundSetEntityDataPacket(entity.id, dataValues))
            }

            return LuaEntity(entity)
        }
    }

    /**
     * world.removeEntityFor(player, entity|entityId) — убирает приватную сущность
     * у указанного игрока (пакет удаления).
     */
    private inner class RemovePrivateEntityFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            val lvl = level ?: return NIL
            val player = resolveServerPlayer(lvl, arg1) ?: return FALSE
            val entity = when {
                arg2?.isnumber() == true -> lvl.getEntity(arg2.toint())
                arg2 != null && arg2.isEntity() -> arg2.toEntity()
                else -> null
            } ?: return FALSE

            player.connection.send(ClientboundRemoveEntitiesPacket(entity.id))
            return TRUE
        }
    }

    /**
     * world.setBlockFor(player, x, y, z, blockState | pos, blockState) — показывает игроку
     * блок-иллюзию: реальный блок мира не меняется, пакет обновления уходит только ему.
     */
    private inner class SetBlockForPlayerFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val lvl = level ?: return NIL

            val player = resolveServerPlayer(lvl, args.arg(1))
                ?: return error("Invalid arguments: expected player (entity, id or name)")

            val (blockPos, blockState) = parseBlockPosWithBlockState(args.arg(2), args.arg(3), args.arg(4), args.arg(5))
                ?: return error("Invalid arguments: expected (x, y, z, BlockState) or (BlockPos, BlockState)")

            player.connection.send(ClientboundBlockUpdatePacket(blockPos.immutable(), blockState))
            return TRUE
        }
    }

    /**
     * world.resetBlockFor(player, x, y, z | pos) — восстанавливает реальное состояние блока
     * у игрока после setBlockFor.
     */
    private inner class ResetBlockForPlayerFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val lvl = level ?: return NIL

            val player = resolveServerPlayer(lvl, args.arg(1))
                ?: return error("Invalid arguments: expected player (entity, id or name)")

            val blockPos = parseBlockPos(args.arg(2), args.arg(3), args.arg(4))
                ?: return error("Invalid arguments: expected (x, y, z) or BlockPos")

            player.connection.send(ClientboundBlockUpdatePacket(lvl, blockPos))
            return TRUE
        }
    }

    /**
     * world.updateEntityFor(player, entity) — синхронизирует приватную сущность с клиентом
     * игрока: позиция/поворот, поворот головы, метаданные, броня и активные эффекты.
     * Вызывайте после изменения x/y/z/yaw/pitch/health/custom_name/эффектов и т.п.
     *
     * Приватные сущности не зарегистрированы в мире (поиск по id невозможен),
     * поэтому entity принимается только объектом (LuaEntity / Entity).
     */
    private inner class UpdatePrivateEntityFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            val lvl = level ?: return NIL
            val player = resolveServerPlayer(lvl, arg1) ?: return FALSE
            val entity = when {
                arg2 != null && arg2.isEntity() -> arg2.toEntity()
                else -> null
            } ?: return FALSE

            // Позиция и поворот
            player.connection.send(
                ClientboundTeleportEntityPacket(
                    entity.id,
                    PositionMoveRotation(entity.position(), entity.deltaMovement, entity.yRot, entity.xRot),
                    EnumSet.noneOf(Relative::class.java),
                    entity.onGround()
                )
            )
            player.connection.send(
                ClientboundRotateHeadPacket(entity, (entity.yHeadRot * 256f / 360f).toInt().toByte())
            )

            // Метаданные (флаги, кастомное имя, невидимость и т.п.)
            val dataValues = entity.entityData.nonDefaultValues
            if (!dataValues.isNullOrEmpty()) {
                player.connection.send(ClientboundSetEntityDataPacket(entity.id, dataValues))
            }

            if (entity is LivingEntity) {
                // Броня и предметы в руках
                val equipment = mutableListOf<Pair<EquipmentSlot, ItemStack>>()
                for (slot in EquipmentSlot.entries) {
                    val stack = entity.getItemBySlot(slot)
                    if (!stack.isEmpty) {
                        equipment.add(Pair(slot, stack))
                    }
                }
                if (equipment.isNotEmpty()) {
                    player.connection.send(ClientboundSetEquipmentPacket(entity.id, equipment))
                }

                // Эффекты: снимаем все известные у клиента и накладываем актуальные.
                // Пакет удаления несуществующего эффекта безвреден.
                val active = entity.activeEffectsMap.keys.toHashSet()
                for (holder in BuiltInRegistries.MOB_EFFECT) {
                    if (holder !in active) {
                        player.connection.send(ClientboundRemoveMobEffectPacket(entity.id, holder))
                    }
                }
                for ((holder, instance) in entity.activeEffectsMap) {
                    player.connection.send(ClientboundUpdateMobEffectPacket(entity.id, instance, false))
                }
            }

            return TRUE
        }
    }

    private inner class PlaySoundFunction : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val lvl = level ?: return NIL

            // Получаем первые 3 аргумента для попытки парсинга вектора
            val a1 = args?.arg(1)
            val a2 = args?.arg(2)
            val a3 = args?.arg(3)

            val (position, offset) = parseVec3(a1, a2, a3)

            if (position == null) {
                return NIL
            }

            val nextArgIndex = 1 + offset

            val soundId = args?.arg(nextArgIndex)?.tojstring() ?: return NIL

            val volume = args.arg(nextArgIndex + 1)?.optdouble(1.0) ?: 1.0
            val pitch = args.arg(nextArgIndex + 2)?.optdouble(1.0) ?: 1.0

            val finalVolume = (volume / 100.0).toFloat().coerceIn(0f, 1f)

            val soundEvent = try {
                val resourceLocation = Identifier.parse(soundId)
                SoundEvent.createVariableRangeEvent(resourceLocation)
            } catch (e: Exception) {
                return NIL
            }

            // null вместо игрока-исключения: звук услышат все игроки рядом
            lvl.playSound(
                null,
                position.x,
                position.y,
                position.z,
                soundEvent,
                SoundSource.MASTER,
                finalVolume,
                pitch.toFloat()
            )

            return TRUE
        }
    }

    override fun typename(): String = "world"
    override fun tojstring(): String = "WorldObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}
