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

    // Функция создания итератора: scanner.new_iterator(x, y, z, radius)
    inner class NewIteratorFunc : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return BlockIteratorObject(
                args.checkint(1),
                args.checkint(2),
                args.checkint(3),
                args.checkint(4)
            )
        }
    }
}

// Объект Итератора
class BlockIteratorObject(
    val centerX: Int, val centerY: Int, val centerZ: Int,
    private val radius: Int
) : LuaValue() {

    private var currX = centerX - radius
    private var currZ = centerZ - radius
    private var currY = mc.level?.minY ?: -60

    private val maxX = centerX + radius
    private val maxZ = centerZ + radius
    private val maxY = mc.level?.maxY ?: 320

    private val mutablePos = BlockPos.MutableBlockPos()

    override fun type(): Int = TUSERDATA
    override fun typename(): String = "block_iterator"

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "next_batch" -> NextBatchFunc()
            "progress" -> valueOf(currY.toDouble() / maxY.toDouble())
            else -> super.get(key)
        }
    }

    // Главная функция для предотвращения лагов: берем блоки пачкой
    inner class NextBatchFunc : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val limit = args.optint(1, 1000)
            val result = LuaTable()
            var count = 0

            while (count < limit && currY <= maxY) {
                mutablePos.set(currX, currY, currZ)
                val state = mc.level?.getBlockState(mutablePos)

                // Пропускаем воздух в батче для экономии памяти и CPU
                if (state != null && !state.isAir) {
                    val entry = LuaTable()
                    entry.set(1, valueOf(currX))
                    entry.set(2, valueOf(currY))
                    entry.set(3, valueOf(currZ))
                    entry.set(4, LuaBlockState(state))

                    count++
                    result.set(count, entry)
                }

                // Логика перемещения по координатам
                currX++
                if (currX > maxX) {
                    currX = (centerX - radius)
                    currZ++
                    if (currZ > maxZ) {
                        currZ = (centerZ - radius)
                        currY++
                    }
                }
            }

            // Возвращаем (таблица_блоков, есть_ли_еще_блоки)
            return varargsOf(result, valueOf(currY <= maxY))
        }
    }
}