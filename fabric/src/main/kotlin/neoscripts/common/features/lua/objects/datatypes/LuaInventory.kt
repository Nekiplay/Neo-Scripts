package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import com.nekiplay.neoscripts.client.sugar.isItem
import com.nekiplay.neoscripts.client.sugar.toItem
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JavaInstance

class LuaInventory(val container: Container) : LuaUserdata(container) {

    override fun get(key: LuaValue): LuaValue {
        // Доступ по слоту (нумерация с 1): inventory[1]
        if (key.isnumber()) {
            val slot = key.toint()
            if (slot < 1 || slot > container.containerSize) return NIL
            val stack = container.getItem(slot - 1)
            return if (!stack.isEmpty) LuaItemStack(stack) else NIL
        }

        return when (val field = key.tojstring()) {
            "javaClass", "class" -> JavaInstance(container)
            "size" -> valueOf(container.containerSize)
            "is_empty","isEmpty" -> valueOf(container.isEmpty)

            // Список предметов
            "get_items", "getItems", "items", "inventory_items", "inventoryItems" -> GetItemsFunction()

            // Выдача предметов
            "give_item", "add_item", "giveItem", "addItem" -> GiveItemFunction()

            // Убирание предметов
            "take_item", "remove_item", "takeItem", "removeItem" -> RemoveItemFunction()

            // Установка предмета в слот
            "set_item", "setItem" -> SetItemFunction()

            else -> super.get(key)
        }
    }

    override fun set(key: LuaValue, value: LuaValue) {
        // Присваивание по слоту (нумерация с 1): inventory[1] = itemstack
        if (key.isnumber()) {
            setSlot(key.toint(), value)
            return
        }
        super.set(key, value)
    }

    private fun setSlot(slot: Int, value: LuaValue?): LuaValue {
        if (slot < 1 || slot > container.containerSize) return FALSE
        val stack = toStack(value) ?: return FALSE
        container.setItem(slot - 1, stack)
        container.setChanged()
        return TRUE
    }

    private fun toStack(value: LuaValue?): ItemStack? = when {
        value == null || value.isnil() -> ItemStack.EMPTY
        value.isItem() -> value.toItem()
        else -> null
    }

    // Добавление стака в контейнер: сначала слияние с существующими стопками, затем пустые слоты.
    // Возвращает фактически добавленное количество.
    private fun addToContainer(stack: ItemStack): Int {
        var remaining = stack.count

        if (stack.isStackable) {
            for (i in 0 until container.containerSize) {
                if (remaining <= 0) break
                val target = container.getItem(i)
                if (target.isEmpty || !ItemStack.isSameItemSameComponents(stack, target)) continue
                val canAdd = minOf(target.maxStackSize - target.count, remaining)
                if (canAdd <= 0) continue
                target.count += canAdd
                remaining -= canAdd
            }
        }

        for (i in 0 until container.containerSize) {
            if (remaining <= 0) break
            if (!container.getItem(i).isEmpty) continue
            val put = minOf(stack.maxStackSize, remaining)
            container.setItem(i, stack.copyWithCount(put))
            remaining -= put
        }

        if (stack.count != remaining) {
            container.setChanged()
        }
        return stack.count - remaining
    }

    // Получение списка предметов: get_items() -> { {slot=..., item=...}, ... } (slot с 1)
    private inner class GetItemsFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val result = tableOf()
            var index = 1
            for (slot in 1..container.containerSize) {
                val stack = container.getItem(slot - 1)
                if (stack.isEmpty) continue
                val entry = tableOf()
                entry.set("slot", valueOf(slot))
                entry.set("item", LuaItemStack(stack))
                result.set(index++, entry)
            }
            return result
        }
    }

    // Выдача предмета: give_item(itemstack[, count]) -> сколько фактически добавлено
    private inner class GiveItemFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            val source = toStack(arg1) ?: return NIL
            if (source.isEmpty) return valueOf(0)
            if (arg2?.isnumber() == true) {
                source.count = arg2.toint()
            }
            if (source.isEmpty) return valueOf(0)
            return valueOf(addToContainer(source))
        }
    }

    // Убирание предметов: remove_item(slot[, count]) или remove_item(itemstack|identifier[, amount]) -> сколько удалено
    private inner class RemoveItemFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            // Удаление из конкретного слота
            if (arg1?.isnumber() == true) {
                val slot = arg1.toint()
                if (slot < 1 || slot > container.containerSize) return valueOf(0)
                val stack = container.getItem(slot - 1)
                if (stack.isEmpty) return valueOf(0)
                val count = if (arg2?.isnumber() == true) arg2.toint() else stack.count
                val removed = container.removeItem(slot - 1, count)
                container.setChanged()
                return valueOf(removed.count)
            }

            // Удаление по предмету/идентификатору
            val lookupStack = if (arg1 != null && arg1.isItem()) arg1.toItem() else null
            val identifier = when {
                lookupStack != null ->
                    BuiltInRegistries.ITEM.getKey(lookupStack.item).toString()
                arg1?.isstring() == true -> arg1.tojstring()
                else -> return NIL
            }
            var remaining = if (arg2?.isnumber() == true) arg2.toint() else 1
            if (remaining <= 0) return valueOf(0)

            var removedTotal = 0
            for (slot in 0 until container.containerSize) {
                if (remaining <= 0) break
                val stack = container.getItem(slot)
                if (stack.isEmpty) continue
                if (BuiltInRegistries.ITEM.getKey(stack.item).toString() != identifier) continue
                val take = minOf(remaining, stack.count)
                stack.count -= take
                if (stack.isEmpty) {
                    container.setItem(slot, ItemStack.EMPTY)
                }
                remaining -= take
                removedTotal += take
            }
            if (removedTotal > 0) {
                container.setChanged()
            }
            return valueOf(removedTotal)
        }
    }

    // Установка предмета в слот: set_item(slot, itemstack|nil)
    private inner class SetItemFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            if (arg1?.isnumber() != true) return NIL
            return setSlot(arg1.toint(), arg2)
        }
    }

    override fun typename(): String = "inventory"
    override fun tojstring(): String = "LuaInventory(size=${container.containerSize})"
}
