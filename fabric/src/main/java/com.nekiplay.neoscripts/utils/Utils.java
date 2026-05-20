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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import net.minecraft.world.scores.*;
import org.jetbrains.annotations.NotNull;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import static com.nekiplay.neoscripts.utils.itemlist.recipes.SkyblockRecipe.LOGGER;

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
            LOGGER.error("[Neo Scripts] Failed to get location from sidebar", e);
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
        if (client.level == null) return;

        try {
            Scoreboard scoreboard = client.level.getScoreboard();

            // 1. Пытаемся получить обьектив сайдбара
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

            // Если через SIDEBAR не нашлось (бывает на некоторых серверах),
            // берем первый попавшийся обьектив, у которого есть очки
            if (objective == null) {
                objective = scoreboard.getObjectives().stream().findFirst().orElse(null);
            }

            if (objective == null) return;

            // Временные списки
            ArrayList<Component> newTextLines = new ArrayList<>();
            ArrayList<String> newStringLines = new ArrayList<>();

            // 2. Получаем все записи для этого обьектива
            java.util.Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective);
            if (scores.isEmpty()) return;

            // 3. Сортируем (как в ванилле: сначала по значению, потом по имени)
            List<PlayerScoreEntry> sortedScores = scores.stream()
                    .sorted(Comparator.comparingInt(PlayerScoreEntry::value)
                            .reversed()
                            .thenComparing(entry -> entry.owner(), String.CASE_INSENSITIVE_ORDER))
                    .toList();

            for (PlayerScoreEntry entry : sortedScores) {
                // Имя записи (может быть "fake player" именем или реальным ником)
                String owner = entry.owner();
                PlayerTeam team = scoreboard.getPlayersTeam(owner);

                // В 1.21 важно правильно собрать компоненты
                MutableComponent fullLine = Component.empty();

                if (team != null) {
                    fullLine.append(team.getPlayerPrefix());
                    // Если имя игрока — это просто технический ID (например, #line1),
                    // то ownerName() может быть не нужен, но обычно на серверах там пробелы для цвета
                    fullLine.append(entry.ownerName());
                    fullLine.append(team.getPlayerSuffix());
                } else {
                    fullLine.append(entry.ownerName());
                }

                // Убираем лишние пробелы и проверяем на пустоту
                String rawText = fullLine.getString();
                if (rawText.trim().isEmpty()) continue;

                newTextLines.add(fullLine);
                newStringLines.add(ChatFormatting.stripFormatting(rawText));
            }

            // 4. Добавляем заголовок
            Component title = objective.getDisplayName();
            newTextLines.add(0, title);
            newStringLines.add(0, ChatFormatting.stripFormatting(title.getString()));

            // Атомарно обновляем основные списки
            TEXT_SCOREBOARD.clear();
            TEXT_SCOREBOARD.addAll(newTextLines);

            STRING_SCOREBOARD.clear();
            STRING_SCOREBOARD.addAll(newStringLines);

            if (isOnSkyblock) {
                Utils.updatePurse();
                updateArea();
            }

        } catch (Exception e) {
            // Чтобы увидеть ошибку в логах, если она есть
            e.printStackTrace();
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
            LOGGER.error("[Neo Scripts] Failed to get bits from sidebar", e);
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

    public static String getHWID8() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        ComputerSystem cs = hal.getComputerSystem();
        CentralProcessor cpu = hal.getProcessor();

        List<String> components = new ArrayList<>();

        addIfValid(components, cs.getSerialNumber());
        addIfValid(components, cs.getBaseboard().getSerialNumber());
        addIfValid(components, cpu.getProcessorIdentifier().getProcessorID());

        if (components.isEmpty()) {
            String mac = getPrimaryMac(hal.getNetworkIFs());
            if (mac != null) components.add(mac);
            components.add(cpu.getProcessorIdentifier().getName());
        }

        Collections.sort(components);
        String seed = String.join("|", components);

        CRC32 crc = new CRC32();
        crc.update(seed.getBytes(StandardCharsets.UTF_8));
        return String.format("%08X", crc.getValue());
    }

    private static void addIfValid(List<String> list, String value) {
        if (value == null) return;
        String s = value.trim().toUpperCase();
        if (s.isEmpty()) return;
        if (s.contains("TO BE FILLED") || s.contains("NOT AVAILABLE")
                || s.contains("DEFAULT STRING") || s.contains("NONE")
                || s.contains("SERIAL") || s.contains("0123456789")) {
            return;
        }
        list.add(s);
    }

    private static String getPrimaryMac(List<NetworkIF> networkIFs) {
        for (NetworkIF nif : networkIFs) {
            String mac = nif.getMacaddr();
            if (mac == null || mac.isEmpty() || "00:00:00:00:00:00".equals(mac)) continue;

            String name = nif.getName();
            if (name != null && (name.startsWith("lo") || name.startsWith("docker")
                    || name.startsWith("br-") || name.startsWith("veth"))) continue;

            return mac.replace(":", "").replace("-", "").toUpperCase();
        }
        return null;
    }
}
