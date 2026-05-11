package com.nekiplay.neoscripts.sugar

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.mixins.PlayerListHudAccessor
import com.nekiplay.neoscripts.utils.Utils
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.PlayerTeam
import java.util.*
import kotlin.run

data class CurrentTabList(
    val header: Optional<Component>,
    val footer: Optional<Component>,
    val body: List<Component>,
)

fun LocalPlayer.getTab(): CurrentTabList {
    val tab = mc.gui.tabList ?: return CurrentTabList(Optional.empty(), Optional.empty(), listOf())
    val tabAccessor = tab as PlayerListHudAccessor

    val entries = tabAccessor.collectPlayerEntries_neoscripts()
        .map {
            it.tabListDisplayName ?: run {
                val team = it.team
                val name = it.profile.name
                PlayerTeam.formatNameForTeam(team, Component.literal(name))
            }
        }
    return CurrentTabList(
        header = Optional.ofNullable(tabAccessor.header_neoscripts),
        footer = Optional.ofNullable(tabAccessor.footer_neoscripts),
        body = entries,
    )
}