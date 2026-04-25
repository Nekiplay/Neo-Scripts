package com.nekiplay.neoscripts.features.lua.objects.world

import com.nekiplay.neoscripts.Main
import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.features.lua.customArgs.FourArgFunction
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.features.lua.objects.datatypes.phys.LuaRaycast
import com.nekiplay.neoscripts.mixins.minecraft.LevelRendererAccessor
import com.nekiplay.neoscripts.utils.RaycastUtils
import com.nekiplay.neoscripts.utils.RotationuUtils
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.util.ArrayList
import kotlin.collections.forEachIndexed

class WorldObject : LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    private fun parseBlockPos(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): BlockPos? {
        return when {
            arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos -> {
                (arg1.touserdata() as LuaBlockPos).pos
            }
            arg1?.isuserdata() == true && arg1.touserdata() is BlockPos -> {
                arg1.touserdata() as BlockPos
            }
            arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true -> {
                BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
            }
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
            // Вариант 1: Один аргумент - userdata (LuaVector3d)
            arg1?.isuserdata() == true && arg1.touserdata() is LuaVector3d -> {
                val vec = (arg1.touserdata() as LuaVector3d).location
                Pair(vec, 1) // Потреблен 1 аргумент
            }
            // Вариант 2: Один аргумент - userdata (Vec3)
            arg1?.isuserdata() == true && arg1.touserdata() is Vec3 -> {
                val vec = arg1.touserdata() as Vec3
                Pair(vec, 1) // Потреблен 1 аргумент
            }
            // Вариант 3: Три числа (x, y, z)
            arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true -> {
                val vec = Vec3(arg1.todouble(), arg2.todouble(), arg3.todouble())
                Pair(vec, 3) // Потреблено 3 аргумента
            }
            // Вариант 4: Таблица с полями x, y, z
            arg1?.istable() == true -> {
                // Проверка на наличие полей, чтобы избежать ошибок, если таблица пустая
                val xVal = arg1.get("x")
                val yVal = arg1.get("y")
                val zVal = arg1.get("z")

                if (xVal.isnumber() && yVal.isnumber() && zVal.isnumber()) {
                    val vec = Vec3(xVal.todouble(), yVal.todouble(), zVal.todouble())
                    Pair(vec, 1) // Потреблен 1 аргумент (таблица)
                } else {
                    Pair(null, 0) // Неверный формат таблицы
                }
            }
            else -> Pair(null, 0) // Ничего не подошло
        }
    }

    private fun parseBlockPosWithBlockState(
        arg1: LuaValue?,
        arg2: LuaValue?,
        arg3: LuaValue?,
        arg4: LuaValue?
    ): Pair<BlockPos, BlockState>? {
        return when {
            arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos -> {
                val pos = (arg1.touserdata() as LuaBlockPos).pos
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

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // Functions
            "getRotation" -> GetRotationFunction()
            "getBlock", "getBlockState" -> GetBlockFunction()
            "setBlock" -> SetBlockFunction()
            "isBlockLoaded" -> IsBlockLoadedFunction()

            "getEntities" -> GetEntitiesFunction()
            "getLivingEntities" -> GetLivingEntitiesFunction()
            "getArmorStandEntities" -> GetArmorStandEntitiesFunction()

            "getEntitiesInBox" -> GetEntitiesInBoxFunction()
            "getArmorStandEntitiesInBox" -> GetArmorStandEntitiesInBoxFunction()

            "getEntityById" -> GetEntityByIdFunction()

            "getCollisionBoxes" -> GetCollisionBoxesFunction()
            "getOutlineBoxes" -> GetOutlineBoxesFunction()

            "raycast" -> RaycastFunction()
            "raycastToBlocks" -> RaycastToBlocksFunction()
            "getBreakingBlocksInfo" -> GetBreakingBlocksInfo()
            "playSound" -> PlaySoundFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class GetBreakingBlocksInfo : ZeroArgFunction() {
        override fun call(): LuaValue {
            val accessed = mc.levelRenderer as LevelRendererAccessor
            var index = 1
            val list = tableOf()
            accessed.`neoscripts$getBlockBreakingInfos`().forEach { (i, progress) ->
                val tableInfo = tableOf()
                tableInfo.set("progress", valueOf(progress.progress))
                tableInfo.set("blockpos", LuaBlockPos(progress.pos))
                tableInfo.set("id", valueOf(progress.id))
                tableInfo.set("updatedRenderTick", valueOf(progress.updatedRenderTick))
                list.set(index, tableInfo)
                index++
            }
            return list
        }
    }

    private inner class GetOutlineBoxesFunction : FourArgFunction() {
        override fun invoke(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?, arg4: LuaValue?): LuaValue {
            val level: ClientLevel = mc.level ?: return error("No world loaded")

            val (blockPos, blockState) = parseBlockPosWithBlockState(arg1, arg2, arg3, arg4)
                ?: return error("Invalid arguments: expected BlockPos + BlockState or x, y, z + BlockState")

            val shape = try {
                blockState.getShape(level, blockPos)
            } catch (e: Exception) {
                return error("Error getting shape: ${e.message}")
            }

            if (shape.isEmpty) {
                return tableOf()
            }

            val result = tableOf()
            var index = 1
            shape.toAabbs().forEach { voxel ->
                result.set(index, LuaBox(voxel))
                index++
            }

            return result
        }
    }

    private inner class GetCollisionBoxesFunction : FourArgFunction() {
        override fun invoke(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?, arg4: LuaValue?): LuaValue {
            val level: ClientLevel = mc.level ?: return error("No world loaded")

            val (blockPos, blockState) = parseBlockPosWithBlockState(arg1, arg2, arg3, arg4)
                ?: return error("Invalid arguments: expected BlockPos + BlockState or x, y, z + BlockState")

            val collisionShape = try {
                blockState.getCollisionShape(level, blockPos)
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
                result.set(index, LuaBox(voxel))
                index++
            }

            return result
        }
    }

    private inner class RaycastFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.istable() == true) {
                val startX = arg.get("startX").optdouble(0.0)
                val startY = arg.get("startY").optdouble(0.0)
                val startZ = arg.get("startZ").optdouble(0.0)
                val endX = arg.get("endX").optdouble(0.0)
                val endY = arg.get("endY").optdouble(0.0)
                val endZ = arg.get("endZ").optdouble(0.0)

                val include_fluid = arg.get("include_fluid").optboolean(false)
                val include_entity = arg.get("include_entity").optboolean(false)

                val startVec = Vec3(startX, startY, startZ)
                val endVec = Vec3(endX, endY, endZ)

                val player = mc.player;

                if (player != null) {

                    var context = ClipContext(
                        startVec,
                        endVec,
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        player
                    )

                    if (include_fluid) {
                        context = ClipContext(
                            startVec,
                            endVec,
                            ClipContext.Block.OUTLINE,
                            ClipContext.Fluid.ANY,
                            player
                        )
                    }

                    var hitResult: HitResult? = null

                    hitResult = if (!include_entity) {
                        mc.level?.clip(context)
                    } else {
                        RaycastUtils.fastRayTrace(mc.player, startVec, endVec, ArrayList())
                    }

                    if (include_entity) {
                        val sub = endVec.subtract(startVec)
                        val distance = sub.x * sub.x * sub.y * sub.y * sub.z * sub.z
                        return LuaRaycast(RaycastUtils.findCrosshairTarget(mc.player, startVec, endVec, distance, distance))
                    }

                    return if (hitResult != null) {
                        LuaRaycast(hitResult)
                    } else {
                        NIL
                    }
                }
                else {
                    NIL
                }
            } else {
                NIL
            }
        }
    }

    private inner class RaycastToBlocksFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.istable() == true) {
                val startX = arg.get("startX").optdouble(0.0)
                val startY = arg.get("startY").optdouble(0.0)
                val startZ = arg.get("startZ").optdouble(0.0)
                val endX = arg.get("endX").optdouble(0.0)
                val endY = arg.get("endY").optdouble(0.0)
                val endZ = arg.get("endZ").optdouble(0.0)

                val startVec = Vec3(startX, startY, startZ)
                val endVec = Vec3(endX, endY, endZ)

                // Получаем список блоков для проверки (если указаны)
                val blocksTable = arg.get("blocks")
                val targetBlocks = mutableListOf<Block>()
                if (blocksTable.istable()) {
                    val len = blocksTable.length() // или lua_len(arg2)
                    for (i in 1..len) {
                        val value = blocksTable.get(i)
                        if (value.isint()) {
                            val id = value.toint()
                            val state = Block.stateById(id)

                            if (state != null) {
                                targetBlocks.add(state.block)
                            } else {
                                Main.LOGGER?.warn("No block found for ID: $id")
                            }
                        }
                    }
                } else {
                }

                val hitResult = RaycastUtils.rayTraceToBlocks(startVec, endVec, targetBlocks)
                return if (hitResult != null) {
                    LuaRaycast(hitResult)
                } else {
                    NIL
                }
            } else {
                NIL
            }
        }
    }

    private inner class GetRotationFunction : ThreeArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?
        ): LuaValue? {
            return if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {
                val yaw = RotationuUtils.getYaw(Vec3(arg1.todouble(), arg2.todouble(), arg3.todouble()))
                val pitch = RotationuUtils.getPitch(Vec3(arg1.todouble(), arg2.todouble(), arg3.todouble()))
                val table = tableOf()
                table.set("yaw", valueOf(yaw))
                table.set("pitch", valueOf(pitch))
                table
            }
            else if (arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos) {
                val pos = arg1.touserdata() as LuaBlockPos

                val yaw = RotationuUtils.getYaw(Vec3(pos.pos.x.toDouble(), pos.pos.y.toDouble(), pos.pos.z.toDouble()))
                val pitch = RotationuUtils.getPitch(Vec3(pos.pos.x.toDouble(), pos.pos.y.toDouble(), pos.pos.z.toDouble()))
                val table = tableOf()
                table.set("yaw", valueOf(yaw))
                table.set("pitch", valueOf(pitch))
                table
            }
            else {
                NIL
            }
        }
    }

    private inner class SetBlockFunction : FourArgFunction() {
        override fun invoke(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?,
            arg4: LuaValue?
        ): LuaValue? {
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isnumber() == true) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val blockId = arg4.toint()
                val blockState = Block.stateById(blockId)

                mc.level?.setBlockAndUpdate(blockPos, blockState)
                mc.level?.updateNeighborsAt(blockPos, blockState.block)
                return TRUE
            }
            else if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isuserdata() == true && arg4.touserdata() is LuaBlockState) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val blockState = arg4.touserdata() as LuaBlockState

                mc.level?.setBlockAndUpdate(blockPos, blockState.blockState)
                mc.level?.updateNeighborsAt(blockPos, blockState.blockState.block)
                return TRUE
            }
            else if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true && arg4?.isuserdata() == true && arg4.touserdata() is BlockState) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val blockState = arg4.touserdata() as BlockState

                mc.level?.setBlockAndUpdate(blockPos, blockState)
                mc.level?.updateNeighborsAt(blockPos, blockState.block)
                return TRUE
            }
            else if (arg1?.istable() ?: false) {
                val x: Int = if (arg1.get("x").isnumber()) arg1.get("x").toint() else 0
                val y: Int = if (arg1.get("y").isnumber()) arg1.get("y").toint() else 0
                val z: Int = if (arg1.get("z").isnumber()) arg1.get("z").toint() else 0
                val id: Int = if (arg1.get("id").isnumber()) arg1.get("id").toint() else 0

                val blockPos = BlockPos(x, y, z)
                val blockState = Block.stateById(id)

                mc.level?.setBlockAndUpdate(blockPos, blockState)
                mc.level?.updateNeighborsAt(blockPos, blockState.block)
                return TRUE
            }
            else if (arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos) {
                val pos = arg1.touserdata() as LuaBlockPos
                val blockId = arg2?.optint(1) ?: 1
                val blockState = Block.stateById(blockId)
                mc.level?.setBlockAndUpdate(pos.pos, blockState)
                mc.level?.updateNeighborsAt(pos.pos, blockState.block)
            }
            else if (arg1?.isuserdata() == true && arg1.touserdata() is BlockPos) {
                val pos = arg1.touserdata() as BlockPos
                val blockId = arg2?.optint(1) ?: 1
                val blockState = Block.stateById(blockId)
                mc.level?.setBlockAndUpdate(pos, blockState)
                mc.level?.updateNeighborsAt(pos, blockState.block)
            }
            else if (arg1?.isuserdata() == true && arg1.touserdata() is LuaBlockPos && arg2?.isuserdata() == true && arg2.touserdata() is LuaBlockState) {
                val pos = arg1.touserdata() as LuaBlockPos
                val state = arg2.touserdata() as LuaBlockState
                mc.level?.setBlockAndUpdate(pos.pos, state.blockState)
                mc.level?.updateNeighborsAt(pos.pos, state.blockState.block)
            }
            else if (arg1?.isuserdata() == true && arg1.touserdata() is BlockPos && arg2?.isuserdata() == true && arg2.touserdata() is BlockState) {
                val pos = arg1.touserdata() as BlockPos
                val state = arg2.touserdata() as BlockState
                mc.level?.setBlockAndUpdate(pos, state)
                mc.level?.updateNeighborsAt(pos, state.block)
            }
            return NIL
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
                val state = mc.level?.getBlockState(blockPos)
                if (state != null) {
                    return LuaBlockState(state);
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
                return valueOf(mc.level?.isLoaded(blockPos) ?: false);
            }
            return NIL
        }
    }

    private inner class GetEntitiesFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val entitiesTable = tableOf()

            mc.level?.entitiesForRendering()?.forEachIndexed { index, entity ->
                entitiesTable.set(index + 1, LuaEntity(entity))
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
                var index = 0
                mc.level?.getEntitiesOfClass(ArmorStand::class.java, box)?.forEach { entity ->
                    entitiesTable.set(index + 1, LuaEntity(entity))
                    index++
                }
                return entitiesTable
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
                mc.level?.getEntitiesOfClass(Entity::class.java, box)?.forEach { entity ->
                    entitiesTable.set(index, LuaEntity(entity))
                    index++
                }
                return entitiesTable
            }
            return entitiesTable
        }
    }

    private inner class GetArmorStandEntitiesFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val entitiesTable = tableOf()

            var index = 1
            mc.level?.entitiesForRendering()?.forEach { entity ->
                if (entity is ArmorStand) {
                    entitiesTable.set(index, LuaEntity(entity))
                    index++
                }
            }

            return entitiesTable
        }
    }

    private inner class GetLivingEntitiesFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val entitiesTable = tableOf()

            var index = 1
            mc.level?.entitiesForRendering()?.forEach { entity ->
                if (entity is LivingEntity) {
                    entitiesTable.set(index, LuaEntity(entity))
                    index++
                }
            }

            return entitiesTable
        }
    }

    private inner class GetEntityByIdFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                val entityId = arg.toint()
                val entity = mc.level?.getEntity(entityId)
                if (entity != null) {
                    LuaEntity(entity)
                }
                else {
                    NIL
                }
            } else {
                NIL
            }
        }
    }

    override fun typename(): String = "world"
    override fun tojstring(): String = "WorldObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    private inner class PlaySoundFunction : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs? {
            val level = mc.level ?: return NIL

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

            val player = mc.player ?: return NIL

            val field = try {
                mc.javaClass.getDeclaredField("field_44867")
            } catch (e: Exception) {
                null
            }
            field?.isAccessible = true
            val gameVolume = try {
                (field?.get(mc) as? Double)?.toFloat() ?: 1.0f
            } catch (e: Exception) {
                1.0f
            }

            val compensatedVolume = if (gameVolume > 0) finalVolume / gameVolume else finalVolume

            level.playSeededSound(
                player,
                position.x,
                position.y,
                position.z,
                soundEvent,
                SoundSource.MASTER,
                compensatedVolume,
                pitch.toFloat(),
                1
            )

            return TRUE
        }
    }
}