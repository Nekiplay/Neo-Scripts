package com.nekiplay.hypixelcry.features.lua.utils.block

import net.minecraft.item.ItemStack
import org.luaj.vm2.LuaValue

object ItemStackUtils {
    // Функция для преобразования ItemStack в Lua таблицу
    public fun ToLua(itemStack: ItemStack?): LuaValue? {
        if (itemStack == null || itemStack.isEmpty) {
            return LuaValue.NIL
        }

        val table = LuaValue.tableOf()

        // Основная информация о предмете
        table.set("is_empty", LuaValue.valueOf(itemStack.isEmpty))
        table.set("count", LuaValue.valueOf(itemStack.count.toDouble()))
        table.set("max_count", LuaValue.valueOf(itemStack.maxCount.toDouble()))
        table.set("name", LuaValue.valueOf(itemStack.item.name.string))

        // Дополнительные свойства
        table.set("is_damageable", LuaValue.valueOf(itemStack.isDamageable))
        table.set("is_stackable", LuaValue.valueOf(itemStack.isStackable))
        table.set("is_enchantable", LuaValue.valueOf(itemStack.isEnchantable))

        // Если предмет имеет повреждения
        if (itemStack.isDamageable) {
            table.set("damage", LuaValue.valueOf(itemStack.damage.toDouble()))
            table.set("max_damage", LuaValue.valueOf(itemStack.maxDamage.toDouble()))
        }
        return table
    }
}