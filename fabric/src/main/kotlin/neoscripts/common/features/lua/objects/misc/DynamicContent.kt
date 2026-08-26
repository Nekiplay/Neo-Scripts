package com.nekiplay.neoscripts.common.features.lua.objects.misc

import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaContentSettings
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Динамическая регистрация предметов и блоков из Lua-скриптов.
 *
 * Ванильные реестры замораживаются после загрузки игры (а скрипты автозагрузки
 * выполняются позже — в Minecraft.onGameLoadFinished), поэтому перед регистрацией
 * реестры временно размораживаются через рефлексию (поле MappedRegistry.frozen,
 * подтверждено для 26.2) и замораживаются обратно.
 *
 * Зарегистрированные предметы доступны сразу во всех стандартных API
 * (items.get, /give на интегрированном сервере и т.д.). Ванильная модель не
 * создается — предмет отображается стандартной моделью, но имя можно задать
 * через LuaContentSettings, а путь текстуры сохранить в textureOverrides
 * для использования рендером или скриптами.
 */
object DynamicContent {

    // Защита от повторной регистрации при повторном запуске автозагрузки
    // (повторный вход в мир на интегрированном сервере)
    private val knownIds = ConcurrentHashMap.newKeySet<String>()

    // Путь к текстуре по id предмета/блока ("neoscripts:my_item" -> "neoscripts:textures/item/my_item.png")
    val textureOverrides = ConcurrentHashMap<String, String>()

    fun getTextureOverride(rawId: String): String? = textureOverrides[rawId]

    private fun setFrozen(frozen: Boolean) {
        val registries = listOf(BuiltInRegistries.BLOCK, BuiltInRegistries.ITEM)
        for (registry in registries) {
            try {
                var cls: Class<*>? = registry.javaClass
                var done = false
                while (cls != null && !done) {
                    val field = cls.declaredFields.firstOrNull {
                        it.type == Boolean::class.javaPrimitiveType &&
                            !Modifier.isStatic(it.modifiers) &&
                            (it.name == "frozen" || it.name == "locked")
                    }
                    if (field != null) {
                        field.isAccessible = true
                        field.setBoolean(registry, frozen)
                        done = true
                    } else {
                        cls = cls.superclass
                    }
                }
                if (!done) {
                    ClientMain.LOGGER?.warn("[Neo Scripts] Frozen flag field not found for registry $registry")
                }
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("[Neo Scripts] Failed to set frozen=$frozen for registry $registry", e)
            }
        }
    }

    fun isRegistered(type: String, rawId: String): Boolean {
        return knownIds.contains("$type:$rawId")
    }

    /**
     * Регистрирует простой предмет. Возвращает зарегистрированный Item или null.
     */
    fun registerItem(rawId: String, settings: LuaContentSettings? = null): Item? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("item:$rawId")) {
                val existing = BuiltInRegistries.ITEM.get(id)
                if (existing.isPresent) return existing.get().value()
            }

            settings?.texture?.let { textureOverrides[rawId] = it }

            setFrozen(false)
            try {
                val props = settings?.applyTo(Item.Properties()) ?: Item.Properties()
                val customName = settings?.displayName()

                val item = if (customName != null) {
                    object : Item(props) {
                        override fun getName(stack: net.minecraft.world.item.ItemStack) = customName
                    }
                } else {
                    Item(props)
                }

                Registry.register(BuiltInRegistries.ITEM, id, item)
                knownIds.add("item:$rawId")
                ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic item $rawId")
                item
            } finally {
                setFrozen(true)
            }
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic item $rawId", e)
            null
        }
    }

    /**
     * Регистрирует блок. Возвращает зарегистрированный Block или null.
     */
    fun registerBlock(rawId: String, settings: LuaContentSettings? = null): Block? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("block:$rawId")) {
                val existing = BuiltInRegistries.BLOCK.get(id)
                if (existing.isPresent) return existing.get().value()
            }

            settings?.texture?.let { textureOverrides[rawId] = it }

            setFrozen(false)
            try {
                val props = BlockBehaviour.Properties.of()
                val customName = settings?.displayName()

                val block = if (customName != null) {
                    object : Block(props) {
                        override fun getName() = customName
                    }
                } else {
                    Block(props)
                }

                Registry.register(BuiltInRegistries.BLOCK, id, block)
                knownIds.add("block:$rawId")
                ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic block $rawId")
                block
            } finally {
                setFrozen(true)
            }
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic block $rawId", e)
            null
        }
    }

    /**
     * Регистрирует предмет-блок (BlockItem) для уже зарегистрированного блока.
     */
    fun registerBlockItem(rawId: String, block: Block, settings: LuaContentSettings? = null): Item? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("item:$rawId")) {
                val existing = BuiltInRegistries.ITEM.get(id)
                if (existing.isPresent) return existing.get().value()
            }

            settings?.texture?.let { textureOverrides[rawId] = it }

            setFrozen(false)
            try {
                val props = settings?.applyTo(Item.Properties()) ?: Item.Properties()
                val customName = settings?.displayName()

                val item = if (customName != null) {
                    object : BlockItem(block, props) {
                        override fun getName(stack: net.minecraft.world.item.ItemStack) = customName
                    }
                } else {
                    BlockItem(block, props)
                }

                Registry.register(BuiltInRegistries.ITEM, id, item)
                knownIds.add("item:$rawId")
                ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic block item $rawId")
                item
            } finally {
                setFrozen(true)
            }
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic block item $rawId", e)
            null
        }
    }
}
