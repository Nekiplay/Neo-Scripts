package com.nekiplay.neoscripts.server.features.lua.objects

import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.client.features.lua.customArgs.FourArgFunction
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaMutableBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaRaycast
import com.nekiplay.neoscripts.common.features.lua.objects.misc.LuaEntityType
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JavaInstance

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
                arg1 is LuaBlockPos -> arg1.pos
                arg1?.isuserdata() == true && arg1.touserdata() is BlockPos.MutableBlockPos ->
                    arg1.touserdata() as BlockPos.MutableBlockPos
                arg1?.isuserdata() == true && arg1.touserdata() is BlockPos ->
                    arg1.touserdata() as BlockPos
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
                arg1?.isuserdata() == true && arg1.touserdata() is LuaVector3d -> {
                    Pair((arg1.touserdata() as LuaVector3d).location, 1)
                }
                arg1?.isuserdata() == true && arg1.touserdata() is Vec3 -> {
                    Pair(arg1.touserdata() as Vec3, 1)
                }
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
                arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos -> {
                    val pos = (arg1.touserdata() as LuaBlockPos).pos
                    val state = parseBlockState(arg2)
                    if (state != null) pos to state else null
                }
                arg1?.isuserdata() == true && arg1.touserdata() is BlockPos.MutableBlockPos -> {
                    val pos = arg1.touserdata() as BlockPos.MutableBlockPos
                    val state = parseBlockState(arg2)
                    if (state != null) pos to state else null
                }
                arg1?.isuserdata() == true && arg1.touserdata() is BlockPos -> {
                    val pos = arg1.touserdata() as BlockPos
                    val state = parseBlockState(arg2)
                    if (state != null) pos to state else null
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
            return when {
                arg?.isuserdata(LuaBlockState::class.java) == true -> {
                    (arg.touserdata() as? LuaBlockState)?.blockState
                }
                arg?.isuserdata(BlockState::class.java) == true -> {
                    arg.touserdata() as? BlockState
                }
                else -> null
            }
        }

        /**
         * Разрешает аргумент в EntityType: LuaEntityType, сырой userdata EntityType
         * или строковый идентификатор ("minecraft:pig").
         */
        private fun resolveEntityType(arg: LuaValue?): EntityType<*>? {
            return when {
                arg is LuaEntityType -> arg.entityType
                arg?.isuserdata() == true && arg.touserdata() is EntityType<*> -> arg.touserdata() as EntityType<*>
                arg?.isstring() == true -> try {
                    val holder = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(arg.tojstring()))
                    if (holder.isPresent) holder.get().value() else null
                } catch (e: Exception) {
                    null
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
            put(valueOf("getCollisionBoxes"), GetCollisionBoxesFunction())
            put(valueOf("getOutlineBoxes"), GetOutlineBoxesFunction())
            put(valueOf("getBlocksInBox"), GetBlocksInBoxFunction())
            put(valueOf("getBlocksFromList"), GetBlocksFromListFunction())
            put(valueOf("raycast"), RaycastFunction())
            put(valueOf("raycastFromRotation"), RaycastFromRotationFunction())
            put(valueOf("raycastToBlocksFromId"), RaycastToBlocksFunction())
            put(valueOf("raycastToBlocksFromIdentifier"), RaycastToBlocksFromIdentifierFunction())
            put(valueOf("playSound"), PlaySoundFunction())
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

            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isuserdata() == true && arg4.touserdata() is LuaBlockState) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val blockState = arg4.touserdata() as LuaBlockState

                lvl.setBlockAndUpdate(blockPos, blockState.blockState)
                lvl.updateNeighborsAt(blockPos, blockState.blockState.block)
                return TRUE
            }
            else if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isuserdata() == true && arg4.touserdata() is BlockState) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val blockState = arg4.touserdata() as BlockState

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
            else if (arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos && arg2?.isuserdata() == true && arg2.touserdata() is LuaBlockState) {
                val pos = arg1.touserdata() as LuaBlockPos
                val state = arg2.touserdata() as LuaBlockState
                lvl.setBlockAndUpdate(pos.pos, state.blockState)
                lvl.updateNeighborsAt(pos.pos, state.blockState.block)
                return TRUE
            }
            else if (arg1?.isuserdata() == true && arg1.touserdata() is BlockPos && arg2?.isuserdata() == true && arg2.touserdata() is BlockState) {
                val pos = arg1.touserdata() as BlockPos
                val state = arg2.touserdata() as BlockState
                lvl.setBlockAndUpdate(pos, state)
                lvl.updateNeighborsAt(pos, state.block)
                return TRUE
            }
            else if (arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos) {
                val pos = arg1.touserdata() as LuaBlockPos
                val blockId = arg2?.optint(1) ?: 1
                val blockState = Block.stateById(blockId)
                lvl.setBlockAndUpdate(pos.pos, blockState)
                lvl.updateNeighborsAt(pos.pos, blockState.block)
                return TRUE
            }
            else if (arg1?.isuserdata() == true && arg1.touserdata() is BlockPos) {
                val pos = arg1.touserdata() as BlockPos
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
            val box = when {
                arg.isuserdata() && arg.touserdata() is LuaBox -> (arg.touserdata() as LuaBox).box
                arg is LuaBox -> arg.box
                arg.isuserdata() && arg.touserdata() is AABB -> arg.touserdata() as AABB
                else -> null
            }
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
            val box = when {
                arg.isuserdata() && arg.touserdata() is LuaBox -> (arg.touserdata() as LuaBox).box
                arg is LuaBox -> arg.box
                arg.isuserdata() && arg.touserdata() is AABB -> arg.touserdata() as AABB
                else -> null
            }
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
     * type: строка "minecraft:sheep", creator.createEntity(...) или EntityType.
     * Возвращает заспавненную LuaEntity или NIL.
     */
    private inner class SpawnEntityFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val lvl = level ?: return NIL

            val type = resolveEntityType(args.arg(1))
                ?: return error("Unknown entity type: expected identifier string, entitytype userdata or EntityType")

            val (posVec, offset) = parseVec3(args.arg(2), args.arg(3), args.arg(4))
                .let { if (it.second > 0) it else Pair<Vec3?, Int>(null, 0) }
            if (posVec == null) {
                return error("Invalid position: expected x, y, z numbers or vector")
            }

            val nextArgIndex = 2 + offset
            val yaw = args.arg(nextArgIndex)?.optdouble(0.0) ?: 0.0
            val pitch = args.arg(nextArgIndex + 1)?.optdouble(0.0) ?: 0.0

            val entity = type.create(lvl, EntitySpawnReason.COMMAND) ?: return NIL
            entity.snapTo(posVec.x, posVec.y, posVec.z, yaw.toFloat(), pitch.toFloat())
            lvl.addFreshEntity(entity)

            return LuaEntity(entity)
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
            if (arg1.isuserdata() && (arg1.touserdata() is LuaBox || arg1.touserdata() is AABB)) {
                val aabb = if (arg1.touserdata() is LuaBox) {
                    (arg1.touserdata() as LuaBox).box
                } else {
                    arg1.touserdata() as AABB
                }
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
