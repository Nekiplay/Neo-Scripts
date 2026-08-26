package com.nekiplay.neoscripts.common.features.creative

import com.nekiplay.neoscripts.ServerMain
import com.nekiplay.neoscripts.common.features.lua.objects.misc.DynamicContent
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Креативная вкладка "Neo Scripts" для всех предметов, зарегистрированных через Lua `content.*`.
 * Создается в `ServerMain.onInitialize` (entrypoint `main` — выполняется и на клиенте, и на сервере).
 * Содержимое — динамическое: на каждый `displayItems` собирает актуальные `DynamicContent.getKnownIds()` c префиксом `item:`.
 * Иконка — первый зарегистрированный предмет, иначе алмаз.
 */
object DynamicCreativeTab {
    val TAB_KEY: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Identifier.fromNamespaceAndPath(ServerMain.NAMESPACE, "neo_scripts")
    )

    lateinit var TAB: CreativeModeTab
        private set

    fun register() {
        TAB = FabricCreativeModeTab.builder()
            .icon {
                val firstRaw = DynamicContent.getKnownIds()
                    .firstOrNull { it.startsWith("item:") }
                    ?.removePrefix("item:")
                if (firstRaw != null) {
                    try {
                        val id = Identifier.parse(firstRaw)
                        val holder = BuiltInRegistries.ITEM.get(id)
                        if (holder.isPresent) {
                            return@icon ItemStack(holder.get().value())
                        }
                    } catch (_: Exception) {
                    }
                }
                ItemStack(Items.DIAMOND)
            }
            .title(Component.literal("Neo Scripts"))
            .displayItems { _, output ->
                val items = DynamicContent.getKnownIds()
                    .filter { it.startsWith("item:") }
                    .map { it.removePrefix("item:") }
                    .sorted()
                for (rawId in items) {
                    try {
                        val id = Identifier.parse(rawId)
                        val holder = BuiltInRegistries.ITEM.get(id)
                        if (holder.isPresent) {
                            output.accept(holder.get().value() as net.minecraft.world.level.ItemLike)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
            .build()

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB)
        ServerMain.LOGGER?.info("[Neo Scripts] Registered creative tab $TAB_KEY")
    }
}
