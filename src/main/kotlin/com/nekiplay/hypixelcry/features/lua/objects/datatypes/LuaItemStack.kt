package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import com.nekiplay.hypixelcry.HypixelCry.mc
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import com.nekiplay.hypixelcry.sugar.*
import com.nekiplay.hypixelcry.utils.ItemUtils
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.component.ItemLore
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaItemStack(L: Lua, val stack: ItemStack) : SimpleLuaWrapper(L) {
    override fun push(): LuaValue {
        val luaValue = super.push()

        if (L.getMetatable(-1) != 0) {
            L.push(JFunction { l ->
                l.push(stack.item.name.string)
                1
            })
            L.setField(-2, "__tostring")
            L.pop(1)
        }

        return luaValue
    }
    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "count" -> stack.count.toDouble()
            "max_count" -> stack.maxStackSize.toDouble()
            "name" -> stack.item.name.string
            "display_name" -> stack.getDisplayName().getFormattedString()
            "is_empty" -> stack.isEmpty
            "head_texture" -> stack.getHeadTexture()
            "skyblock_id" -> stack.getItemId()
            "neu_id" -> stack.getNeuId()
            "reforge_modifier" -> stack.getReforgeModifier()
            "is_stackable" -> stack.isStackable
            "is_recombobulated" -> stack.isRecombobulated()
            "is_museum_donated" -> stack.isMuseumDonated()
            "is_enchanted" -> stack.isEnchanted
            "uuid" -> stack.getItemUuid()

            "map" -> {
                if (stack.item is MapItem && mc.level != null) {
                    val level = mc.level ?: return null
                    val mapData = MapItem.getSavedData(stack, level);
                    if (mapData != null) {
                        LuaMapData(l, mapData)
                    }
                }
                null
            }

            "lore", "lores" -> {
                l.newTable()
                val loreList = ItemUtils.getLore(stack)
                loreList.forEachIndexed { index, line ->
                    l.push(line.string)
                    l.rawSetI(-2, index + 1)
                }
                l.get()
            }
            "enchantments", "ench" -> {
                // 1. Создаем главную таблицу (список зачарований)
                l.newTable()

                val enchantmentsList = ItemUtils.getHypixelEnchantments(stack)
                var index = 1

                enchantmentsList.forEach { (id, level) ->
                    // 2. Создаем под-таблицу для конкретного зачарования
                    l.newTable()

                    // Наполняем под-таблицу (она сейчас на индексе -1)
                    l.push(id)
                    l.setField(-2, "name")

                    l.push(level)
                    l.setField(-2, "level")

                    // 3. Кладем под-таблицу (-1) в главную таблицу (-2)
                    // Метод rawSetI заберет под-таблицу со стека и положит в главную
                    l.rawSetI(-2, index)

                    index++
                }

                // 4. Возвращаем главную таблицу
                l.get()
            }
            else -> null
        }
    }

    override fun setFieldValue(l: Lua, key: String, value: LuaValue): Boolean {
        if (stack.isEmpty) return false

        when (key) {
            "display_name" -> {
                if (value.type() != Lua.LuaType.NIL) {
                    val name = Component.literal(value.toString())
                    stack.set(DataComponents.CUSTOM_NAME, name)
                } else {
                    stack.remove(DataComponents.CUSTOM_NAME)
                }
            }
            "count" -> {
                val count = value.toInteger().toInt()
                if (count in 1..stack.maxStackSize) {
                    stack.count = count
                }
            }
            "lore", "lores" -> {
                setLore(value)
            }
            else -> return false
        }
        return true
    }

    private fun setLore(loreValue: LuaValue) {
        val loreLines = mutableListOf<Component>()

        // Проверка на таблицу
        if (loreValue.type() == Lua.LuaType.TABLE) {
            var index = 1
            while (true) {
                val currentValue = loreValue.get(index)
                if (currentValue.type() == Lua.LuaType.NIL) break

                val line = currentValue.toString()
                loreLines.add(Component.literal(line))
                index++
            }
        }

        if (loreLines.isNotEmpty()) {
            stack.set(DataComponents.LORE, ItemLore(loreLines))
        } else {
            stack.remove(DataComponents.LORE)
        }
    }
}