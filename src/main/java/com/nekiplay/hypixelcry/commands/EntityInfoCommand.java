package com.nekiplay.hypixelcry.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.Command;
import com.nekiplay.hypixelcry.HypixelCry;
import com.nekiplay.hypixelcry.annotations.Init;
import com.nekiplay.hypixelcry.utils.EntityUtils;
import com.nekiplay.hypixelcry.utils.TextUtils;
import com.nekiplay.hypixelcry.utils.Utils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityInfoCommand {
    @Init
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("entityinfo")
                            .executes(context -> {
                                execute(context.getSource());
                                return Command.SINGLE_SUCCESS;
                            })
            );
        });
    }

    private static void execute(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        StringBuilder copy = new StringBuilder();

        try {
            Entity nametagEntity = getNearestEntityWithName(client);
            Entity nameEntity = getNearestEntity(client);

            if (nametagEntity != null && nameEntity != null) {
                Text nametag = nametagEntity.getCustomName();
                if (nametag != null) {
                    String formattedNametag = TextUtils.textToFormattingString(nametag);
                    source.sendFeedback(Text.literal(HypixelCry.PREFIX + "[Custom name] " + formattedNametag));
                    copy.append("[Custom name] ").append(formattedNametag).append("\n");
                }

                String formattedName = TextUtils.textToFormattingString(nameEntity.getName());
                source.sendFeedback(Text.literal(HypixelCry.PREFIX + "[Name] " + formattedName));
                copy.append("[Name] ").append(formattedName).append("\n");
            }

            ArmorStandEntity skullEntity = getNearestSkullEntity(client);
            if (skullEntity != null) {
                ItemStack helmet = skullEntity.getEquippedStack(EquipmentSlot.HEAD);
                if (helmet != null && helmet.isOf(Items.PLAYER_HEAD)) {
                    ProfileComponent profile = helmet.get(DataComponentTypes.PROFILE);
                    if (profile != null && profile.getGameProfile() != null && profile.getGameProfile().id() != null) {
                        String id = profile.getGameProfile().id().toString();
                        source.sendFeedback(Text.literal(HypixelCry.PREFIX + "[ArmorStand SkullOwner] " + id));
                        copy.append("[ArmorStand SkullOwner] ").append(id).append("\n");
                    }
                }
            }

            ArmorStandEntity headEntity = getNearestHeadNameEntity(client);
            if (headEntity != null) {
                ItemStack helmet = headEntity.getEquippedStack(EquipmentSlot.HEAD);
                if (helmet != null && !helmet.isEmpty()) {
                    String helmetName = TextUtils.textToFormattingString(helmet.getName());
                    source.sendFeedback(Text.literal(HypixelCry.PREFIX + "[ArmorStand Head Name] " + helmetName));
                    copy.append("[ArmorStand Head name] ").append(helmetName).append("\n");
                }
            }

            PlayerEntity playerEntity = getNearestPlayer(client);
            if (playerEntity != null) {
                GameProfile profile = playerEntity.getGameProfile();
                if (profile != null) {
                    Collection<Property> textures = profile.properties().get("textures");
                    for (Property entry : textures) {
                        if (entry != null && entry.value() != null) {
                            String playerName = playerEntity.getName().getString();
                            source.sendFeedback(Text.literal(HypixelCry.PREFIX + "[" + playerName + "] [Skin id] " + entry.value()));
                            copy.append("[").append(playerName).append("] [Skin id] ").append(entry.value()).append("\n");
                        }
                    }
                }
            }

            if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
                List<ArmorStandEntity> armorStands = client.world.getEntitiesByClass(
                        ArmorStandEntity.class,
                        entityHit.getEntity().getBoundingBox().expand(0, 0.1, 0),
                        e -> e != null && !TextUtils.textToFormattingString(e.getName()).equals("§e§lCLICK")
                );

                if (!armorStands.isEmpty()) {
                    armorStands.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(entityHit.getEntity())));
                    for (ArmorStandEntity armorStand : armorStands) {
                        if (armorStand != null) {
                            String armorStandName = TextUtils.textToFormattingString(armorStand.getName());
                            source.sendFeedback(Text.literal(HypixelCry.PREFIX + "[Entity above cursor] [Name] " + armorStandName));
                            copy.append("[Entity above cursor] [Name] ").append(armorStandName).append("\n");
                        }
                    }

                    ArmorStandEntity first = armorStands.getFirst();
                    if (first != null) {
                        String firstArmorStandName = TextUtils.textToFormattingString(first.getName());
                        source.sendFeedback(Text.literal(HypixelCry.PREFIX + "[Entity cursor] [Name] " + firstArmorStandName));
                        copy.append("[Entity cursor] [Name] ").append(firstArmorStandName).append("\n");
                    }
                }
            }
        } catch (Exception ignored) {

        }
    }

    @Nullable
    private static Entity getNearestEntity(MinecraftClient client) {
        Iterator<Entity> iterator = client.world.getEntities().iterator();
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity != client.player) {
                double distance = entity.squaredDistanceTo(client.player);
                if (distance < closestDistance) {
                    closest = entity;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    @Nullable
    private static Entity getNearestEntityWithName(MinecraftClient client) {
        Iterator<Entity> iterator = client.world.getEntities().iterator();
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity.hasCustomName()) {
                double distance = entity.squaredDistanceTo(client.player);
                if (distance < closestDistance) {
                    closest = entity;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    @Nullable
    private static ArmorStandEntity getNearestHeadNameEntity(MinecraftClient client) {
        List<ArmorStandEntity> armorStands = new ArrayList<>();

        // First collect all armor stands with helmets
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ArmorStandEntity armorStand) {
                if (!armorStand.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                    armorStands.add(armorStand);
                }
            }
        }

        // Then find the closest one
        return armorStands.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(client.player)))
                .orElse(null);
    }

    @Nullable
    private static ArmorStandEntity getNearestSkullEntity(MinecraftClient client) {
        List<ArmorStandEntity> armorStands = new ArrayList<>();

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ArmorStandEntity armorStand) {
                if (EntityUtils.getArmorStandSkullOwner(armorStand) != null) {
                    armorStands.add(armorStand);
                }
            }
        }

        return armorStands.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(client.player)))
                .orElse(null);
    }

    @Nullable
    private static PlayerEntity getNearestPlayer(MinecraftClient client) {
        List<PlayerEntity> players = new ArrayList<>();

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity != client.player) {
                players.add((PlayerEntity) entity);
            }
        }

        return players.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(client.player)))
                .orElse(null);
    }
}
