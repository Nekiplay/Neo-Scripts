package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.mixins.gui.AbstractSignEditScreenAccessor
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.sugar.getFormattedString
import com.nekiplay.hypixelcry.utils.InventoryUtils
import com.nekiplay.hypixelcry.utils.itemlist.ItemRepository
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import net.minecraft.world.inventory.ChestMenu
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction


class InventoryObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "isSignOpened" -> IsSignOpenedFunction()
            "isAnyScreenOpened" -> IsAnyScreenOpened()
            "getContainerSlots" -> GetContainerSlotsFunction()
            "getChestTitle" -> GetChestTitleFunction()

            "getStackFromContainer" -> GetStackFromContainerFunction()
            "getStack" -> GetStackFunction()
            "getStackFromId" -> GetStackFromIDFunction()
            "getSignText" -> GetSignTextFunction()
            "setSignText" -> SetSignTextFunction()

            "leftClick" -> LeftClickFunction()
            "dropAll" -> DropFunction()
            "rightClick" -> RightClickFunction()

            "closeScreen" -> CloseScreenFunction()
            "openInventory" -> OpenInventoryFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class OpenInventoryFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val player = mc.player
            if (player != null) {
                mc.setScreen(InventoryScreen(player))
                player.sendOpenInventory()
                return TRUE
            }
            else {
                return FALSE
            }
        }
    }

    private inner class GetStackFromIDFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) {
                error("create item expects a string as 1st argument (item neu id)")
            }
            val idString = args.arg(1).checkjstring()

            val stack = ItemRepository.getItemStack(idString)
            return if (stack != null) {
                LuaItemStack(stack)
            } else {
                NIL
            }
        }
    }

    private inner class IsAnyScreenOpened : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.screen
            return valueOf(screen != null)
        }
    }

    private inner class SetSignTextFunction : TwoArgFunction() {
        override fun call(arg: LuaValue, arg2: LuaValue): LuaValue {
            if (arg.isnumber() && arg2.isstring()) {
                val screen = mc.screen
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
                val screen = mc.screen
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
            mc.player?.closeContainer()
            return TRUE
        }
    }

    private inner class IsSignOpenedFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.screen
            return if (screen is SignEditScreen) {
                TRUE
            } else {
                FALSE
            }
        }
    }

    private inner class GetChestTitleFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.screen
            return if (screen is ContainerScreen) {
                valueOf(screen.title.getFormattedString())
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

    private inner class DropFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                InventoryUtils.dropAllFromSlot(arg.toint())
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
            val screen = mc.player?.containerMenu
            if (screen is ChestMenu) {
                val slots = screen.slots.size
                return valueOf(slots)
            }
            else {
                return NIL
            }
        }
    }

    private inner class GetStackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            if (arg == null || !arg.isnumber()) return NIL

            val slot = arg.toint()

            val player = mc.player ?: return NIL
            val inv = player.inventory ?: return NIL

            val stack = inv.getItem(slot)
            if (stack == null || stack.isEmpty) return NIL
            return LuaItemStack(stack)
        }
    }

    private inner class GetStackFromContainerFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                if (mc.player != null && mc.player?.containerMenu != null) {
                    val screen = mc.player!!.containerMenu
                    if (screen is ChestMenu) {
                        val stack = screen.getSlot(arg.toint()).item
                        if (stack == null || stack.isEmpty) return NIL

                        return LuaItemStack(stack)
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