package com.nekiplay.hypixelcry.features.modules

import com.nekiplay.hypixelcry.events.KeyEvent
import com.nekiplay.hypixelcry.events.MouseButtonEvent
import com.nekiplay.hypixelcry.features.modules.impl.macros.AspectOfTheTeleport
import com.nekiplay.hypixelcry.features.modules.impl.macros.HealingWands
import com.nekiplay.hypixelcry.features.modules.impl.macros.RogueSword
import com.nekiplay.hypixelcry.features.modules.impl.macros.WitherCloak
import com.nekiplay.hypixelcry.features.modules.impl.macros.ZombieSword
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction
import net.minecraft.util.ActionResult

object ModuleManager {
    @Suppress("LongMethod")
    fun registerInbuilt() {
        val modules = arrayOf(
            AspectOfTheTeleport,
            HealingWands,
            RogueSword,
            WitherCloak,
            ZombieSword
        )

        modules.forEach { module ->
            if (module is BindableClientModule) {
                registerBindable(module)
            }
            module.init()
        }
    }

    private fun registerBindable(module: BindableClientModule) {
        fun handlePress(bind: Int, action: KeyAction) {
            when (action) {
                KeyAction.Press -> {
                    module.press()
                }
                KeyAction.Release -> {
                    module.release()
                }
                KeyAction.Repeat -> {
                    module.repeat()
                }
            }
        }

        // Подписка на событие клавиатуры
        KeyEvent.EVENT.register(KeyEvent.KeyCallback { keyEvent ->
            if (keyEvent.key == module.getKeybind()) {
                handlePress(keyEvent.key, keyEvent.action)
            }
            ActionResult.PASS
        })

        // Подписка на событие мыши
        MouseButtonEvent.EVENT.register(MouseButtonEvent.KeyCallback { mouseButtonEvent ->
            if (mouseButtonEvent.button == module.getKeybind()) {
                handlePress(mouseButtonEvent.button, mouseButtonEvent.action)
            }
            ActionResult.PASS
        })
    }
}