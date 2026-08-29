package com.nekiplay.neoscripts.client.features.lua.objects.misc

import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.common.features.lua.objects.misc.DynamicContent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.MetadataSectionType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackCompatibility
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.repository.RepositorySource
import net.minecraft.server.packs.resources.IoSupplier
import net.minecraft.world.flag.FeatureFlagSet
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * Рантайм ресурспак с моделями и текстурами динамических предметов/блоков.
 *
 * Генерирует:
 *  Для предметов (items):
 *   assets/<ns>/items/<path>.json        — item model definition (26.x)
 *   assets/<ns>/models/item/<path>.json  — модель (parent = settings.model или item/generated)
 *   assets/<ns>/textures/item/<path>.png
 *  Для блоков:
 *   assets/<ns>/blockstates/<path>.json  — variants -> model "ns:block/path"
 *   assets/<ns>/models/block/<path>.json — модель (parent = settings.model или cube_all), текстуры all/particle
 *   assets/<ns>/textures/block/<path>.png
 *   + для BlockItem (если тот же id зарегистрирован как item) — item-модель ссылается на block
 *
 * Поддерживает settings.model:
 *  - "minecraft:block/cube_all" — Identifier родительской модели
 *  - "minecraft:diamond_block"   — короткая запись (разворачивается в "minecraft:block/diamond_block" для блоков)
 *  - "tinker_construct:block/cast" — любой неймспейс
 *  - путь к файлу "*.json"       — байты файла отдаются напрямую как model json
 */
object DynamicContentResourcePack : PackResources {

    const val PACK_ID: String = "neoscripts_dynamic_content"

    private val locationInfo = PackLocationInfo(
        PACK_ID,
        Component.literal("NeoScripts Dynamic Content"),
        PackSource.BUILT_IN,
        Optional.empty()
    )

    /**
     * Источник пака для PackRepository. Пак пересоздается на каждом reload(),
     * поэтому предметы, зарегистрированные до загрузки ресурсов, подхватятся.
     */
    val repositorySource: RepositorySource = RepositorySource { consumer ->
        consumer.accept(createPack())
    }

    fun createPack(): Pack {
        val supplier = object : Pack.ResourcesSupplier {
            override fun openPrimary(info: PackLocationInfo): PackResources =
                this@DynamicContentResourcePack

            override fun openFull(info: PackLocationInfo, metadata: Pack.Metadata): PackResources =
                this@DynamicContentResourcePack
        }
        val metadata = Pack.Metadata(
            Component.literal("Textures and models for Lua-registered items"),
            PackCompatibility.COMPATIBLE,
            FeatureFlagSet.of(),
            emptyList()
        )
        // required + fixed: пак всегда включен и не отключается в меню паков.
        // BOTTOM: минимальный приоритет — другие паки/моды могут переопределять
        // модели динамических предметов.
        val selectionConfig = PackSelectionConfig(true, Pack.Position.BOTTOM, true)
        return Pack(locationInfo, supplier, metadata, selectionConfig)
    }

    // ═══ Генерация ассетов ═══

    private fun expandModel(parent: String, isBlock: Boolean): String {
        // Уже полный идентификатор модели (содержит '/') — оставляем как есть
        if (parent.contains("/")) return parent
        // Короткая запись "namespace:path" без слэша — разворачиваем в "namespace:block/path" или "namespace:item/path"
        if (parent.contains(":")) {
            val (ns, path) = parent.split(":", limit = 2)
            return if (isBlock) "$ns:block/$path" else "$ns:item/$path"
        }
        // Без неймспейса — считаем minecraft
        return if (isBlock) "minecraft:block/$parent" else "minecraft:item/$parent"
    }

    private fun buildStairsBlockstate(ns: String, path: String): String {
        val m = "$ns:block/$path"
        val mi = "$ns:block/${path}_inner"
        val mo = "$ns:block/${path}_outer"
        return """{"variants":{"facing=east,half=bottom,shape=straight":{"model":"$m"},"facing=west,half=bottom,shape=straight":{"model":"$m","y":180,"uvlock":true},"facing=south,half=bottom,shape=straight":{"model":"$m","y":90,"uvlock":true},"facing=north,half=bottom,shape=straight":{"model":"$m","y":270,"uvlock":true},"facing=east,half=bottom,shape=inner_left":{"model":"$mi"},"facing=west,half=bottom,shape=inner_left":{"model":"$mi","y":180,"uvlock":true},"facing=south,half=bottom,shape=inner_left":{"model":"$mi","y":90,"uvlock":true},"facing=north,half=bottom,shape=inner_left":{"model":"$mi","y":270,"uvlock":true},"facing=east,half=bottom,shape=inner_right":{"model":"$mo"},"facing=west,half=bottom,shape=inner_right":{"model":"$mo","y":180,"uvlock":true},"facing=south,half=bottom,shape=inner_right":{"model":"$mo","y":90,"uvlock":true},"facing=north,half=bottom,shape=inner_right":{"model":"$mo","y":270,"uvlock":true},"facing=east,half=bottom,shape=outer_left":{"model":"$mo"},"facing=west,half=bottom,shape=outer_left":{"model":"$mo","y":180,"uvlock":true},"facing=south,half=bottom,shape=outer_left":{"model":"$mo","y":90,"uvlock":true},"facing=north,half=bottom,shape=outer_left":{"model":"$mo","y":270,"uvlock":true},"facing=east,half=bottom,shape=outer_right":{"model":"$mi"},"facing=west,half=bottom,shape=outer_right":{"model":"$mi","y":180,"uvlock":true},"facing=south,half=bottom,shape=outer_right":{"model":"$mi","y":90,"uvlock":true},"facing=north,half=bottom,shape=outer_right":{"model":"$mi","y":270,"uvlock":true},"facing=east,half=top,shape=straight":{"model":"$m","x":180,"uvlock":true},"facing=west,half=top,shape=straight":{"model":"$m","x":180,"y":180,"uvlock":true},"facing=south,half=top,shape=straight":{"model":"$m","x":180,"y":90,"uvlock":true},"facing=north,half=top,shape=straight":{"model":"$m","x":180,"y":270,"uvlock":true}}}"""
    }

    private fun buildDoorBlockstate(ns: String, path: String): String {
        val b = "$ns:block/${path}_bottom"
        val t = "$ns:block/${path}_top"
        return """{"variants":{"facing=east,half=lower,hinge=left,open=false":{"model":"$b"},"facing=south,half=lower,hinge=left,open=false":{"model":"$b","y":90},"facing=west,half=lower,hinge=left,open=false":{"model":"$b","y":180},"facing=north,half=lower,hinge=left,open=false":{"model":"$b","y":270},"facing=east,half=lower,hinge=left,open=true":{"model":"$b","y":90},"facing=south,half=lower,hinge=left,open=true":{"model":"$b","y":180},"facing=west,half=lower,hinge=left,open=true":{"model":"$b","y":270},"facing=north,half=lower,hinge=left,open=true":{"model":"$b"},"facing=east,half=upper,hinge=left,open=false":{"model":"$t"},"facing=south,half=upper,hinge=left,open=false":{"model":"$t","y":90},"facing=west,half=upper,hinge=left,open=false":{"model":"$t","y":180},"facing=north,half=upper,hinge=left,open=false":{"model":"$t","y":270}}}"""
    }

    private fun buildTrapdoorBlockstate(ns: String, path: String): String {
        val m = "$ns:block/$path"
        return """{"variants":{"facing=north,half=bottom,open=false":{"model":"$m"},"facing=south,half=bottom,open=false":{"model":"$m","y":180},"facing=east,half=bottom,open=false":{"model":"$m","y":90},"facing=west,half=bottom,open=false":{"model":"$m","y":270},"facing=north,half=top,open=false":{"model":"$m","x":180},"facing=north,half=bottom,open=true":{"model":"$m","x":90}}}"""
    }

    private fun buildFenceBlockstate(ns: String, path: String): String {
        // Упрощённый multipart-подобный через variants (не идеален, но работает для дефолта)
        val m = "$ns:block/$path"
        return """{"multipart":[{"when":{"north":"true"},"apply":{"model":"$ns:block/${path}_side","uvlock":true}},{"when":{"east":"true"},"apply":{"model":"$ns:block/${path}_side","y":90,"uvlock":true}},{"when":{"south":"true"},"apply":{"model":"$ns:block/${path}_side","y":180,"uvlock":true}},{"when":{"west":"true"},"apply":{"model":"$ns:block/${path}_side","y":270,"uvlock":true}},{"apply":{"model":"$m"}}]}"""
    }

    private fun buildFiles(): ConcurrentHashMap<String, ByteArray> {
        val files = ConcurrentHashMap<String, ByteArray>()

        // Собираем все rawId, для которых нужно генерировать ассеты
        val allRawIds = mutableSetOf<String>()
        allRawIds.addAll(DynamicContent.textureData.keys)
        allRawIds.addAll(DynamicContent.modelOverrides.keys)
        allRawIds.addAll(DynamicContent.modelData.keys)
        allRawIds.addAll(DynamicContent.multiTextureData.keys)
        for (entry in DynamicContent.getKnownIds()) {
            val raw = entry.substringAfter(":") // "block:ns:path" -> "ns:path"
            if (raw.contains(":")) allRawIds.add(raw)
        }

        for (rawId in allRawIds) {
            try {
                val id = Identifier.parse(rawId)
                val ns = id.namespace
                val path = id.path

                val blockKnown = DynamicContent.isBlockRegistered(rawId)
                val itemKnown = DynamicContent.isItemRegistered(rawId)

                // Если не зарегистрирован ни как блок ни как предмет, но есть текстура/модель — считаем предметом по умолчанию
                val treatAsBlock = blockKnown
                val treatAsItem = itemKnown || (!blockKnown && !itemKnown)

                val pngBytes = DynamicContent.textureData[rawId]

                // ── БЛОК ──
                if (treatAsBlock) {
                    val blockType = DynamicContent.getBlockType(rawId) ?: "cube"
                    val tex = "$ns:block/$path"
                    val blockModelBytes = DynamicContent.modelData[rawId]
                    val rawParent = DynamicContent.modelOverrides[rawId]

                    when (blockType) {
                        "slab" -> {
                            // blockstates для slab
                            files["assets/$ns/blockstates/$path.json"] =
                                """{"variants":{"type=bottom":{"model":"$ns:block/$path"},"type=top":{"model":"$ns:block/${path}_top"},"type=double":{"model":"$ns:block/$path"}}}"""
                                    .toByteArray(StandardCharsets.UTF_8)
                            if (blockModelBytes != null) {
                                files["assets/$ns/models/block/$path.json"] = blockModelBytes
                                files["assets/$ns/models/block/${path}_top.json"] = blockModelBytes
                            } else {
                                val parent = if (rawParent != null) expandModel(rawParent, true) else "minecraft:block/slab"
                                // если parent уже slab/cube — ок
                                files["assets/$ns/models/block/$path.json"] =
                                    """{"parent":"$parent","textures":{"bottom":"$tex","top":"$tex","side":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                                files["assets/$ns/models/block/${path}_top.json"] =
                                    """{"parent":"minecraft:block/slab_top","textures":{"bottom":"$tex","top":"$tex","side":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                            }
                            if (pngBytes != null) files["assets/$ns/textures/block/$path.png"] = pngBytes
                        }
                        "stairs" -> {
                            files["assets/$ns/blockstates/$path.json"] = buildStairsBlockstate(ns, path).toByteArray(StandardCharsets.UTF_8)
                            if (blockModelBytes != null) {
                                files["assets/$ns/models/block/$path.json"] = blockModelBytes
                            } else {
                                val parent = if (rawParent != null) expandModel(rawParent, true) else "minecraft:block/stairs"
                                files["assets/$ns/models/block/$path.json"] =
                                    """{"parent":"$parent","textures":{"bottom":"$tex","top":"$tex","side":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                                files["assets/$ns/models/block/${path}_inner.json"] =
                                    """{"parent":"minecraft:block/inner_stairs","textures":{"bottom":"$tex","top":"$tex","side":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                                files["assets/$ns/models/block/${path}_outer.json"] =
                                    """{"parent":"minecraft:block/outer_stairs","textures":{"bottom":"$tex","top":"$tex","side":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                            }
                            if (pngBytes != null) files["assets/$ns/textures/block/$path.png"] = pngBytes
                        }
                        "door" -> {
                            files["assets/$ns/blockstates/$path.json"] = buildDoorBlockstate(ns, path).toByteArray(StandardCharsets.UTF_8)
                            if (blockModelBytes != null) {
                                files["assets/$ns/models/block/${path}_bottom.json"] = blockModelBytes
                                files["assets/$ns/models/block/${path}_top.json"] = blockModelBytes
                            } else {
                                files["assets/$ns/models/block/${path}_bottom.json"] =
                                    """{"parent":"minecraft:block/door_bottom","textures":{"bottom":"$tex","top":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                                files["assets/$ns/models/block/${path}_top.json"] =
                                    """{"parent":"minecraft:block/door_top","textures":{"bottom":"$tex","top":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                            }
                            if (pngBytes != null) files["assets/$ns/textures/block/$path.png"] = pngBytes
                        }
                        "trapdoor" -> {
                            files["assets/$ns/blockstates/$path.json"] = buildTrapdoorBlockstate(ns, path).toByteArray(StandardCharsets.UTF_8)
                            if (blockModelBytes != null) {
                                files["assets/$ns/models/block/$path.json"] = blockModelBytes
                            } else {
                                files["assets/$ns/models/block/$path.json"] =
                                    """{"parent":"minecraft:block/trapdoor","textures":{"texture":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                            }
                            if (pngBytes != null) files["assets/$ns/textures/block/$path.png"] = pngBytes
                        }
                        "fence" -> {
                            files["assets/$ns/blockstates/$path.json"] = buildFenceBlockstate(ns, path).toByteArray(StandardCharsets.UTF_8)
                            if (blockModelBytes != null) {
                                files["assets/$ns/models/block/$path.json"] = blockModelBytes
                            } else {
                                files["assets/$ns/models/block/$path.json"] =
                                    """{"parent":"minecraft:block/fence_post","textures":{"texture":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                                files["assets/$ns/models/block/${path}_side.json"] =
                                    """{"parent":"minecraft:block/fence_side","textures":{"texture":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                            }
                            if (pngBytes != null) files["assets/$ns/textures/block/$path.png"] = pngBytes
                        }
                        else -> {
                            // обычный куб
                            files["assets/$ns/blockstates/$path.json"] =
                                """{"variants":{"":{"model":"$ns:block/$path"}}}"""
                                    .toByteArray(StandardCharsets.UTF_8)
                            if (blockModelBytes != null) {
                                files["assets/$ns/models/block/$path.json"] = blockModelBytes
                            } else {
                                val parent = if (rawParent != null) expandModel(rawParent, true) else "minecraft:block/cube_all"
                                val texturesJson = when {
                                    parent.endsWith("/cross") || parent == "minecraft:block/cross" -> """"cross":"$tex""""
                                    else -> """"all":"$tex","particle":"$tex""""
                                }
                                files["assets/$ns/models/block/$path.json"] =
                                    """{"parent":"$parent","textures":{$texturesJson}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                            }
                            if (pngBytes != null) files["assets/$ns/textures/block/$path.png"] = pngBytes
                        }
                    }
                }

                // ── МНОЖЕСТВЕННЫЕ ТЕКСТУРЫ (для кастомных моделей Blockbench) ──
                val multiTextures = DynamicContent.multiTextureData[rawId]
                if (!multiTextures.isNullOrEmpty()) {
                    for ((key, texBytes) in multiTextures) {
                        // Сохраняем текстуры в textures/block/<path>_<key>.png и textures/item/<path>_<key>.png
                        if (treatAsBlock) {
                            files["assets/$ns/textures/block/${path}_$key.png"] = texBytes
                        }
                        if (treatAsItem) {
                            files["assets/$ns/textures/item/${path}_$key.png"] = texBytes
                        }
                    }
                }

                // ── ПРЕДМЕТ ──
                if (treatAsItem) {
                    val customItemModelBytes = if (!treatAsBlock) DynamicContent.modelData[rawId] else null
                    // Если это BlockItem (один и тот же id и блок и предмет) — item ссылается на блок
                    if (treatAsBlock && itemKnown) {
                        // item definition -> block model
                        files["assets/$ns/items/$path.json"] =
                            """{"model":{"type":"minecraft:model","model":"$ns:block/$path"}}"""
                                .toByteArray(StandardCharsets.UTF_8)
                        // item model (опционален, но для совместимости) — parent block
                        if (customItemModelBytes == null) {
                            // Если для этого id есть кастомная модель-файл, но он уже использован для блока — для предмета делаем ссылку
                            files["assets/$ns/models/item/$path.json"] =
                                """{"parent":"$ns:block/$path"}"""
                                    .toByteArray(StandardCharsets.UTF_8)
                        } else {
                            files["assets/$ns/models/item/$path.json"] = customItemModelBytes
                        }
                    } else {
                        // Чистый предмет
                        if (customItemModelBytes != null) {
                            files["assets/$ns/models/item/$path.json"] = customItemModelBytes
                            files["assets/$ns/items/$path.json"] =
                                """{"model":{"type":"minecraft:model","model":"$ns:item/$path"}}"""
                                    .toByteArray(StandardCharsets.UTF_8)
                        } else {
                            val rawParent = DynamicContent.modelOverrides[rawId]
                            val parent = if (rawParent != null) expandModel(rawParent, isBlock = false) else "minecraft:item/generated"
                            // Если parent уже является готовой моделью блока/предмета (например "minecraft:block/diamond_block"),
                            // то в items/<path>.json можно сослаться напрямую на неё без генерации models/item
                            val isDirectModelRef = rawParent != null && rawParent != "minecraft:item/generated" && rawParent != "minecraft:item/handheld"
                            // Для parent типа item/generated нужен layer0, для block/cube_all — all
                            if (isDirectModelRef && (parent.startsWith("minecraft:block/") || parent.contains(":block/"))) {
                                files["assets/$ns/items/$path.json"] =
                                    """{"model":{"type":"minecraft:model","model":"$parent"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                                // Не генерируем models/item — используется чужой блок
                            } else {
                                files["assets/$ns/items/$path.json"] =
                                    """{"model":{"type":"minecraft:model","model":"$ns:item/$path"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                                val tex = "$ns:item/$path"
                                files["assets/$ns/models/item/$path.json"] =
                                    """{"parent":"$parent","textures":{"layer0":"$tex"}}"""
                                        .toByteArray(StandardCharsets.UTF_8)
                            }
                        }
                    }

                    if (pngBytes != null) {
                        // Текстура предмета всегда в textures/item
                        files["assets/$ns/textures/item/$path.png"] = pngBytes
                        // Дублируем в textures/block для блоков-итемов, если ещё не создана
                        if (treatAsBlock && !files.containsKey("assets/$ns/textures/block/$path.png")) {
                            files["assets/$ns/textures/block/$path.png"] = pngBytes
                        }
                    }
                }

            } catch (e: Exception) {
                ClientMain.LOGGER?.error("[Neo Scripts] Failed to build assets for $rawId", e)
            }
        }
        return files
    }

    // ── SERVER DATA: теги добычи + loot_table ──
    private fun buildServerFiles(): ConcurrentHashMap<String, ByteArray> {
        val files = ConcurrentHashMap<String, ByteArray>()
        // Группируем по инструменту
        val toolGroups = mutableMapOf<String, MutableList<String>>()
        for ((rawId, tool) in DynamicContent.blockMineableTool) {
            if (!DynamicContent.isBlockRegistered(rawId)) continue
            toolGroups.getOrPut(tool) { mutableListOf() }.add(rawId)
        }
        for ((tool, ids) in toolGroups) {
            if (ids.isEmpty()) continue
            val json = buildString {
                append("{\"replace\":false,\"values\":[")
                append(ids.joinToString(",") { "\"$it\"" })
                append("]}")
            }
            files["data/minecraft/tags/block/mineable/${tool}.json"] = json.toByteArray(StandardCharsets.UTF_8)
        }
        // Группируем по тиру
        val tierMap = mapOf(
            "wood" to "needs_wood_tool", "stone" to "needs_stone_tool",
            "iron" to "needs_iron_tool", "diamond" to "needs_diamond_tool",
            "netherite" to "needs_netherite_tool", "gold" to "needs_gold_tool"
        )
        val tierGroups = mutableMapOf<String, MutableList<String>>()
        for ((rawId, tier) in DynamicContent.blockMiningTier) {
            if (!DynamicContent.isBlockRegistered(rawId)) continue
            tierGroups.getOrPut(tier) { mutableListOf() }.add(rawId)
        }
        for ((tier, ids) in tierGroups) {
            val tag = tierMap[tier] ?: "needs_${tier}_tool"
            val json = buildString {
                append("{\"replace\":false,\"values\":[")
                append(ids.joinToString(",") { "\"$it\"" })
                append("]}")
            }
            files["data/minecraft/tags/block/${tag}.json"] = json.toByteArray(StandardCharsets.UTF_8)
        }
        // Loot tables для каждого динамического блока (https://docs.fabricmc.net/develop/blocks/first-block#adding-block-drops)
        // Генерируем как loot_table (1.21.5+/26.2, singular) и loot_tables (legacy, plural) для совместимости
        val allBlockRawIds = mutableSetOf<String>()
        for (entry in DynamicContent.getKnownIds()) {
            if (entry.startsWith("block:")) {
                val raw = entry.substringAfter("block:")
                if (raw.contains(":")) allBlockRawIds.add(raw)
            }
        }
        // также блоки, у которых явно заданы drops, даже если knownIds еще не содержит (на всякий)
        allBlockRawIds.addAll(DynamicContent.blockDrops.keys)
        for (rawId in allBlockRawIds) {
            if (!DynamicContent.isBlockRegistered(rawId) && !DynamicContent.blockDrops.containsKey(rawId)) continue
            try {
                val id = Identifier.parse(rawId)
                val ns = id.namespace
                val path = id.path
                val json = DynamicContent.buildBlockLootJson(rawId)
                val bytes = json.toByteArray(StandardCharsets.UTF_8)
                files["data/$ns/loot_table/blocks/$path.json"] = bytes
                files["data/$ns/loot_tables/blocks/$path.json"] = bytes
            } catch (_: Exception) {}
        }
        // Worldgen для руд (https://wiki.fabricmc.net/tutorial:ores)
        for ((fid, _) in DynamicContent.oreGens) {
            try {
                val fidId = Identifier.parse(fid)
                val ns = fidId.namespace
                val path = fidId.path
                val cfgJson = DynamicContent.buildOreConfiguredJson(fid)
                val plcJson = DynamicContent.buildOrePlacedJson(fid)
                files["data/$ns/worldgen/configured_feature/$path.json"] = cfgJson.toByteArray(StandardCharsets.UTF_8)
                files["data/$ns/worldgen/placed_feature/$path.json"] = plcJson.toByteArray(StandardCharsets.UTF_8)
            } catch (_: Exception) {}
        }
        // Теги предметов/блоков для рецептов (https://docs.fabricmc.net/develop/data-generation/tags)
        for ((tagId, ids) in DynamicContent.itemTags) {
            if (ids.isEmpty()) continue
            try {
                val tid = Identifier.parse(tagId)
                val ns = tid.namespace
                val path = tid.path
                val json = buildString {
                    append("{\"replace\":false,\"values\":[")
                    append(ids.joinToString(",") { "\"$it\"" })
                    append("]}")
                }
                files["data/$ns/tags/item/$path.json"] = json.toByteArray(StandardCharsets.UTF_8)
            } catch (_: Exception) {}
        }
        for ((tagId, ids) in DynamicContent.blockTags) {
            if (ids.isEmpty()) continue
            try {
                val tid = Identifier.parse(tagId)
                val ns = tid.namespace
                val path = tid.path
                val json = buildString {
                    append("{\"replace\":false,\"values\":[")
                    append(ids.joinToString(",") { "\"$it\"" })
                    append("]}")
                }
                files["data/$ns/tags/block/$path.json"] = json.toByteArray(StandardCharsets.UTF_8)
            } catch (_: Exception) {}
        }
        // Рецепты (https://wiki.fabricmc.net/tutorial:recipes)
        for ((rid, json) in DynamicContent.recipes) {
            try {
                val ridId = Identifier.parse(rid)
                val ns = ridId.namespace
                val path = ridId.path
                val bytes = json.toByteArray(StandardCharsets.UTF_8)
                files["data/$ns/recipe/$path.json"] = bytes
                files["data/$ns/recipes/$path.json"] = bytes
            } catch (_: Exception) {}
        }
        return files
    }

    private fun clientFile(relativePath: String, type: PackType): IoSupplier<InputStream>? {
        val bytes = when (type) {
            PackType.CLIENT_RESOURCES -> buildFiles()[relativePath]
            PackType.SERVER_DATA -> buildServerFiles()[relativePath]
            else -> null
        } ?: return null
        return IoSupplier { ByteArrayInputStream(bytes) }
    }

    // ═══ PackResources ═══

    override fun getRootResource(vararg segments: String): IoSupplier<InputStream>? {
        if (segments.size == 1 && segments[0] == "pack.mcmeta") {
            val json = """{"pack":{"pack_format":99,"description":"NeoScripts dynamic content"}}"""
            return IoSupplier { ByteArrayInputStream(json.toByteArray(StandardCharsets.UTF_8)) }
        }
        return null
    }

    override fun getResource(type: PackType, id: Identifier): IoSupplier<InputStream>? {
        return when (type) {
            PackType.CLIENT_RESOURCES -> clientFile("assets/${id.namespace}/${id.path}", type)
            PackType.SERVER_DATA -> clientFile("data/${id.namespace}/${id.path}", type)
            else -> null
        }
    }

    override fun listResources(
        type: PackType,
        namespace: String,
        prefix: String,
        output: PackResources.ResourceOutput
    ) {
        val files = when (type) {
            PackType.CLIENT_RESOURCES -> buildFiles()
            PackType.SERVER_DATA -> buildServerFiles()
            else -> return
        }
        val fullPrefix = when (type) {
            PackType.CLIENT_RESOURCES -> "assets/$namespace/$prefix"
            PackType.SERVER_DATA -> "data/$namespace/$prefix"
            else -> return
        }
        for ((path, bytes) in files) {
            if (path.startsWith(fullPrefix)) {
                val rel = path.removePrefix(
                    when (type) {
                        PackType.CLIENT_RESOURCES -> "assets/"
                        PackType.SERVER_DATA -> "data/"
                        else -> continue
                    }
                ).substringAfter('/')
                try {
                    val id = Identifier.fromNamespaceAndPath(namespace, rel)
                    output.accept(id, IoSupplier { ByteArrayInputStream(bytes) })
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun getNamespaces(type: PackType): Set<String> {
        return when (type) {
            PackType.CLIENT_RESOURCES -> {
                val all = mutableSetOf<String>()
                all.addAll(DynamicContent.textureData.keys)
                all.addAll(DynamicContent.modelOverrides.keys)
                all.addAll(DynamicContent.modelData.keys)
                all.addAll(DynamicContent.multiTextureData.keys)
                all.addAll(DynamicContent.getKnownIds().mapNotNull {
                    try { it.substringAfter(":").let { r -> Identifier.parse(r).namespace } } catch (_: Exception) { null }
                })
                all.mapNotNull {
                    try { Identifier.parse(it).namespace } catch (_: Exception) { null }
                }.toSet()
            }
            PackType.SERVER_DATA -> {
                val namespaces = mutableSetOf<String>()
                if (DynamicContent.blockMineableTool.isNotEmpty() || DynamicContent.blockMiningTier.isNotEmpty()) namespaces.add("minecraft")
                // loot tables namespaces
                for (rawId in DynamicContent.blockDrops.keys) {
                    try { namespaces.add(Identifier.parse(rawId).namespace) } catch (_: Exception) {}
                }
                for (entry in DynamicContent.getKnownIds()) {
                    if (entry.startsWith("block:")) {
                        try { namespaces.add(Identifier.parse(entry.substringAfter("block:")).namespace) } catch (_: Exception) {}
                    }
                }
                // ore worldgen
                for (fid in DynamicContent.oreGens.keys) {
                    try { namespaces.add(Identifier.parse(fid).namespace) } catch (_: Exception) {}
                }
                // tags
                for (tagId in DynamicContent.itemTags.keys) {
                    try { namespaces.add(Identifier.parse(tagId).namespace) } catch (_: Exception) {}
                }
                for (tagId in DynamicContent.blockTags.keys) {
                    try { namespaces.add(Identifier.parse(tagId).namespace) } catch (_: Exception) {}
                }
                for (rid in DynamicContent.recipes.keys) {
                    try { namespaces.add(Identifier.parse(rid).namespace) } catch (_: Exception) {}
                }
                namespaces.ifEmpty { setOf("minecraft") }
            }
            else -> emptySet()
        }
    }

    override fun <T : Any> getMetadataSection(sectionType: MetadataSectionType<T>): T? = null

    override fun location(): PackLocationInfo = locationInfo

    override fun close() {}
}
