package com.nekiplay.neoscripts.features.modules

import com.nekiplay.neoscripts.features.modules.impl.misc.LuaEvents

object ModuleManager {
    private val modulesMap = mutableMapOf<String, ClientModule>()
    private val modulesClassMap = mutableMapOf<Class<*>, ClientModule>()
    @Suppress("LongMethod")
    fun registerInbuilt() {
        val modules = arrayOf(
            /* Misc */
            LuaEvents
        )

        modules.forEach { module ->
            modulesMap[module.get_name()] = module
            modulesClassMap[module.javaClass] = module
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
}