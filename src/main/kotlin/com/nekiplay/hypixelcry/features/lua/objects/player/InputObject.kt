package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.sugar.attackBlock
import com.nekiplay.hypixelcry.sugar.attackEntity
import com.nekiplay.hypixelcry.sugar.interactBlock
import com.nekiplay.hypixelcry.sugar.interactEntity
import com.nekiplay.hypixelcry.sugar.leftClick
import com.nekiplay.hypixelcry.sugar.rightClick
import com.nekiplay.hypixelcry.sugar.silentUse
import com.nekiplay.hypixelcry.sugar.syncSelectedSlot
import com.nekiplay.hypixelcry.sugar.useItem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction


class InputObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "useItem" -> UseFunction()
            "silentUse" -> SilentUseFunction()
            "syncSelectedSlot" -> SyncSelectedSlotFunction()

            "attackBlock" -> AttackBlockFunction()
            "attackEntity" -> AttackEntityFunction()

            "interactBlock" -> InteractBlockFunction()
            "interactEntity" -> InteractEntityFunction()

            "leftClick" -> LeftClickFunction()
            "rightClick" -> RightClickFunction()
            // Setters
            "setSelectedSlot" -> SetSelectedSlotFunction()

            // KeyBoard
            "setPressedSprinting" -> SetPressedSprintingFunction()
            "setPressedJump" -> SetPressedJumpFunction()
            "setPressedSneak" -> SetPressedSneakFunction()

            "setPressedForward" -> SetPressedForwardFunction()
            "setPressedBack" -> SetPressedBackFunction()
            "setPressedLeft" -> SetPressedLeftFunction()
            "setPressedRight" -> SetPressedRightFunction()

            // Mouse
            "setPressedAttack" -> SetPressedAttackFunction()
            "setPressedUse" -> SetPressedUseFunction()

            // Getters
            "getSelectedSlot" -> GetSelectedSlotFunction()

            // KeyBoard
            "isPressedSprinting" -> IsPressedSprintingFunction()
            "isPressedJump" -> IsPressedJumpFunction()
            "isPressedSneak" -> IsPressedSneakFunction()

            "isPressedForward" -> IsPressedForwardFunction()
            "isPressedBack" -> IsPressedBackFunction()
            "isPressedLeft" -> IsPressedLeftFunction()
            "isPressedRight" -> IsPressedRightFunction()

            // Mouse
            "isPressedAttack" -> IsPressedAttackFunction()
            "isPressedUse" -> IsPressedUseFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class SyncSelectedSlotFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.interactionManager?.syncSelectedSlot() == true)
        }
    }

    private inner class LeftClickFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.leftClick())
        }
    }

    private inner class RightClickFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            mc.rightClick()
            return TRUE
        }
    }

    private inner class AttackBlockFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return LuaValue.valueOf(mc.interactionManager?.attackBlock() == true)
        }
    }

    private inner class AttackEntityFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return LuaValue.valueOf(mc.interactionManager?.attackEntity() == true)
        }
    }

    private inner class InteractEntityFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return LuaValue.valueOf(mc.interactionManager?.interactEntity() == true)
        }
    }

    private inner class InteractBlockFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return LuaValue.valueOf(mc.interactionManager?.interactBlock() == true)
        }
    }

    private inner class UseFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return LuaValue.valueOf(mc.interactionManager?.useItem() == true)
        }
    }

    private inner class SilentUseFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isnumber()) {
                val slot = arg.toint()
                if (slot in 0..8) {
                    if (mc.currentScreen == null) {
                        mc.interactionManager?.silentUse(slot)
                        return TRUE
                    }
                    FALSE
                } else {
                    FALSE
                }
            } else {
                NIL
            }
        }
    }

    // Setters
    private inner class SetSelectedSlotFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isnumber()) {
                val slot = arg.toint()
                if (slot in 0..8) {
                    if (mc.currentScreen == null) {
                        mc.player?.inventory?.selectedSlot = slot
                        return TRUE
                    }
                    FALSE
                } else {
                    FALSE
                }
            } else {
                NIL
            }
        }
    }

    private inner class SetPressedSprintingFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.currentScreen == null) {
                    val sprintKey: KeyBinding = mc.options.sprintKey
                    sprintKey.isPressed = arg.toboolean()
                    TRUE
                }
                FALSE
            }
            else {
                NIL
            }
        }
    }

    private inner class SetPressedForwardFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.currentScreen == null) {
                    val sprintKey: KeyBinding = mc.options.forwardKey
                    sprintKey.isPressed = arg.toboolean()
                    TRUE
                }
                FALSE
            }
            else {
                NIL
            }
        }
    }

    private inner class SetPressedBackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.currentScreen == null) {
                    val sprintKey: KeyBinding = mc.options.backKey
                    sprintKey.isPressed = arg.toboolean()
                    TRUE
                }
                FALSE
            }
            else {
                NIL
            }
        }
    }

    private inner class SetPressedLeftFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.currentScreen == null) {
                    val sprintKey: KeyBinding = mc.options.leftKey
                    sprintKey.isPressed = arg.toboolean()
                    TRUE
                }
                FALSE
            }
            else {
                NIL
            }
        }
    }

    private inner class SetPressedRightFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.currentScreen == null) {
                    val sprintKey: KeyBinding = mc.options.rightKey
                    sprintKey.isPressed = arg.toboolean()
                    TRUE
                }
                FALSE
            }
            else {
                NIL
            }
        }
    }

    private inner class SetPressedJumpFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.currentScreen == null) {
                    val sprintKey: KeyBinding = mc.options.jumpKey
                    sprintKey.isPressed = arg.toboolean()
                    TRUE
                }
                FALSE
            }
            else {
                NIL
            }
        }
    }

    private inner class SetPressedSneakFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.currentScreen == null) {
                    val sprintKey: KeyBinding = mc.options.sneakKey
                    sprintKey.isPressed = arg.toboolean()
                    TRUE
                }
                FALSE
            }
            else {
                NIL
            }
        }
    }

    private inner class SetPressedAttackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.currentScreen == null) {
                    val sprintKey: KeyBinding = mc.options.attackKey
                    sprintKey.isPressed = arg.toboolean()
                    TRUE
                }
                FALSE
            }
            else {
                NIL
            }
        }
    }

    private inner class SetPressedUseFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.currentScreen == null) {
                    val sprintKey: KeyBinding = mc.options.useKey
                    sprintKey.isPressed = arg.toboolean()
                    TRUE
                }
                FALSE
            }
            else {
                NIL
            }
        }
    }

    // Getters
    private inner class GetSelectedSlotFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc.player?.inventory?.selectedSlot ?: 0)
        }
    }


    private inner class IsPressedSprintingFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = mc.options.sprintKey
            return valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedForwardFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = mc.options.forwardKey
            return valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedBackFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = mc.options.backKey
            return valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedLeftFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = mc.options.leftKey
            return valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedRightFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = mc.options.rightKey
            return valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedJumpFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = mc.options.jumpKey
            return valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedSneakFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = mc.options.sneakKey
            return valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedAttackFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = mc.options.attackKey
            return valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedUseFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = mc.options.useKey
            return valueOf(sprintKey.isPressed)
        }
    }

    // Переопределяем необходимые методы LuaValue
    override fun typename(): String = "input"
    override fun tojstring(): String = "InputObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}