package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.features.lua.utils.ItemStackUtils
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.utils.InventoryUtils
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class InventoryObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "isChestOpened" -> IsChestOpenedFunction()
            "isDoubleChestOpened" -> IsDoubleChestOpenedFunction()

            "getStack" -> GetStackFunction()

            "leftClick" -> LeftClickFunction()
            "rightClick" -> RightClickFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class LeftClickFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                InventoryUtils.leftClickSlot(arg.toint())
                TRUE
            } else {
                NIL
            }
        }
    }

    private inner class RightClickFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                InventoryUtils.rightClickSlot(arg.toint())
                TRUE
            } else {
                NIL
            }
        }
    }

    private inner class IsDoubleChestOpenedFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.currentScreen
            if (screen is GenericContainerScreen) {
                val container = screen.screenHandler
                val slots = container.slots.size
                val chestType = when (slots) {
                    54 -> TRUE
                    else -> FALSE
                }
                return chestType
            }
            else {
                return FALSE
            }
        }
    }

    private inner class IsChestOpenedFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.currentScreen
            if (screen is GenericContainerScreen) {
                val container = screen.screenHandler
                val slots = container.slots.size
                val chestType = when (slots) {
                    27 -> TRUE
                    else -> FALSE
                }
                return chestType
            }
            else {
                return FALSE
            }
        }
    }

    private inner class GetStackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                return ItemStackUtils.ToLua(mc.player?.inventory?.getStack(arg.toint())) ?: NIL
            } else {
                NIL
            }
        }
    }

    override fun typename(): String = "inventory"
    override fun tojstring(): String = "InventoryObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}