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

    private fun buildFiles(): ConcurrentHashMap<String, ByteArray> {
        val files = ConcurrentHashMap<String, ByteArray>()

        // Собираем все rawId, для которых нужно генерировать ассеты
        val allRawIds = mutableSetOf<String>()
        allRawIds.addAll(DynamicContent.textureData.keys)
        allRawIds.addAll(DynamicContent.modelOverrides.keys)
        allRawIds.addAll(DynamicContent.modelData.keys)
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
                    // blockstates
                    files["assets/$ns/blockstates/$path.json"] =
                        """{"variants":{"":{"model":"$ns:block/$path"}}}"""
                            .toByteArray(StandardCharsets.UTF_8)

                    // block model
                    val blockModelBytes = DynamicContent.modelData[rawId]
                    if (blockModelBytes != null) {
                        files["assets/$ns/models/block/$path.json"] = blockModelBytes
                    } else {
                        val rawParent = DynamicContent.modelOverrides[rawId]
                        val parent = if (rawParent != null) expandModel(rawParent, isBlock = true) else "minecraft:block/cube_all"
                        // текстура для блока — ns:block/path
                        val tex = "$ns:block/$path"
                        // Если parent = cross (растение), то нужен текстурный ключ "cross"
                        val texturesJson = when {
                            parent.endsWith("/cross") || parent == "minecraft:block/cross" -> """"cross":"$tex""""
                            else -> """"all":"$tex","particle":"$tex""""
                        }
                        files["assets/$ns/models/block/$path.json"] =
                            """{"parent":"$parent","textures":{$texturesJson}}"""
                                .toByteArray(StandardCharsets.UTF_8)
                    }

                    if (pngBytes != null) {
                        files["assets/$ns/textures/block/$path.png"] = pngBytes
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

    private fun clientFile(relativePath: String): IoSupplier<InputStream>? {
        val bytes = buildFiles()[relativePath] ?: return null
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
        if (type != PackType.CLIENT_RESOURCES) return null
        return clientFile("assets/${id.namespace}/${id.path}")
    }

    override fun listResources(
        type: PackType,
        namespace: String,
        prefix: String,
        output: PackResources.ResourceOutput
    ) {
        if (type != PackType.CLIENT_RESOURCES) return
        val files = buildFiles()
        val fullPrefix = "assets/$namespace/$prefix"
        for ((path, bytes) in files) {
            if (path.startsWith(fullPrefix)) {
                // path = "assets/<ns>/<rest>"; отрезаем "assets/<ns>/" для Identifier
                val rel = path.removePrefix("assets/").substringAfter('/')
                try {
                    val id = Identifier.fromNamespaceAndPath(namespace, rel)
                    output.accept(id, IoSupplier { ByteArrayInputStream(bytes) })
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun getNamespaces(type: PackType): Set<String> {
        if (type != PackType.CLIENT_RESOURCES) return emptySet()
        val all = mutableSetOf<String>()
        all.addAll(DynamicContent.textureData.keys)
        all.addAll(DynamicContent.modelOverrides.keys)
        all.addAll(DynamicContent.modelData.keys)
        all.addAll(DynamicContent.getKnownIds().mapNotNull {
            try { it.substringAfter(":").let { r -> Identifier.parse(r).namespace } } catch (_: Exception) { null }
        })
        return all.mapNotNull {
            try { Identifier.parse(it).namespace } catch (_: Exception) { null }
        }.toSet()
    }

    override fun <T : Any> getMetadataSection(sectionType: MetadataSectionType<T>): T? = null

    override fun location(): PackLocationInfo = locationInfo

    override fun close() {}
}
