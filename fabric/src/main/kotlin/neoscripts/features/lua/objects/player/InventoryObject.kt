 package com.nekiplay.neoscripts.features.lua.objects.player

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.neoscripts.mixins.gui.AbstractSignEditScreenAccessor
import com.nekiplay.neoscripts.mixins.gui.AnvilScreenAccessor
import com.nekiplay.neoscripts.sugar.getFormattedString
import com.nekiplay.neoscripts.utils.InventoryUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction


 class InventoryObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    // Precomputed dynamic keys
    private val carriedItemKey = LuaValue.valueOf("carriedItem")
    private val carriedItemStackKey = LuaValue.valueOf("carriedItemStack")

    override fun get(key: LuaValue): LuaValue {
        return when {
            key == carriedItemKey || key == carriedItemStackKey -> {
                val carriedStack = mc.player?.containerMenu?.carried
                if (carriedStack != null) {
                    LuaItemStack(carriedStack)
                } else {
                    NIL
                }
            }
            key.type() == LuaValue.TSTRING -> functions[key] ?: NIL
            else -> NIL
        }
    }

    private val functions: Map<LuaValue, LuaValue> by lazy {
        buildMap {
            put(LuaValue.valueOf("isSignOpened"), IsSignOpenedFunction())
            put(LuaValue.valueOf("isAnyScreenOpened"), IsAnyScreenOpened())
            put(LuaValue.valueOf("isContainerScreenOpened"), IsContainerScreenOpened())
            put(LuaValue.valueOf("isContainerOpened"), IsContainerScreenOpened())
            put(LuaValue.valueOf("isChatOpened"), IsChatOpened())
            put(LuaValue.valueOf("isAnvilOpened"), IsAnvilOpened())
            put(LuaValue.valueOf("getContainerSlots"), GetContainerSlotsFunction())
            put(LuaValue.valueOf("getChestTitle"), GetChestTitleFunction())
            put(LuaValue.valueOf("getContainerTitle"), GetChestTitleFunction())
            put(LuaValue.valueOf("setStackInContainer"), SetStackInContainerFunction())
            put(LuaValue.valueOf("setItemInContainer"), SetStackInContainerFunction())
            put(LuaValue.valueOf("setStack"), SetStackFunction())
            put(LuaValue.valueOf("setItem"), SetStackFunction())
            put(LuaValue.valueOf("setItemStack"), SetStackFunction())
            put(LuaValue.valueOf("getStackFromContainer"), GetStackFromContainerFunction())
            put(LuaValue.valueOf("getItemFromContainer"), GetStackFromContainerFunction())
            put(LuaValue.valueOf("getItemStackFromContainer"), GetStackFromContainerFunction())
            put(LuaValue.valueOf("getStack"), GetStackFunction())
            put(LuaValue.valueOf("getItem"), GetStackFunction())
            put(LuaValue.valueOf("getItemStack"), GetStackFunction())
            put(LuaValue.valueOf("getSignText"), GetSignTextFunction())
            put(LuaValue.valueOf("setSignText"), SetSignTextFunction())
            put(LuaValue.valueOf("doneSign"), DoneSignFunction())
            put(LuaValue.valueOf("confirmSign"), DoneSignFunction())
            put(LuaValue.valueOf("getAnvilText"), GetAnvilText())
            put(LuaValue.valueOf("doneAnvil"), DoneAnvilFunction())
            put(LuaValue.valueOf("confirmAnvil"), DoneAnvilFunction())
            put(LuaValue.valueOf("leftClick"), LeftClickFunction())
            put(LuaValue.valueOf("rightClick"), RightClickFunction())
            put(LuaValue.valueOf("shiftLeftClick"), ShiftLeftClickFunction())
            put(LuaValue.valueOf("shiftRightClick"), ShiftRightClickFunction())
            put(LuaValue.valueOf("middleClick"), MiddleClickFunction())
            put(LuaValue.valueOf("drop"), DropFunction())
            put(LuaValue.valueOf("dropAll"), DropAllFunction())
            put(LuaValue.valueOf("closeScreen"), CloseScreenFunction())
            put(LuaValue.valueOf("openInventory"), OpenInventoryFunction())
        }
    }

     private class GetAnvilText : ZeroArgFunction() {
         override fun call(): LuaValue {
             val screen = mc.gui.screen()
             if (screen is AnvilScreen) {
                 val accessed = screen as AnvilScreenAccessor
                 return valueOf(accessed.name.value)
             } else {
                 return NIL
             }
         }
     }

     private class DoneAnvilFunction : OneArgFunction() {
         override fun call(arg1: LuaValue): LuaValue {
             val screen = mc.gui.screen()
             if (screen is AnvilScreen && arg1.isstring()) {
                 mc.player?.connection?.send(ServerboundRenameItemPacket(arg1.tojstring()));
                 return TRUE
             } else {
                 return FALSE
             }
         }
     }

     private class IsAnvilOpened : ZeroArgFunction() {
         override fun call(): LuaValue {
             val screen = mc?.gui?.screen()
             return if (screen is AnvilScreen) {
                 TRUE
             } else {
                 FALSE
             }
         }
     }

    private class IsChatOpened : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc?.gui?.screen()
            return if (screen is ChatScreen) {
                TRUE
            } else {
                FALSE
            }
        }
    }

    private class IsContainerScreenOpened : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc?.gui?.screen()
            return if (screen is AbstractContainerScreen<*>) {
                TRUE
            } else {
                FALSE
            }
        }
    }

    private class OpenInventoryFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val player = mc.player
            if (player != null) {
                mc.gui.setScreen(InventoryScreen(player))
                player.sendOpenInventory()
                return TRUE
            }
            else {
                return FALSE
            }
        }
    }

    private class IsAnyScreenOpened : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.gui.screen()
            return valueOf(screen != null)
        }
    }

    private class DoneSignFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.gui.screen()
            if (screen is SignEditScreen) {
                val sign = screen as AbstractSignEditScreenAccessor

                val clientPacketListener: ClientPacketListener? = mc.connection
                clientPacketListener?.send(
                    ServerboundSignUpdatePacket(
                        sign.sign.blockPos,
                        sign.isFrontText,
                        sign.messages[0],
                        sign.messages[1],
                        sign.messages[2],
                        sign.messages[3]
                    )
                )
                return TRUE
            } else {
                return FALSE
            }
        }
    }

    private class SetSignTextFunction : TwoArgFunction() {
        override fun call(arg: LuaValue, arg2: LuaValue): LuaValue {
            if (arg.isnumber() && arg2.isstring()) {
                val screen = mc.gui.screen()
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

    private class GetSignTextFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            if (arg.isnumber()) {
                val screen = mc.gui.screen()
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

    private class CloseScreenFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            mc.player?.closeContainer()
            return TRUE
        }
    }

    private class IsSignOpenedFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.gui.screen()
            return if (screen is SignEditScreen) {
                TRUE
            } else {
                FALSE
            }
        }
    }

    private class GetChestTitleFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.gui.screen()
            return if (screen is AbstractContainerScreen<*>) {
                valueOf(screen.title.getFormattedString())
            } else {
                NIL
            }
        }
    }

    private class MiddleClickFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                InventoryUtils.middleClickSlot(arg.toint())
                TRUE
            } else {
                NIL
            }
        }
    }

    private class ShiftLeftClickFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                InventoryUtils.shiftLeftClickSlot(arg.toint())
                TRUE
            } else {
                NIL
            }
        }
    }

    private class ShiftRightClickFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                InventoryUtils.shiftRightClickSlot(arg.toint())
                TRUE
            } else {
                NIL
            }
        }
    }

    private class LeftClickFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                InventoryUtils.leftClickSlot(arg.toint())
                TRUE
            } else {
                NIL
            }
        }
    }

    private class RightClickFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                InventoryUtils.rightClickSlot(arg.toint())
                TRUE
            } else {
                NIL
            }
        }
    }

    private class DropFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                InventoryUtils.dropSlot(arg.toint())
                TRUE
            } else {
                NIL
            }
        }
    }

    private class DropAllFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                InventoryUtils.dropAllFromSlot(arg.toint())
                TRUE
            } else {
                NIL
            }
        }
    }

    private class GetContainerSlotsFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val screen = mc.player?.containerMenu
            if (screen is AbstractContainerMenu) {
                val slots = screen.slots.size
                return valueOf(slots)
            }
            else {
                return NIL
            }
        }
    }

    private class GetStackFunction : OneArgFunction() {
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

    private class SetStackFunction : TwoArgFunction() {
        override fun call(arg: LuaValue?, arg2: LuaValue?): LuaValue {
            if (arg == null || !arg.isnumber()) return NIL

            val itemStack = when {
                arg2?.isuserdata() == true && arg2.touserdata() is LuaItemStack -> (arg2.touserdata() as LuaItemStack).stack
                arg2?.isuserdata() == true && arg2.touserdata() is ItemStack -> arg2.touserdata() as ItemStack
                else -> null
            }

            if (itemStack != null) {
                val slot = arg.toint()

                val player = mc.player ?: return NIL
                val inv = player.inventory ?: return NIL

                player.inventory.setItem(slot, itemStack)
                return TRUE
            }
            return FALSE
        }
    }

    private class SetStackInContainerFunction : TwoArgFunction() {
        override fun call(arg: LuaValue?, arg2: LuaValue?): LuaValue {
            if (arg == null || !arg.isnumber()) return NIL

            val itemStack = when {
                arg2?.isuserdata() == true && arg2.touserdata() is LuaItemStack -> (arg2.touserdata() as LuaItemStack).stack
                arg2?.isuserdata() == true && arg2.touserdata() is ItemStack -> arg2.touserdata() as ItemStack
                else -> null
            }

            if (mc.player != null && mc.player?.containerMenu != null && itemStack != null) {
                val screen = mc.player!!.containerMenu
                if (screen is AbstractContainerMenu) {
                    val slot = arg.toint()

                    screen.setItem(slot, screen.stateId, itemStack)
                    return TRUE
                }
            }
            return FALSE
        }
    }

    private class GetStackFromContainerFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            return if (arg?.isnumber() == true) {
                if (mc.player != null && mc.player?.containerMenu != null) {
                    val screen = mc.player!!.containerMenu
                    if (screen is AbstractContainerMenu) {
                        val stack = screen.getSlot(arg.toint()).item
                        if (stack == null || stack.isEmpty) return NIL

                        LuaItemStack(stack)
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