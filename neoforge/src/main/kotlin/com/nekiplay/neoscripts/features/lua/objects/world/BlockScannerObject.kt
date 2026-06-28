package com.nekiplay.neoscripts.features.lua.objects.world

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaBlockState
import net.minecraft.core.BlockPos
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction

class BlockScannerObject() : LuaValue() {

    override fun type(): Int = TUSERDATA
    override fun typename(): String = "block_scanner"
    override fun tojstring(): String = "BlockScannerObject"

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
                "new_iterator" -> NewIteratorFunc()
            else -> super.get(key)
        }
    }

    inner class NewIteratorFunc : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x = args.checkint(1)
            val y = args.checkint(2)
            val z = args.checkint(3)
            val hRadius = args.checkint(4)

            val hUp = args.optint(5, hRadius)
            val hDown = args.optint(6, hRadius)

            return BlockIteratorObject(x, y, z, hRadius, hUp, hDown)
        }
    }
}

class BlockIteratorObject(
    val centerX: Int, val centerY: Int, val centerZ: Int,
    private val hRadius: Int,
    private val heightUp: Int,
    private val heightDown: Int
) : LuaValue() {

    // Границы мира для безопасности
    private val worldMinY = mc.level?.minY ?: -64
    private val worldMaxY = mc.level?.maxY ?: 320

    // Вычисляем фактические границы сканирования
    private val startY = (centerY - heightDown).coerceAtLeast(worldMinY)
    private val endY = (centerY + heightUp).coerceAtMost(worldMaxY)

    private val startX = centerX - hRadius
    private val endX = centerX + hRadius

    private val startZ = centerZ - hRadius
    private val endZ = centerZ + hRadius

    // Текущие координаты (начинаем с самого низа)
    private var currX = startX
    private var currZ = startZ
    private var currY = startY

    private val mutablePos = BlockPos.MutableBlockPos()

    override fun type(): Int = TUSERDATA
    override fun typename(): String = "block_iterator"

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "next_batch" -> NextBatchFunc()
            "progress" -> {
                val totalHeight = (endY - startY).toDouble().coerceAtLeast(1.0)
                val currentProgress = (currY - startY).toDouble()
                valueOf(currentProgress / totalHeight)
            }
            else -> super.get(key)
        }
    }

    inner class NextBatchFunc : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val limit = args.optint(1, 1000)
            val result = LuaTable()
            var count = 0

            val level = mc.level ?: return varargsOf(result, FALSE)

            while (count < limit && currY <= endY) {
                mutablePos.set(currX, currY, currZ)

                // Проверка, загружен ли чанк (чтобы избежать подвисаний)
                if (level.hasChunkAt(mutablePos)) {
                    val state = level.getBlockState(mutablePos)

                    if (!state.isAir) {
                        val entry = LuaTable()
                        entry.set(1, valueOf(currX))
                        entry.set(2, valueOf(currY))
                        entry.set(3, valueOf(currZ))
                        entry.set(4, LuaBlockState(state))

                        count++
                        result.set(count, entry)
                    }
                }

                // Итерация по осям (сначала X, потом Z, потом Y)
                currX++
                if (currX > endX) {
                    currX = startX
                    currZ++
                    if (currZ > endZ) {
                        currZ = startZ
                        currY++
                    }
                }
            }

            // Возвращаем таблицу найденных блоков и флаг "есть ли продолжение"
            val hasMore = currY <= endY
            return varargsOf(result, valueOf(hasMore))
        }
    }
}