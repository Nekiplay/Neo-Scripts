package com.nekiplay.neoscripts.common.features.lua.objects.misc

import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaContentSettings
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.BlockTags
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ToolMaterial
import net.minecraft.world.item.component.Consumables
import net.minecraft.world.item.component.Tool
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import java.io.File
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

    // Путь к файлу текстуры по id предмета/блока
    // ("neoscripts:my_item" -> "config/neoscripts/textures/my_item.png")
    val textureOverrides = ConcurrentHashMap<String, String>()

    // Содержимое PNG-файлов по id — отдается рантайм ресурспаком клиенту
    val textureData = ConcurrentHashMap<String, ByteArray>()

    fun getTextureOverride(rawId: String): String? = textureOverrides[rawId]

    /**
     * Сохраняет путь и читает содержимое файла текстуры для ресурспака.
     * Вызывается при регистрации (до загрузки ресурсов — поэтому файл
     * попадает в первый же бейк моделей).
     */
    fun storeTexture(rawId: String, path: String) {
        textureOverrides[rawId] = path
        try {
            val file = File(path)
            if (file.exists() && file.isFile) {
                textureData[rawId] = file.readBytes()
            } else {
                ClientMain.LOGGER?.warn("[Neo Scripts] Texture file not found for $rawId: $path")
            }
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to read texture for $rawId from $path", e)
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
     * Регистрирует предмет по паттерну Fabric API:
     * ResourceKey.create + Properties.setId(key) + Registry.register.
     * Работает напрямую из скриптов автозапуска (onInitialize, реестр ещё не
     * заморожен). Если реестр уже заморожен (запуск через /slua в рантайме) —
     * временно размораживает через рефлексию и регистрирует всё равно.
     * Возвращает зарегистрированный Item или null.
     */
    fun registerItem(rawId: String, settings: LuaContentSettings? = null): Item? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("item:$rawId")) {
                val existing = BuiltInRegistries.ITEM.get(id)
                if (existing.isPresent) return existing.get().value()
            }

            settings?.texture?.let { storeTexture(rawId, it) }

            val key = ResourceKey.create(Registries.ITEM, id)
            val props = settings?.applyTo(Item.Properties())?.setId(key) ?: Item.Properties().setId(key)
            val customName = settings?.displayName()

            val item = if (customName != null) {
                object : Item(props) {
                    override fun getName(stack: net.minecraft.world.item.ItemStack) = customName
                }
            } else {
                Item(props)
            }

            registerWithFreezeFallback(BuiltInRegistries.ITEM as Registry<Item>) { Registry.register(BuiltInRegistries.ITEM, key, item) }
            knownIds.add("item:$rawId")
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic item $rawId")
            item
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic item $rawId", e)
            null
        }
    }

    /**
     * Регистрирует блок по паттерну Fabric API. Возвращает Block или null.
     */
    fun registerBlock(rawId: String, settings: LuaContentSettings? = null): Block? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("block:$rawId")) {
                val existing = BuiltInRegistries.BLOCK.get(id)
                if (existing.isPresent) return existing.get().value()
            }

            settings?.texture?.let { storeTexture(rawId, it) }

            val key = ResourceKey.create(Registries.BLOCK, id)
            val props = settings?.applyTo(BlockBehaviour.Properties.of())?.setId(key)
                ?: BlockBehaviour.Properties.of().setId(key)
            val customName = settings?.displayName()

            val block = if (customName != null) {
                object : Block(props) {
                    override fun getName() = customName
                }
            } else {
                Block(props)
            }

            registerWithFreezeFallback(BuiltInRegistries.BLOCK as Registry<Block>) { Registry.register(BuiltInRegistries.BLOCK, key, block) }
            knownIds.add("block:$rawId")
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic block $rawId")
            block
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic block $rawId", e)
            null
        }
    }

    fun buildFoodProperties(nutrition: Int, saturation: Float, alwaysEdible: Boolean): FoodProperties {
        val builder = FoodProperties.Builder()
            .nutrition(nutrition)
            .saturationModifier(saturation)
        if (alwaysEdible) builder.alwaysEdible()
        return builder.build()
    }

    fun parseFoodTable(table: org.luaj.vm2.LuaValue?): FoodProperties? {
        if (table == null || !table.istable()) return null
        val nutrition = if (table.get("nutrition").isnumber()) table.get("nutrition").toint() else return null
        val saturation = if (table.get("saturation").isnumber()) table.get("saturation").tofloat() else 0.6f
        val alwaysEdible = table.get("alwaysEdible").optboolean(false) || table.get("always_edible").optboolean(false)
        return buildFoodProperties(nutrition, saturation, alwaysEdible)
    }

    /**
     * Регистрирует еду: Item с FoodProperties (+ дефолтный Consumable).
     */
    fun registerFood(rawId: String, settings: LuaContentSettings? = null, food: FoodProperties? = null): Item? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("item:$rawId")) {
                val existing = BuiltInRegistries.ITEM.get(id)
                if (existing.isPresent) return existing.get().value()
            }
            settings?.texture?.let { storeTexture(rawId, it) }
            val key = ResourceKey.create(Registries.ITEM, id)
            var props = settings?.applyTo(Item.Properties())?.setId(key) ?: Item.Properties().setId(key)
            if (food != null) props = props.food(food)
            val customName = settings?.displayName()
            val item = if (customName != null) {
                object : Item(props) {
                    override fun getName(stack: net.minecraft.world.item.ItemStack) = customName
                }
            } else {
                Item(props)
            }
            registerWithFreezeFallback(BuiltInRegistries.ITEM as Registry<Item>) { Registry.register(BuiltInRegistries.ITEM, key, item) }
            knownIds.add("item:$rawId")
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic food $rawId")
            item
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic food $rawId", e)
            null
        }
    }

    /**
     * Регистрирует напиток: Item с FoodProperties + Consumables.DEFAULT_DRINK (анимация питья).
     */
    fun registerDrink(rawId: String, settings: LuaContentSettings? = null, food: FoodProperties? = null): Item? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("item:$rawId")) {
                val existing = BuiltInRegistries.ITEM.get(id)
                if (existing.isPresent) return existing.get().value()
            }
            settings?.texture?.let { storeTexture(rawId, it) }
            val key = ResourceKey.create(Registries.ITEM, id)
            var props = settings?.applyTo(Item.Properties())?.setId(key) ?: Item.Properties().setId(key)
            if (food != null) props = props.food(food, Consumables.DEFAULT_DRINK)
            else props = props.food(FoodProperties.Builder().nutrition(0).saturationModifier(0f).build(), Consumables.DEFAULT_DRINK)
            val customName = settings?.displayName()
            val item = if (customName != null) {
                object : Item(props) {
                    override fun getName(stack: net.minecraft.world.item.ItemStack) = customName
                }
            } else {
                Item(props)
            }
            registerWithFreezeFallback(BuiltInRegistries.ITEM as Registry<Item>) { Registry.register(BuiltInRegistries.ITEM, key, item) }
            knownIds.add("item:$rawId")
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic drink $rawId")
            item
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic drink $rawId", e)
            null
        }
    }

    /**
     * Регистрирует предмет-блок (BlockItem) для блока.
     */
    fun registerBlockItem(rawId: String, block: Block, settings: LuaContentSettings? = null): Item? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("item:$rawId")) {
                val existing = BuiltInRegistries.ITEM.get(id)
                if (existing.isPresent) return existing.get().value()
            }

            settings?.texture?.let { storeTexture(rawId, it) }

            val key = ResourceKey.create(Registries.ITEM, id)
            val props = settings?.applyTo(Item.Properties())?.setId(key) ?: Item.Properties().setId(key)
            val customName = settings?.displayName()

            val item = if (customName != null) {
                object : BlockItem(block, props) {
                    override fun getName(stack: net.minecraft.world.item.ItemStack) = customName
                }
            } else {
                BlockItem(block, props)
            }

            registerWithFreezeFallback(BuiltInRegistries.ITEM as Registry<Item>) { Registry.register(BuiltInRegistries.ITEM, key, item) }
            knownIds.add("item:$rawId")
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic block item $rawId")
            item
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic block item $rawId", e)
            null
        }
    }

    fun parseToolMaterial(name: String?): ToolMaterial = when (name?.lowercase()) {
        "wood", "wooden" -> ToolMaterial.WOOD
        "stone" -> ToolMaterial.STONE
        "copper" -> ToolMaterial.COPPER
        "iron" -> ToolMaterial.IRON
        "diamond" -> ToolMaterial.DIAMOND
        "gold", "golden" -> ToolMaterial.GOLD
        "netherite" -> ToolMaterial.NETHERITE
        else -> ToolMaterial.IRON
    }

    fun registerTool(rawId: String, settings: LuaContentSettings? = null, toolTable: org.luaj.vm2.LuaValue? = null): Item? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("item:$rawId")) {
                val existing = BuiltInRegistries.ITEM.get(id)
                if (existing.isPresent) return existing.get().value()
            }
            settings?.texture?.let { storeTexture(rawId, it) }
            val key = ResourceKey.create(Registries.ITEM, id)
            var props = settings?.applyTo(Item.Properties())?.setId(key) ?: Item.Properties().setId(key)
            val type = toolTable?.get("type")?.tojstring()?.lowercase() ?: "pickaxe"
            val material = parseToolMaterial(toolTable?.get("material")?.tojstring())
            val damage = toolTable?.get("damage")?.takeIf { it.isnumber() }?.tofloat() ?: 1f
            val speed = toolTable?.get("speed")?.takeIf { it.isnumber() }?.tofloat() ?: -2.8f
            props = when (type) {
                "pickaxe", "pick" -> props.pickaxe(material, damage, speed)
                "axe" -> props.axe(material, damage, speed)
                "shovel" -> props.shovel(material, damage, speed)
                "hoe" -> props.hoe(material, damage, speed)
                "sword" -> props.sword(material, damage, speed)
                "paxel" -> {
                    // Мульти-инструмент: комбинирует pickaxe/axe/shovel/hoe
                    val lookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK)
                    val rules = listOf(
                        Tool.Rule.deniesDrops(lookup.getOrThrow(material.incorrectBlocksForDrops())),
                        Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), speed),
                        Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_AXE), speed),
                        Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), speed),
                        Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_HOE), speed)
                    )
                    val paxelTool = Tool(rules, 1.0f, 1, true)
                    // Сначала получаем атрибуты/компоненты через любой tool, затем заменяем Tool
                    val tmp = props.pickaxe(material, damage, speed)
                    tmp.component(DataComponents.TOOL, paxelTool)
                }
                else -> props.pickaxe(material, damage, speed)
            }
            val customName = settings?.displayName()
            val item = if (customName != null) {
                object : Item(props) {
                    override fun getName(stack: net.minecraft.world.item.ItemStack) = customName
                }
            } else {
                Item(props)
            }
            registerWithFreezeFallback(BuiltInRegistries.ITEM as Registry<Item>) { Registry.register(BuiltInRegistries.ITEM, key, item) }
            knownIds.add("item:$rawId")
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic tool $rawId type=$type")
            item
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic tool $rawId", e)
            null
        }
    }

    /**
     * Пытается зарегистрировать напрямую; если реестр уже заморожен
     * (скрипт запущен не в onInitialize, а позже — /slua и т.п.) —
     * размораживает реестр через рефлексию и повторяет попытку.
     */
    private inline fun registerWithFreezeFallback(registry: Registry<*>, crossinline action: () -> Unit) {
        try {
            action()
        } catch (e: IllegalStateException) {
            setFrozen(false)
            try {
                action()
            } finally {
                setFrozen(true)
            }
        }
    }
}
