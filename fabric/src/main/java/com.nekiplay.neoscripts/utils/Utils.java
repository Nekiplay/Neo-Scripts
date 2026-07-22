package com.nekiplay.neoscripts.utils;

import com.nekiplay.neoscripts.annotations.Init;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.*;
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
import java.util.zip.CRC32;

public class Utils {
    public static final ObjectArrayList<String> STRING_SCOREBOARD = new ObjectArrayList<>();
    public static final ObjectArrayList<Component> TEXT_SCOREBOARD = new ObjectArrayList<>();

    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();
    private static final String ALTERNATE_HYPIXEL_ADDRESS = System.getProperty("skyblocker.alternateHypixelAddress", "");

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

    public static UUID getUuid() {
        return Minecraft.getInstance().getUser().getProfileId();
    }

    public static void update() {
        Minecraft client = Minecraft.getInstance();
        updateScoreboard(client);
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

        } catch (Exception e) {
            // Чтобы увидеть ошибку в логах, если она есть
            e.printStackTrace();
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
