package com.nekiplay.hypixelcry.features.esp.mining.glacitemineshafts;

import com.nekiplay.hypixelcry.HypixelCry;
import com.nekiplay.hypixelcry.annotations.Init;
import com.nekiplay.hypixelcry.config.enums.ESPFeatures;
import com.nekiplay.hypixelcry.events.SkyblockEvents;
import com.nekiplay.hypixelcry.features.esp.pathfinder.PathFinderWorker;
import com.nekiplay.hypixelcry.utils.ItemUtils;
import com.nekiplay.hypixelcry.utils.Location;
import com.nekiplay.hypixelcry.utils.SpecialColor;
import com.nekiplay.hypixelcry.utils.render.RenderHelper;
import com.nekiplay.hypixelcry.utils.scheduler.Scheduler;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.EnumUtils;

import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nekiplay.hypixelcry.HypixelCry.*;

public class FrozenCourpes {
    private static boolean isLocationCorrect = false;

    private static final Pattern CORPSE_FOUND_PATTERN = Pattern.compile("([A-Z]+) CORPSE LOOT!");
    private static final Map<CorpseType, List<Corpse>> corpsesByType = new EnumMap<>(CorpseType.class);

    @Init
    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((ignored, ignored2, ignored3) -> {
            isLocationCorrect = false;
            clearPathFinder();
            corpsesByType.clear();
        });
        SkyblockEvents.LOCATION_CHANGE.register(FrozenCourpes::handleLocationChange);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(FrozenCourpes::render);
        ClientReceiveMessageEvents.GAME.register(FrozenCourpes::onChatMessage);
        Scheduler.INSTANCE.scheduleCyclic(FrozenCourpes::onTick, 2);
        config.esp.glaciteMineshafts.frozenCourpes.enabledPathFinder.whenChanged((old, current) -> {
            if (!current) {
                clearPathFinder();
                corpsesByType.clear();
            }
        });
    }

    private static void clearPathFinder() {
        for (List<Corpse> corpses : corpsesByType.values()) {
            for (Corpse corpse : corpses) {
                if (PathFinderWorker.hasPath(Integer.toString(corpse.entity.getId()))) {
                    PathFinderWorker.removePath(Integer.toString(corpse.entity.getId()));
                }
            }
        }
    }

    private static void handleLocationChange(Location location) {
        isLocationCorrect = location == Location.GLACITE_MINESHAFT;
        if (!isLocationCorrect) {
            clearPathFinder();
            corpsesByType.clear();
        }
    }

    private static void onChatMessage(Text text, boolean b) {
        if (MinecraftClient.getInstance().player == null) return;
        String string = text.getString();
        if (string.contains(MinecraftClient.getInstance().getSession().getUsername())) return; // Ignore your own messages

        Matcher matcherCorpse = CORPSE_FOUND_PATTERN.matcher(string);
        if (!matcherCorpse.find()) return;

        LOGGER.debug(PREFIX + "Triggered code for onChatMessage");
        LOGGER.debug(PREFIX + "State of corpsesByType: {}", corpsesByType);
        String corpseTypeString = matcherCorpse.group(1).toUpperCase(Locale.ENGLISH);
        CorpseType corpseType = EnumUtils.getEnum(CorpseType.class, corpseTypeString, CorpseType.UNKNOWN);

        List<Corpse> corpses = corpsesByType.get(corpseType);
        if (corpses == null) {
            LOGGER.warn(PREFIX + "Couldn't get corpses! corpse type string: {}, parsed corpse type: {}", corpseTypeString, corpseType);
            return;
        }
        corpses.stream() // Since squared distance comparison will yield the same result as normal distance comparison, we can use squared distance to avoid square root calculation
                .min(Comparator.comparingDouble(corpse -> corpse.entity.squaredDistanceTo(MinecraftClient.getInstance().player)))
                .ifPresentOrElse(
                        corpse -> {
                            if (PathFinderWorker.hasPath(Integer.toString(corpse.entity.getId()))) {
                                PathFinderWorker.removePath(Integer.toString(corpse.entity.getId()));
                            }
                            LOGGER.info(PREFIX + "Found corpse, marking as found! {}: {}", corpse.entity.getType(), corpse.entity.getBlockPos().toShortString());
                            corpse.found = true;
                        },
                        () -> LOGGER.warn(PREFIX + "Couldn't find the closest corpse despite triggering onChatMessage!")
                );
    }

    public static void checkIfCorpse(Entity entity) {
        if (entity instanceof ArmorStandEntity armorStand) checkIfCorpse(armorStand);
    }

    public static void checkIfCorpse(ArmorStandEntity armorStand) {
        if (!isLocationCorrect) return;
        if (armorStand.hasCustomName() || armorStand.isInvisible() || armorStand.shouldShowBasePlate()) return;
        handleArmorStand(armorStand);
    }

    private static void handleArmorStand(ArmorStandEntity armorStand) {
        String helmetItemId = ItemUtils.getItemId(armorStand.getEquippedStack(EquipmentSlot.HEAD));
        CorpseType corpseType = CorpseType.fromHelmetItemId(helmetItemId);
        if (corpseType == CorpseType.UNKNOWN) return;

        LOGGER.debug(PREFIX + "Triggered code for handleArmorStand and matched with ITEM_IDS");
        List<Corpse> corpses = corpsesByType.computeIfAbsent(corpseType, k -> new ArrayList<>());
        if (corpses.stream().noneMatch(c -> c.entity.getBlockPos().equals(armorStand.getBlockPos()))) {
            Corpse newCorpse = new Corpse(armorStand, armorStand.getBlockPos().up(), corpseType);
            corpses.add(newCorpse);
            if (config.esp.glaciteMineshafts.frozenCourpes.enabledPathFinder.get()) {
                float[] colorComponents = SpecialColor.toSpecialColorFloatArray(newCorpse.corpseType.color);
                PathFinderWorker.addOrUpdatePath(Integer.toString(armorStand.getId()), armorStand.getBlockPos().up(),
                    colorComponents, newCorpse.corpseType.name());
            }
        }
    }


    private static void render(WorldRenderContext worldRenderContext) {
        if (!isLocationCorrect || !config.esp.glaciteMineshafts.frozenCourpes.enabled) return;

        float lineWidth = 1.5f;
        boolean throughWalls = true;

        for (List<Corpse> corpses : corpsesByType.values()) {
            for (Corpse corpse : corpses) {
                float[] colorComponents = SpecialColor.toSpecialColorFloatArray(corpse.corpseType.color);
                float alpha = colorComponents[3];
                int colorComponentsNoAlpha = SpecialColor.toSpecialColorIntNoAlpha(corpse.corpseType.color);

                if (!corpse.found) {
                    if (HypixelCry.config.esp.glaciteMineshafts.frozenCourpes.features.contains(ESPFeatures.Box)) {
                        RenderHelper.renderFilled(worldRenderContext, corpse.blockPos, colorComponents, alpha, throughWalls);
                        RenderHelper.renderOutline(worldRenderContext, corpse.blockPos, colorComponents, lineWidth, throughWalls);
                    }
                    if (HypixelCry.config.esp.glaciteMineshafts.frozenCourpes.features.contains(ESPFeatures.Text)) {
                        RenderHelper.renderText(worldRenderContext, Text.of(corpse.corpseType.name()).asOrderedText(), corpse.blockPos.toCenterPos().add(0, 1.9, 0), colorComponentsNoAlpha, 1, 0.5f, throughWalls);
                    }
                }
            }
        }
    }

    private static void onTick() {
        if (mc.world != null && mc.player != null) {
            mc.world.getEntities().forEach((FrozenCourpes::checkIfCorpse));
        }
    }

    static class Corpse {
        private final ArmorStandEntity entity;
        /**
         * Waypoint position is always 1 above entity position
         */
        private final BlockPos blockPos;

        public boolean found = false;
        /**
         * Type of the corpse, fully uppercased.
         */
        private final CorpseType corpseType;

        Corpse(ArmorStandEntity entity, BlockPos blockPos, CorpseType corpseType) {
            this.entity = entity;
            this.blockPos = blockPos;
            this.corpseType = corpseType;
        }
    }

    public enum CorpseType implements StringIdentifiable {
        LAPIS("LAPIS_ARMOR_HELMET", null, "0:127:0:0:255"), // dark blue looks bad and these two never exist in same shaft
        UMBER("ARMOR_OF_YOG_HELMET", "UMBER_KEY", "0:127:255:255:0"),
        TUNGSTEN("MINERAL_HELMET", "TUNGSTEN_KEY", "0:127:127:127:127"),
        VANGUARD("VANGUARD_HELMET", "SKELETON_KEY",  "0:127:0:255:255"),
        UNKNOWN("UNKNOWN", null, "0:127:255:0:0");

        public final String helmetItemId;
        public final String keyItemId;
        public final String color;

        CorpseType(String helmetItemId, String keyItemId, String color) {
            this.helmetItemId = helmetItemId;
            this.keyItemId = keyItemId;
            this.color = color;
        }

        static CorpseType fromHelmetItemId(String helmetItemId) {
            for (CorpseType value : values()) {
                if (value.helmetItemId.equals(helmetItemId)) {
                    return value;
                }
            }
            return UNKNOWN;
        }

        @Override
        public String asString() {
            return name().toLowerCase();
        }
    }
}
