package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.features.lua.utils.ItemStackUtils
import com.nekiplay.hypixelcry.mixins.gui.AbstractSignEditScreenAccessor
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.utils.InventoryUtils
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.SignEditScreen
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.ScreenHandler
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction


class InventoryObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "isSignOpened" -> IsSignOpenedFunction()
            "getContainerSlots" -> GetContainerSlotsFunction()
            "getChestTitle" -> GetChestTitleFunction()

            "getStackFromContainer" -> GetStackFromContainerFunction()
            "getStack" -> GetStackFunction()
            "getSignText" -> GetSignTextFunction()
            "setSignText" -> SetSignTextFunction()

            "leftClick" -> LeftClickFunction()
            "rightClick" -> RightClickFunction()

            "closeScreen" -> CloseScreenFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class SetSignTextFunction : TwoArgFunction() {
        override fun call(arg: LuaValue, arg2: LuaValue): LuaValue {
            if (arg.isnumber() && arg2.isstring()) {
                val screen = mc.currentScreen
                if (screen is SignEditScreen) {
                    val sign = screen as AbstractSignEditScreenAccessor
                    sign.messages[arg.toint()] = arg2.tojstring()
                    return TRUE
                } else {
                    return FALSE
                }
            }
            return NIL
        }
    }

    private inner class GetSignTextFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            if (arg.isnumber()) {
                val screen = mc.currentScreen
                if (screen is SignEditScreen) {
                    val sign = screen as AbstractSignEditScreenAccessor
                    return valueOf(sign.messages[arg.toint()])
                } else {
                    return FALSE
                }
            }
            return NIL
        }
    }

    private inner class CloseScreenFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            mc.player?.closeHandledScreen()
            return TRUE
        }
    }

    private inner class IsSignOpenedFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.currentScreen
            return if (screen is SignEditScreen) {
                TRUE
            } else {
                FALSE
            }
        }
    }

    private inner class GetChestTitleFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.player?.currentScreenHandler
            return if (screen is GenericContainerScreen) {
                valueOf(screen.title.string)
            } else {
                NIL
            }
        }
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
    private inner class GetContainerSlotsFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.player?.currentScreenHandler
            if (screen is GenericContainerScreen) {
                val container = screen.screenHandler
                val slots = container.slots.size
                return valueOf(slots)
            }
            else {
                return NIL
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

    private inner class GetStackFromContainerFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                if (mc.player != null) {
                    val screenHandler: ScreenHandler? = mc.player!!.currentScreenHandler
                    if (screenHandler is GenericContainerScreenHandler) {
                        return ItemStackUtils.ToLua(screenHandler.getSlot(arg.toint()).stack) ?: NIL
                    }
                    else {
                        NIL
                    }
                }
                else {
                    NIL
                }
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