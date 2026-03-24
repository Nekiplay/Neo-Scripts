package com.nekiplay.hypixelcry.features.lua.objects.misc

import ai.catboost.CatBoostModel
import com.nekiplay.hypixelcry.features.lua.objects.misc.catboost.CatBoostModelLua
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua

class CatboostLib(val L: Lua) {

    // Метод для регистрации библиотеки catboost в глобальной области
    fun register() {
        L.newTable() // Создаем таблицу библиотеки

        L.push(JFunction { loadModel(it) })
        L.setField(-2, "loadModel")

        L.setGlobal("catboost") // Регистрируем таблицу как глобальную переменную
    }

    private fun loadModel(l: Lua): Int {
        if (l.isString(1)) {
            val path = l.toString(1)
            if (path != null) {
                val model = CatBoostModel.loadModel(path)
                // Оборачиваем модель в объект с метатаблицей и пушим в стек
                l.push(CatBoostModelLua(l, model).push())
                return 1
            }
        }

        l.pushNil()
        return 1
    }
}