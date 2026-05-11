package com.nekiplay.neoscripts.sugar

import com.nekiplay.neoscripts.utils.ItemUtils
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

fun ItemStack.getDisplayName(): Component {
    return ItemUtils.getDisplayName(this)
}
fun ItemStack.setDisplayName(text: Component) {
    return ItemUtils.setDisplayName(this, text)
}
fun ItemStack.getHeadTexture(): String {
    return ItemUtils.getHeadTexture(this)
}
fun ItemStack.getItemUuid(): String {
    return ItemUtils.getItemUuid(this)
}
fun ItemStack.getItemId(): String {
    return ItemUtils.getItemId(this)
}
fun ItemStack.getReforgeModifier(): String {
    return ItemUtils.getReforgeModifier(this)
}
fun ItemStack.isRecombobulated(): Boolean {
    return ItemUtils.isRecombobulated(this)
}
fun ItemStack.isMuseumDonated(): Boolean {
    return ItemUtils.isMuseumDonated(this)
}