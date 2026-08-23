package com.nekiplay.neoscripts.client.sugar

import com.nekiplay.neoscripts.common.mixins.MinecraftClientAccessor
import net.minecraft.client.Minecraft

fun Minecraft.rightClick() {
    (this as MinecraftClientAccessor).doItemUse()
}
fun Minecraft.leftClick(): Boolean {
    return (this as MinecraftClientAccessor).doAttack()
}