package com.nekiplay.hypixelcry.sugar

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.nekiplay.hypixelcry.mixins.PlayerListHudAccessor
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.gui.hud.InGameHud
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.Team
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import java.util.Optional

fun ClientPlayerEntity.getScoreabordLines(): List<Text> {
    val scoreboard = mc.player?.scoreboard ?: return listOf()
    val activeObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) ?: return listOf()
    return scoreboard.getScoreboardEntries(activeObjective)
        .filter { !it.hidden() }
        .sortedWith(InGameHud.SCOREBOARD_ENTRY_COMPARATOR)
        .take(15).map {
            val team = scoreboard.getScoreHolderTeam(it.owner)
            val text = it.name()
            Team.decorateName(team, text)
        }
}

data class CurrentTabList(
    val header: Optional<Text>,
    val footer: Optional<Text>,
    val body: List<Text>,
)

fun ClientPlayerEntity.getTab(): CurrentTabList {
    val tab = mc.inGameHud.playerListHud ?: return CurrentTabList(Optional.empty(), Optional.empty(), listOf())
    val tabAccessor = tab as PlayerListHudAccessor

    val entries = tabAccessor.collectPlayerEntries_hypixel_cry()
        .map {
            it.displayName ?: run {
                val team = it.scoreboardTeam
                val name = it.profile.name
                Team.decorateName(team, Text.literal(name))
            }
        }
    return CurrentTabList(
        header = Optional.ofNullable(tabAccessor.header_hypixel_cry),
        footer = Optional.ofNullable(tabAccessor.footer_hypixel_cry),
        body = entries,
    )
}