package com.nekiplay.hypixelcry.sugar

import com.nekiplay.hypixelcry.mixins.PlayerListHudAccessor
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.gui.Gui
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import java.util.Optional

fun LocalPlayer.getScoreabordLines(): List<Component> {
    val scoreboard = mc.player?.team?.scoreboard ?: return listOf()
    val activeObjective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return listOf()
    return scoreboard.listPlayerScores(activeObjective)
        .filter { !it.isHidden }
        .sortedWith(Gui.SCORE_DISPLAY_ORDER)
        .take(15).map {
            val team = scoreboard.getPlayerTeam(it.owner)
            val text = it.display
            if (text == null) {
                PlayerTeam.formatNameForTeam(team, text)
            }
            else {
                Component.empty()
            }
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