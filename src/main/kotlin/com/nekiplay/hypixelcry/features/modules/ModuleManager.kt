package com.nekiplay.hypixelcry.features.modules

import com.nekiplay.hypixelcry.events.KeyEvent
import com.nekiplay.hypixelcry.events.MouseButtonEvent
import com.nekiplay.hypixelcry.features.modules.impl.macros.AspectOfTheTeleport
import com.nekiplay.hypixelcry.features.modules.impl.macros.HealingWands
import com.nekiplay.hypixelcry.features.modules.impl.macros.RogueSword
import com.nekiplay.hypixelcry.features.modules.impl.macros.WitherCloak
import com.nekiplay.hypixelcry.features.modules.impl.macros.ZombieSword
import com.nekiplay.hypixelcry.features.modules.impl.misc.LuaEvents
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction
import net.minecraft.util.ActionResult

object ModuleManager {
    private val modulesMap = mutableMapOf<String, ClientModule>()
    private val modulesClassMap = mutableMapOf<Class<*>, ClientModule>()
    @Suppress("LongMethod")
    fun registerInbuilt() {
        val modules = arrayOf(
            /* Macros */
            AspectOfTheTeleport,
            HealingWands,
            RogueSword,
            WitherCloak,
            ZombieSword,

            /* Misc */
            LuaEvents
        )

        modules.forEach { module ->
            modulesMap[module.get_name()] = module
            modulesClassMap[module.javaClass] = module

            if (module is BindableClientModule) {
                registerBindable(module)
            }
            module.init()
        }
    }

    /**
     * Получить модуль по его названию
     * @param name Название модуля
     * @return Модуль или null, если не найден
     */
    fun getModuleByName(name: String): ClientModule? {
        return modulesMap[name]
    }

    /**
     * Получить модуль по его классу
     * @param clazz Класс модуля
     * @return Модуль или null, если не найден
     */
    fun <T : ClientModule> getModuleByClass(clazz: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return modulesClassMap[clazz] as? T
    }

    /**
     * Получить все зарегистрированные модули
     * @return Список всех модулей
     */
    fun getAllModules(): List<ClientModule> {
        return modulesMap.values.toList()
    }

    private fun registerBindable(module: BindableClientModule) {
        fun handlePress(action: KeyAction) {
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
                handlePress(keyEvent.action)
            }
            ActionResult.PASS
        })

        // Подписка на событие мыши
        MouseButtonEvent.EVENT.register(MouseButtonEvent.KeyCallback { mouseButtonEvent ->
            if (mouseButtonEvent.button == module.getKeybind()) {
                handlePress(mouseButtonEvent.action)
            }
            ActionResult.PASS
        })
    }
}