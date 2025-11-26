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
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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
        Minecraft client = Minecraft.getInstance();
        StringBuilder copy = new StringBuilder();

        try {
            Entity nametagEntity = getNearestEntityWithName(client);
            Entity nameEntity = getNearestEntity(client);

            if (nametagEntity != null && nameEntity != null) {
                Component nametag = nametagEntity.getCustomName();
                if (nametag != null) {
                    String formattedNametag = TextUtils.textToFormattingString(nametag);
                    source.sendFeedback(Component.literal(HypixelCry.PREFIX + "[Custom name] " + formattedNametag));
                    copy.append("[Custom name] ").append(formattedNametag).append("\n");
                }

                String formattedName = TextUtils.textToFormattingString(nameEntity.getName());
                source.sendFeedback(Component.literal(HypixelCry.PREFIX + "[Name] " + formattedName));
                copy.append("[Name] ").append(formattedName).append("\n");
            }

            ArmorStand skullEntity = getNearestSkullEntity(client);
            if (skullEntity != null) {
                ItemStack helmet = skullEntity.getItemBySlot(EquipmentSlot.HEAD);
                if (helmet != null && helmet.is(Items.PLAYER_HEAD)) {
                    ResolvableProfile profile = helmet.get(DataComponents.PROFILE);
                    if (profile != null && profile.partialProfile() != null && profile.partialProfile().id() != null) {
                        String id = profile.partialProfile().id().toString();
                        source.sendFeedback(Component.literal(HypixelCry.PREFIX + "[ArmorStand SkullOwner] " + id));
                        copy.append("[ArmorStand SkullOwner] ").append(id).append("\n");
                    }
                }
            }

            ArmorStand headEntity = getNearestHeadNameEntity(client);
            if (headEntity != null) {
                ItemStack helmet = headEntity.getItemBySlot(EquipmentSlot.HEAD);
                if (helmet != null && !helmet.isEmpty()) {
                    String helmetName = TextUtils.textToFormattingString(helmet.getHoverName());
                    source.sendFeedback(Component.literal(HypixelCry.PREFIX + "[ArmorStand Head Name] " + helmetName));
                    copy.append("[ArmorStand Head name] ").append(helmetName).append("\n");
                }
            }

            Player playerEntity = getNearestPlayer(client);
            if (playerEntity != null) {
                GameProfile profile = playerEntity.getGameProfile();
                if (profile != null) {
                    Collection<Property> textures = profile.properties().get("textures");
                    for (Property entry : textures) {
                        if (entry != null && entry.value() != null) {
                            String playerName = playerEntity.getName().getString();
                            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "[" + playerName + "] [Skin id] " + entry.value()));
                            copy.append("[").append(playerName).append("] [Skin id] ").append(entry.value()).append("\n");
                        }
                    }
                }
            }

            if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) client.hitResult;
                List<ArmorStand> armorStands = client.level.getEntitiesOfClass(
                        ArmorStand.class,
                        entityHit.getEntity().getBoundingBox().inflate(0, 0.1, 0),
                        e -> e != null && !TextUtils.textToFormattingString(e.getName()).equals("§e§lCLICK")
                );

                if (!armorStands.isEmpty()) {
                    armorStands.sort(Comparator.comparingDouble(e -> e.distanceToSqr(entityHit.getEntity())));
                    for (ArmorStand armorStand : armorStands) {
                        if (armorStand != null) {
                            String armorStandName = TextUtils.textToFormattingString(armorStand.getName());
                            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "[Entity above cursor] [Name] " + armorStandName));
                            copy.append("[Entity above cursor] [Name] ").append(armorStandName).append("\n");
                        }
                    }

                    ArmorStand first = armorStands.getFirst();
                    if (first != null) {
                        String firstArmorStandName = TextUtils.textToFormattingString(first.getName());
                        source.sendFeedback(Component.literal(HypixelCry.PREFIX + "[Entity cursor] [Name] " + firstArmorStandName));
                        copy.append("[Entity cursor] [Name] ").append(firstArmorStandName).append("\n");
                    }
                }
            }
        } catch (Exception ignored) {

        }
    }

    @Nullable
    private static Entity getNearestEntity(Minecraft client) {
        Iterator<Entity> iterator = client.level.entitiesForRendering().iterator();
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity != client.player) {
                double distance = entity.distanceToSqr(client.player);
                if (distance < closestDistance) {
                    closest = entity;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    @Nullable
    private static Entity getNearestEntityWithName(Minecraft client) {
        Iterator<Entity> iterator = client.level.entitiesForRendering().iterator();
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity.hasCustomName()) {
                double distance = entity.distanceToSqr(client.player);
                if (distance < closestDistance) {
                    closest = entity;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    @Nullable
    private static ArmorStand getNearestHeadNameEntity(Minecraft client) {
        List<ArmorStand> armorStands = new ArrayList<>();

        // First collect all armor stands with helmets
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand armorStand) {
                if (!armorStand.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                    armorStands.add(armorStand);
                }
            }
        }

        // Then find the closest one
        return armorStands.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(client.player)))
                .orElse(null);
    }

    @Nullable
    private static ArmorStand getNearestSkullEntity(Minecraft client) {
        List<ArmorStand> armorStands = new ArrayList<>();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand armorStand) {
                if (EntityUtils.getArmorStandSkullOwner(armorStand) != null) {
                    armorStands.add(armorStand);
                }
            }
        }

        return armorStands.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(client.player)))
                .orElse(null);
    }

    @Nullable
    private static Player getNearestPlayer(Minecraft client) {
        List<Player> players = new ArrayList<>();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Player && entity != client.player) {
                players.add((Player) entity);
            }
        }

        return players.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(client.player)))
                .orElse(null);
    }
}
