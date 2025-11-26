package com.nekiplay.hypixelcry.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.JsonOps;
import com.nekiplay.hypixelcry.utils.itemlist.*;
import it.unimi.dsi.fastutil.doubles.DoubleBooleanPair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.component.type.*;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ItemUtils {
    public static final String ID = "id";

    /**
     * Gets the Skyblock item id of the item stack.
     *
     * @param stack the item stack to get the internal name from
     * @return the Skyblock item id of the item stack, or an empty string if the item stack does not have a Skyblock id
     */
    public static @NotNull String getItemUuid(@NotNull DataComponentHolder stack) {
        return getCustomData(stack).getStringOr("uuid", "");
    }

    public static @NotNull String getItemId(@NotNull DataComponentHolder stack) {
        return getCustomData(stack).getStringOr(ID, "");
    }

    public static @NotNull Boolean isRecombobulated(@NotNull DataComponentHolder stack) {
        return getCustomData(stack).getIntOr("rarity_upgrades", 0) > 0;
    }

    public static @NotNull Boolean isMuseumDonated(@NotNull DataComponentHolder stack) {
        return getCustomData(stack).getBooleanOr("donated_museum", false);
    }

    public static @NotNull String getReforgeModifier(@NotNull DataComponentHolder stack) {
        return getCustomData(stack).getStringOr("modifier", "");
    }

    public static @NotNull String getHeadTexture(@NotNull ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD) || !stack.has(DataComponents.PROFILE)) return "";

        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return "";

        return profile.partialProfile().properties().get("textures").stream().filter(Objects::nonNull)
                .map(Property::value)
                .findFirst()
                .orElse("");
    }



    public static @NotNull List<Component> getLore(ItemStack stack) {
        return stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).styledLines();
    }

    public static @NotNull Component getDisplayName(ItemStack stack) {
        if (stack == null || stack.getCustomName() == null) {
            return Component.empty();
        }
        return stack.getCustomName();
    }

    public static void setDisplayName(ItemStack stack, Component name) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(DataComponents.CUSTOM_NAME, name);
    }

    public static @NotNull Map<String, Integer> getHypixelEnchantments(ItemStack itemStack) {
        Map<String, Integer> result = new HashMap<>();
        CompoundTag extraAttributes = getCustomData(itemStack);

        Optional<CompoundTag> enchantmentsOpt = extraAttributes.getCompound("enchantments");
        if (enchantmentsOpt.isEmpty()) {
            return result;
        }

        CompoundTag enchantments = enchantmentsOpt.get();

        for (String key : enchantments.keySet()) {
            result.put(key, enchantments.getIntOr(key, 0));
        }

        return result;
    }

    public static @NotNull PropertyMap propertyMapWithTexture(String textureValue) {
        return ExtraCodecs.PROPERTY_MAP.parse(JsonOps.INSTANCE, JsonParser.parseString("[{\"name\":\"textures\",\"value\":\"" + textureValue + "\"}]")).getOrThrow();
    }

    @SuppressWarnings("Varargs")
    public static @NotNull ItemStack createSkull(String textureBase64) {
        GameProfile profile = new GameProfile(java.util.UUID.randomUUID(), "a", propertyMapWithTexture(textureBase64));
        return createSkull(profile);
    }

    public static @NotNull ItemStack createSkull(GameProfile profile) {
        try {
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            return stack;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ItemStack createItemStack(Item item, String displayName, String model) {
        return createItemStack(item, displayName, List.of(""), 1, 0, "");
    }

    // Overload to avoid spread operators
    public static ItemStack createItemStack(Item item, String displayName, String[] loreArray, int amount, int damage, String model) {
        return createItemStack(item, displayName, List.of(loreArray), amount, damage, model);
    }

    public static ItemStack setLore(ItemStack stack, List<String> lore) {
        stack.set(DataComponents.LORE, new ItemLore(lore.stream().map(Component::nullToEmpty)
             .collect(Collectors.toList())));

        return stack;
    }

    public static ItemStack setCustomItemName(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty(name));

        return stack;
    }

    // Taken from NEU
    public static ItemStack createItemStack(Item item, String displayName, List<String> lore, int amount, int damage, String model) {
        ItemStack stack = new ItemStack(item, amount);
        stack = setCustomItemName(stack, displayName);
        stack = setLore(stack, lore);
        var tooltip = net.minecraft.world.item.component.TooltipDisplay.DEFAULT
             .withHidden(DataComponents.DAMAGE, true)
             .withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true)
             .withHidden(DataComponents.UNBREAKABLE, true);
        if (!model.isEmpty()) {
            CustomModelData customModel = new CustomModelData(
                    List.of(),             // floats
                    List.of(),             // flags
                    List.of(model),   // strings, сюда можно записать свой идентификатор или тег для модели
                    List.of()              // colors
            );

            stack.set(DataComponents.CUSTOM_MODEL_DATA, customModel);
        }
        if (displayName.isBlank() && lore.isEmpty()) {
             tooltip = new net.minecraft.world.item.component.TooltipDisplay(true, tooltip.hiddenComponents());
         }
        stack.set(DataComponents.TOOLTIP_DISPLAY, tooltip);
        return stack;
    }

    /**
     * Gets the nbt in the custom data component of the item stack.
     * @return The {@link DataComponents#CUSTOM_DATA custom data} of the itemstack,
     *         or an empty {@link CompoundTag} if the itemstack is missing a custom data component
     */
    @SuppressWarnings("deprecation")
    public static @NotNull CompoundTag getCustomData(@NotNull DataComponentHolder stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static List<ItemStack> getArmor(LivingEntity entity) {
        return EquipmentSlotGroup.ARMOR.slots().stream()
                .filter(es -> es.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
                .map(entity::getItemBySlot)
                .toList();
    }

    @NotNull
    public static PetInfo getPetInfo(ItemStack stack) {
        if (!getItemId(stack).equals("PET")) return PetInfo.EMPTY;

        String petInfo = getCustomData(stack).getStringOr("petInfo", "");

        if (!petInfo.isEmpty()) {
            try {
                JsonElement jsonElement = JsonParser.parseString(petInfo);

                // Add item name into PetInfo to be used for wiki lookup
                jsonElement.getAsJsonObject().addProperty("name", stack.getHoverName().getString());
                return PetInfo.CODEC.parse(JsonOps.INSTANCE, jsonElement)
                        .setPartial(PetInfo.EMPTY)
                        .getPartialOrThrow();
            } catch (Exception ignored) {}
        }

        return PetInfo.EMPTY;
    }

    public static @NotNull String getNeuId(ItemStack stack) {
        if (stack == null) return "";
        String id = getItemId(stack);
        CompoundTag customData = ItemUtils.getCustomData(stack);
        return switch (id) {
            case "ENCHANTED_BOOK" -> {
                CompoundTag enchantments = customData.getCompoundOrEmpty("enchantments");
                String enchant = enchantments.keySet().stream().findFirst().orElse("");
                yield enchant.toUpperCase(Locale.ENGLISH) + ";" + enchantments.getIntOr(enchant, 0);
            }
            case "PET" -> {
                if (!customData.contains("petInfo")) yield id;
                PetInfo petInfo = PetInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(customData.getStringOr("petInfo", ""))).getOrThrow();
                yield petInfo.type() + ';' + petInfo.tierIndex();
            }
            case "RUNE" -> {
                CompoundTag runes = customData.getCompoundOrEmpty("runes");
                String rune = runes.keySet().stream().findFirst().orElse("");
                yield rune.toUpperCase(Locale.ENGLISH) + "_RUNE;" + runes.getIntOr(rune, 0);
            }
            case "POTION" -> "POTION_" + customData.getStringOr("potion", "").toUpperCase(Locale.ENGLISH) + ";" + customData.getIntOr("potion_level", 0);
            case "ATTRIBUTE_SHARD" -> {
                Attribute attribute = Attributes.getAttributeFromItemName(stack);
                if (attribute == null) yield id;
                yield ItemRepository.getBazaarStocks().getOrDefault(attribute.apiId(), id);
            }
            case "PARTY_HAT_CRAB", "BALLOON_HAT_2024", "BALLOON_HAT_2025" -> id + "_" + customData.getStringOr("party_hat_color", "").toUpperCase(Locale.ENGLISH);
            case "PARTY_HAT_CRAB_ANIMATED" -> "PARTY_HAT_CRAB_" + customData.getStringOr("party_hat_color", "").toUpperCase(Locale.ENGLISH) + "_ANIMATED";
            case "PARTY_HAT_SLOTH" -> id + "_" + customData.getStringOr("party_hat_emoji", "").toUpperCase(Locale.ENGLISH);
            default -> id.replace(":", "-");
        };
    }
}
