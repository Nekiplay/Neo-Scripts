package com.nekiplay.neoscripts.features.lua.objects.player

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.datatypes.phys.LuaRaycast
import com.nekiplay.neoscripts.mixins.minecraft.GamemodeAccessor
import com.nekiplay.neoscripts.sugar.*
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items
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

            // Breaking progress
            "getBreakingProgress" -> GetBreakingProgress()
            "setBreakingProgress" -> SetBreakingProgress()

            // Elytra
            "startElytraFly" -> StartElytraFly()
            else -> NIL
        } as LuaValue
    }

    private class StartElytraFly : ZeroArgFunction() {
        override fun call(): LuaValue? {
            if (mc.player != null && mc.player?.connection != null) {
                if (mc.player?.inventory?.getItem(EquipmentSlot.CHEST.id)?.`is`(Items.ELYTRA) == true) {
                    mc.player?.connection?.send(
                        ServerboundPlayerCommandPacket(
                            mc.player!!,
                            ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
                        )
                    )
                }
                return TRUE
            }
            return FALSE
        }
    }


    private class GetBreakingProgress : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val accessed = mc.gameMode as GamemodeAccessor
            val result = tableOf()
            result.set("progress", valueOf(accessed.`neoscripts$getBreakingProgress`().toDouble()))
            result.set("blockpos", LuaBlockPos(accessed.`neoscripts$getCurrentBreakingBlockPos`()))
            return result
        }
    }

    private class SetBreakingProgress : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val accessed = mc.gameMode as GamemodeAccessor
            accessed.`neoscripts$setCurrentBreakingProgress`(arg.tofloat());
            return TRUE
        }
    }

    private class SyncSelectedSlotFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.gameMode?.syncSelectedSlot() == true)
        }
    }

    private class LeftClickFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.leftClick())
        }
    }

    private class RightClickFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            mc.rightClick()
            return TRUE
        }
    }

    private class MineBlockFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return if (args.narg() == 0) {
                valueOf(mc?.gameMode?.mineBlock() == true)
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
                    valueOf(mc?.gameMode?.mineBlock(result) == true)
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

    private class AttackBlockFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return if (args.narg() == 0) {
                valueOf(mc?.gameMode?.attackBlock() == true)
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
                    valueOf(mc?.gameMode?.attackBlock(result) == true)
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

    private class AttackEntityFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return if (args.narg() == 0) {
                valueOf(mc?.gameMode?.attackEntity() == true)
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
                    valueOf(mc?.gameMode?.attackEntity(result) == true)
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

    private class InteractEntityFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return if (args.narg() == 0) {
                valueOf(mc?.gameMode?.interactEntity() == true)
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
                    valueOf(mc?.gameMode?.interactEntity(result) == true)
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

    private class InteractBlockFunction : VarArgFunction() {
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

    private class UseFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc.gameMode?.useItem() == true)
        }
    }

    private class SilentUseFunction : OneArgFunction() {
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
    private class SetSelectedSlotFunction : OneArgFunction() {
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

    private class SetPressedSprintingFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.screen == null) {
                    val sprintKey = mc.options.keySprint
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyMappingHelper.getBoundKeyOf(sprintKey)
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

    private class SetPressedForwardFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyUp
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyMappingHelper.getBoundKeyOf(sprintKey)
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

    private class SetPressedBackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyDown
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyMappingHelper.getBoundKeyOf(sprintKey)
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

    private class SetPressedLeftFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyLeft
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyMappingHelper.getBoundKeyOf(sprintKey)
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

    private class SetPressedRightFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyRight
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyMappingHelper.getBoundKeyOf(sprintKey)
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

    private class SetPressedJumpFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyJump
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyMappingHelper.getBoundKeyOf(sprintKey)
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

    private class SetPressedSneakFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyShift
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyMappingHelper.getBoundKeyOf(sprintKey)
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

    private class SetPressedAttackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyAttack
                    sprintKey.isDown = arg.toboolean()
                    val key = KeyMappingHelper.getBoundKeyOf(sprintKey)
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

    private class SetPressedUseFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                if (mc.screen == null) {
                    val sprintKey = mc.options.keyUse
                    sprintKey.isDown = arg.toboolean()
                    KeyMapping.set(KeyMappingHelper.getBoundKeyOf(sprintKey), arg.toboolean())
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
    private class GetSelectedSlotFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return valueOf(mc.player?.inventory?.selectedSlot ?: 0)
        }
    }


    private class IsPressedSprintingFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keySprint
            return valueOf(sprintKey.isDown)
        }
    }

    private class IsPressedForwardFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyUp
            return valueOf(sprintKey.isDown)
        }
    }

    private class IsPressedBackFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyDown
            return valueOf(sprintKey.isDown)
        }
    }

    private class IsPressedLeftFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyLeft
            return valueOf(sprintKey.isDown)
        }
    }

    private class IsPressedRightFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyRight
            return valueOf(sprintKey.isDown)
        }
    }

    private class IsPressedJumpFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyJump
            return valueOf(sprintKey.isDown)
        }
    }

    private class IsPressedSneakFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyShift
            return valueOf(sprintKey.isDown)
        }
    }

    private class IsPressedAttackFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey = mc.options.keyAttack
            return valueOf(sprintKey.isDown)
        }
    }

    private class IsPressedUseFunction : ZeroArgFunction() {
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