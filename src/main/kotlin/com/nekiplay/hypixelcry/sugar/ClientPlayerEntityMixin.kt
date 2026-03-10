package com.nekiplay.hypixelcry.sugar

import com.nekiplay.hypixelcry.mixins.PlayerListHudAccessor
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerScoreEntry
import net.minecraft.world.scores.PlayerTeam
import java.lang.String
import java.util.*
import kotlin.run

fun LocalPlayer.getScorebordLines(): List<Component> {
    val scoreboard = mc.player?.team?.scoreboard ?: return listOf()
    val activeObjective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return listOf()
    val scoreboardComparator = compareByDescending<PlayerScoreEntry> { it.value }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.owner }

    return scoreboard.listPlayerScores(activeObjective)
        .filter { !it.isHidden }
        .sortedWith(scoreboardComparator)
        .take(15).map {
            val team = scoreboard.getPlayerTeam(it.owner)
            val text = it.display ?: Component.literal(it.owner)
            PlayerTeam.formatNameForTeam(team, text)
        }
}

data class CurrentTabList(
    val header: Optional<Component>,
    val footer: Optional<Component>,
    val body: List<Component>,
)

fun LocalPlayer.getTab(): CurrentTabList {
    val tab = mc.gui.tabList ?: return CurrentTabList(Optional.empty(), Optional.empty(), listOf())
    val tabAccessor = tab as PlayerListHudAccessor

    val entries = tabAccessor.collectPlayerEntries_hypixel_cry()
        .map {
            it.tabListDisplayName ?: run {
                val team = it.team
                val name = it.profile.name
                PlayerTeam.formatNameForTeam(team, Component.literal(name))
            }
        }
    return CurrentTabList(
        header = Optional.ofNullable(tabAccessor.header_hypixel_cry),
        footer = Optional.ofNullable(tabAccessor.footer_hypixel_cry),
        body = entries,
    )
}