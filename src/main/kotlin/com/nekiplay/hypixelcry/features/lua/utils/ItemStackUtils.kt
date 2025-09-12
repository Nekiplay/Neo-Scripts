package com.nekiplay.hypixelcry.features.lua.utils

import com.nekiplay.hypixelcry.utils.ItemUtils
import net.minecraft.item.ItemStack
import org.luaj.vm2.LuaValue

object ItemStackUtils {
    // Функция для преобразования ItemStack в Lua таблицу
    fun ToLua(itemStack: ItemStack?): LuaValue? {
        if (itemStack == null || itemStack.isEmpty) {
            return LuaValue.NIL
        }

        val table = LuaValue.tableOf()

        // Основная информация о предмете
        table.set("is_empty", LuaValue.valueOf(itemStack.isEmpty))
        table.set("count", LuaValue.valueOf(itemStack.count.toDouble()))
        table.set("max_count", LuaValue.valueOf(itemStack.maxCount.toDouble()))
        table.set("name", LuaValue.valueOf(itemStack.item.name.string))
        table.set("name", LuaValue.valueOf(itemStack.item.name.string))

        table.set("head_texture", LuaValue.valueOf(ItemUtils.getHeadTexture(itemStack)))
        table.set("skyblock_id", LuaValue.valueOf(ItemUtils.getItemId(itemStack)))

        table.set("reforge_modifier", LuaValue.valueOf(ItemUtils.getReforgeModifier(itemStack)))

        // Дополнительные свойства
        table.set("is_stackable", LuaValue.valueOf(itemStack.isStackable))
        table.set("is_recombobulated", LuaValue.valueOf(ItemUtils.isRecombobulated(itemStack)))
        table.set("is_museum_donated", LuaValue.valueOf(ItemUtils.isMuseumDonated(itemStack)))

        val loreTable = LuaValue.tableOf();
        val loreList = ItemUtils.getLore(itemStack)
        loreList.forEachIndexed { index, line ->
            loreTable.set(index + 1, line.string)
        }
        table.set("lore",loreTable )

        val enchantsTable = LuaValue.tableOf();
        val enchantmentsList = ItemUtils.getHypixelEnchantments(itemStack)
        var index = 1
        enchantmentsList.forEach { (id, level) ->
            val enchantTable = LuaValue.tableOf();
            enchantTable.set("name", id)
            enchantTable.set("level", level)

            enchantsTable.set(index, enchantTable)
            index++
        }
        table.set("enchantmets", enchantsTable)
        return table
    }
}