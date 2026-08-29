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
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.FenceBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.fabricmc.loader.api.FabricLoader
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

    // Родительская модель (Identifier строкой), например "minecraft:block/cube_all"
    // или короткая запись "minecraft:diamond_block" (будет развёрнута в паке),
    // или путь к JSON-файлу ("config/neoscripts/models/foo.json" -> bytes)
    val modelOverrides = ConcurrentHashMap<String, String>()
    // Байты кастомной JSON-модели, если model указал путь к файлу
    val modelData = ConcurrentHashMap<String, ByteArray>()
    val modelFilePath = ConcurrentHashMap<String, String>()

    // Тип блока-варианта: "slab","stairs","door","trapdoor","fence"
    val blockTypes = ConcurrentHashMap<String, String>()
    val stairsBase = ConcurrentHashMap<String, String>() // stairs rawId -> base block rawId
    val doorBlockSetType = ConcurrentHashMap<String, String>()
    // Кастомные VoxelShape для коллизии/очертания (поддержка высоты >1 блока: 0..32, 16=1 блок)
    val blockShapes = ConcurrentHashMap<String, VoxelShape>()
    // Множественные текстуры для кастомных моделей: rawId -> (textureKey -> bytes)
    val multiTextureData = ConcurrentHashMap<String, MutableMap<String, ByteArray>>()
    // Теги добычи: инструмент и уровень (для requiresCorrectToolForDrops)
    val blockMineableTool = ConcurrentHashMap<String, String>() // rawId -> "pickaxe"|"axe"|...
    val blockMiningTier = ConcurrentHashMap<String, String>() // rawId -> "stone"|"iron"|"diamond"|...
    // Лут блока: какие предметы выпадают (https://docs.fabricmc.net/develop/blocks/first-block#adding-block-drops)
    // nil = не задано (дефолт дроп себя), пустой список = ничего, иначе список DropEntry
    val blockDrops = ConcurrentHashMap<String, MutableList<LuaContentSettings.DropEntry>>()
    // Генерация руды (https://wiki.fabricmc.net/tutorial:ores)
    val oreGens = ConcurrentHashMap<String, LuaContentSettings.OreConfig>() // featureId -> config (+ хранит blockId внутри)
    val oreBlockToFeature = ConcurrentHashMap<String, String>() // block rawId -> featureId
    // Теги предметов/блоков для рецептов (https://docs.fabricmc.net/develop/data-generation/tags)
    val itemTags = ConcurrentHashMap<String, MutableSet<String>>() // tagId -> set of item rawIds
    val blockTags = ConcurrentHashMap<String, MutableSet<String>>() // tagId -> set of block rawIds
    // Рецепты (https://wiki.fabricmc.net/tutorial:recipes)
    val recipes = ConcurrentHashMap<String, String>() // recipeId -> json

    private fun normalizeTool(name: String?): String? {
        if (name == null) return null
        return when (name.lowercase()) {
            "pick", "pickaxe" -> "pickaxe"
            "axe", "ax" -> "axe"
            "shovel", "spade" -> "shovel"
            "hoe" -> "hoe"
            "sword" -> "sword"
            else -> null
        }
    }
    private fun normalizeTier(name: String?): String? {
        if (name == null) return null
        return when (name.lowercase()) {
            "wood", "wooden" -> "wood"
            "stone" -> "stone"
            "iron" -> "iron"
            "diamond" -> "diamond"
            "netherite" -> "netherite"
            "gold", "golden" -> "gold"
            else -> null
        }
    }
    private fun storeToolTier(rawId: String, settings: LuaContentSettings?) {
        val tool = normalizeTool(settings?.mineableTool)
        val tier = normalizeTier(settings?.miningTier)
        if (tool != null) blockMineableTool[rawId] = tool else blockMineableTool.remove(rawId)
        if (tier != null) blockMiningTier[rawId] = tier else blockMiningTier.remove(rawId)
    }

    private fun storeDrops(rawId: String, settings: LuaContentSettings?) {
        val drops = settings?.drops
        if (drops != null) {
            // явная настройка (включая пустой список = ничего не дропает)
            blockDrops[rawId] = drops.toMutableList()
        } else {
            // дефолт: блок дропает себя (как в Fabric доке: loot_table с name=self)
            blockDrops[rawId] = mutableListOf(LuaContentSettings.DropEntry(rawId))
        }
    }

    fun getBlockDrops(rawId: String): List<LuaContentSettings.DropEntry>? = blockDrops[rawId]

    /**
     * Позволяет изменить дроп уже зарегистрированного блока в рантайме (для Lua API).
     * Если drops==null -> сбросить к дефолту (себя), пустой список -> ничего.
     */
    fun setBlockDrops(rawId: String, drops: List<LuaContentSettings.DropEntry>?) {
        if (drops == null) {
            blockDrops[rawId] = mutableListOf(LuaContentSettings.DropEntry(rawId))
        } else {
            blockDrops[rawId] = drops.toMutableList()
        }
    }

    fun buildBlockLootJson(rawId: String): String {
        val drops = blockDrops[rawId] ?: listOf(LuaContentSettings.DropEntry(rawId))
        if (drops.isEmpty()) return """{"type":"minecraft:block","pools":[]}"""
        fun entryJson(d: LuaContentSettings.DropEntry): String {
            val func = when {
                d.countMin != null && d.countMax != null -> """{"function":"minecraft:set_count","count":{"min":${d.countMin}.0,"max":${d.countMax}.0,"type":"minecraft:uniform"}}"""
                d.count != 1 -> """{"function":"minecraft:set_count","count":${d.count}}"""
                else -> null
            }
            val funcStr = if (func != null) ""","functions":[$func]""" else ""
            val weightStr = d.weight?.let { ""","weight":$it""" } ?: ""
            val condStr = d.conditions?.takeIf { it.isNotEmpty() }?.let { conds ->
                val joined = conds.joinToString(",")
                ""","conditions":[$joined]"""
            } ?: ""
            return """{"type":"minecraft:item","name":"${d.id}"$weightStr$funcStr$condStr}"""
        }
        // если хотя бы у одного указан weight — делаем один pool со взвешенным выбором (minecraft.wiki/w/Loot_table#weight)
        // иначе каждый дроп — отдельный pool (все выпадают гарантированно, как в Fabric доке)
        val hasWeight = drops.any { it.weight != null }
        return if (hasWeight) {
            val entries = drops.joinToString(",") { entryJson(it) }
            """{"type":"minecraft:block","pools":[{"rolls":1,"entries":[$entries]}]}"""
        } else {
            val pools = drops.joinToString(",") { d ->
                """{"rolls":1,"entries":[${entryJson(d)}]}"""
            }
            """{"type":"minecraft:block","pools":[$pools]}"""
        }
    }

    // ── tags (https://docs.fabricmc.net/develop/data-generation/tags) ──
    private fun storeItemTags(rawId: String, settings: LuaContentSettings?) {
        // сначала очистим старые записи этого предмета
        itemTags.values.forEach { it.remove(rawId) }
        val list = settings?.tags ?: return
        for (tag in list) {
            try { Identifier.parse(tag); itemTags.getOrPut(tag) { ConcurrentHashMap.newKeySet() }.add(rawId) } catch (_: Exception) {}
        }
    }
    private fun storeBlockTags(rawId: String, settings: LuaContentSettings?) {
        blockTags.values.forEach { it.remove(rawId) }
        // blockTags явно или fallback tags для блоков
        val list = settings?.blockTags ?: settings?.tags
        if (list == null) return
        for (tag in list) {
            try { Identifier.parse(tag); blockTags.getOrPut(tag) { ConcurrentHashMap.newKeySet() }.add(rawId) } catch (_: Exception) {}
        }
    }

    // ── recipes (https://wiki.fabricmc.net/tutorial:recipes) ──
    fun registerRecipe(rawId: String, json: String): Boolean {
        return try {
            Identifier.parse(rawId)
            if (json.isBlank()) return false
            // базовая валидация json
            if (!json.trim().startsWith("{")) return false
            recipes[rawId] = json
            ClientMain.LOGGER?.info("[Neo Scripts] Registered recipe $rawId")
            true
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register recipe $rawId", e)
            false
        }
    }
    fun getRecipe(rawId: String): String? = recipes[rawId]
    fun removeRecipe(rawId: String): Boolean = recipes.remove(rawId) != null

    // ── ore generation (https://wiki.fabricmc.net/tutorial:ores) ──
    private fun storeOre(rawId: String, settings: LuaContentSettings?) {
        val ore = settings?.ore ?: return
        // featureId по умолчанию = rawId
        val fid = ore.featureId?.takeIf { it.contains(":") } ?: rawId
        // обновляем ore.featureId для консистентности
        ore.featureId = fid
        registerOreInternal(rawId, ore)
    }

    fun getOre(rawId: String): LuaContentSettings.OreConfig? {
        val fid = oreBlockToFeature[rawId] ?: rawId
        return oreGens[fid] ?: oreGens[rawId]
    }

    fun registerOre(rawId: String, cfg: LuaContentSettings.OreConfig): Boolean {
        return try {
            val fid = cfg.featureId?.takeIf { it.contains(":") } ?: rawId
            cfg.featureId = fid
            registerOreInternal(rawId, cfg)
            true
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register ore $rawId", e)
            false
        }
    }

    private fun registerOreInternal(rawId: String, cfg: LuaContentSettings.OreConfig) {
        // валидация blockId
        Identifier.parse(rawId)
        val fid = cfg.featureId ?: rawId
        Identifier.parse(fid)
        oreGens[fid] = cfg.copy(featureId = fid)
        oreBlockToFeature[rawId] = fid
        // BiomeModifications — вызовется в onInitialize и позже; если уже инициализирован — пробуем сразу
        try {
            val placedKey = ResourceKey.create(Registries.PLACED_FEATURE, Identifier.parse(fid))
            val selector = when (cfg.biomes.lowercase()) {
                "overworld", "overworlds", "is_overworld" -> net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld()
                "nether", "is_nether", "foundInNether" -> net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInNether()
                "end", "is_end", "foundInTheEnd" -> net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInTheEnd()
                else -> if (cfg.biomes.startsWith("#")) {
                    val tagId = Identifier.parse(cfg.biomes.removePrefix("#"))
                    val tag = net.minecraft.tags.TagKey.create(Registries.BIOME, tagId)
                    net.fabricmc.fabric.api.biome.v1.BiomeSelectors.tag(tag)
                } else {
                    net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld()
                }
            }
            net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
                selector, net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES, placedKey
            )
            ClientMain.LOGGER?.info("[Neo Scripts] Registered ore $rawId feature=$fid biomes=${cfg.biomes} count=${cfg.count} size=${cfg.size} y=${cfg.minY}..${cfg.maxY}")
        } catch (e: Exception) {
            // если Biome API еще не готов (ранний вызов) — просто логируем, JSON уже сгенерит feature; повторная регистрация произойдет при следующем вызове
            ClientMain.LOGGER?.warn("[Neo Scripts] BiomeModifications addFeature deferred for $fid: ${e.message}")
        }
    }

    fun buildOreConfiguredJson(featureId: String): String {
        val cfg = oreGens[featureId] ?: return """{"type":"minecraft:ore","config":{"size":9,"discard_chance_on_air_exposure":0.0,"targets":[]}}"""
        val blockId = oreBlockToFeature.entries.find { it.value == featureId }?.key ?: featureId
        // targets по replace
        val targetsJson = buildOreTargets(blockId, cfg.replace, cfg.biomes)
        return """{"type":"minecraft:ore","config":{"discard_chance_on_air_exposure":${cfg.discardChance},"size":${cfg.size},"targets":[$targetsJson]}}"""
    }

    fun buildOrePlacedJson(featureId: String): String {
        val cfg = oreGens[featureId] ?: return """{"feature":"$featureId","placement":[]}"""
        val heightType = if (cfg.heightType == "uniform") "minecraft:uniform" else "minecraft:trapezoid"
        val heightJson = """{"type":"minecraft:height_range","height":{"type":"$heightType","min_inclusive":{"absolute":${cfg.minY}},"max_inclusive":{"absolute":${cfg.maxY}}}}"""
        val placement = mutableListOf<String>()
        placement.add("""{"type":"minecraft:count","count":${cfg.count}}""")
        placement.add("""{"type":"minecraft:in_square"}""")
        placement.add(heightJson)
        placement.add("""{"type":"minecraft:biome"}""")
        return """{"feature":"$featureId","placement":[${placement.joinToString(",")}]}"""
    }

    private fun buildOreTargets(blockId: String, replace: String?, biomes: String): String {
        val state = """{"Name":"$blockId"}"""
        fun tagTarget(tag: String) = """{"state":$state,"target":{"predicate_type":"minecraft:tag_match","tag":"$tag"}}"""
        fun blockTarget(block: String) = """{"state":$state,"target":{"predicate_type":"minecraft:block_match","block":"$block"}}"""
        if (replace != null && replace.isNotBlank()) {
            val r = replace.trim()
            // если указано несколько через запятую — генерим несколько таргетов
            if (r.contains(",")) {
                return r.split(",").joinToString(",") { part ->
                    val p = part.trim()
                    if (p.startsWith("minecraft:") && (p.endsWith("_ore_replaceables") || p.startsWith("#"))) {
                        val tag = p.removePrefix("#")
                        tagTarget(tag)
                    } else if (p.contains(":")) {
                        blockTarget(p)
                    } else {
                        tagTarget("minecraft:${p}")
                    }
                }
            }
            if (r.equals("stone", true) || r.equals("stone_ore_replaceables", true) || r == "minecraft:stone_ore_replaceables" || r == "#minecraft:stone_ore_replaceables") return tagTarget("minecraft:stone_ore_replaceables")
            if (r.equals("deepslate", true) || r.equals("deepslate_ore_replaceables", true) || r == "minecraft:deepslate_ore_replaceables") return tagTarget("minecraft:deepslate_ore_replaceables")
            if (r.equals("netherrack", true) || r == "minecraft:netherrack") return blockTarget("minecraft:netherrack")
            if (r.equals("end_stone", true) || r == "minecraft:end_stone") return blockTarget("minecraft:end_stone")
            if (r.startsWith("#")) return tagTarget(r.removePrefix("#"))
            if (r.contains(":")) {
                // считаем tag если содержит ore_replaceables иначе block
                return if (r.contains("ore_replaceables")) tagTarget(r) else blockTarget(r)
            }
            // короткое имя — считаем tag
            return tagTarget("minecraft:$r")
        }
        // дефолт по biomes
        return when (biomes.lowercase()) {
            "nether" -> blockTarget("minecraft:netherrack")
            "end" -> blockTarget("minecraft:end_stone")
            else -> "${tagTarget("minecraft:stone_ore_replaceables")},${tagTarget("minecraft:deepslate_ore_replaceables")}"
        }
    }

    fun getTextureOverride(rawId: String): String? = textureOverrides[rawId]
    fun getModelOverride(rawId: String): String? = modelOverrides[rawId]
    fun getBlockType(rawId: String): String? = blockTypes[rawId]

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

    fun storeModel(rawId: String, modelId: String) {
        val trimmed = modelId.trim()
        if (trimmed.isEmpty()) return

        // 1. Путь к файлу JSON? — проверяем File и gameDir-relative путь
        val isJsonPath = trimmed.endsWith(".json", ignoreCase = true)
        val directFile = File(trimmed)
        val gameDirFile = FabricLoader.getInstance().gameDir.resolve(trimmed).toFile()

        val fileToRead: File? = when {
            isJsonPath && directFile.exists() && directFile.isFile -> directFile
            isJsonPath && gameDirFile.exists() && gameDirFile.isFile -> gameDirFile
            !isJsonPath && directFile.exists() && directFile.isFile -> directFile
            !isJsonPath && gameDirFile.exists() && gameDirFile.isFile -> gameDirFile
            else -> null
        }

        if (fileToRead != null) {
            try {
                modelData[rawId] = fileToRead.readBytes()
                modelFilePath[rawId] = fileToRead.absolutePath
                // Убираем возможный старый identifier-оверрайд
                modelOverrides.remove(rawId)
                ClientMain.LOGGER?.info("[Neo Scripts] Stored custom model file for $rawId from ${fileToRead.absolutePath}")
                return
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("[Neo Scripts] Failed to read model file for $rawId from ${fileToRead.absolutePath}", e)
            }
        }

        // 2. Identifier модели (например "minecraft:block/cube_all" или "minecraft:diamond_block")
        //    Короткая запись без '/' (например "minecraft:diamond_block") будет развёрнута в паке
        try {
            // Валидация: либо содержит ":"/"/", либо считаем как path без namespace
            if (trimmed.contains(":") || trimmed.contains("/")) {
                // Если содержит '/', парсим как есть; если только ':', парсим как short id
                Identifier.parse(trimmed)
            } else {
                // Без namespace — считаем minecraft:path
                Identifier.parse("minecraft:$trimmed")
                modelOverrides[rawId] = "minecraft:$trimmed"
                return
            }
            modelOverrides[rawId] = trimmed
            modelData.remove(rawId)
            modelFilePath.remove(rawId)
        } catch (e: Exception) {
            ClientMain.LOGGER?.warn("[Neo Scripts] Invalid model id for $rawId: $trimmed")
        }
    }

    /**
     * Сохраняет несколько текстур для кастомной модели (Blockbench).
     * textures — таблица key -> file path (например { "0" = "path/tex0.png", "particle" = "path/particle.png" })
     */
    fun storeTextures(rawId: String, textures: Map<String, String>) {
        val id = Identifier.parse(rawId)
        val ns = id.namespace
        val path = id.path
        val map = mutableMapOf<String, ByteArray>()
        for ((key, texPath) in textures) {
            try {
                val file = File(texPath)
                if (file.exists() && file.isFile) {
                    map[key] = file.readBytes()
                } else {
                    ClientMain.LOGGER?.warn("[Neo Scripts] Multi-texture file not found for $rawId key=$key: $texPath")
                }
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("[Neo Scripts] Failed to read multi-texture for $rawId key=$key from $texPath", e)
            }
        }
        if (map.isNotEmpty()) {
            multiTextureData[rawId] = map
        }
    }

    fun buildVoxelShape(boxes: List<DoubleArray>): VoxelShape {
        if (boxes.isEmpty()) return Shapes.block()
        var shape: VoxelShape = Shapes.empty()
        for (b in boxes) {
            if (b.size != 6) continue
            // clamp к -16..32 для поддержки 2+ блоков высоты, но Block.box требует 0..16, расширяем через Shapes?
            // Block.box работает с 0..16, но для >16 используем directly Shapes.box с нормализованными 0..1 (делим на 16)
            // Чтобы поддержать 0..32, делаем нормализацию: делим на 16.
            // Используем Block.box для 0..16, иначе Shapes.box.
            val boxShape = try {
                if (b[0] >= 0 && b[1] >= 0 && b[2] >= 0 && b[3] <= 16 && b[4] <= 16 && b[5] <= 16) {
                    Block.box(b[0], b[1], b[2], b[3], b[4], b[5])
                } else {
                    // нормализованные координаты 0..1 (32 → 2.0)
                    Shapes.box(b[0]/16.0, b[1]/16.0, b[2]/16.0, b[3]/16.0, b[4]/16.0, b[5]/16.0)
                }
            } catch (_: Exception) { Shapes.empty() }
            shape = Shapes.or(shape, boxShape)
        }
        return if (shape.isEmpty) Shapes.block() else shape
    }

    fun getBlockShape(rawId: String): VoxelShape? = blockShapes[rawId]

    private fun createVoxelBlock(props: BlockBehaviour.Properties, shape: VoxelShape, customName: net.minecraft.network.chat.MutableComponent?): Block {
        return if (customName != null) {
            object : Block(props) {
                override fun getName() = customName
                override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape = shape
                override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape = shape
                override fun getOcclusionShape(state: BlockState): VoxelShape = shape
                override fun getVisualShape(state: BlockState, level: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape = shape
                override fun getBlockSupportShape(state: BlockState, getter: BlockGetter, pos: BlockPos): VoxelShape = shape
            }
        } else {
            object : Block(props) {
                override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape = shape
                override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape = shape
                override fun getOcclusionShape(state: BlockState): VoxelShape = shape
                override fun getVisualShape(state: BlockState, level: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape = shape
                override fun getBlockSupportShape(state: BlockState, getter: BlockGetter, pos: BlockPos): VoxelShape = shape
            }
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

    fun getKnownIds(): Set<String> = knownIds
    fun isBlockRegistered(rawId: String): Boolean = knownIds.contains("block:$rawId")
    fun isItemRegistered(rawId: String): Boolean = knownIds.contains("item:$rawId")

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
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeItemTags(rawId, settings)

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
     * Поддерживает кастомную коллизию через settings.shapeBoxes (например в 2 блока высоты: {from={0,0,0},to={16,32,16}}).
     */
    fun registerBlock(rawId: String, settings: LuaContentSettings? = null): Block? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("block:$rawId")) {
                val existing = BuiltInRegistries.BLOCK.get(id)
                if (existing.isPresent) return existing.get().value()
            }

            settings?.texture?.let { storeTexture(rawId, it) }
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeToolTier(rawId, settings)
            storeDrops(rawId, settings)
            storeOre(rawId, settings)
            storeBlockTags(rawId, settings)
            storeItemTags(rawId, settings)

            val key = ResourceKey.create(Registries.BLOCK, id)
            val props = settings?.applyTo(BlockBehaviour.Properties.of())?.setId(key)
                ?: BlockBehaviour.Properties.of().setId(key)
            val customName = settings?.displayName()

            val boxes = settings?.shapeBoxes
            val block: Block = if (boxes != null && boxes.isNotEmpty()) {
                val shape = buildVoxelShape(boxes)
                blockShapes[rawId] = shape
                createVoxelBlock(props, shape, customName)
            } else {
                if (customName != null) {
                    object : Block(props) {
                        override fun getName() = customName
                    }
                } else {
                    Block(props)
                }
            }

            registerWithFreezeFallback(BuiltInRegistries.BLOCK as Registry<Block>) { Registry.register(BuiltInRegistries.BLOCK, key, block) }
            knownIds.add("block:$rawId")
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic block $rawId${if (boxes != null) " shapeBoxes=${boxes.size}" else ""}")
            block
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic block $rawId", e)
            null
        }
    }

    // ═══ Вариант-блоки: slab / stairs / door / trapdoor / fence ═══

    fun parseBlockSetType(name: String?): BlockSetType {
        if (name == null) return BlockSetType.STONE
        return try {
            BlockSetType::class.java.getField(name.uppercase()).get(null) as BlockSetType
        } catch (_: Exception) {
            when (name.lowercase()) {
                "oak", "wood", "wooden" -> BlockSetType.OAK
                "spruce" -> try { BlockSetType::class.java.getField("SPRUCE").get(null) as BlockSetType } catch (_: Exception) { BlockSetType.OAK }
                "birch" -> try { BlockSetType::class.java.getField("BIRCH").get(null) as BlockSetType } catch (_: Exception) { BlockSetType.OAK }
                "jungle" -> try { BlockSetType::class.java.getField("JUNGLE").get(null) as BlockSetType } catch (_: Exception) { BlockSetType.OAK }
                "acacia" -> try { BlockSetType::class.java.getField("ACACIA").get(null) as BlockSetType } catch (_: Exception) { BlockSetType.OAK }
                "dark_oak", "darkoak" -> try { BlockSetType::class.java.getField("DARK_OAK").get(null) as BlockSetType } catch (_: Exception) { BlockSetType.OAK }
                "iron", "metal" -> BlockSetType.IRON
                "stone" -> BlockSetType.STONE
                "copper" -> try { BlockSetType::class.java.getField("COPPER").get(null) as BlockSetType } catch (_: Exception) { BlockSetType.STONE }
                else -> BlockSetType.STONE
            }
        }
    }

    fun registerSlab(rawId: String, settings: LuaContentSettings? = null): Block? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("block:$rawId")) {
                val existing = BuiltInRegistries.BLOCK.get(id)
                if (existing.isPresent) return existing.get().value()
            }
            settings?.texture?.let { storeTexture(rawId, it) }
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeToolTier(rawId, settings)
            storeDrops(rawId, settings)
            storeOre(rawId, settings)
            storeBlockTags(rawId, settings)
            storeItemTags(rawId, settings)
            val key = ResourceKey.create(Registries.BLOCK, id)
            val props = settings?.applyTo(BlockBehaviour.Properties.of())?.setId(key) ?: BlockBehaviour.Properties.of().setId(key)
            val customName = settings?.displayName()
            val block: Block = if (customName != null) {
                object : SlabBlock(props) { override fun getName() = customName }
            } else SlabBlock(props)
            registerWithFreezeFallback(BuiltInRegistries.BLOCK as Registry<Block>) { Registry.register(BuiltInRegistries.BLOCK, key, block) }
            knownIds.add("block:$rawId")
            blockTypes[rawId] = "slab"
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic slab $rawId")
            block
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic slab $rawId", e)
            null
        }
    }

    fun registerStairs(rawId: String, baseBlockId: String?, settings: LuaContentSettings? = null): Block? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("block:$rawId")) {
                val existing = BuiltInRegistries.BLOCK.get(id)
                if (existing.isPresent) return existing.get().value()
            }
            settings?.texture?.let { storeTexture(rawId, it) }
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeToolTier(rawId, settings)
            storeDrops(rawId, settings)
            storeOre(rawId, settings)
            storeBlockTags(rawId, settings)
            storeItemTags(rawId, settings)

            // База для stairs — состояние блока
            val baseIdStr = baseBlockId ?: "minecraft:stone"
            val baseBlock = try {
                val bid = Identifier.parse(baseIdStr)
                BuiltInRegistries.BLOCK.get(bid).orElse(null)?.value() ?: net.minecraft.world.level.block.Blocks.STONE
            } catch (_: Exception) { net.minecraft.world.level.block.Blocks.STONE }
            val baseState = baseBlock.defaultBlockState()

            val key = ResourceKey.create(Registries.BLOCK, id)
            val props = settings?.applyTo(BlockBehaviour.Properties.ofFullCopy(baseBlock))?.setId(key)
                ?: BlockBehaviour.Properties.ofFullCopy(baseBlock).setId(key)
            val customName = settings?.displayName()
            val block: Block = if (customName != null) {
                object : StairBlock(baseState, props) { override fun getName() = customName }
            } else StairBlock(baseState, props)
            registerWithFreezeFallback(BuiltInRegistries.BLOCK as Registry<Block>) { Registry.register(BuiltInRegistries.BLOCK, key, block) }
            knownIds.add("block:$rawId")
            blockTypes[rawId] = "stairs"
            stairsBase[rawId] = baseIdStr
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic stairs $rawId base=$baseIdStr")
            block
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic stairs $rawId", e)
            null
        }
    }

    fun registerDoor(rawId: String, blockSetTypeName: String?, settings: LuaContentSettings? = null): Block? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("block:$rawId")) {
                val existing = BuiltInRegistries.BLOCK.get(id)
                if (existing.isPresent) return existing.get().value()
            }
            settings?.texture?.let { storeTexture(rawId, it) }
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeToolTier(rawId, settings)
            storeDrops(rawId, settings)
            storeOre(rawId, settings)
            storeBlockTags(rawId, settings)
            storeItemTags(rawId, settings)
            val key = ResourceKey.create(Registries.BLOCK, id)
            val props = settings?.applyTo(BlockBehaviour.Properties.of())?.setId(key) ?: BlockBehaviour.Properties.of().setId(key)
            val setType = parseBlockSetType(blockSetTypeName)
            val customName = settings?.displayName()
            val block: Block = if (customName != null) {
                object : DoorBlock(setType, props) { override fun getName() = customName }
            } else DoorBlock(setType, props)
            registerWithFreezeFallback(BuiltInRegistries.BLOCK as Registry<Block>) { Registry.register(BuiltInRegistries.BLOCK, key, block) }
            knownIds.add("block:$rawId")
            blockTypes[rawId] = "door"
            if (blockSetTypeName != null) doorBlockSetType[rawId] = blockSetTypeName
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic door $rawId set=$blockSetTypeName")
            block
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic door $rawId", e)
            null
        }
    }

    fun registerTrapdoor(rawId: String, blockSetTypeName: String?, settings: LuaContentSettings? = null): Block? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("block:$rawId")) {
                val existing = BuiltInRegistries.BLOCK.get(id)
                if (existing.isPresent) return existing.get().value()
            }
            settings?.texture?.let { storeTexture(rawId, it) }
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeToolTier(rawId, settings)
            storeDrops(rawId, settings)
            storeOre(rawId, settings)
            storeBlockTags(rawId, settings)
            storeItemTags(rawId, settings)
            val key = ResourceKey.create(Registries.BLOCK, id)
            val props = settings?.applyTo(BlockBehaviour.Properties.of())?.setId(key) ?: BlockBehaviour.Properties.of().setId(key)
            val setType = parseBlockSetType(blockSetTypeName)
            val customName = settings?.displayName()
            val block: Block = if (customName != null) {
                object : TrapDoorBlock(setType, props) { override fun getName() = customName }
            } else TrapDoorBlock(setType, props)
            registerWithFreezeFallback(BuiltInRegistries.BLOCK as Registry<Block>) { Registry.register(BuiltInRegistries.BLOCK, key, block) }
            knownIds.add("block:$rawId")
            blockTypes[rawId] = "trapdoor"
            if (blockSetTypeName != null) doorBlockSetType[rawId] = blockSetTypeName
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic trapdoor $rawId set=$blockSetTypeName")
            block
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic trapdoor $rawId", e)
            null
        }
    }

    fun registerFence(rawId: String, settings: LuaContentSettings? = null): Block? {
        return try {
            val id = Identifier.parse(rawId)
            if (knownIds.contains("block:$rawId")) {
                val existing = BuiltInRegistries.BLOCK.get(id)
                if (existing.isPresent) return existing.get().value()
            }
            settings?.texture?.let { storeTexture(rawId, it) }
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeToolTier(rawId, settings)
            storeDrops(rawId, settings)
            storeOre(rawId, settings)
            storeBlockTags(rawId, settings)
            storeItemTags(rawId, settings)
            val key = ResourceKey.create(Registries.BLOCK, id)
            val props = settings?.applyTo(BlockBehaviour.Properties.of())?.setId(key) ?: BlockBehaviour.Properties.of().setId(key)
            val customName = settings?.displayName()
            val block: Block = if (customName != null) {
                object : FenceBlock(props) { override fun getName() = customName }
            } else FenceBlock(props)
            registerWithFreezeFallback(BuiltInRegistries.BLOCK as Registry<Block>) { Registry.register(BuiltInRegistries.BLOCK, key, block) }
            knownIds.add("block:$rawId")
            blockTypes[rawId] = "fence"
            ClientMain.LOGGER?.info("[Neo Scripts] Registered dynamic fence $rawId")
            block
        } catch (e: Exception) {
            ClientMain.LOGGER?.error("[Neo Scripts] Failed to register dynamic fence $rawId", e)
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
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeItemTags(rawId, settings)
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
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeItemTags(rawId, settings)
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
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeItemTags(rawId, settings)

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
            settings?.model?.let { storeModel(rawId, it) }
            settings?.textures?.let { storeTextures(rawId, it) }
            storeItemTags(rawId, settings)
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
