package com.nekiplay.neoscripts.features.lua.objects.datatypes

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.sugar.getFormattedString
import com.nekiplay.neoscripts.sugar.getHeadTexture
import com.nekiplay.neoscripts.sugar.getItemId
import com.nekiplay.neoscripts.sugar.getItemUuid
import com.nekiplay.neoscripts.sugar.getReforgeModifier
import com.nekiplay.neoscripts.sugar.isMuseumDonated
import com.nekiplay.neoscripts.sugar.isRecombobulated
import com.nekiplay.neoscripts.sugar.setDisplayName
import com.nekiplay.neoscripts.utils.ItemUtils
import com.nekiplay.neoscripts.utils.Utils
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.InstrumentItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MaceItem
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.ShearsItem
import net.minecraft.world.item.ShieldItem
import net.minecraft.world.item.TridentItem
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaLong
import org.luaj.vm2.LuaNumber
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

class LuaItemStack(val stack: ItemStack) : LuaUserdata(stack) {

    override fun get(key: LuaValue): LuaValue {
        if (stack.isEmpty) return NIL

        return when (val field = key.tojstring()) {
            "count" -> valueOf(stack.count.toDouble())
            "max_count" -> valueOf(stack.maxStackSize.toDouble())
            "name" -> valueOf(stack.item.name.getFormattedString())
            "id" -> valueOf(BuiltInRegistries.ITEM.getId(stack.item))
            "identifier" -> valueOf(stack.item.toString())
            "translation_id" -> valueOf(stack.item.descriptionId)
            "display_name" -> valueOf(stack.displayName.getFormattedString())
            "is_empty" -> valueOf(stack.isEmpty)
            "head_texture" -> valueOf(stack.getHeadTexture())
            "skyblock_id" -> valueOf(stack.getItemId())
            "reforge_modifier" -> valueOf(stack.getReforgeModifier())
            "is_stackable" -> valueOf(stack.isStackable)
            "is_recombobulated" -> valueOf(stack.isRecombobulated())
            "is_museum_donated" -> valueOf(stack.isMuseumDonated())
            "is_enchanted" -> valueOf(stack.isEnchanted)
            "uuid" -> valueOf(stack.getItemUuid())
            "is_сorrect_tool" -> isCorrectToolForDrops()
            "is_sword" -> {
                valueOf(stack.`is`(ItemTags.SWORDS))
            }
            "is_pickaxe" -> {
                valueOf(stack.`is`(ItemTags.PICKAXES))
            }
            "is_axe" -> {
                valueOf(stack.`is`(ItemTags.AXES))
            }
            "is_hoe" -> {
                valueOf(stack.`is`(ItemTags.HOES))
            }
            "is_shovel" -> {
                valueOf(stack.`is`(ItemTags.SHOVELS))
            }
            "is_map" -> {
                valueOf(stack.item is MapItem)
            }
            "is_trident" -> {
                valueOf(stack.item is TridentItem)
            }
            "is_instrument" -> {
                valueOf(stack.item is InstrumentItem)
            }
            "is_shield" -> {
                valueOf(stack.item is ShieldItem)
            }
            "is_shears" -> {
                valueOf(stack.item is ShearsItem)
            }
            "is_mace" -> {
                valueOf(stack.item is MaceItem)
            }
            "is_fishing_rod" -> {
                valueOf(stack.item is FishingRodItem)
            }
            "is_block" -> {
                valueOf(stack.item is BlockItem)
            }

            "map" -> {
                if (stack.item is MapItem && mc?.level != null) {
                    val level = mc?.level ?: return NIL
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
                loreList.forEachIndexed { index, line -> loreTable.set(index + 1, line.getFormattedString()) }
                loreTable
            }
            "hypixel_enchantments", "hypixel_ench" -> {
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
            "enchantments", "ench" -> {
                val enchantsTable = tableOf()
                val enchantments = stack.get(DataComponents.ENCHANTMENTS)
                if (enchantments != null) {
                    var index = 1
                    enchantments.entrySet().forEach { entry ->
                        val enchantHolder = entry.key
                        val level = entry.intValue
                        val enchantTable = tableOf()
                        val enchantName = enchantHolder.registeredName
                        enchantTable.set("name", valueOf(enchantName))
                        enchantTable.set("level", level)
                        enchantsTable.set(index, enchantTable)
                        index++
                    }
                }
                enchantsTable
            }
            "nbt" -> {
                val registryLookup = Utils.getRegistryWrapperLookup()
                val ops = registryLookup.createSerializationContext(NbtOps.INSTANCE)
                val result = ItemStack.CODEC.encodeStart(ops, stack)
                
                // Получаем результат с обработкой ошибок
                val errors = mutableListOf<String>()
                val nbtElement = result.resultOrPartial { error -> errors.add(error) }
                
                if (nbtElement.isPresent) {
                    val element = nbtElement.get()
                    // Element может быть CompoundTag напрямую
                    if (element is Tag) {
                        valueOf(element.toString())
                    } else {
                        valueOf(element.asString().orElse(""))
                    }
                } else {
                    println("NBT encode error: ${errors.joinToString(", ")}")
                    valueOf("")
                }
            }
            else -> super.get(key)
        }
    }

    override fun eq(other: LuaValue?): LuaValue {
        return when (other) {
            is LuaItemStack if stack == other.stack -> {
                TRUE
            }
            is ItemStack if stack == other -> {
                TRUE
            }
            is LuaInteger if BuiltInRegistries.ITEM.getId(stack.item) == other.toint() -> {
                TRUE
            }
            is LuaNumber if BuiltInRegistries.ITEM.getId(stack.item) == other.toint() -> {
                TRUE
            }
            is LuaLong if BuiltInRegistries.ITEM.getId(stack.item) == other.toint() -> {
                TRUE
            }
            is LuaDouble if BuiltInRegistries.ITEM.getId(stack.item) == other.toint() -> {
                TRUE
            }
            else -> {
                FALSE
            }
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

    private inner class isCorrectToolForDrops : OneArgFunction() {
        override fun call(block: LuaValue): LuaValue {
            if (block.touserdata() is BlockState) {
                val blockState = block.touserdata() as BlockState
                return valueOf(stack.isCorrectToolForDrops(blockState))
            }
            else if (block.touserdata() is LuaBlockState) {
                val blockState = block.touserdata() as LuaBlockState
                return valueOf(stack.isCorrectToolForDrops(blockState.blockState))
            }
            return FALSE
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

    override fun typename(): String = "itemstack"
}