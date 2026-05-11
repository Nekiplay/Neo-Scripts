package com.nekiplay.neoscripts.sugar

import com.nekiplay.neoscripts.mixins.MinecraftClientAccessor
import net.minecraft.client.Minecraft

fun Minecraft.rightClick() {
    (this as MinecraftClientAccessor).doItemUse()
}
fun Minecraft.leftClick(): Boolean {
    return (this as MinecraftClientAccessor).doAttack()
}