package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua


class InputObject(L: Lua) : SimpleLuaWrapper(L) {

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            // Actions
            "useItem" -> JFunction { useItem(it) }
            "silentUse" -> JFunction { silentUse(it) }
            "syncSelectedSlot" -> JFunction { syncSelectedSlot(it) }
            "attackBlock" -> JFunction { attackBlock(it) }
            "attackEntity" -> JFunction { attackEntity(it) }
            "interactBlock" -> JFunction { interactBlock(it) }
            "interactEntity" -> JFunction { interactEntity(it) }
            "leftClick" -> JFunction { leftClick(it) }
            "rightClick" -> JFunction { rightClick(it) }

            // Setters
            "setSelectedSlot" -> JFunction { setSelectedSlot(it) }
            "setPressedSprinting" -> JFunction { setPressedSprinting(it) }
            "setPressedJump" -> JFunction { setPressedJump(it) }
            "setPressedSneak" -> JFunction { setPressedSneak(it) }
            "setPressedForward" -> JFunction { setPressedForward(it) }
            "setPressedBack" -> JFunction { setPressedBack(it) }
            "setPressedLeft" -> JFunction { setPressedLeft(it) }
            "setPressedRight" -> JFunction { setPressedRight(it) }
            "setPressedAttack" -> JFunction { setPressedAttack(it) }
            "setPressedUse" -> JFunction { setPressedUse(it) }

            // Getters
            "getSelectedSlot" -> JFunction { getSelectedSlot(it) }
            "isPressedSprinting" -> JFunction { isPressedSprinting(it) }
            "isPressedJump" -> JFunction { isPressedJump(it) }
            "isPressedSneak" -> JFunction { isPressedSneak(it) }
            "isPressedForward" -> JFunction { isPressedForward(it) }
            "isPressedBack" -> JFunction { isPressedBack(it) }
            "isPressedLeft" -> JFunction { isPressedLeft(it) }
            "isPressedRight" -> JFunction { isPressedRight(it) }
            "isPressedAttack" -> JFunction { isPressedAttack(it) }
            "isPressedUse" -> JFunction { isPressedUse(it) }

            else -> null
        }
    }

    // Реализованные вами функции
    private fun syncSelectedSlot(l: Lua): Int {
        l.push(mc.gameMode?.syncSelectedSlot() == true)
        return 1
    }

    private fun leftClick(l: Lua): Int {
        // Логика: вызываем метод и возвращаем результат (обычно boolean)
        l.push(mc.leftClick())
        return 1
    }

    private fun rightClick(l: Lua): Int {
        mc.rightClick()
        l.push(true)
        return 1
    }

    private fun attackBlock(l: Lua): Int {
        l.push(mc.gameMode?.attackBlock() == true)
        return 1
    }

    private fun attackEntity(l: Lua): Int {
        l.push(mc.gameMode?.attackEntity() == true)
        return 1
    }

    private fun interactEntity(l: Lua): Int {
        l.push(mc.gameMode?.interactEntity() == true)
        return 1
    }

    private fun interactBlock(l: Lua): Int {
        l.push(mc.gameMode?.interactBlock() == true)
        return 1
    }

    private fun useItem(l: Lua): Int {
        l.push(mc.gameMode?.useItem() == true)
        return 1
    }

    private fun silentUse(l: Lua): Int {
        if (l.isNumber(1)) {
            val slot = l.toInteger(1).toInt()
            if (slot in 0..8) {
                if (mc.screen == null) {
                    mc.gameMode?.silentUse(slot)
                    l.push(true) // Аналог TRUE
                    return 1
                }
                l.push(false) // Аналог FALSE
                return 1
            } else {
                l.push(false) // Аналог FALSE
                return 1
            }
        } else {
            l.pushNil() // Аналог NIL
            return 1
        }
    }

    private fun setSelectedSlot(l: Lua): Int {
        if (l.isNumber(1)) {
            val slot = l.toInteger(1).toInt()
            if (slot in 0..8) {
                if (mc.screen == null) {
                    mc.player?.inventory?.selectedSlot = slot
                    l.push(true) // TRUE
                    return 1
                }
                l.push(false) // FALSE
            } else {
                l.push(false) // FALSE
            }
        } else {
            l.pushNil() // NIL
        }
        return 1
    }

    // Вспомогательная функция для сохранения идентичной логики во всех кнопках
    private fun handleKeyUpdate(l: Lua, keyBinding: KeyMapping): Int {
        if (l.isBoolean(1)) {
            val state = l.toBoolean(1)
            if (mc.screen == null) {
                keyBinding.isDown = state
                val key = KeyBindingHelper.getBoundKeyOf(keyBinding)
                KeyMapping.set(key, state)
                if (state) {
                    KeyMapping.click(key)
                }
                l.push(true) // TRUE
                return 1
            }
            l.push(false) // FALSE
            return 1
        } else {
            l.pushNil() // NIL
            return 1
        }
    }

    private fun setPressedSprinting(l: Lua): Int {
        return handleKeyUpdate(l, mc.options.keySprint)
    }

    private fun setPressedForward(l: Lua): Int {
        return handleKeyUpdate(l, mc.options.keyUp)
    }

    private fun setPressedBack(l: Lua): Int {
        return handleKeyUpdate(l, mc.options.keyDown)
    }

    private fun setPressedLeft(l: Lua): Int {
        return handleKeyUpdate(l, mc.options.keyLeft)
    }

    private fun setPressedRight(l: Lua): Int {
        return handleKeyUpdate(l, mc.options.keyRight)
    }

    private fun setPressedJump(l: Lua): Int {
        return handleKeyUpdate(l, mc.options.keyJump)
    }

    private fun setPressedSneak(l: Lua): Int {
        return handleKeyUpdate(l, mc.options.keyShift)
    }

    private fun setPressedAttack(l: Lua): Int {
        return handleKeyUpdate(l, mc.options.keyAttack)
    }

    private fun setPressedUse(l: Lua): Int {
        if (l.isBoolean(1)) {
            if (mc.screen == null) {
                val state = l.toBoolean(1)
                val keyBinding = mc.options.keyUse
                keyBinding.isDown = state
                KeyMapping.set(KeyBindingHelper.getBoundKeyOf(keyBinding), state)
                l.push(true) // TRUE
                return 1
            }
            l.push(false) // FALSE
            return 1
        } else {
            l.pushNil() // NIL
            return 1
        }
    }

    // Getters
    private fun getSelectedSlot(l: Lua): Int {
        l.push((mc.player?.inventory?.selectedSlot ?: 0))
        return 1
    }

    private fun isPressedSprinting(l: Lua): Int {
        l.push(mc.options.keySprint.isDown)
        return 1
    }

    private fun isPressedForward(l: Lua): Int {
        l.push(mc.options.keyUp.isDown)
        return 1
    }

    private fun isPressedBack(l: Lua): Int {
        l.push(mc.options.keyDown.isDown)
        return 1
    }

    private fun isPressedLeft(l: Lua): Int {
        l.push(mc.options.keyLeft.isDown)
        return 1
    }

    private fun isPressedRight(l: Lua): Int {
        l.push(mc.options.keyRight.isDown)
        return 1
    }

    private fun isPressedJump(l: Lua): Int {
        l.push(mc.options.keyJump.isDown)
        return 1
    }

    private fun isPressedSneak(l: Lua): Int {
        l.push(mc.options.keyShift.isDown)
        return 1
    }

    private fun isPressedAttack(l: Lua): Int {
        l.push(mc.options.keyAttack.isDown)
        return 1
    }

    private fun isPressedUse(l: Lua): Int {
        l.push(mc.options.keyUse.isDown)
        return 1
    }
}