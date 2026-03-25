package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import com.nekiplay.hypixelcry.mixins.gui.AbstractSignEditScreenAccessor
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.sugar.getFormattedString
import com.nekiplay.hypixelcry.utils.InventoryUtils
import com.nekiplay.hypixelcry.utils.itemlist.ItemRepository
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import net.minecraft.world.inventory.ChestMenu
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua


class InventoryObject(l: Lua?) : SimpleLuaWrapper(l) {

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "isAnyScreenOpened" -> JFunction { it.push(mc.screen != null); 1 }
            "openInventory" -> JFunction { openInventory(it) }
            "getStackFromId" -> JFunction { getStackFromId(it) }
            "getStack" -> JFunction { getStack(it) }
            "getContainerSlots" -> JFunction { getContainerSlots(it) }
            "getChestTitle" -> JFunction { getChestTitle(it) }
            "middleClick" -> JFunction { middleClick(it) }
            "leftClick" -> JFunction { leftClick(it) }
            "rightClick" -> JFunction { rightClick(it) }
            "drop" -> JFunction { drop(it) }
            "getStackFromContainer" -> JFunction { getStackFromContainer(it) }
            "closeScreen" -> JFunction {
                mc.execute { mc.setScreen(null) }
                it.push(true)
                1
            }

            "isSignOpened" -> JFunction { isSignOpened(it) }
            "getSignText" -> JFunction { getSignText(it) }
            "setSignText" -> JFunction { setSignText(it) }
            "doneSign" -> JFunction { doneSign(it) }

            else -> null
        }
    }

    private fun openInventory(l: Lua): Int {
        val player = mc.player
        return if (player != null) {
            mc.execute {
                mc.setScreen(InventoryScreen(player))
                player.sendOpenInventory()
            }
            l.push(true)
            1
        } else {
            l.push(false)
            1
        }
    }

    private fun getStackFromId(l: Lua): Int {
        val idString = l.toString(1)
        if (idString == null) {
            l.error("getStackFromId expects a string (item id)")
            return 0
        }

        val stack = ItemRepository.getItemStack(idString)
        return if (stack != null && !stack.isEmpty) {
            // Обязательно вызываем .push() у обертки
            LuaItemStack(l, stack).push()
            1
        } else {
            l.pushNil()
            1
        }
    }

    private fun isSignOpened(l: Lua): Int {
        l.push(mc.screen is SignEditScreen)
        return 1
    }

    private fun closeScreen(l: Lua): Int {
        mc.player?.closeContainer()
        l.push(true)
        return 1
    }

    private fun getSignText(l: Lua): Int {
        if (l.isNumber(1)) {
            val screen = mc.screen
            if (screen is SignEditScreen) {
                val sign = screen as AbstractSignEditScreenAccessor
                val index = l.toInteger(1).toInt()
                if (index in 0..3) {
                    l.push(sign.messages[index])
                    return 1
                }
            }
        }
        l.push(false)
        return 1
    }

    private fun setSignText(l: Lua): Int {
        if (l.isNumber(1) && l.isString(2)) {
            val screen = mc.screen
            if (screen is SignEditScreen) {
                val sign = screen as AbstractSignEditScreenAccessor
                val index = l.toInteger(1).toInt()
                val text = l.toString(2)
                if (index in 0..3 && text != null) {
                    sign.messages[index] = text
                    l.push(true)
                    return 1
                }
            }
        }
        l.push(false)
        return 1
    }

    private fun doneSign(l: Lua): Int {
        val screen = mc.screen
        if (screen is SignEditScreen) {
            val sign = screen as AbstractSignEditScreenAccessor
            mc.connection?.send(
                ServerboundSignUpdatePacket(
                    sign.sign.blockPos,
                    sign.isFrontText,
                    sign.messages[0],
                    sign.messages[1],
                    sign.messages[2],
                    sign.messages[3]
                )
            )
            l.push(true)
            return 1
        }
        l.push(false)
        return 1
    }


    private fun getChestTitle(l: Lua): Int {
        val screen = mc.screen
        if (screen is ContainerScreen) {
            l.push(screen.title.string)
        } else {
            l.pushNil()
        }
        return 1
    }

    private fun middleClick(l: Lua): Int {
        if (l.isNumber(1)) {
            InventoryUtils.middleClickSlot(l.toInteger(1).toInt())
            l.push(true)
        } else {
            l.push(false)
        }
        return 1
    }

    private fun leftClick(l: Lua): Int {
        if (l.isNumber(1)) {
            InventoryUtils.leftClickSlot(l.toInteger(1).toInt())
            l.push(true)
        } else {
            l.push(false)
        }
        return 1
    }

    private fun rightClick(l: Lua): Int {
        if (l.isNumber(1)) {
            InventoryUtils.rightClickSlot(l.toInteger(1).toInt())
            l.push(true)
        } else {
            l.push(false)
        }
        return 1
    }

    private fun drop(l: Lua): Int {
        if (l.isNumber(1)) {
            InventoryUtils.dropAllFromSlot(l.toInteger(1).toInt())
            l.push(true)
        } else {
            l.push(false)
        }
        return 1
    }

    private fun getContainerSlots(l: Lua): Int {
        val container = mc.player?.containerMenu
        if (container is ChestMenu) {
            l.push(container.slots.size)
        } else {
            l.pushNil()
        }
        return 1
    }

    private fun getStack(l: Lua): Int {
        if (!l.isNumber(1)) {
            l.pushNil()
            return 1
        }

        val slot = l.toInteger(1).toInt()

        val player = mc.player ?: run {
            l.pushNil()
            return 1
        }
        val inv = player.inventory ?: run {
            l.pushNil()
            return 1
        }

        val stack = inv.getItem(slot)
        if (stack == null || stack.isEmpty) {
            l.pushNil()
            return 1
        }

        LuaItemStack(l, stack).push()
        return 1
    }

    private fun getStackFromContainer(l: Lua): Int {
        if (l.isNumber(1)) {
            if (mc.player != null && mc.player?.containerMenu != null) {
                val screen = mc.player!!.containerMenu
                if (screen is ChestMenu) {
                    val stack = screen.getSlot(l.toInteger(1).toInt()).item
                    if (stack == null || stack.isEmpty) {
                        l.pushNil()
                    } else {
                        LuaItemStack(l, stack).push()
                    }
                } else {
                    l.pushNil()
                }
            } else {
                l.pushNil()
            }
        } else {
            l.pushNil()
        }
        return 1
    }
}