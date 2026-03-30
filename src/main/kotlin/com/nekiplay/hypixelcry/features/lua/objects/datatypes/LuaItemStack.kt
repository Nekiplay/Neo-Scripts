package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import com.nekiplay.hypixelcry.HypixelCry.mc
import com.nekiplay.hypixelcry.sugar.*
import com.nekiplay.hypixelcry.utils.ItemUtils
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.block.Block
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaItemStack(val stack: ItemStack) : LuaUserdata(stack) {

    override fun get(key: LuaValue): LuaValue {
        if (stack.isEmpty) return LuaValue.NIL

        return when (val field = key.tojstring()) {
            "count" -> valueOf(stack.count.toDouble())
            "max_count" -> valueOf(stack.maxStackSize.toDouble())
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
            "is_enchanted" -> valueOf(stack.isEnchanted)
            "uuid" -> valueOf(stack.getItemUuid())

            "map" -> {
                if (stack.item is MapItem && mc.level != null) {
                    val level = mc.level ?: return NIL
                    val mapData = MapItem.getSavedData(stack, level);
                    if (mapData != null) {
                        return LuaMapData(mapData)
                    }
                }
                NIL
            }
            "blockstate" -> {
                val block = Block.byItem(stack.item)
                return if (block != null) {
                    LuaBlockState(block.defaultBlockState())
                }
                else {
                    NIL
                }
            }

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
                    val name = Component.literal(value.tojstring())
                    stack.setDisplayName(name)
                } else {
                    // Удаление пользовательского имени
                    stack.remove(DataComponents.CUSTOM_NAME)
                }
            }
            "count" -> {
                val count = value.toint()
                if (count in 1..stack.maxStackSize) {
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
        val loreLines = mutableListOf<Component>()
        val styledLines = mutableListOf<Component>()

        if (loreValue.istable()) {
            var index = 1
            while (true) {
                val currentValue = loreValue.get(index)
                if (currentValue.isnil()) {
                    break
                }

                val loreLine = currentValue.tojstring()
                val textLine = Component.literal(loreLine)

                loreLines.add(textLine)
                styledLines.add(textLine)

                index++
            }
        }

        if (loreLines.isNotEmpty()) {
            val loreComponent = ItemLore(loreLines, styledLines)
            stack.set(DataComponents.LORE, loreComponent)
        } else {
            stack.remove(DataComponents.LORE)
        }
    }
}