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
            "setPressedSprinting" -> setPressedSprintingFunction()
            "setPressedJump" -> setPressedJumpFunction()
            "setPressedSneak" -> setPressedSneakFunction()

            "setPressedForward" -> setPressedForwardFunction()
            "setPressedBack" -> setPressedBackFunction()
            "setPressedLeft" -> setPressedLeftFunction()
            "setPressedRight" -> setPressedRightFunction()
            
            // Mouse
            "setPressedAttack" -> setPressedAttackFunction()
            "setPressedUse" -> setPressedUseFunction()
            else -> LuaValue.NIL
        } as LuaValue
    }

    private inner class setPressedSprintingFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.sprintKey
                sprintKey.setPressed(arg.toboolean())
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class setPressedForwardFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.forwardKey
                sprintKey.setPressed(arg.toboolean())
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class setPressedBackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.backKey
                sprintKey.setPressed(arg.toboolean())
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class setPressedLeftFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.leftKey
                sprintKey.setPressed(arg.toboolean())
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class setPressedRightFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.rightKey
                sprintKey.setPressed(arg.toboolean())
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class setPressedJumpFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.jumpKey
                sprintKey.setPressed(arg.toboolean())
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class setPressedSneakFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.sneakKey
                sprintKey.setPressed(arg.toboolean())
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class setPressedAttackFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.attackKey
                sprintKey.setPressed(arg.toboolean())
                LuaValue.NIL
            }
            else {
                LuaValue.NIL
            }
        }
    }

    private inner class setPressedUseFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return if (arg != null && arg.isboolean()) {
                val sprintKey: KeyBinding = client.options.useKey
                sprintKey.setPressed(arg.toboolean())
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