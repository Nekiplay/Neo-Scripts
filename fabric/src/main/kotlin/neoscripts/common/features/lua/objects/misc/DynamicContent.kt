package com.nekiplay.neoscripts.common.features.lua.objects.misc

import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaContentSettings
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

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

    // Путь к файлу текстуры по id предмета/блока
    // ("neoscripts:my_item" -> "config/neoscripts/textures/my_item.png")
    val textureOverrides = ConcurrentHashMap<String, String>()

    // Загруженные из файлов текстуры: rawId -> Identifier DynamicTexture
    private val loadedTextures = ConcurrentHashMap<String, Identifier>()

    fun getTextureOverride(rawId: String): String? = textureOverrides[rawId]

    /**
     * Возвращает Identifier текстуры предмета/блока, загружая файл с диска
     * при первом обращении (тот же механизм, что и 2d renderer renderImage:
     * File -> NativeImage.read -> DynamicTexture -> textureManager.register).
     * Клиентский API: на выделенном сервере вернет null.
     */
    fun getDynamicTexture(rawId: String): Identifier? {
        val path = textureOverrides[rawId] ?: return null
        loadedTextures[rawId]?.let { return it }

        return try {
            val file = File(path)
            if (!file.exists() || !file.isFile) return null

            FileInputStream(file).use { inputStream ->
                val nativeImage = NativeImage.read(inputStream)
                val name = "neoscripts:dynamic_${rawId.replace(':', '_')}"
                val texture = DynamicTexture(Supplier { name }, nativeImage)
                val identifier = Identifier.parse(name)
                ClientMain.mc.textureManager.register(identifier, texture)
                loadedTextures[rawId] = identifier
                identifier
            }
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to load dynamic texture for $rawId from $path", e)
            null
        }
    }

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
                val props = settings?.applyTo(BlockBehaviour.Properties.of()) ?: BlockBehaviour.Properties.of()
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
