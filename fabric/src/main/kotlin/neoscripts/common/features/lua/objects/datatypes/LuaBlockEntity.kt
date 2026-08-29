package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.Container
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JavaInstance

class LuaBlockEntity(var blockEntity: BlockEntity) : LuaUserdata(blockEntity) {

    private fun container(): Container? = blockEntity as? Container

    /**
     * Поля печки (lit time / cook progress) лежат в защищённом поле dataAccess,
     * поэтому достаем их через рефлексию по иерархии классов.
     */
    private fun furnaceData(): ContainerData? {
        var clazz: Class<*>? = blockEntity.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField("dataAccess")
                field.isAccessible = true
                return field.get(blockEntity) as? ContainerData
            } catch (e: Exception) {
                clazz = clazz.superclass
            }
        }
        return null
    }

    override fun get(key: LuaValue): LuaValue {
        val c = container()
        val furnaceData = furnaceData()
        val state = blockEntity.blockState

        return when (val field = key.tojstring()) {
            "javaClass", "class" -> JavaInstance(blockEntity);
            "blockstate", "blockState", "block" -> LuaBlockState(state);
            "x" -> valueOf(blockEntity.blockPos.x)
            "y" -> valueOf(blockEntity.blockPos.y)
            "z" -> valueOf(blockEntity.blockPos.z)
            "pos", "blockpos", "blockPos" -> LuaBlockPos(blockEntity.blockPos)
            "type", "identifier", "entity_type" ->
                valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.wrapAsHolder(blockEntity.type).registeredName)
            "is_container", "has_inventory" -> valueOf(c != null)
            "size" -> {
                if (c != null) valueOf(c.containerSize) else NIL
            }
            "inventory" -> {
                if (c != null) LuaInventory(c) else NIL
            }

            // --- Свойства печки ---
            "is_lit", "lit", "is_burning" -> {
                if (furnaceData != null) {
                    valueOf(furnaceData.get(AbstractFurnaceBlockEntity.DATA_LIT_TIME) > 0)
                } else {
                    val lit = state.getOptionalValue(BlockStateProperties.LIT)
                    valueOf(lit.isPresent && lit.get())
                }
            }
            "burn_time", "burntime", "fuel" -> {
                if (furnaceData == null) return NIL
                valueOf(furnaceData.get(AbstractFurnaceBlockEntity.DATA_LIT_TIME))
            }
            "burn_duration", "burn_duration_total", "fuel_duration" -> {
                if (furnaceData == null) return NIL
                valueOf(furnaceData.get(AbstractFurnaceBlockEntity.DATA_LIT_DURATION))
            }
            "cook_time", "cooking_progress" -> {
                if (furnaceData == null) return NIL
                valueOf(furnaceData.get(AbstractFurnaceBlockEntity.DATA_COOKING_PROGRESS))
            }
            "cook_total_time", "cook_time_total", "cooking_total_time" -> {
                if (furnaceData == null) return NIL
                valueOf(furnaceData.get(AbstractFurnaceBlockEntity.DATA_COOKING_TOTAL_TIME))
            }
            else -> super.get(key)
        }
    }

    override fun set(key: LuaValue, value: LuaValue) {
        val c = container()

        // Доступ по индексу слота: blockentity[3] = itemstack
        if (c != null && key.isnumber()) {
            val index = key.toint()
            if (index in 1..c.containerSize) {
                applySlot(c, index - 1, value)
                return
            }
        }

        when (val field = key.tojstring()) {
            "inventory" -> {
                val cont = c ?: return
                if (!value.istable()) return
                val len = value.length()
                for (i in 1..minOf(len, cont.containerSize)) {
                    applySlot(cont, i - 1, value.get(i))
                }
            }

            // --- Свойства печки ---
            "burn_time", "burntime", "fuel" -> {
                val data = furnaceData() ?: return
                if (value.isnumber()) {
                    data.set(AbstractFurnaceBlockEntity.DATA_LIT_TIME, value.toint().coerceAtLeast(0))
                }
            }
            "burn_duration", "burn_duration_total", "fuel_duration" -> {
                val data = furnaceData() ?: return
                if (value.isnumber()) {
                    data.set(AbstractFurnaceBlockEntity.DATA_LIT_DURATION, value.toint().coerceAtLeast(0))
                }
            }
            "cook_time", "cooking_progress" -> {
                val data = furnaceData() ?: return
                if (value.isnumber()) {
                    data.set(AbstractFurnaceBlockEntity.DATA_COOKING_PROGRESS, value.toint().coerceAtLeast(0))
                }
            }
            "cook_total_time", "cook_time_total", "cooking_total_time" -> {
                val data = furnaceData() ?: return
                if (value.isnumber()) {
                    data.set(AbstractFurnaceBlockEntity.DATA_COOKING_TOTAL_TIME, value.toint().coerceAtLeast(0))
                }
            }
            "is_lit", "lit", "is_burning" -> {
                val data = furnaceData() ?: return
                if (value.isboolean()) {
                    data.set(
                        AbstractFurnaceBlockEntity.DATA_LIT_TIME,
                        if (value.toboolean()) data.get(AbstractFurnaceBlockEntity.DATA_LIT_DURATION).coerceAtLeast(200) else 0
                    )
                }
            }
            else -> super.set(key, value)
        }

        blockEntity.setChanged()
    }

    private fun applySlot(c: Container, slot: Int, value: LuaValue) {
        when {
            value.isnil() || value == null -> c.setItem(slot, ItemStack.EMPTY)
            value is LuaItemStack -> {
                c.setItem(slot, value.stack.copy())
            }
            else -> return
        }
        c.setChanged()
    }

    override fun typename(): String = "blockentity"
}
