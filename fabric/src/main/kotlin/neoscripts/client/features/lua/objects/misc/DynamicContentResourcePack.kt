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
 * Рантайм ресурспак с моделями и текстурами динамических предметов.
 *
 * Для каждого предмета, зарегистрированного через content.registerItem(...)
 * с settings.texture, генерирует стандартные ассеты:
 *   assets/<ns>/items/<path>.json        — item model definition (26.x)
 *   assets/<ns>/models/item/<path>.json  — модель item/generated с layer0
 *   assets/<ns>/textures/item/<path>.png — содержимое файла текстуры
 *
 * Пак добавляется в PackRepository через миксин (PackRepositoryMixin) при
 * создании репозитория — до первой загрузки ресурсов, поэтому текстуры
 * попадают в первый же бейк моделей и видны в инвентаре/в руке.
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

    private fun buildFiles(): ConcurrentHashMap<String, ByteArray> {
        val files = ConcurrentHashMap<String, ByteArray>()
        for ((rawId, pngBytes) in DynamicContent.textureData) {
            try {
                val id = Identifier.parse(rawId)
                val ns = id.namespace
                val path = id.path

                files["assets/$ns/items/$path.json"] =
                    """{"model":{"type":"minecraft:model","model":"$ns:item/$path"}}"""
                        .toByteArray(StandardCharsets.UTF_8)

                files["assets/$ns/models/item/$path.json"] =
                    """{"parent":"minecraft:item/generated","textures":{"layer0":"$ns:item/$path"}}"""
                        .toByteArray(StandardCharsets.UTF_8)

                files["assets/$ns/textures/item/$path.png"] = pngBytes
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
        return DynamicContent.textureData.keys.mapNotNull {
            try {
                Identifier.parse(it).namespace
            } catch (_: Exception) {
                null
            }
        }.toSet()
    }

    override fun <T : Any> getMetadataSection(sectionType: MetadataSectionType<T>): T? = null

    override fun location(): PackLocationInfo = locationInfo

    override fun close() {}
}
