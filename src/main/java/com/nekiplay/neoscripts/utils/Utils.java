package com.nekiplay.neoscripts.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nekiplay.neoscripts.annotations.Init;
import com.nekiplay.neoscripts.events.SkyblockEvents;
import com.nekiplay.neoscripts.utils.purse.PurseChangeCause;
import com.nekiplay.neoscripts.utils.scheduler.MessageScheduler;
import com.nekiplay.neoscripts.utils.scheduler.Scheduler;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.azureaaron.hmapi.data.rank.PackageRank;
import net.azureaaron.hmapi.data.rank.RankType;
import net.azureaaron.hmapi.data.server.Environment;
import net.azureaaron.hmapi.events.HypixelPacketEvents;
import net.azureaaron.hmapi.network.HypixelNetworking;
import net.azureaaron.hmapi.network.packet.s2c.ErrorS2CPacket;
import net.azureaaron.hmapi.network.packet.s2c.HelloS2CPacket;
import net.azureaaron.hmapi.network.packet.s2c.HypixelS2CPacket;
import net.azureaaron.hmapi.network.packet.v1.s2c.LocationUpdateS2CPacket;
import net.azureaaron.hmapi.network.packet.v1.s2c.PlayerInfoS2CPacket;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nekiplay.neoscripts.Main.LOGGER;

public class Utils {
    public static final ObjectArrayList<String> STRING_SCOREBOARD = new ObjectArrayList<>();
    public static final ObjectArrayList<Component> TEXT_SCOREBOARD = new ObjectArrayList<>();

    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();
    private static final String ALTERNATE_HYPIXEL_ADDRESS = System.getProperty("skyblocker.alternateHypixelAddress", "");
    private static final String PROFILE_PREFIX = "Profile: ";
    private static final String PROFILE_MESSAGE_PREFIX = "§aYou are playing on profile: §e";
    public static final String PROFILE_ID_PREFIX = "Profile ID: ";
    private static final String PROFILE_ID_SUGGEST_PREFIX = "CLICK THIS TO SUGGEST IT IN CHAT";
    private static final Pattern PURSE = Pattern.compile("(Purse|Piggy): (?<purse>[0-9,.]+)( \\((?<change>[+\\-][0-9,.]+)\\))?");

    private static boolean isOnHypixel = false;
    private static boolean isOnSkyblock = false;

    @SuppressWarnings("JavadocDeclaration")
    @NotNull
    private static Environment environment = Environment.PRODUCTION;

    public static boolean copyToClipboard(String text) {
        try {
            StringSelection selection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка при копировании в буфер обмена: " + e.getMessage());
            return false;
        }
    }

    @Init
    public static void init() {
        //Register Mod API stuff
        ClientReceiveMessageEvents.ALLOW_GAME.register(Utils::onChatMessage);
        HypixelNetworking.registerToEvents(Util.make(new Object2IntOpenHashMap<>(), map -> map.put(LocationUpdateS2CPacket.ID, 1)));
        HypixelPacketEvents.HELLO.register(Utils::onPacket);
        HypixelPacketEvents.LOCATION_UPDATE.register(Utils::onPacket);
        HypixelPacketEvents.PLAYER_INFO.register(Utils::onPacket);
    }

    /**
     * Updates {@link #isOnSkyblock} if in a development environment and {@link #isOnHypixel} in all environments.
     */
    private static void updatePlayerPresence(Minecraft client) {
        FabricLoader fabricLoader = FabricLoader.getInstance();
        if (client.level == null || client.isLocalServer()) {
            if (fabricLoader.isDevelopmentEnvironment()) { // Pretend we're always in skyblock when in dev
                isOnSkyblock = true;
            }
        }

        if (fabricLoader.isDevelopmentEnvironment() || isConnectedToHypixel(client)) {
            if (!isOnHypixel) {
                isOnHypixel = true;
            }
        } else if (isOnHypixel) {
            isOnHypixel = false;
        }
    }

    private static boolean isConnectedToHypixel(Minecraft client) {
        String serverAddress = (client.getCurrentServer() != null) ? client.getCurrentServer().ip.toLowerCase() : "";
        String serverBrand = (client.player != null && client.player.connection != null && client.player.connection.serverBrand() != null) ? client.player.connection.serverBrand() : "";

        return (!serverAddress.isEmpty() && serverAddress.equalsIgnoreCase(ALTERNATE_HYPIXEL_ADDRESS)) || serverAddress.contains("hypixel.net") || serverAddress.contains("hypixel.io") || serverBrand.contains("Hypixel BungeeCord");
    }

    public static void openUrl(String url) {
        try {
            Desktop desk = Desktop.getDesktop();
            desk.browse(new URI(url));
        } catch (UnsupportedOperationException | IOException | URISyntaxException ignored) {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("xdg-open " + url);
            } catch (IOException e) {
            }
        }
    }

    @NotNull
    private static Location location = Location.UNKNOWN;

    @NotNull
    private static String area = "";

    @NotNull
    private static String profile = "";
    private static String profileId = "";
    @NotNull
    public static Location getLocation() {
        return location;
    }
    @NotNull
    public static String getRawLocation() {
        return locationRaw;
    }

    public static boolean isOnHypixel() {
        return isOnHypixel;
    }

    public static boolean isOnSkyblock() {
        return isOnSkyblock;
    }

    public static boolean isInDungeons() {
        return location == Location.DUNGEON;
    }

    public static boolean isInCrystalHollows() {
        return location == Location.CRYSTAL_HOLLOWS;
    }

    public static boolean isInDwarvenMines() {
        return location == Location.DWARVEN_MINES || location == Location.GLACITE_MINESHAFT;
    }

    public static boolean isInTheRift() {
        return location == Location.THE_RIFT;
    }

    public static boolean isInGarden() {
        return location == Location.GARDEN;
    }

    /**
     * @return if the player is in the end island
     */
    public static boolean isInTheEnd() {
        return location == Location.THE_END;
    }

    public static boolean isInKuudra() {
        return location == Location.KUUDRAS_HOLLOW;
    }

    public static boolean isInCrimson() {
        return location == Location.CRIMSON_ISLE;
    }

    public static boolean isInGalatea() {
        return location == Location.GALATEA;
    }

    public static boolean isOnBingo() {
        return profile.endsWith("Ⓑ");
    }

    @NotNull
    private static RankType rank = PackageRank.NONE;

    @NotNull
    private static String server = "";
    @NotNull
    private static String gameType = "";
    @NotNull
    private static String locationRaw = "";
    @NotNull
    private static String map = "";
    @NotNull
    private static double purse = 0;

    @NotNull
    private static int profileIdRequest = 0;
    private static int profileSuggestionMessages = Integer.MAX_VALUE / 2;

    public static String getProfile() {
        return profile;
    }

    public static String getProfileId() {
        return profileId;
    }

    private static boolean firstProfileUpdate = true;

    public static double getPurse() {
        return purse;
    }

    @NotNull
    public static String getArea() {
        return area;
    }

    public static RankType getRank() {
        return rank;
    }

    public static String getIslandArea() {
        try {
            for (String sidebarLine : STRING_SCOREBOARD) {
                if (sidebarLine.contains("⏣") || sidebarLine.contains("ф") /* Rift */) {
                    return sidebarLine.strip();
                }
            }
        } catch (IndexOutOfBoundsException e) {
            LOGGER.error("[HypixelCry] Failed to get location from sidebar", e);
        }
        return "Unknown";
    }
    public static UUID getUuid() {
        return Minecraft.getInstance().getUser().getProfileId();
    }

    public static void update() {
        Minecraft client = Minecraft.getInstance();
        updateScoreboard(client);
        updatePlayerPresence(client);
        updateFromPlayerList(client);
    }

    private static void updateScoreboard(Minecraft client) {
        try {
            TEXT_SCOREBOARD.clear();
            STRING_SCOREBOARD.clear();

            LocalPlayer player = client.player;
            if (player == null) return;

            Scoreboard scoreboard = player.getTeam() != null ? player.getTeam().getScoreboard() : null;
            if (scoreboard == null) return;
            
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective == null) return;

            ObjectArrayList<Component> textLines = new ObjectArrayList<>();
            ObjectArrayList<String> stringLines = new ObjectArrayList<>();

            // Получаем заголовок scoreboard
            Component title = objective.getDisplayName();
            if (title != null && !title.getString().trim().isEmpty()) {
                stringLines.add(title.getString());
                textLines.add(title.copy());
            }

            // Собираем все строки scoreboard
            java.util.List<net.minecraft.world.scores.ScoreHolder> scoredHolders = new java.util.ArrayList<>();
            for (ScoreHolder scoreHolder : scoreboard.getTrackedPlayers()) {
                var scoresMap = scoreboard.listPlayerScores(scoreHolder);
                if (scoresMap.get(objective) != null) {
                    scoredHolders.add(scoreHolder);
                }
            }
            
            // Сортируем по score (убывание - сверху вниз)
            scoredHolders.sort((a, b) -> {
                int scoreA = scoreboard.listPlayerScores(a).get(objective);
                int scoreB = scoreboard.listPlayerScores(b).get(objective);
                return Integer.compare(scoreB, scoreA); // descending
            });
            
            for (ScoreHolder scoreHolder : scoredHolders) {
                String scoreboardName = scoreHolder.getScoreboardName();
                PlayerTeam team = scoreboard.getPlayersTeam(scoreboardName);
                
                Component displayName;
                String plainName;
                
                if (team != null) {
                    displayName = PlayerTeam.formatNameForTeam(team, Component.literal(scoreboardName));
                    plainName = team.getPlayerPrefix().getString() + scoreboardName + team.getPlayerSuffix().getString();
                } else {
                    displayName = Component.literal(scoreboardName);
                    plainName = scoreboardName;
                }
                
                if (!plainName.trim().isEmpty()) {
                    String formatted = ChatFormatting.stripFormatting(plainName);
                    if (!formatted.trim().isEmpty()) {
                        textLines.add(displayName);
                        stringLines.add(formatted);
                    }
                }
            }

            TEXT_SCOREBOARD.addAll(textLines);
            STRING_SCOREBOARD.addAll(stringLines);
            if (isOnSkyblock) {
                Utils.updatePurse();
                updateArea();
            }
        } catch (Exception e) {
            //Do nothing
        }
    }

    private static void updatePurse() {
        STRING_SCOREBOARD.stream().filter(s -> s.contains("Piggy:") || s.contains("Purse:")).findFirst().ifPresent(purseString -> {
            Matcher matcher = PURSE.matcher(purseString);
            if (matcher.find()) {
                try {
                    double newPurse = Double.parseDouble(matcher.group("purse").replaceAll(",", ""));
                    double changeSinceLast = newPurse - Utils.purse;
                    if (changeSinceLast == 0) return;
                    SkyblockEvents.PURSE_CHANGE.invoker().onPurseChange(changeSinceLast, PurseChangeCause.getCause(changeSinceLast));
                    Utils.purse = newPurse;
                } catch (NumberFormatException e) {
                    LOGGER.error("[Skyblocker] Failed to parse purse string. Input: '{}'", purseString, e);
                }
            }
        });
    }

    private static void updateFromPlayerList(Minecraft client) {
        if (client.getConnection() == null) {
            return;
        }
        for (PlayerInfo playerListEntry : client.getConnection().getOnlinePlayers()) {
            if (playerListEntry.getTabListDisplayName() == null) {
                continue;
            }
            String name = playerListEntry.getTabListDisplayName().getString();
            if (name.startsWith(PROFILE_PREFIX)) {
                profile = name.substring(PROFILE_PREFIX.length());
            }
        }
    }

    private static void updateArea() {
        String areaName = getIslandArea().replaceAll("[⏣ф]", "").strip();
        area = areaName;

        if (!areaName.equals(area)) SkyblockEvents.AREA_CHANGE.invoker().onSkyblockAreaChange(areaName);
    }

    public static int getBits() {
        int bits = 0;
        String bitsString = null;
        try {
            for (String sidebarLine : STRING_SCOREBOARD) {
                if (sidebarLine.contains("Bits")) bitsString = sidebarLine;
            }
            if (bitsString != null) {
                bits = Integer.parseInt(bitsString.replaceAll("[^0-9.]", "").strip());
            }
        } catch (IndexOutOfBoundsException e) {
            LOGGER.error("[Hypixel Cry] Failed to get bits from sidebar", e);
        }
        return bits;
    }

    private static void tickProfileId() {
        profileIdRequest++;

        Scheduler.INSTANCE.schedule(new Runnable() {
            private final int requestId = profileIdRequest;

            @Override
            public void run() {
                if (requestId == profileIdRequest) {
                    MessageScheduler.INSTANCE.sendMessageAfterCooldown("/profileid", true);
                    profileSuggestionMessages = 0;
                }
            }
        }, 20 * 8); //8 seconds
    }

    private static void onPacket(HypixelS2CPacket packet) {
        switch (packet) {
            case HelloS2CPacket(Environment environment) -> {
                Utils.environment = environment;

                //Request the player's rank information
                HypixelNetworking.sendPlayerInfoC2SPacket(1);
            }

            case LocationUpdateS2CPacket(var serverName, var serverType, var _lobbyName, var mode, var map) -> {
                Utils.server = serverName;
                String previousServerType = Utils.gameType;
                Utils.gameType = serverType.orElse("");
                Utils.locationRaw = mode.orElse("");
                Utils.location = Location.from(locationRaw);
                Utils.map = map.orElse("");

                SkyblockEvents.LOCATION_CHANGE.invoker().onSkyblockLocationChange(location);

                if (Utils.gameType.equals("SKYBLOCK")) {
                    isOnSkyblock = true;
                    tickProfileId();

                    if (!previousServerType.equals("SKYBLOCK")) SkyblockEvents.JOIN.invoker().onSkyblockJoin();
                } else if (previousServerType.equals("SKYBLOCK")) {
                    isOnSkyblock = false;
                    SkyblockEvents.LEAVE.invoker().onSkyblockLeave();
                }
            }

            case ErrorS2CPacket(var id, var error) when id.equals(LocationUpdateS2CPacket.ID) -> {
                server = "";
                gameType = "";
                locationRaw = "";
                location = Location.UNKNOWN;
                map = "";

                LocalPlayer player = Minecraft.getInstance().player;
                LOGGER.error("[Skyblocker] Failed to update your current location! Some features of the mod may not work correctly :( - Error: {}", error);
            }

            case PlayerInfoS2CPacket(var playerRank, var packageRank, var monthlyPackageRank, var _prefix) -> {
                rank = RankType.getEffectiveRank(playerRank, packageRank, monthlyPackageRank);
            }

            default -> {} //Do Nothing
        }
    }

    /**
     * Parses the /locraw reply from the server and updates the player's profile id
     *
     * @return not display the message in chat if the command is sent by the mod
     */
    public static boolean onChatMessage(Component text, boolean overlay) {
        if (overlay) return true;
        String message = text.getString();

        if (message.startsWith("{\"server\":") && message.endsWith("}")) {
            parseLocRaw(message);
        }

        if (isOnSkyblock) {
            if (message.startsWith(PROFILE_MESSAGE_PREFIX)) {
                profile = message.substring(PROFILE_MESSAGE_PREFIX.length()).split("§b")[0];
            } else if (message.startsWith(PROFILE_ID_PREFIX)) {
                String prevProfileId = profileId;
                profileId = message.substring(PROFILE_ID_PREFIX.length());
                profileIdRequest++;

                if (!prevProfileId.equals(profileId)) {
                    SkyblockEvents.PROFILE_CHANGE.invoker().onSkyblockProfileChange(prevProfileId, profileId);
                }
            } else if (ChatFormatting.stripFormatting(message).startsWith(PROFILE_ID_SUGGEST_PREFIX)) {
                int suggestions = profileSuggestionMessages;
                profileSuggestionMessages++;

                return suggestions >= 2;
            }
        }

        return true;
    }

    /**
     * Parses /locraw chat message and updates {@link #server}, {@link #gameType}, {@link #locationRaw}, {@link #map}
     * and {@link #location}
     *
     * @param message json message from chat
     * @deprecated Retained just in case the mod api doesn't work or gets disabled.
     */
    @Deprecated
    private static void parseLocRaw(String message) {
        JsonObject locRaw = JsonParser.parseString(message).getAsJsonObject();

        if (locRaw.has("server")) {
            server = locRaw.get("server").getAsString();
        }
        if (locRaw.has("gametype")) {
            gameType = locRaw.get("gametype").getAsString();
            isOnSkyblock = gameType.equals("SKYBLOCK");
        }
        if (locRaw.has("mode")) {
            locationRaw = locRaw.get("mode").getAsString();
            location = Location.from(locationRaw);
        } else {
            location = Location.UNKNOWN;
        }
        if (locRaw.has("map")) {
            map = locRaw.get("map").getAsString();
        }
    }


    public static HolderLookup.Provider getRegistryWrapperLookup() {
        Minecraft client = Minecraft.getInstance();
        // Null check on client for tests
        return client != null && client.getConnection() != null && client.getConnection().registryAccess() != null ? client.getConnection().registryAccess() : LOOKUP;
    }
}
