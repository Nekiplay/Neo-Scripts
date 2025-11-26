package com.nekiplay.hypixelcry.features.lua.objects.world

import com.nekiplay.hypixelcry.features.lua.customArgs.FourArgFunction
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.hypixelcry.features.lua.utils.EntityUtils
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.utils.RaycastUtils
import com.nekiplay.hypixelcry.utils.Rotations
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class WorldObject : LuaValue() {
    override fun call(): LuaValue {
        return this
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
            "getEntityById" -> GetEntityByIdFunction()
            "getCollisionBoxes" -> GetCollisionBoxesFunction()
            "getOutlineBoxes" -> GetOutlineBoxesFunction()

            "raycast" -> RaycastFunction()
            "raycastToBlocks" -> RaycastToBlocksFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class GetOutlineBoxesFunction : FourArgFunction() {
        override fun invoke(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?, arg4: LuaValue?): LuaValue {
            // Проверяем обязательные аргументы (координаты)
            if (arg1?.isnumber() != true || arg2?.isnumber() != true || arg3?.isnumber() != true) {
                return LuaValue.error("Expected three number arguments for coordinates")
            }

            // Проверяем blockState аргумент
            if (arg4 == null) {
                return LuaValue.error("BlockState argument is required")
            }

            val x = arg1.toint()
            val y = arg2.toint()
            val z = arg3.toint()
            val blockPos = BlockPos(x, y, z)

            val blockState = when {
                arg4.isuserdata(LuaBlockState::class.java) -> {
                    val luaBlockState = arg4.touserdata() as? LuaBlockState
                    luaBlockState?.getState()
                }
                arg4.isuserdata(BlockState::class.java) -> {
                    arg4.touserdata() as? BlockState
                }
                else -> null
            }

            // Если не удалось получить BlockState, возвращаем пустой список
            if (blockState == null) {
                return LuaValue.error("Invalid BlockState provided")
            }

            // Получаем collision shape
            val collisionShape = try {
                blockState.getShape(mc.level, blockPos)
            } catch (e: Exception) {
                return LuaValue.error("Error getting collision shape: ${e.message}")
            }

            // Если shape пустой, возвращаем пустую таблицу
            if (collisionShape.isEmpty) {
                return LuaValue.tableOf()
            }

            // Конвертируем VoxelShape в Lua таблицу с bounding boxes
            val result = LuaValue.tableOf()

            var index: Int = 1
            collisionShape.toAabbs().forEach { voxel ->
                val boxTable = LuaValue.tableOf()
                boxTable.set("minX", LuaValue.valueOf(voxel.minX))
                boxTable.set("minY", LuaValue.valueOf(voxel.minY))
                boxTable.set("minZ", LuaValue.valueOf(voxel.minZ))
                boxTable.set("maxX", LuaValue.valueOf(voxel.maxX))
                boxTable.set("maxY", LuaValue.valueOf(voxel.maxY))
                boxTable.set("maxZ", LuaValue.valueOf(voxel.maxZ))
                result.set(index, boxTable)
                index++
            }

            return result
        }
    }

    private inner class GetCollisionBoxesFunction : FourArgFunction() {
        override fun invoke(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?, arg4: LuaValue?): LuaValue {
            // Проверяем обязательные аргументы (координаты)
            if (arg1?.isnumber() != true || arg2?.isnumber() != true || arg3?.isnumber() != true) {
                return LuaValue.error("Expected three number arguments for coordinates")
            }

            // Проверяем blockState аргумент
            if (arg4 == null) {
                return LuaValue.error("BlockState argument is required")
            }

            val x = arg1.toint()
            val y = arg2.toint()
            val z = arg3.toint()
            val blockPos = BlockPos(x, y, z)

            val blockState = when {
                arg4.isuserdata(LuaBlockState::class.java) -> {
                    val luaBlockState = arg4.touserdata() as? LuaBlockState
                    luaBlockState?.getState()
                }
                arg4.isuserdata(BlockState::class.java) -> {
                    arg4.touserdata() as? BlockState
                }
                else -> null
            }

            // Если не удалось получить BlockState, возвращаем пустой список
            if (blockState == null) {
                return LuaValue.error("Invalid BlockState provided")
            }

            // Получаем collision shape
            val collisionShape = try {
                blockState.getCollisionShape(mc.level, blockPos)
            } catch (e: Exception) {
                return LuaValue.error("Error getting collision shape: ${e.message}")
            }

            // Если shape пустой, возвращаем пустую таблицу
            if (collisionShape.isEmpty) {
                return LuaValue.tableOf()
            }

            // Конвертируем VoxelShape в Lua таблицу с bounding boxes
            val result = LuaValue.tableOf()

            var index: Int = 1
            collisionShape.toAabbs().forEach { voxel ->
                val boxTable = LuaValue.tableOf()
                boxTable.set("minX", LuaValue.valueOf(voxel.minX))
                boxTable.set("minY", LuaValue.valueOf(voxel.minY))
                boxTable.set("minZ", LuaValue.valueOf(voxel.minZ))
                boxTable.set("maxX", LuaValue.valueOf(voxel.maxX))
                boxTable.set("maxY", LuaValue.valueOf(voxel.maxY))
                boxTable.set("maxZ", LuaValue.valueOf(voxel.maxZ))
                result.set(index, boxTable)
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

                val startVec = Vec3(startX, startY, startZ)
                val endVec = Vec3(endX, endY, endZ)

                var context = ClipContext(
                    startVec,
                    endVec,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    mc.player
                )

                if (include_fluid) {
                    context = ClipContext(
                        startVec,
                        endVec,
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.ANY,
                        mc.player
                    )
                }

                val hitResult = mc.level?.clip(context)
                return if (hitResult != null) {
                    processHitResult(hitResult)
                } else {
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
                    var i = 1
                    while (true) {
                        val blockName = blocksTable.get(i).optint(0)
                        val state = Block.stateById(blockName)
                        if (state != null) {
                            targetBlocks.add(state.block)
                        }
                        i++
                    }
                }

                val hitResult = RaycastUtils.rayTraceToBlocks(startVec, endVec, targetBlocks)
                return if (hitResult != null) {
                    processHitResult(hitResult)
                } else {
                    NIL
                }
            } else {
                NIL
            }
        }
    }

    private fun processHitResult(hitResult: HitResult): LuaValue {
        when (hitResult.type)
        {
            HitResult.Type.ENTITY -> {
                val table = tableOf()
                table.set("type", "entity")
                table.set("data", LuaEntity((hitResult as EntityHitResult).entity))
                return table
            }
            HitResult.Type.BLOCK -> {
                val result = hitResult as BlockHitResult
                val table = tableOf()
                table.set("type", "block")
                table.set("x", valueOf(result.blockPos.x))
                table.set("y", valueOf(result.blockPos.y))
                table.set("z", valueOf(result.blockPos.z))
                table.set("side", valueOf(result.direction.toString()))

                val blockPos = tableOf()
                blockPos.set("x", valueOf(result.blockPos.x))
                blockPos.set("y", valueOf(result.blockPos.y))
                blockPos.set("z", valueOf(result.blockPos.z))
                table.set("blockPos", blockPos)

                return table
            }
            else -> {
                val table = tableOf()
                table.set("type", "miss")
                return table
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
                val yaw = Rotations.getYaw(Vec3(arg1.todouble(), arg2.todouble(), arg3.todouble()))
                val pitch = Rotations.getPitch(Vec3(arg1.todouble(), arg2.todouble(), arg3.todouble()))
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

                /*mc.worldRenderer.updateBlock(
                --    mc.level,
                    blockPos,
                    mc.level?.getBlockState(blockPos),
                    blockState,
                    0
                )*/
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

                /*mc.worldRenderer.updateBlock(
                    mc.world,
                    blockPos,
                    mc.world?.getBlockState(blockPos),
                    blockState,
                    0
                )*/
                mc.level?.updateNeighborsAt(blockPos, blockState.block)
                return TRUE
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
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val state = mc.level?.getBlockState(blockPos)
                if (state != null) {
                    return LuaBlockState(state);
                }
            }
            else if (arg1?.istable() == true) {
                val x: Int = if (arg1.get("x").isnumber()) arg1.get("x").toint() else 0
                val y: Int = if (arg1.get("y").isnumber()) arg1.get("y").toint() else 0
                val z: Int = if (arg1.get("z").isnumber()) arg1.get("z").toint() else 0
                val blockPos = BlockPos(x, y, z)
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
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {
                val blockPos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                return valueOf(mc.level?.isLoaded(blockPos) ?: false);
            }
            else if (arg1?.istable() == true) {
                val x: Int = if (arg1.get("x").isnumber()) arg1.get("x").toint() else 0
                val y: Int = if (arg1.get("y").isnumber()) arg1.get("y").toint() else 0
                val z: Int = if (arg1.get("z").isnumber()) arg1.get("z").toint() else 0
                val blockPos = BlockPos(x, y, z)
                return valueOf(mc.level?.isLoaded(blockPos) ?: false);
            }
            return NIL
        }
    }

    private inner class GetEntitiesFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return EntityUtils.GetAllEntities()
        }
    }

    private inner class GetLivingEntitiesFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return EntityUtils.GetAllLivingEntities()
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
}