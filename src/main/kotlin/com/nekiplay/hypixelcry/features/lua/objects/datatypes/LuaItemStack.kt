package com.nekiplay.hypixelcry.features.lua.objects.datatypes

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
            "is_empty" -> valueOf(stack.isEmpty)
            "head_texture" -> valueOf(ItemUtils.getHeadTexture(stack))
            "skyblock_id" -> valueOf(ItemUtils.getItemId(stack))
            "reforge_modifier" -> valueOf(ItemUtils.getReforgeModifier(stack))
            "is_stackable" -> valueOf(stack.isStackable)
            "is_recombobulated" -> valueOf(ItemUtils.isRecombobulated(stack))
            "is_museum_donated" -> valueOf(ItemUtils.isMuseumDonated(stack))
            "lore" -> {
                val loreTable = tableOf()
                val loreList = ItemUtils.getLore(stack)
                loreList.forEachIndexed { index, line -> loreTable.set(index + 1, line.string) }
                loreTable
            }
            "enchantments", "enchantments" -> {
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
            else -> super.get(key)  // fallback
        }
    }

    fun getItemStack(): ItemStack = stack
}