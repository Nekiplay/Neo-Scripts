package com.nekiplay.hypixelcry.features.lua.objects.world

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.pathfinder.utils.world
import com.nekiplay.hypixelcry.utils.RaycastUtils
import com.nekiplay.hypixelcry.utils.Rotations
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import party.iroiro.luajava.Lua
import java.util.ArrayList

class WorldObject(private val lua: Lua) {
    fun register() {
        // Создаем таблицу
        lua.newTable()

        // Регистрируем методы
        lua.push { l -> getBlock(l) }; lua.setField(-2, "getBlock")
        lua.push { l -> getBlock(l) }; lua.setField(-2, "getBluaockState")
        lua.push { l -> setBlock(l) }; lua.setField(-2, "setBluaock")

        lua.push { l -> isBlockLoaded(l) }; lua.setField(-2, "isBluaockluaoaded")
        lua.push { l -> isBlockLoaded(l) }; lua.setField(-2, "isluaoaded")

        lua.push { l -> getRotation(l) }; lua.setField(-2, "getRotation")

        lua.push { l -> getEntities(l) }; lua.setField(-2, "getEntities")
        lua.push { l -> getEntityById(l) }; lua.setField(-2, "getEntityById")
        lua.push { l -> getLivingEntities(l) }; lua.setField(-2, "getLivingEntities")
        lua.push { l -> getArmorStandEntities(l) }; lua.setField(-2, "getArmorStandEntities")

        lua.push { l -> getEntitiesInBox(l) }; lua.setField(-2, "getEntitiesInBox")
        lua.push { l -> getArmorStandEntitiesInBox(l) }; lua.setField(-2, "getArmorStandEntitiesInBox")

        lua.push { l -> getCollisionBoxes(l) }; lua.setField(-2, "getCollisionBoxes")
        lua.push { l -> getOutlineBoxes(l) }; lua.setField(-2, "getOutlineBoxes")

        lua.push { l -> raycast(l) }; lua.setField(-2, "raycast")
        lua.push { l -> raycastToBlocks(l) }; lua.setField(-2, "raycastToBlocks")


        // Устанавливаем таблицу как глобальную переменную "world"
        lua.setGlobal("world")
    }

    private fun getOutlineBoxes(l: Lua): Int {
        // 1. Проверяем аргументы (x, y, z)
        if (!l.isNumber(1) || !l.isNumber(2) || !l.isNumber(3)) {
            l.error("Expected three number arguments for coordinates")
            return 0
        }

        // 2. Проверяем наличие BlockState (4-й аргумент)
        if (l.isNoneOrNil(4)) {
            l.error("BlockState argument is required")
            return 0
        }

        val level = mc.level ?: run {
            l.error("No world loaded")
            return 0
        }

        val x = l.toInteger(1).toInt()
        val y = l.toInteger(2).toInt()
        val z = l.toInteger(3).toInt()
        val blockPos = BlockPos(x, y, z)

        // 3. Извлекаем BlockState из Java-объекта
        // В iroiro то, что мы пушили через pushJavaObject, достается через toJavaObject
        val rawObject = l.toJavaObject(4)
        val blockState: BlockState? = when (rawObject) {
            is LuaBlockState -> rawObject.blockState
            is BlockState -> rawObject
            else -> null
        }

        if (blockState == null) {
            l.error("Invalid BlockState provided")
            return 0
        }

        // 4. Получаем коллизию
        val collisionShape = try {
            // Для GetOutlineBoxes используйте blockState.getShape(level, blockPos)
            blockState.getShape(level, blockPos)
        } catch (e: Exception) {
            l.error("Error getting collision shape: ${e.message}")
            return 0
        }

        // 5. Создаем результирующую таблицу
        l.newTable()
        if (collisionShape.isEmpty) {
            return 1 // Возвращаем пустую таблицу
        }

        var index = 1
        collisionShape.toAabbs().forEach { aabb ->
            // Предполагаем, что LuaBox — это SimpleLuaWrapper(l, aabb)
            val luaBoxValue = LuaBox(l, aabb).push()
            l.rawSetI(-2, index++)
        }

        return 1
    }

    private fun getCollisionBoxes(l: Lua): Int {
        // 1. Проверяем аргументы (x, y, z)
        if (!l.isNumber(1) || !l.isNumber(2) || !l.isNumber(3)) {
            l.error("Expected three number arguments for coordinates")
            return 0
        }

        // 2. Проверяем наличие BlockState (4-й аргумент)
        if (l.isNoneOrNil(4)) {
            l.error("BlockState argument is required")
            return 0
        }

        val level = mc.level ?: run {
            l.error("No world loaded")
            return 0
        }

        val x = l.toInteger(1).toInt()
        val y = l.toInteger(2).toInt()
        val z = l.toInteger(3).toInt()
        val blockPos = BlockPos(x, y, z)

        // 3. Извлекаем BlockState из Java-объекта
        // В iroiro то, что мы пушили через pushJavaObject, достается через toJavaObject
        val rawObject = l.toJavaObject(4)
        val blockState: BlockState? = when (rawObject) {
            is LuaBlockState -> rawObject.blockState
            is BlockState -> rawObject
            else -> null
        }

        if (blockState == null) {
            l.error("Invalid BlockState provided")
            return 0
        }

        // 4. Получаем коллизию
        val collisionShape = try {
            // Для GetOutlineBoxes используйте blockState.getShape(level, blockPos)
            blockState.getCollisionShape(level, blockPos)
        } catch (e: Exception) {
            l.error("Error getting collision shape: ${e.message}")
            return 0
        }

        // 5. Создаем результирующую таблицу
        l.newTable()
        if (collisionShape.isEmpty) {
            return 1 // Возвращаем пустую таблицу
        }

        var index = 1
        collisionShape.toAabbs().forEach { aabb ->
            // Предполагаем, что LuaBox — это SimpleLuaWrapper(l, aabb)
            val luaBoxValue = LuaBox(l, aabb).push()
            l.rawSetI(-2, index++)
        }

        return 1
    }

    private fun raycast(l: Lua): Int {
        // 1. Проверяем, что передан аргумент-таблица
        if (!l.isTable(1)) return 0

        // 2. Извлекаем координаты начала (startX, startY, startZ)
        l.getField(1, "startX"); val sx = l.toNumber(-1); l.pop(1)
        l.getField(1, "startY"); val sy = l.toNumber(-1); l.pop(1)
        l.getField(1, "startZ"); val sz = l.toNumber(-1); l.pop(1)

        // 3. Извлекаем координаты конца (endX, endY, endZ)
        l.getField(1, "endX"); val ex = l.toNumber(-1); l.pop(1)
        l.getField(1, "endY"); val ey = l.toNumber(-1); l.pop(1)
        l.getField(1, "endZ"); val ez = l.toNumber(-1); l.pop(1)

        // 4. Извлекаем флаги (include_fluid, include_entity)
        l.getField(1, "include_fluid"); val includeFluid = l.toBoolean(-1); l.pop(1)
        l.getField(1, "include_entity"); val includeEntity = l.toBoolean(-1); l.pop(1)

        val startVec = Vec3(sx, sy, sz)
        val endVec = Vec3(ex, ey, ez)
        val player = mc.player ?: return 0

        // 5. Настройка контекста рейкаста блоков
        val fluidMode = if (includeFluid) ClipContext.Fluid.ANY else ClipContext.Fluid.NONE
        val context = ClipContext(
            startVec,
            endVec,
            ClipContext.Block.OUTLINE,
            fluidMode,
            player
        )

        var hitResult: HitResult? = null

        // 6. Выполнение рейкаста
        if (!includeEntity) {
            // Только блоки
            hitResult = mc.level?.clip(context)
        } else {
            // Блоки + Сущности (используем ваши RaycastUtils)
            hitResult = RaycastUtils.fastRayTrace(player, startVec, endVec, ArrayList())

            // Логика из вашего оригинала для поиска цели под перекрестием
            val sub = endVec.subtract(startVec)
            // Внимание: в оригинале было перемножение квадратов всех осей
            val distance = sub.x * sub.x * sub.y * sub.y * sub.z * sub.z

            val crosshairTarget = RaycastUtils.findCrosshairTarget(player, startVec, endVec, distance, distance)
            if (crosshairTarget != null) {
                hitResult = crosshairTarget
            }
        }

        // 7. Возврат результата в Lua
        return if (hitResult != null) {
            // Используем функцию из предыдущего ответа для наполнения таблицы
            pushHitResult(l, hitResult)
            1
        } else {
            0 // Вернет nil в Lua
        }
    }

    private fun raycastToBlocks(l: Lua): Int {
        // Проверяем, что первый аргумент — таблица {startX=..., endX=..., blocks={...}}
        if (!l.isTable(1)) return 0

        // Извлекаем координаты начала (StartX, StartY, StartZ)
        l.getField(1, "startX"); val sx = l.toNumber(-1); l.pop(1)
        l.getField(1, "startY"); val sy = l.toNumber(-1); l.pop(1)
        l.getField(1, "startZ"); val sz = l.toNumber(-1); l.pop(1)
        val startVec = Vec3(sx, sy, sz)

        // Извлекаем координаты конца
        l.getField(1, "endX"); val ex = l.toNumber(-1); l.pop(1)
        l.getField(1, "endY"); val ey = l.toNumber(-1); l.pop(1)
        l.getField(1, "endZ"); val ez = l.toNumber(-1); l.pop(1)
        val endVec = Vec3(ex, ey, ez)

        // Извлекаем список блоков (необязательный)
        val targetBlocks = mutableListOf<Block>()
        l.getField(1, "blocks")
        if (l.isTable(-1)) {
            var i = 1
            while (true) {
                l.rawGetI(-1, i) // Берем blocks[i]
                if (l.isNil(-1)) {
                    l.pop(1)
                    break
                }
                val blockId = l.toInteger(-1).toInt()
                Block.stateById(blockId)?.let { targetBlocks.add(it.block) }
                l.pop(1)
                i++
            }
        }
        l.pop(1) // Удаляем таблицу blocks со стека

        // Выполняем рейкаст через вашу утилиту
        val hitResult = RaycastUtils.rayTraceToBlocks(startVec, endVec, targetBlocks)

        if (hitResult != null) {
            // Наполняем стек результатом
            pushHitResult(l, hitResult)
            return 1
        }

        return 0 // Ничего не нашли (nil)
    }

    private fun pushHitResult(l: Lua, hitResult: HitResult) {
        l.newTable() // Результирующая таблица

        when (hitResult.type) {
            HitResult.Type.ENTITY -> {
                val entityHit = hitResult as EntityHitResult
                l.push("entity"); l.setField(-2, "type")

                // Кладём обертку сущности (не забываем .push())
                LuaEntity(l, entityHit.entity).push()
                l.setField(-2, "data")
            }
            HitResult.Type.BLOCK -> {
                val blockHit = hitResult as BlockHitResult
                l.push("block"); l.setField(-2, "type")

                // Координаты блока
                l.push(blockHit.blockPos.x.toDouble()); l.setField(-2, "x")
                l.push(blockHit.blockPos.y.toDouble()); l.setField(-2, "y")
                l.push(blockHit.blockPos.z.toDouble()); l.setField(-2, "z")
                l.push(blockHit.direction.toString()); l.setField(-2, "side")
                
                l.newTable()
                l.push(blockHit.blockPos.x.toDouble()); l.setField(-2, "x")
                l.push(blockHit.blockPos.y.toDouble()); l.setField(-2, "y")
                l.push(blockHit.blockPos.z.toDouble()); l.setField(-2, "z")
                l.setField(-2, "blockPos")
            }
            else -> {
                l.push("miss"); l.setField(-2, "type")
            }
        }
    }

    private fun getRotation(l: Lua): Int {
        val x: Double
        val y: Double
        val z: Double

        if (l.isTable(1)) {
            // Если пришла таблица {x=1, y=2, z=3}
            l.getField(1, "x"); x = l.toNumber(-1); l.pop(1)
            l.getField(1, "y"); y = l.toNumber(-1); l.pop(1)
            l.getField(1, "z"); z = l.toNumber(-1); l.pop(1)
        } else {
            // Если пришли числа: getBlock(x, y, z)
            x = l.toNumber(1)
            y = l.toNumber(2)
            z = l.toNumber(3)
        }

        val vec = Vec3(x, y, z)

        val yaw = Rotations.getYaw(vec)
        val pitch = Rotations.getPitch(vec)

        l.newTable()
        l.push(yaw); l.setField(-2, "yaw")
        l.push(pitch); l.setField(-2, "pitch")
        return 1
    }

    private fun setBlock(l: Lua): Int {
        val level = mc.level ?: return 0
        val x: Int
        val y: Int
        val z: Int
        val id: Int

        if (l.isTable(1)) {
            // Если пришла таблица {x=1, y=2, z=3}
            l.getField(1, "x"); x = l.toInteger(-1).toInt(); l.pop(1)
            l.getField(1, "y"); y = l.toInteger(-1).toInt(); l.pop(1)
            l.getField(1, "z"); z = l.toInteger(-1).toInt(); l.pop(1)
            l.getField(1, "id"); id = l.toInteger(-1).toInt(); l.pop(1)
        } else {
            // Если пришли числа: getBlock(x, y, z)
            x = l.toInteger(1).toInt()
            y = l.toInteger(2).toInt()
            z = l.toInteger(3).toInt()
            id = l.toInteger(4).toInt()
        }
        val blockpos = BlockPos(x, y, z)
        val blockstate = Block.stateById(id)
        val state = level.setBlockAndUpdate(BlockPos(x, y, z), blockstate)
        mc.level?.updateNeighborsAt(blockpos, blockstate.block)

        return 1
    }

    private fun getBlock(l: Lua): Int {
        val level = mc.level ?: return 0
        val x: Int
        val y: Int
        val z: Int

        if (l.isTable(1)) {
            // Если пришла таблица {x=1, y=2, z=3}
            l.getField(1, "x"); x = l.toInteger(-1).toInt(); l.pop(1)
            l.getField(1, "y"); y = l.toInteger(-1).toInt(); l.pop(1)
            l.getField(1, "z"); z = l.toInteger(-1).toInt(); l.pop(1)
        } else {
            // Если пришли числа: getBlock(x, y, z)
            x = l.toInteger(1).toInt()
            y = l.toInteger(2).toInt()
            z = l.toInteger(3).toInt()
        }

        val state = level.getBlockState(BlockPos(x, y, z))

        // Используем обертку LuaBlockState
        LuaBlockState(null, state).push(l)
        return 1
    }

    private fun isBlockLoaded(l: Lua): Int {
        val level = mc.level ?: return 0 // Если мира нет, возвращаем nil (0 значений)

        val x: Int
        val y: Int
        val z: Int

        if (l.isTable(1)) {
            l.getField(1, "x"); x = l.toInteger(-1).toInt(); l.pop(1)
            l.getField(1, "y"); y = l.toInteger(-1).toInt(); l.pop(1)
            l.getField(1, "z"); z = l.toInteger(-1).toInt(); l.pop(1)
        } else if (l.isNumber(1) && l.isNumber(2) && l.isNumber(3)) {
            x = l.toInteger(1).toInt()
            y = l.toInteger(2).toInt()
            z = l.toInteger(3).toInt()
        } else {
            return 0
        }

        val result = level.isLoaded(BlockPos(x, y, z))

        // Кладём результат на стек Lua
        l.push(result)

        // Возвращаем количество значений (одно — наш булеан)
        return 1
    }

    private fun getEntities(l: Lua): Int {
        // 1. Получаем список сущностей из мира
        val level = mc.level ?: return 0
        val entities = level.entitiesForRendering()

        // 2. Создаем новую таблицу на стеке Lua
        l.newTable()
        // Сейчас таблица находится на вершине стека (индекс -1)

        var luaIndex = 1
        entities.forEach { entity ->
            // 3. Создаем обертку LuaEntity (она должна быть SimpleLuaWrapper)
            val luaEntityWrapper = LuaEntity(l, entity).push()

            // 5. Устанавливаем значение в таблицу: t[luaIndex] = luaEntity
            // Таблица сейчас на индексе -2, значение сущности на -1.
            // Метод rawSetI забирает значение с вершины стека и кладет в таблицу.
            l.rawSetI(-2, luaIndex)

            luaIndex++
        }

        // Возвращаем 1, так как на стеке осталась одна заполненная таблица
        return 1
    }

    private fun getAABBFromLua(l: Lua, index: Int): AABB? {
        val obj = l.toJavaObject(index)
        return when (obj) {
            is LuaBox -> obj.box
            is AABB -> obj
            else -> null
        }
    }

    // world.getEntityById(id)
    private fun getEntityById(l: Lua): Int {
        if (!l.isNumber(1)) return 0

        val entityId = l.toInteger(1).toInt()
        val entity = mc.level?.getEntity(entityId)

        return if (entity != null) {
            // Обязательно вызываем .push() у нашей обетки
            LuaEntity(l, entity).push()
            1
        } else 0
    }

    // world.getLivingEntities()
    private fun getLivingEntities(l: Lua): Int {
        val entities = mc.level?.entitiesForRendering() ?: return 0

        l.newTable()
        var index = 1
        entities.forEach { entity ->
            if (entity is LivingEntity) {
                LuaEntity(l, entity).push()
                l.rawSetI(-2, index++)
            }
        }
        return 1
    }

    // world.getArmorStandEntities()
    private fun getArmorStandEntities(l: Lua): Int {
        val entities = mc.level?.entitiesForRendering() ?: return 0

        l.newTable()
        var index = 1
        entities.forEach { entity ->
            if (entity is ArmorStand) {
                LuaEntity(l, entity).push()
                l.rawSetI(-2, index++)
            }
        }
        return 1
    }

    // world.getEntitiesInBox(luaBox)
    private fun getEntitiesInBox(l: Lua): Int {
        val box = getAABBFromLua(l, 1) ?: return 0
        val entities = mc.level?.getEntitiesOfClass(Entity::class.java, box) ?: return 0

        l.newTable()
        var index = 1
        entities.forEach { entity ->
            LuaEntity(l, entity).push()
            l.rawSetI(-2, index++)
        }
        return 1
    }

    // world.getArmorStandEntitiesInBox(luaBox)
    private fun getArmorStandEntitiesInBox(l: Lua): Int {
        val box = getAABBFromLua(l, 1) ?: return 0
        val entities = mc.level?.getEntitiesOfClass(ArmorStand::class.java, box) ?: return 0

        l.newTable()
        var index = 1
        entities.forEach { entity ->
            LuaEntity(l, entity).push()
            l.rawSetI(-2, index++)
        }
        return 1
    }
}