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
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.dynamic.Codecs;
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
    public static @NotNull String getItemUuid(@NotNull ComponentHolder stack) {
        return getCustomData(stack).getString("uuid", "");
    }

    public static @NotNull String getItemId(@NotNull ComponentHolder stack) {
        return getCustomData(stack).getString(ID, "");
    }

    public static @NotNull Boolean isRecombobulated(@NotNull ComponentHolder stack) {
        return getCustomData(stack).getInt("rarity_upgrades", 0) > 0;
    }

    public static @NotNull Boolean isMuseumDonated(@NotNull ComponentHolder stack) {
        return getCustomData(stack).getBoolean("donated_museum", false);
    }

    public static @NotNull String getReforgeModifier(@NotNull ComponentHolder stack) {
        return getCustomData(stack).getString("modifier", "");
    }

    public static @NotNull String getHeadTexture(@NotNull ItemStack stack) {
        if (!stack.isOf(Items.PLAYER_HEAD) || !stack.contains(DataComponentTypes.PROFILE)) return "";

        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
        if (profile == null) return "";

        return profile.getGameProfile().properties().get("textures").stream().filter(Objects::nonNull)
                .map(Property::value)
                .findFirst()
                .orElse("");
    }



    public static @NotNull List<Text> getLore(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).styledLines();
    }

    public static @NotNull Text getDisplayName(ItemStack stack) {
        if (stack == null || stack.getCustomName() == null) {
            return Text.empty();
        }
        return stack.getCustomName();
    }

    public static void setDisplayName(ItemStack stack, Text name) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(DataComponentTypes.CUSTOM_NAME, name);
    }

    public static @NotNull Map<String, Integer> getHypixelEnchantments(ItemStack itemStack) {
        Map<String, Integer> result = new HashMap<>();
        NbtCompound extraAttributes = getCustomData(itemStack);

        Optional<NbtCompound> enchantmentsOpt = extraAttributes.getCompound("enchantments");
        if (enchantmentsOpt.isEmpty()) {
            return result;
        }

        NbtCompound enchantments = enchantmentsOpt.get();

        for (String key : enchantments.getKeys()) {
            result.put(key, enchantments.getInt(key, 0));
        }

        return result;
    }

    public static @NotNull PropertyMap propertyMapWithTexture(String textureValue) {
        return Codecs.GAME_PROFILE_PROPERTY_MAP.parse(JsonOps.INSTANCE, JsonParser.parseString("[{\"name\":\"textures\",\"value\":\"" + textureValue + "\"}]")).getOrThrow();
    }

    @SuppressWarnings("Varargs")
    public static @NotNull ItemStack createSkull(String textureBase64) {
        GameProfile profile = new GameProfile(java.util.UUID.randomUUID(), "a", propertyMapWithTexture(textureBase64));
        return createSkull(profile);
    }

    public static @NotNull ItemStack createSkull(GameProfile profile) {
        try {
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            stack.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(profile));
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
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore.stream().map(Text::of)
             .collect(Collectors.toList())));

        return stack;
    }

    public static ItemStack setCustomItemName(ItemStack stack, String name) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.of(name));

        return stack;
    }

    // Taken from NEU
    public static ItemStack createItemStack(Item item, String displayName, List<String> lore, int amount, int damage, String model) {
        ItemStack stack = new ItemStack(item, amount);
        stack = setCustomItemName(stack, displayName);
        stack = setLore(stack, lore);
        var tooltip = net.minecraft.component.type.TooltipDisplayComponent.DEFAULT
             .with(DataComponentTypes.DAMAGE, true)
             .with(DataComponentTypes.ATTRIBUTE_MODIFIERS, true)
             .with(DataComponentTypes.UNBREAKABLE, true);
        if (!model.isEmpty()) {
            CustomModelDataComponent customModel = new CustomModelDataComponent(
                    List.of(),             // floats
                    List.of(),             // flags
                    List.of(model),   // strings, сюда можно записать свой идентификатор или тег для модели
                    List.of()              // colors
            );

            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, customModel);
        }
        if (displayName.isBlank() && lore.isEmpty()) {
             tooltip = new net.minecraft.component.type.TooltipDisplayComponent(true, tooltip.hiddenComponents());
         }
        stack.set(DataComponentTypes.TOOLTIP_DISPLAY, tooltip);
        return stack;
    }

    /**
     * Gets the nbt in the custom data component of the item stack.
     * @return The {@link DataComponentTypes#CUSTOM_DATA custom data} of the itemstack,
     *         or an empty {@link NbtCompound} if the itemstack is missing a custom data component
     */
    @SuppressWarnings("deprecation")
    public static @NotNull NbtCompound getCustomData(@NotNull ComponentHolder stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }

    public static List<ItemStack> getArmor(LivingEntity entity) {
        return AttributeModifierSlot.ARMOR.getSlots().stream()
                .filter(es -> es.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
                .map(entity::getEquippedStack)
                .toList();
    }

    @NotNull
    public static PetInfo getPetInfo(ItemStack stack) {
        if (!getItemId(stack).equals("PET")) return PetInfo.EMPTY;

        String petInfo = getCustomData(stack).getString("petInfo", "");

        if (!petInfo.isEmpty()) {
            try {
                JsonElement jsonElement = JsonParser.parseString(petInfo);

                // Add item name into PetInfo to be used for wiki lookup
                jsonElement.getAsJsonObject().addProperty("name", stack.getName().getString());
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
        NbtCompound customData = ItemUtils.getCustomData(stack);
        return switch (id) {
            case "ENCHANTED_BOOK" -> {
                NbtCompound enchantments = customData.getCompoundOrEmpty("enchantments");
                String enchant = enchantments.getKeys().stream().findFirst().orElse("");
                yield enchant.toUpperCase(Locale.ENGLISH) + ";" + enchantments.getInt(enchant, 0);
            }
            case "PET" -> {
                if (!customData.contains("petInfo")) yield id;
                PetInfo petInfo = PetInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(customData.getString("petInfo", ""))).getOrThrow();
                yield petInfo.type() + ';' + petInfo.tierIndex();
            }
            case "RUNE" -> {
                NbtCompound runes = customData.getCompoundOrEmpty("runes");
                String rune = runes.getKeys().stream().findFirst().orElse("");
                yield rune.toUpperCase(Locale.ENGLISH) + "_RUNE;" + runes.getInt(rune, 0);
            }
            case "POTION" -> "POTION_" + customData.getString("potion", "").toUpperCase(Locale.ENGLISH) + ";" + customData.getInt("potion_level", 0);
            case "ATTRIBUTE_SHARD" -> {
                Attribute attribute = Attributes.getAttributeFromItemName(stack);
                if (attribute == null) yield id;
                yield ItemRepository.getBazaarStocks().getOrDefault(attribute.apiId(), id);
            }
            case "PARTY_HAT_CRAB", "BALLOON_HAT_2024", "BALLOON_HAT_2025" -> id + "_" + customData.getString("party_hat_color", "").toUpperCase(Locale.ENGLISH);
            case "PARTY_HAT_CRAB_ANIMATED" -> "PARTY_HAT_CRAB_" + customData.getString("party_hat_color", "").toUpperCase(Locale.ENGLISH) + "_ANIMATED";
            case "PARTY_HAT_SLOTH" -> id + "_" + customData.getString("party_hat_emoji", "").toUpperCase(Locale.ENGLISH);
            default -> id.replace(":", "-");
        };
    }
}
