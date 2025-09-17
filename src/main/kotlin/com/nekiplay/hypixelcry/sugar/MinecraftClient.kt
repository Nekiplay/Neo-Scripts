package com.nekiplay.hypixelcry.sugar

import com.nekiplay.hypixelcry.mixins.MinecraftClientAccessor
import net.minecraft.client.MinecraftClient

fun MinecraftClient.rightClick() {
    (this as MinecraftClientAccessor).doItemUse()
}