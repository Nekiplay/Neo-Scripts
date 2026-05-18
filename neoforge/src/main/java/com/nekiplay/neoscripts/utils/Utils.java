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

            ClientLevel world = client.level;
            if (world == null) return;

            Scoreboard scoreboard = world.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

            if (objective == null) return;

            // Получаем список записей
            var scoreEntries = scoreboard.listPlayerScores(objective);

            // Создаем список и сортируем его по значению очков (от большего к меньшему)
            ArrayList<PlayerScoreEntry> sortedScores = new ArrayList<PlayerScoreEntry>(scoreEntries);
            sortedScores.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());

            for (PlayerScoreEntry entry : sortedScores) {
                // В вашей версии это entry.owner() (строка-идентификатор)
                String owner = entry.owner();

                // Получаем команду, используя строку owner
                PlayerTeam team = scoreboard.getPlayersTeam(owner);

                // entry.ownerName() возвращает либо display name, либо literal от owner
                Component lineBase = entry.ownerName();

                MutableComponent fullLine = Component.empty();

                if (team != null) {
                    // Склеиваем: Префикс + Основной текст + Суффикс
                    fullLine.append(team.getPlayerPrefix())
                            .append(lineBase)
                            .append(team.getPlayerSuffix());
                } else {
                    fullLine.append(lineBase);
                }

                String rawString = fullLine.getString();

                if (!rawString.trim().isEmpty()) {
                    TEXT_SCOREBOARD.add(fullLine);
                    // ChatFormatting.stripFormatting убирает значки §
                    STRING_SCOREBOARD.add(ChatFormatting.stripFormatting(rawString));
                }
            }

            // Добавляем заголовок в начало списка
            STRING_SCOREBOARD.add(0, ChatFormatting.stripFormatting(objective.getDisplayName().getString()));
            TEXT_SCOREBOARD.add(0, objective.getDisplayName().copy());
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
