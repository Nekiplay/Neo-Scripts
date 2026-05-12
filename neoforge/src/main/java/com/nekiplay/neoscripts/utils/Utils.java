package com.nekiplay.neoscripts.utils;

import com.nekiplay.neoscripts.Main;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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

            Scoreboard scoreboard = player.getTeam() != null ? player.getTeam().getScoreboard() : null;
            if (scoreboard == null) return;

            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective == null) return;

            ObjectArrayList<net.minecraft.network.chat.Component> textLines = new ObjectArrayList<>();
            ObjectArrayList<String> stringLines = new ObjectArrayList<>();

            // Получаем заголовок scoreboard
            net.minecraft.network.chat.Component title = objective.getDisplayName();
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

                net.minecraft.network.chat.Component displayName;
                String plainName;

                if (team != null) {
                    displayName = PlayerTeam.formatNameForTeam(team, net.minecraft.network.chat.Component.literal(scoreboardName));
                    plainName = team.getPlayerPrefix().getString() + scoreboardName + team.getPlayerSuffix().getString();
                } else {
                    displayName = Component.literal(scoreboardName);
                    plainName = scoreboardName;
                }

                String trimmed = plainName.trim();
                if (!trimmed.isEmpty()) {
                    String stripped = ChatFormatting.stripFormatting(trimmed);
                    if (!stripped.trim().isEmpty()) {
                        textLines.add(displayName);
                        stringLines.add(trimmed); // Сохраняем с цветовыми кодами
                    }
                }
            }

            TEXT_SCOREBOARD.addAll(textLines);
            STRING_SCOREBOARD.addAll(stringLines);
        } catch (Exception e) {
            //Do nothing
        }
    }

    public static HolderLookup.Provider getRegistryWrapperLookup() {
        Minecraft client = Minecraft.getInstance();
        // Null check on client for tests
        return client != null && client.getConnection() != null && client.getConnection().registryAccess() != null ? client.getConnection().registryAccess() : LOOKUP;
    }
}
