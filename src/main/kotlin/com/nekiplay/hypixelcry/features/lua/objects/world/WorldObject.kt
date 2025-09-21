package com.nekiplay.hypixelcry.features.lua.objects.world

import com.nekiplay.hypixelcry.features.lua.customArgs.FourArgFunction
import com.nekiplay.hypixelcry.features.lua.utils.BlockUtil
import com.nekiplay.hypixelcry.features.lua.utils.EntityUtils
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.utils.RaycastUtils
import com.nekiplay.hypixelcry.utils.Rotations
import net.minecraft.block.Block
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class WorldObject : LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // Functions
            "getRotation" -> GetRotationFunction()
            "getBlock" -> GetBlockFunction()
            "setBlock" -> SetBlockFunction()
            "getBlocksInRadius" -> GetBlocksInRadiusFunction()
            "isBlockLoaded" -> IsBlockLoadedFunction()

            "getEntities" -> GetEntitiesFunction()
            "getLivingEntities" -> GetLivingEntitiesFunction()
            "getEntityById" -> GetEntityByIdFunction()
            "getEntitiesInRadius" -> GetEntitiesInRadiusFunction()

            "raycast" -> RaycastFunction()
            "raycastToBlocks" -> RaycastToBlocksFunction()
            else -> NIL
        } as LuaValue
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

                val startVec = Vec3d(startX, startY, startZ)
                val endVec = Vec3d(endX, endY, endZ)

                val context = RaycastContext(startVec, endVec, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player)

                val hitResult = mc.world?.raycast(context)

                processHitResult(hitResult)
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

                val startVec = Vec3d(startX, startY, startZ)
                val endVec = Vec3d(endX, endY, endZ)

                // Получаем список блоков для проверки (если указаны)
                val blocksTable = arg.get("blocks")
                val targetBlocks = mutableListOf<Block>()
                if (blocksTable.istable()) {
                    var i = 1
                    while (true) {
                        val blockName = blocksTable.get(i).optint(0)
                        val state = Block.getStateFromRawId(blockName)
                        if (state != null) {
                            targetBlocks.add(state.block)
                        }
                        i++
                    }
                }

                val hitResult = RaycastUtils.rayTraceToBlocks(startVec, endVec, targetBlocks)

                processHitResult(hitResult)
            } else {
                NIL
            }
        }
    }

    public fun processHitResult(hitResult: HitResult?): LuaValue {
        return when (hitResult?.type) {
            HitResult.Type.BLOCK -> {
                val table = tableOf()
                table.set("type", "block")
                table.set("x", valueOf(hitResult.pos.x))
                table.set("y", valueOf(hitResult.pos.y))
                table.set("z", valueOf(hitResult.pos.z))

                if (hitResult is BlockHitResult) {
                    val blockPos = tableOf()
                    blockPos.set("x", valueOf(hitResult.blockPos.x))
                    blockPos.set("y", valueOf(hitResult.blockPos.y))
                    blockPos.set("z", valueOf(hitResult.blockPos.y))
                    table.set("blockPos", blockPos)
                }
                table
            }
            HitResult.Type.ENTITY -> {
                val table = tableOf()
                if (hitResult is EntityHitResult) {
                    table.set("type", "entity")
                    table.set("data", EntityUtils.ToLua(hitResult.entity))
                }
                table
            }
            HitResult.Type.MISS -> {
                val table = tableOf()
                table.set("type", "miss")
                table
            }
            else -> NIL
        }
    }

    private inner class GetRotationFunction : ThreeArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?
        ): LuaValue? {
            return if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {

                val yaw = Rotations.getYaw(Vec3d(arg1.todouble(), arg2.todouble(), arg3.todouble()))
                val pitch = Rotations.getPitch(Vec3d(arg1.todouble(), arg2.todouble(), arg3.todouble()))

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
                val blockState = Block.getStateFromRawId(blockId)

                mc.world?.setBlockState(blockPos, blockState)

                mc.worldRenderer.updateBlock(
                    mc.world,
                    blockPos,
                    mc.world?.getBlockState(blockPos),
                    blockState,
                    0
                )
                mc.world?.updateNeighbors(blockPos, blockState.block)
                return TRUE
            }
            else if (arg1?.istable() ?: false) {
                val x: Int = if (arg1.get("x").isnumber()) arg1.get("x").toint() else 0
                val y: Int = if (arg1.get("y").isnumber()) arg1.get("y").toint() else 0
                val z: Int = if (arg1.get("z").isnumber()) arg1.get("z").toint() else 0
                val id: Int = if (arg1.get("id").isnumber()) arg1.get("id").toint() else 0

                val blockPos = BlockPos(x, y, z)
                val blockState = Block.getStateFromRawId(id)

                mc.world?.setBlockState(blockPos, blockState)

                mc.worldRenderer.updateBlock(
                    mc.world,
                    blockPos,
                    mc.world?.getBlockState(blockPos),
                    blockState,
                    0
                )
                mc.world?.updateNeighbors(blockPos, blockState.block)
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
                val state = mc.world?.getBlockState(blockPos)
                return BlockUtil.ToLua(state);
            }
            else if (arg1?.istable() == true) {
                val x: Int = if (arg1.get("x").isnumber()) arg1.get("x").toint() else 0
                val y: Int = if (arg1.get("y").isnumber()) arg1.get("y").toint() else 0
                val z: Int = if (arg1.get("z").isnumber()) arg1.get("z").toint() else 0
                val blockPos = BlockPos(x, y, z)
                val state = mc.world?.getBlockState(blockPos)
                return BlockUtil.ToLua(state);
            }
            return NIL
        }
    }

    private inner class GetBlocksInRadiusFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            val radius = if (arg2?.isnumber() == true) arg2.toint() else 10
            val centerX: Int
            val centerY: Int
            val centerZ: Int

            if (arg1?.istable() == true) {
                // Если первый аргумент - таблица с координатами
                centerX = arg1.get("x").optint(mc.player?.blockPos?.x ?: 0)
                centerY = arg1.get("y").optint(mc.player?.blockPos?.y ?: 0)
                centerZ = arg1.get("z").optint(mc.player?.blockPos?.z ?: 0)
            } else if (arg1?.isnumber() == true && arg2 == null) {
                // Если только один числовой аргумент (радиус от игрока)
                centerX = mc.player?.blockPos?.x ?: 0
                centerY = mc.player?.blockPos?.y ?: 0
                centerZ = mc.player?.blockPos?.z ?: 0
            } else {
                // По умолчанию от позиции игрока
                centerX = mc.player?.blockPos?.x ?: 0
                centerY = mc.player?.blockPos?.y ?: 0
                centerZ = mc.player?.blockPos?.z ?: 0
            }

            val resultTable = tableOf()
            var index = 1

            val centerPos = BlockPos(centerX, centerY, centerZ)

            // Перебираем все блоки в кубе радиуса
            for (x in centerX - radius..centerX + radius) {
                for (y in centerY - radius..centerY + radius) {
                    for (z in centerZ - radius..centerZ + radius) {
                        val blockPos = BlockPos(x, y, z)
                        val distance = centerPos.getSquaredDistance(blockPos).toDouble()

                        if (distance <= radius * radius) {
                            val state = mc.world?.getBlockState(blockPos)
                            val blockTable = BlockUtil.ToLua(state)
                            if (blockTable != null && !blockTable.isnil()) {
                                blockTable.set("x", blockPos.x)
                                blockTable.set("y", blockPos.y)
                                blockTable.set("z", blockPos.z)
                                resultTable.set(index++, blockTable)
                            }
                        }
                    }
                }
            }

            return resultTable
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
                return valueOf(mc.world?.isPosLoaded(blockPos) ?: false);
            }
            else if (arg1?.istable() == true) {
                val x: Int = if (arg1.get("x").isnumber()) arg1.get("x").toint() else 0
                val y: Int = if (arg1.get("y").isnumber()) arg1.get("y").toint() else 0
                val z: Int = if (arg1.get("z").isnumber()) arg1.get("z").toint() else 0
                val blockPos = BlockPos(x, y, z)
                return valueOf(mc.world?.isPosLoaded(blockPos) ?: false);
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
                val entity = mc.world?.getEntityById(entityId)
                EntityUtils.ToLua(entity) ?: NIL
            } else {
                NIL
            }
        }
    }

    private inner class GetEntitiesInRadiusFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            val radius = if (arg2?.isnumber() == true) arg2.todouble() else 10.0
            val centerX: Double
            val centerY: Double
            val centerZ: Double

            if (arg1?.istable() == true) {
                // Если первый аргумент - таблица с координатами
                centerX = arg1.get("x").optdouble(mc.player?.x ?: 0.0)
                centerY = arg1.get("y").optdouble(mc.player?.y ?: 0.0)
                centerZ = arg1.get("z").optdouble(mc.player?.z ?: 0.0)
            } else if (arg1?.isnumber() == true && arg2 == null) {
                // Если только один числовой аргумент (радиус от игрока)
                centerX = mc.player?.x ?: 0.0
                centerY = mc.player?.y ?: 0.0
                centerZ = mc.player?.z ?: 0.0
            } else {
                // По умолчанию от позиции игрока
                centerX = mc.player?.x ?: 0.0
                centerY = mc.player?.y ?: 0.0
                centerZ = mc.player?.z ?: 0.0
            }

            val resultTable = LuaValue.tableOf()
            var index = 1

            mc.world?.entities?.forEach { entity ->
                val distance = entity.pos.distanceTo(Vec3d(centerX, centerY, centerZ))
                if (distance <= radius) {
                    resultTable.set(index++, EntityUtils.ToLua(entity) ?: LuaValue.NIL)
                }
            }

            return resultTable
        }
    }

    override fun typename(): String = "world"
    override fun tojstring(): String = "WorldObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}