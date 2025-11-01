package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import com.nekiplay.hypixelcry.sugar.*
import com.nekiplay.hypixelcry.utils.ItemUtils
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaItemStack(val stack: ItemStack) : LuaUserdata(stack) {

    override fun get(key: LuaValue): LuaValue {
        if (stack.isEmpty) return LuaValue.NIL

        return when (val field = key.tojstring()) {
            "count" -> valueOf(stack.count.toDouble())
            "max_count" -> valueOf(stack.maxCount.toDouble())
            "name" -> valueOf(stack.item.name.string)
            "display_name" -> valueOf(stack.getDisplayName().getFormattedString())
            "is_empty" -> valueOf(stack.isEmpty)
            "head_texture" -> valueOf(stack.getHeadTexture())
            "skyblock_id" -> valueOf(stack.getItemId())
            "neu_id" -> valueOf(stack.getNeuId())
            "reforge_modifier" -> valueOf(stack.getReforgeModifier())
            "is_stackable" -> valueOf(stack.isStackable)
            "is_recombobulated" -> valueOf(stack.isRecombobulated())
            "is_museum_donated" -> valueOf(stack.isMuseumDonated())
            "uuid" -> valueOf(stack.getItemUuid())

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

    override fun set(key: LuaValue, value: LuaValue) {
        if (stack.isEmpty) return

        when (val field = key.tojstring()) {
            "display_name" -> {
                if (!value.isnil()) {
                    val name = Text.literal(value.tojstring())
                    stack.setDisplayName(name)
                } else {
                    // Удаление пользовательского имени
                    stack.remove(DataComponentTypes.CUSTOM_NAME)
                }
            }
            "count" -> {
                val count = value.toint()
                if (count in 1..stack.maxCount) {
                    stack.count = count
                }
            }
            "lore", "lores" -> {
                setLore(value)
            }
            else -> super.set(key, value)
        }
    }

    private fun setLore(loreValue: LuaValue) {
        val loreLines = mutableListOf<Text>()
        val styledLines = mutableListOf<Text>()

        if (loreValue.istable()) {
            var index = 1
            while (true) {
                val currentValue = loreValue.get(index)
                if (currentValue.isnil()) {
                    break
                }

                val loreLine = currentValue.tojstring()
                val textLine = Text.literal(loreLine)

                loreLines.add(textLine)
                styledLines.add(textLine)

                index++
            }
        }

        if (loreLines.isNotEmpty()) {
            val loreComponent = LoreComponent(loreLines, styledLines)
            stack.set(DataComponentTypes.LORE, loreComponent)
        } else {
            stack.remove(DataComponentTypes.LORE)
        }
    }


    fun getItemStack(): ItemStack = stack
}