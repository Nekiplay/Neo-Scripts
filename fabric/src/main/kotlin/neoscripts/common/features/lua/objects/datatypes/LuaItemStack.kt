package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import com.nekiplay.neoscripts.ClientMain.mc
import com.nekiplay.neoscripts.client.sugar.getFormattedString
import com.nekiplay.neoscripts.client.sugar.getHeadTexture
import com.nekiplay.neoscripts.client.sugar.getItemId
import com.nekiplay.neoscripts.client.sugar.getItemUuid
import com.nekiplay.neoscripts.client.sugar.getNeuId
import com.nekiplay.neoscripts.client.sugar.getReforgeModifier
import com.nekiplay.neoscripts.client.sugar.isMuseumDonated
import com.nekiplay.neoscripts.client.sugar.isRecombobulated
import com.nekiplay.neoscripts.client.sugar.setDisplayName
import com.nekiplay.neoscripts.client.sugar.toBlock
import com.nekiplay.neoscripts.client.utils.ItemUtils
import com.nekiplay.neoscripts.client.utils.Utils
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongArrayTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.component.ResolvableProfile
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import com.google.common.collect.LinkedHashMultimap
import java.util.UUID
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
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaLong
import org.luaj.vm2.LuaNumber
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JavaInstance

class LuaItemStack(val stack: ItemStack) : LuaUserdata(stack) {

    override fun get(key: LuaValue): LuaValue {
        if (stack.isEmpty) return NIL

        return when (val field = key.tojstring()) {
            "javaClass", "class" -> JavaInstance(stack);
            "count" -> valueOf(stack.count.toDouble())
            "max_count" -> valueOf(stack.maxStackSize.toDouble())
            "name" -> valueOf(stack.item.getName(stack).getFormattedString())
            "id" -> valueOf(BuiltInRegistries.ITEM.getId(stack.item))
            "identifier" -> valueOf(stack.item.toString())
            "translation_id" -> valueOf(stack.item.descriptionId)
            "display_name" -> valueOf(stack.displayName.getFormattedString())
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
            "is_correct_tool" -> isCorrectToolForDrops()
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
                    if (element is net.minecraft.nbt.Tag) {
                        valueOf(element.toString())
                    } else {
                        valueOf(element.asString().orElse(""))
                    }
                } else {
                    println("NBT encode error: ${errors.joinToString(", ")}")
                    valueOf("")
                }
            }
            "profile" -> {
                val rp = stack.get(DataComponents.PROFILE)
                if (rp == null) {
                    NIL
                } else {
                    val gp = rp.partialProfile()
                    val t = tableOf()
                    val idArr = tableOf()
                    gp.id?.let {
                        idArr.set(1, valueOf((it.mostSignificantBits shr 32).toInt()))
                        idArr.set(2, valueOf(it.mostSignificantBits.toInt()))
                        idArr.set(3, valueOf((it.leastSignificantBits shr 32).toInt()))
                        idArr.set(4, valueOf(it.leastSignificantBits.toInt()))
                    }
                    t.set("id", idArr)
                    t.set("name", valueOf(gp.name ?: ""))
                    val propsT = tableOf()
                    var idx = 1
                    for (entry in gp.properties.entries()) {
                        val prop = entry.value
                        val p = tableOf()
                        p.set("name", valueOf(prop.name()))
                        p.set("value", valueOf(prop.value()))
                        p.set("signature", valueOf(prop.signature() ?: ""))
                        propsT.set(idx++, p)
                    }
                    t.set("properties", propsT)
                    t
                }
            }
            else -> getUnknownData(field, key)
        }
    }

    private fun getUnknownData(field: String, key: LuaValue): LuaValue {
        return try {
            val tag = ItemUtils.getCustomData(stack)
            val value = tag.get(field) ?: return super.get(key)
            nbtTagToLua(value)
        } catch (e: Exception) {
            super.get(key)
        }
    }

    private fun nbtTagToLua(tag: Tag): LuaValue = when (tag) {
        is CompoundTag -> {
            val table = tableOf()
            tag.keySet().forEach { tagKey ->
                tag.get(tagKey)?.let { table.set(valueOf(tagKey), nbtTagToLua(it)) }
            }
            table
        }
        is ListTag -> {
            val table = tableOf()
            for (i in 0 until tag.size) {
                table.set(i + 1, nbtTagToLua(tag[i]))
            }
            table
        }
        is ByteArrayTag -> {
            val table = tableOf()
            tag.asByteArray.forEachIndexed { i, v -> table.set(i + 1, valueOf(v.toInt())) }
            table
        }
        is IntArrayTag -> {
            val table = tableOf()
            tag.asIntArray.forEachIndexed { i, v -> table.set(i + 1, valueOf(v)) }
            table
        }
        is LongArrayTag -> {
            val table = tableOf()
            tag.asLongArray.forEachIndexed { i, v -> table.set(i + 1, valueOf(v)) }
            table
        }
        is StringTag -> valueOf(tag.asString().orElse(""))
        is NumericTag -> valueOf(tag.doubleValue())
        else -> NIL
    }

    override fun eq(other: LuaValue?): LuaValue {
        return when (other) {
            is LuaItemStack if stack == other.stack -> {
                LuaValue.TRUE
            }
            is LuaInteger if BuiltInRegistries.ITEM.getId(stack.item) == other.toint() -> {
                LuaValue.TRUE
            }
            is LuaNumber if BuiltInRegistries.ITEM.getId(stack.item) == other.toint() -> {
                LuaValue.TRUE
            }
            is LuaLong if BuiltInRegistries.ITEM.getId(stack.item) == other.toint() -> {
                LuaValue.TRUE
            }
            is LuaDouble if BuiltInRegistries.ITEM.getId(stack.item) == other.toint() -> {
                LuaValue.TRUE
            }
            else -> {
                LuaValue.FALSE
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
            "profile" -> {
                // value: { id = {i1,i2,i3,i4}, properties = { {name=, value=}, ... } }
                // Инлайн-профиль с textures-свойством без подписи; миксин
                // YggdrasilSessionServiceMixin доверяет таким свойствам, если URL
                // указывает на официальный домен textures.minecraft.net.
                if (value.istable()) {
                    val v = value.checktable()
                    val idT = v.get("id")
                    val uuid = if (idT.istable()) {
                        val i1 = idT.get(1).toint().toLong() and 0xFFFFFFFFL
                        val i2 = idT.get(2).toint().toLong() and 0xFFFFFFFFL
                        val i3 = idT.get(3).toint().toLong() and 0xFFFFFFFFL
                        val i4 = idT.get(4).toint().toLong() and 0xFFFFFFFFL
                        val high = (i1 shl 32) or i2
                        val low = (i3 shl 32) or i4
                        UUID(high, low)
                    } else {
                        UUID.randomUUID()
                    }
                    val multimap = LinkedHashMultimap.create<String, Property>()
                    val propsT = v.get("properties")
                    if (propsT.istable()) {
                        var idx = 1
                        while (true) {
                            val p = propsT.get(idx)
                            if (p.isnil()) break
                            val pname = p.get("name").tojstring()
                            val pval = p.get("value").tojstring()
                            if (pname.isNotEmpty() && pval.isNotEmpty()) {
                                multimap.put(pname, Property(pname, pval, ""))
                            }
                            idx++
                        }
                    }
                    stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(GameProfile(uuid, "", PropertyMap(multimap))))
                }
            }
            else -> setCustomVariable(field, value)
        }
    }

    /**
     * Произвольные пользовательские переменные предмета хранятся в компоненте
     * CUSTOM_DATA и автоматически синхронизируются между сервером и клиентом
     * вместе с ItemStack. Чтение выполняется через getUnknownData.
     */
    private fun setCustomVariable(field: String, value: LuaValue) {
        val tag = ItemUtils.getCustomData(stack).copy()

        if (value.isnil()) {
            tag.remove(field)
        } else {
            val nbt = luaToNbt(value) ?: return
            tag.put(field, nbt)
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    }

    /**
     * Конвертирует Lua-значение в NBT-тег:
     * boolean -> ByteTag, целое число -> IntTag, дробное -> DoubleTag,
     * строка -> StringTag, таблица -> ListTag (последовательные ключи 1..n)
     * или CompoundTag (остальные таблицы).
     */
    private fun luaToNbt(value: LuaValue): Tag? = when {
        value.isboolean() -> ByteTag.valueOf(value.toboolean())
        value.isint() -> IntTag.valueOf(value.toint())
        value.isnumber() -> DoubleTag.valueOf(value.todouble())
        value.isstring() -> StringTag.valueOf(value.tojstring())
        value.istable() -> {
            val t = value.checktable()
            val len = t.length()
            var isList = len > 0
            val list = ListTag()
            if (isList) {
                for (i in 1..len) {
                    val element = t.get(i)
                    if (element.isnil()) {
                        isList = false
                        break
                    }
                    val converted = luaToNbt(element)
                    if (converted == null) {
                        isList = false
                        break
                    }
                    list.add(converted)
                }
            }
            if (isList) {
                list
            } else {
                val compound = CompoundTag()
                var k: LuaValue = LuaValue.NIL
                while (true) {
                    val entry = t.next(k)
                    k = entry.arg(1)
                    if (k.isnil()) break
                    luaToNbt(entry.arg(2))?.let { compound.put(k.tojstring(), it) }
                }
                compound
            }
        }
        else -> null
    }

    private inner class isCorrectToolForDrops : OneArgFunction() {
        override fun call(block: LuaValue): LuaValue {
            val blockState = block.toBlock()
            if (blockState != null) {
                return valueOf(stack.isCorrectToolForDrops(blockState))
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