package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import com.nekiplay.hypixelcry.sugar.getFormattedString
import com.nekiplay.hypixelcry.utils.ItemUtils
import net.minecraft.item.ItemStack
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaItemStack(val stack: ItemStack) : LuaUserdata(stack) {

    override fun get(key: LuaValue): LuaValue {
        if (stack.isEmpty) return LuaValue.NIL

        return when (val field = key.tojstring()) {
            "count" -> valueOf(stack.count.toDouble())
            "max_count" -> valueOf(stack.maxCount.toDouble())
            "name" -> valueOf(stack.item.name.string)
            "display_name" -> valueOf(ItemUtils.getDisplayName(stack).getFormattedString())
            "is_empty" -> valueOf(stack.isEmpty)
            "head_texture" -> valueOf(ItemUtils.getHeadTexture(stack))
            "skyblock_id" -> valueOf(ItemUtils.getItemId(stack))
            "reforge_modifier" -> valueOf(ItemUtils.getReforgeModifier(stack))
            "is_stackable" -> valueOf(stack.isStackable)
            "is_recombobulated" -> valueOf(ItemUtils.isRecombobulated(stack))
            "is_museum_donated" -> valueOf(ItemUtils.isMuseumDonated(stack))
            "uuid" -> valueOf(ItemUtils.getItemUuid(stack))

            "lore", "lores" -> {
                val loreTable = tableOf()
                val loreList = ItemUtils.getLore(stack)
                loreList.forEachIndexed { index, line -> loreTable.set(index + 1, line.string) }
                loreTable
            }
            "enchantments", "ench" -> {
                val enchantsTable = tableOf()
                val enchantmentsList = ItemUtils.getHypixelEnchantments(stack)
                var index = 1
                enchantmentsList.forEach { (id, level) ->
                    val enchantTable = tableOf()
                    enchantTable.set("name", id)
                    enchantTable.set("level", level)
                    enchantsTable.set(index, enchantTable)
                    index++
                }
                enchantsTable
            }
            else -> super.get(key)
        }
    }

    fun getItemStack(): ItemStack = stack
}