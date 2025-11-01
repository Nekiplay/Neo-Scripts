package com.nekiplay.hypixelcry.sugar

import com.nekiplay.hypixelcry.utils.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.text.Text

fun ItemStack.getDisplayName(): Text {
    return ItemUtils.getDisplayName(this)
}
fun ItemStack.setDisplayName(text: Text) {
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
fun ItemStack.getNeuId(): String {
    return ItemUtils.getNeuId(this)
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