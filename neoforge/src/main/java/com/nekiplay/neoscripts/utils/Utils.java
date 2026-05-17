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
        try {
            TEXT_SCOREBOARD.clear();
            STRING_SCOREBOARD.clear();

            LocalPlayer player = client.player;
            if (player == null) return;

            // Получаем scoreboard из мира клиента
            ClientLevel level = client.level;
            if (level == null) return;
            Scoreboard scoreboard = level.getScoreboard();
            if (scoreboard == null) return;

            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective == null) return;

            ObjectArrayList<Component> textLines = new ObjectArrayList<>();
            ObjectArrayList<String> stringLines = new ObjectArrayList<>();

            // Заголовок
            Component title = objective.getDisplayName();
            if (title != null && !title.getString().trim().isEmpty()) {
                stringLines.add(title.getString());
                textLines.add(title.copy());
            }

            // Собираем всех участников с оценками по текущей цели
            java.util.List<ScoreHolder> scoredHolders = new java.util.ArrayList<>();
            for (ScoreHolder holder : scoreboard.getTrackedPlayers()) {
                if (scoreboard.listPlayerScores(holder).get(objective) != null) {
                    scoredHolders.add(holder);
                }
            }

            // Сортировка по убыванию очков
            scoredHolders.sort((a, b) -> {
                int scoreA = scoreboard.listPlayerScores(a).get(objective);
                int scoreB = scoreboard.listPlayerScores(b).get(objective);
                return Integer.compare(scoreB, scoreA);
            });

            for (ScoreHolder holder : scoredHolders) {
                String name = holder.getScoreboardName();
                PlayerTeam team = scoreboard.getPlayersTeam(name);

                Component displayName;
                String plainName;

                if (team != null) {
                    displayName = PlayerTeam.formatNameForTeam(team, Component.literal(name));
                    plainName = team.getPlayerPrefix().getString() + name + team.getPlayerSuffix().getString();
                } else {
                    displayName = Component.literal(name);
                    plainName = name;
                }

                String trimmed = plainName.trim();
                if (!trimmed.isEmpty()) {
                    String stripped = ChatFormatting.stripFormatting(trimmed);
                    if (!stripped.trim().isEmpty()) {
                        textLines.add(displayName);
                        stringLines.add(trimmed);
                    }
                }
            }

            TEXT_SCOREBOARD.addAll(textLines);
            STRING_SCOREBOARD.addAll(stringLines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HolderLookup.Provider getRegistryWrapperLookup() {
        Minecraft client = Minecraft.getInstance();
        // Null check on client for tests
        return client != null && client.getConnection() != null && client.getConnection().registryAccess() != null ? client.getConnection().registryAccess() : LOOKUP;
    }
}
