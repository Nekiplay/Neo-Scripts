package com.nekiplay.neoscripts.client.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;

import static com.nekiplay.neoscripts.ClientMain.mc;

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
    public static String getArmorStandSkullOwner(ArmorStand entity) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || !helmet.is(Items.PLAYER_HEAD)) {
            return null;
        }

        // Получаем компоненты предмета
        DataComponentMap components = helmet.getComponents();

        // Получаем компонент с данными черепа
        ResolvableProfile profileComponent = components.get(DataComponents.PROFILE);
        if (profileComponent != null) {
            GameProfile profile = profileComponent.partialProfile();
            if (profile != null && profile.id() != null) {
                return profile.id().toString();
            }
        }
        return null;
    }

    @Nullable
    public static String getArmorStandHeadName(ArmorStand entity) {
        ItemStack helmet = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            return helmet.getHoverName().tryCollapseToString();
        }
        return null;
    }

    @Nullable
    public static String getPlayerSkin(Player player) {
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
    public static List<ArmorStand> getArmorStandAboveEntity(Entity targetEntity, float maxDistance, List<String> blackListNames) {
        if (targetEntity == null) {
            return null;
        }

        List<ArmorStand> entities = new ArrayList<>();

        Level world = mc.level;
        // Ищем ArmorStand в небольшом кубе над сущностью
        List<Entity> nearbyEntities = world.getEntities(
                targetEntity,
                targetEntity.getBoundingBox()
                        .inflate(0, maxDistance, 0)  // расширяем только по Y (высота)
        );

        for (Entity entity : nearbyEntities) {
            if (entity instanceof ArmorStand) {
                ArmorStand armorStand = (ArmorStand) entity;
                if (!blackListNames.contains(armorStand.getName().getString())) {
                    entities.add(armorStand);
                }
            }
        }

        // Сортируем по расстоянию до целевой сущности
        entities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(targetEntity)));

        return entities;
    }
}
