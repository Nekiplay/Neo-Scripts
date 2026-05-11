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
            val accessed = mc?.gameMode as GamemodeAccessor
            val result = tableOf()
            result.set("progress", valueOf(accessed.`neoscripts$getBreakingProgress`().toDouble()))
            result.set("blockpos", LuaBlockPos(accessed.`neoscripts$getCurrentBreakingBlockPos`()))
            return result
        }
    }

    private inner class SetBreakingProgress : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val accessed = mc?.gameMode as GamemodeAccessor
            accessed.`neoscripts$setCurrentBreakingProgress`(arg.tofloat());
            return TRUE
        }
    }

    private inner class SyncSelectedSlotFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc?.gameMode?.syncSelectedSlot() == true)
        }
    }

    private inner class LeftClickFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc?.leftClick() == true)
        }
    }

    private inner class RightClickFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            mc?.rightClick()
            return TRUE
        }
    }

    private inner class MineBlockFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc?.gameMode?.mineBlock() == true)
        }
    }

    private inner class AttackBlockFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc?.gameMode?.attackBlock() == true)
        }
    }

    private inner class AttackEntityFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc?.gameMode?.attackEntity() == true)
        }
    }

    private inner class InteractEntityFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc?.gameMode?.interactEntity() == true)
        }
    }

    private inner class InteractBlockFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return if (args.narg() == 0) {
                valueOf(mc?.gameMode?.interactBlock() == true)
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
                    valueOf(mc?.gameMode?.interactBlock(result) == true)
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
            return valueOf(mc?.gameMode?.useItem() == true)
        }
    }

    private inner class SilentUseFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isnumber()) {
                val slot = arg.toint()
                if (slot in 0..8) {
                    if (mc?.screen == null) {
                        mc?.gameMode?.silentUse(slot)
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
                    if (mc?.screen == null) {
                        mc?.player?.inventory?.selectedSlot = slot
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
            if (arg == null || !arg.isboolean() || mc?.screen != null) {
                return if (arg != null && arg.isboolean()) FALSE else NIL
            }

            val shouldToggle = arg.toboolean()
            val key = mc?.options?.keySprint
            if (shouldToggle) {
                key?.clickCount++
            } else {
                key?.clickCount = 0
                key?.consumeClick()
            }

            return TRUE
        }
    }

    private inner class SetPressedForwardFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg == null || !arg.isboolean() || mc?.screen != null) {
                return if (arg != null && arg.isboolean()) FALSE else NIL
            }

            val shouldToggle = arg.toboolean()
            val key = mc?.options?.keyUp
            if (shouldToggle) {
                key?.clickCount++
            } else {
                key?.clickCount = 0
                key?.consumeClick()
            }

            return TRUE
        }
    }

    private inner class SetPressedBackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg == null || !arg.isboolean() || mc?.screen != null) {
                return if (arg != null && arg.isboolean()) FALSE else NIL
            }

            val shouldToggle = arg.toboolean()
            val key = mc?.options?.keyDown
            if (shouldToggle) {
                key?.clickCount++
            } else {
                key?.clickCount = 0
                key?.consumeClick()
            }

            return TRUE
        }
    }

    private inner class SetPressedLeftFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg == null || !arg.isboolean() || mc?.screen != null) {
                return if (arg != null && arg.isboolean()) FALSE else NIL
            }

            val shouldToggle = arg.toboolean()
            val key = mc?.options?.keyLeft
            if (shouldToggle) {
                key?.clickCount++
            } else {
                key?.clickCount = 0
                key?.consumeClick()
            }

            return TRUE
        }
    }

    private inner class SetPressedRightFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg == null || !arg.isboolean() || mc?.screen != null) {
                return if (arg != null && arg.isboolean()) FALSE else NIL
            }

            val shouldToggle = arg.toboolean()
            val key = mc?.options?.keyRight
            if (shouldToggle) {
                key?.clickCount++
            } else {
                key?.clickCount = 0
                key?.consumeClick()
            }

            return TRUE
        }
    }

    private inner class SetPressedJumpFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg == null || !arg.isboolean() || mc?.screen != null) {
                return if (arg != null && arg.isboolean()) FALSE else NIL
            }

            val shouldToggle = arg.toboolean()
            val key = mc?.options?.keyJump
            if (shouldToggle) {
                key?.clickCount++
            } else {
                key?.clickCount = 0
                key?.consumeClick()
            }

            return TRUE
        }
    }

    private inner class SetPressedSneakFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg == null || !arg.isboolean() || mc?.screen != null) {
                return if (arg != null && arg.isboolean()) FALSE else NIL
            }

            val shouldToggle = arg.toboolean()
            val key = mc?.options?.keyShift
            if (shouldToggle) {
                key?.clickCount++
            } else {
                key?.clickCount = 0
                key?.consumeClick()
            }

            return TRUE
        }
    }

    private inner class SetPressedAttackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg == null || !arg.isboolean() || mc?.screen != null) {
                return if (arg != null && arg.isboolean()) FALSE else NIL
            }

            val shouldToggle = arg.toboolean()
            val key = mc?.options?.keyAttack
            if (shouldToggle) {
                key?.clickCount++
            } else {
                key?.clickCount = 0
                key?.consumeClick()
            }

            return TRUE
        }
    }

    private inner class SetPressedUseFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg == null || !arg.isboolean() || mc?.screen != null) {
                return if (arg != null && arg.isboolean()) FALSE else NIL
            }

            val shouldToggle = arg.toboolean()
            val key = mc?.options?.keyUse
            if (shouldToggle) {
                key?.clickCount++
            } else {
                key?.clickCount = 0
                key?.consumeClick()
            }

            return TRUE
        }
    }

    // Getters
    private inner class GetSelectedSlotFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc?.player?.inventory?.selectedSlot ?: 0)
        }
    }


    private inner class IsPressedSprintingFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc?.options?.keySprint
            return valueOf(sprintKey?.isDown == true)
        }
    }

    private inner class IsPressedForwardFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc?.options?.keyUp
            return valueOf(sprintKey?.isDown == true)
        }
    }

    private inner class IsPressedBackFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc?.options?.keyDown
            return valueOf(sprintKey?.isDown == true)
        }
    }

    private inner class IsPressedLeftFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc?.options?.keyLeft
            return valueOf(sprintKey?.isDown == true)
        }
    }

    private inner class IsPressedRightFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc?.options?.keyRight
            return valueOf(sprintKey?.isDown == true)
        }
    }

    private inner class IsPressedJumpFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc?.options?.keyJump
            return valueOf(sprintKey?.isDown == true)
        }
    }

    private inner class IsPressedSneakFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc?.options?.keyShift
            return valueOf(sprintKey?.isDown == true)
        }
    }

    private inner class IsPressedAttackFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc?.options?.keyAttack
            return valueOf(sprintKey?.isDown == true)
        }
    }

    private inner class IsPressedUseFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc?.options?.keyUse
            return valueOf(sprintKey?.isDown == true)
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