package com.nekiplay.hypixelcry.features.lua.objects.player

import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction


class InputObject: LuaValue() {
    private val client: MinecraftClient = MinecraftClient.getInstance()

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
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
            else -> LuaValue.NIL
        } as LuaValue
    }

    private inner class SetPressedSprintingFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.sprintKey
                sprintKey.isPressed = arg.toboolean()
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class SetPressedForwardFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.forwardKey
                sprintKey.isPressed = arg.toboolean()
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class SetPressedBackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.backKey
                sprintKey.isPressed = arg.toboolean()
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class SetPressedLeftFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.leftKey
                sprintKey.isPressed = arg.toboolean()
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class SetPressedRightFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.rightKey
                sprintKey.isPressed = arg.toboolean()
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class SetPressedJumpFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.jumpKey
                sprintKey.isPressed = arg.toboolean()
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class SetPressedSneakFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.sneakKey
                sprintKey.isPressed = arg.toboolean()
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class SetPressedAttackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.attackKey
                sprintKey.isPressed = arg.toboolean()
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class SetPressedUseFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.useKey
                sprintKey.isPressed = arg.toboolean()
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    // Переопределяем необходимые методы LuaValue
    override fun typename(): String = "input"
    override fun tojstring(): String = "InputObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return LuaValue.TUSERDATA
    }
}