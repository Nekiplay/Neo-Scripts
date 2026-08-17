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
        return if (key.type() == LuaValue.TSTRING) functions[key] ?: NIL else NIL
    }

    private val functions: Map<LuaValue, LuaValue> by lazy {
        buildMap {
            put(LuaValue.valueOf("useItem"), UseFunction())
            put(LuaValue.valueOf("silentUse"), SilentUseFunction())
            put(LuaValue.valueOf("syncSelectedSlot"), SyncSelectedSlotFunction())
            put(LuaValue.valueOf("attackBlock"), AttackBlockFunction())
            put(LuaValue.valueOf("mineBlock"), MineBlockFunction())
            put(LuaValue.valueOf("attackEntity"), AttackEntityFunction())
            put(LuaValue.valueOf("interactBlock"), InteractBlockFunction())
            put(LuaValue.valueOf("interactEntity"), InteractEntityFunction())
            put(LuaValue.valueOf("leftClick"), LeftClickFunction())
            put(LuaValue.valueOf("rightClick"), RightClickFunction())
            put(LuaValue.valueOf("setSelectedSlot"), SetSelectedSlotFunction())
            put(LuaValue.valueOf("setPressedSprinting"), SetPressedSprintingFunction())
            put(LuaValue.valueOf("setPressedJump"), SetPressedJumpFunction())
            put(LuaValue.valueOf("setPressedSneak"), SetPressedSneakFunction())
            put(LuaValue.valueOf("setPressedForward"), SetPressedForwardFunction())
            put(LuaValue.valueOf("setPressedBack"), SetPressedBackFunction())
            put(LuaValue.valueOf("setPressedLeft"), SetPressedLeftFunction())
            put(LuaValue.valueOf("setPressedRight"), SetPressedRightFunction())
            put(LuaValue.valueOf("setPressedAttack"), SetPressedAttackFunction())
            put(LuaValue.valueOf("setPressedUse"), SetPressedUseFunction())
            put(LuaValue.valueOf("getSelectedSlot"), GetSelectedSlotFunction())
            put(LuaValue.valueOf("isPressedSprinting"), IsPressedSprintingFunction())
            put(LuaValue.valueOf("isPressedJump"), IsPressedJumpFunction())
            put(LuaValue.valueOf("isPressedSneak"), IsPressedSneakFunction())
            put(LuaValue.valueOf("isPressedForward"), IsPressedForwardFunction())
            put(LuaValue.valueOf("isPressedBack"), IsPressedBackFunction())
            put(LuaValue.valueOf("isPressedLeft"), IsPressedLeftFunction())
            put(LuaValue.valueOf("isPressedRight"), IsPressedRightFunction())
            put(LuaValue.valueOf("isPressedAttack"), IsPressedAttackFunction())
            put(LuaValue.valueOf("isPressedUse"), IsPressedUseFunction())
            put(LuaValue.valueOf("getBreakingProgress"), GetBreakingProgress())
            put(LuaValue.valueOf("setBreakingProgress"), SetBreakingProgress())
            put(LuaValue.valueOf("startElytraFly"), StartElytraFly())
        }
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
                    if (mc.gui.screen() == null) {
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
                    if (mc.gui.screen() == null) {
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
                if (mc.gui.screen() == null) {
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
                if (mc.gui.screen() == null) {
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
                if (mc.gui.screen() == null) {
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
                if (mc.gui.screen() == null) {
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
                if (mc.gui.screen() == null) {
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
                if (mc.gui.screen() == null) {
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
                if (mc.gui.screen() == null) {
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
                if (mc.gui.screen() == null) {
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
                if (mc.gui.screen() == null) {
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