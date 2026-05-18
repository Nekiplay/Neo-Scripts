package com.nekiplay.neoscripts.utils;

import com.nekiplay.neoscripts.Main;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Main.ID, value = Dist.CLIENT)
public class Utils {

    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();
    public static final ObjectArrayList<String> STRING_SCOREBOARD = new ObjectArrayList<>();
    public static final ObjectArrayList<Component> TEXT_SCOREBOARD = new ObjectArrayList<>();

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
    @SubscribeEvent
    public static void update(ClientTickEvent.Pre event) {
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
}
