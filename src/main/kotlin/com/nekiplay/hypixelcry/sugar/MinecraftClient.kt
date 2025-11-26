package com.nekiplay.hypixelcry.sugar

import com.nekiplay.hypixelcry.mixins.MinecraftClientAccessor
import net.minecraft.client.Minecraft

fun Minecraft.rightClick() {
    (this as MinecraftClientAccessor).doItemUse()
}
fun Minecraft.leftClick(): Boolean {
    return (this as MinecraftClientAccessor).doAttack()
}