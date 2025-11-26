package com.nekiplay.hypixelcry.pathfinder.utils

import net.minecraft.client.Minecraft

val mc
    get() = Minecraft.getInstance()
val player
    get() = mc.player
val world
    get() = mc.level