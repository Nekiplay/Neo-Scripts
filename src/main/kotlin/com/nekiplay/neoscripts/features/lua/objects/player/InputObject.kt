package com.nekiplay.neoscripts.features.lua.objects.player

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.datatypes.phys.LuaRaycast
import com.nekiplay.neoscripts.mixins.minecraft.GamemodeAccessor
import com.nekiplay.neoscripts.sugar.attackBlock
import com.nekiplay.neoscripts.sugar.attackEntity
import com.nekiplay.neoscripts.sugar.interactBlock
import com.nekiplay.neoscripts.sugar.interactEntity
import com.nekiplay.neoscripts.sugar.leftClick
import com.nekiplay.neoscripts.sugar.mineBlock
import com.nekiplay.neoscripts.sugar.rightClick
import com.nekiplay.neoscripts.sugar.silentUse
import com.nekiplay.neoscripts.sugar.syncSelectedSlot
import com.nekiplay.neoscripts.sugar.useItem
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.HitResult
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction
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
            "mineBlock" -> MineBlockFunction()
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

            "getBreakingProgress" -> GetBreakingProgress()
            "setBreakingProgress" -> SetBreakingProgress()
            else -> NIL
        } as LuaValue
    }

    private inner class GetBreakingProgress : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val accessed = mc.gameMode as GamemodeAccessor
            val result = tableOf()
            result.set("progress", valueOf(accessed.`neoscripts$getBreakingProgress`().toDouble()))
            result.set("blockpos", LuaBlockPos(accessed.`neoscripts$getCurrentBreakingBlockPos`()))
            return result
        }
    }

    private inner class SetBreakingProgress : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val accessed = mc.gameMode as GamemodeAccessor
            accessed.`neoscripts$setCurrentBreakingProgress`(arg.tofloat());
            return TRUE
        }
    }

    private inner class SyncSelectedSlotFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.gameMode?.syncSelectedSlot() == true)
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

    private inner class MineBlockFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc.gameMode?.mineBlock() == true)
        }
    }

    private inner class AttackBlockFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc.gameMode?.attackBlock() == true)
        }
    }

    private inner class AttackEntityFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc.gameMode?.attackEntity() == true)
        }
    }

    private inner class InteractEntityFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc.gameMode?.interactEntity() == true)
        }
    }

    private inner class InteractBlockFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return if (args.narg() == 0) {
                valueOf(mc.gameMode?.interactBlock() == true)
            }
            else if (args.narg() == 1) {
                val result = when {
                    args.arg1()?.isuserdata() == true && args.arg1().touserdata() is LuaRaycast -> {
                        (args.arg1().touserdata() as LuaRaycast).hitResult
                    }
                    args.arg1()?.isuserdata() == true && args.arg1().touserdata() is HitResult -> {
                        (args.arg1().touserdata() as HitResult)
                    }
                    else -> null
                }
                if (result != null) {
                    valueOf(mc.gameMode?.interactBlock(result) == true)
                }
                else {
                    FALSE
                }
            }
            else {
                FALSE
            }
        }
    }

    private inner class UseFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc.gameMode?.useItem() == true)
        }
    }

    private inner class SilentUseFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isnumber()) {
                val slot = arg.toint()
                if (slot in 0..8) {
                    if (mc.screen == null) {
                        mc.gameMode?.silentUse(slot)
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
                    if (mc.screen == null) {
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
                if (mc.screen == null) {
                    val sprintKey = mc.options.keySprint
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyBindingHelper.getBoundKeyOf(sprintKey)
                    KeyMapping.set(key, arg.toboolean())
                    if (arg.toboolean()) {
                        KeyMapping.click(key)
                    }
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
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyUp
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyBindingHelper.getBoundKeyOf(sprintKey)
                    KeyMapping.set(key, arg.toboolean())
                    if (arg.toboolean()) {
                        KeyMapping.click(key)
                    }
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
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyDown
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyBindingHelper.getBoundKeyOf(sprintKey)
                    KeyMapping.set(key, arg.toboolean())
                    if (arg.toboolean()) {
                        KeyMapping.click(key)
                    }
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
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyLeft
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyBindingHelper.getBoundKeyOf(sprintKey)
                    KeyMapping.set(key, arg.toboolean())
                    if (arg.toboolean()) {
                        KeyMapping.click(key)
                    }
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
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyRight
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyBindingHelper.getBoundKeyOf(sprintKey)
                    KeyMapping.set(key, arg.toboolean())
                    if (arg.toboolean()) {
                        KeyMapping.click(key)
                    }
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
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyJump
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyBindingHelper.getBoundKeyOf(sprintKey)
                    KeyMapping.set(key, arg.toboolean())
                    if (arg.toboolean()) {
                        KeyMapping.click(key)
                    }
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
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyShift
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyBindingHelper.getBoundKeyOf(sprintKey)
                    KeyMapping.set(key, arg.toboolean())
                    if (arg.toboolean()) {
                        KeyMapping.click(key)
                    }
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
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyAttack
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyBindingHelper.getBoundKeyOf(sprintKey)
                    KeyMapping.set(key, arg.toboolean())
                    if (arg.toboolean()) {
                        KeyMapping.click(key)
                    }
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
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyUse
                    sprintKey.isDown = arg.toboolean()
                    KeyMapping.set(KeyBindingHelper.getBoundKeyOf(sprintKey), arg.toboolean())
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
            val sprintKey = mc.options.keySprint
            return valueOf(sprintKey.isDown)
        }
    }

    private inner class IsPressedForwardFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyUp
            return valueOf(sprintKey.isDown)
        }
    }

    private inner class IsPressedBackFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyDown
            return valueOf(sprintKey.isDown)
        }
    }

    private inner class IsPressedLeftFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyLeft
            return valueOf(sprintKey.isDown)
        }
    }

    private inner class IsPressedRightFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyRight
            return valueOf(sprintKey.isDown)
        }
    }

    private inner class IsPressedJumpFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyJump
            return valueOf(sprintKey.isDown)
        }
    }

    private inner class IsPressedSneakFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyShift
            return valueOf(sprintKey.isDown)
        }
    }

    private inner class IsPressedAttackFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyAttack
            return valueOf(sprintKey.isDown)
        }
    }

    private inner class IsPressedUseFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyUse
            return valueOf(sprintKey.isDown)
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