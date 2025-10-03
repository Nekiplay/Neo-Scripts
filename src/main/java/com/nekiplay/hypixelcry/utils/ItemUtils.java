package com.nekiplay.hypixelcry.utils;

import com.mojang.authlib.properties.Property;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

        return profile.properties().get("textures").stream().filter(Objects::nonNull)
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

    /**
     * Gets the nbt in the custom data component of the item stack.
     * @return The {@link DataComponentTypes#CUSTOM_DATA custom data} of the itemstack,
     *         or an empty {@link NbtCompound} if the itemstack is missing a custom data component
     */
    @SuppressWarnings("deprecation")
    public static @NotNull NbtCompound getCustomData(@NotNull ComponentHolder stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).getNbt();
    }

    public static List<ItemStack> getArmor(LivingEntity entity) {
        return AttributeModifierSlot.ARMOR.getSlots().stream()
                .filter(es -> es.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
                .map(entity::getEquippedStack)
                .toList();
    }
}
