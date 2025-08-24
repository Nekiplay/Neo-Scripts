package com.nekiplay.hypixelcry.features.lua.objects.player

import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction


class InputObject: LuaValue() {
    private val client: MinecraftClient = MinecraftClient.getInstance()

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // Setters
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
            else -> LuaValue.NIL
        } as LuaValue
    }

    // Setters
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

    // Getters
    private inner class IsPressedSprintingFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = client.options.sprintKey
            return LuaValue.valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedForwardFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = client.options.forwardKey
            return LuaValue.valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedBackFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = client.options.backKey
            return LuaValue.valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedLeftFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = client.options.leftKey
            return LuaValue.valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedRightFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = client.options.rightKey
            return LuaValue.valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedJumpFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = client.options.jumpKey
            return LuaValue.valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedSneakFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = client.options.sneakKey
            return LuaValue.valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedAttackFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = client.options.attackKey
            return LuaValue.valueOf(sprintKey.isPressed)
        }
    }

    private inner class IsPressedUseFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val sprintKey: KeyBinding = client.options.useKey
            return LuaValue.valueOf(sprintKey.isPressed)
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