package com.nekiplay.hypixelcry.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.nekiplay.hypixelcry.HypixelCry.mc;

public class EntityUtils {
    @Nullable
    public static String getCustomNametag(Entity entity) {
        if (entity.hasCustomName() && entity.getCustomName() != null) {
            return entity.getCustomName().getString();
        }
        else {
            return null;
        }
    }

    @Nullable
    public static String getArmorStandSkullOwner(ArmorStandEntity entity) {
        ItemStack helmet = entity.getEquippedStack(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || !helmet.isOf(Items.PLAYER_HEAD)) {
            return null;
        }

        // Получаем компоненты предмета
        ComponentMap components = helmet.getComponents();

        // Получаем компонент с данными черепа
        ProfileComponent profileComponent = components.get(DataComponentTypes.PROFILE);
        if (profileComponent != null) {
            GameProfile profile = profileComponent.getGameProfile();
            if (profile != null && profile.id() != null) {
                return profile.id().toString();
            }
        }
        return null;
    }

    @Nullable
    public static String getArmorStandHeadName(ArmorStandEntity entity) {
        ItemStack helmet = entity.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            return helmet.getName().getLiteralString();
        }
        return null;
    }

    @Nullable
    public static String getPlayerSkin(PlayerEntity player) {
        if (player.getGameProfile() == null) {
            return null;
        }
        Map<String, Collection<Property>> map = player.getGameProfile().properties().asMap();
        Collection<Property> textures = map.get("textures");

        Property texture = textures.stream().findFirst().orElse(null);
        if (texture != null) {
            return texture.value();
        }
        return null;
    }

    @Nullable
    public static List<ArmorStandEntity> getArmorStandAboveEntity(Entity targetEntity, float maxDistance, List<String> blackListNames) {
        if (targetEntity == null) {
            return null;
        }

        List<ArmorStandEntity> entities = new ArrayList<>();

        World world = mc.world;
        // Ищем ArmorStand в небольшом кубе над сущностью
        List<Entity> nearbyEntities = world.getOtherEntities(
                targetEntity,
                targetEntity.getBoundingBox()
                        .expand(0, maxDistance, 0)  // расширяем только по Y (высота)
        );

        for (Entity entity : nearbyEntities) {
            if (entity instanceof ArmorStandEntity) {
                ArmorStandEntity armorStand = (ArmorStandEntity) entity;
                if (!blackListNames.contains(armorStand.getName().getString())) {
                    entities.add(armorStand);
                }
            }
        }

        // Сортируем по расстоянию до целевой сущности
        entities.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(targetEntity)));

        return entities;
    }
}
