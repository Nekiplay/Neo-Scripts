package com.nekiplay.hypixelcry.sugar

import com.nekiplay.hypixelcry.HypixelCry.mc
import com.nekiplay.hypixelcry.mixins.PlayerListHudAccessor
import com.nekiplay.hypixelcry.utils.Utils
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerScoreEntry
import net.minecraft.world.scores.PlayerTeam
import java.lang.String
import java.util.*
import kotlin.run

fun LocalPlayer.getScorebordLines(): List<Component> {
    return Utils.STRING_SCOREBOARD.map { it ->
        Component.literal(it)
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