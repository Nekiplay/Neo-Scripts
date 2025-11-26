package com.nekiplay.hypixelcry.utils.itemlist;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.nekiplay.hypixelcry.annotations.Init;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Attributes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation ATTRIBUTES_FILE = ResourceLocation.fromNamespaceAndPath("hypixelcry", "hunting/attributes.json");
    private static List<Attribute> attributes = List.of();

    @Init
    public static void init() {
        ClientLifecycleEvents.CLIENT_STARTED.register(Attributes::loadShards);
    }

    private static void loadShards(Minecraft client) {
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = client.getResourceManager().openAsReader(ATTRIBUTES_FILE)) {
                attributes = Attribute.LIST_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
            } catch (Exception e) {
                LOGGER.error("[Skyblocker Attributes] Failed to load attributes.", e);
            }
        });
    }

    @Nullable
    public static Attribute getAttributeFromItemName(DataComponentHolder stack) {
        if (!stack.has(DataComponents.CUSTOM_NAME)) return null;
        String name = stack.get(DataComponents.CUSTOM_NAME).getString();
        name = name.replace(" Shard", ""); // Shards outside the hunting box now have "Shard" in their item name.
        name = name.replace("BUY ", "").replace("SELL ", ""); // Bazaar Buy/Sell orders

        for (Attribute attribute : attributes) {
            if (attribute.shardName().equals(name)) return attribute;
        }

        return null;
    }
}