package com.nekiplay.hypixelcry.features.lua.objects.modules

import com.nekiplay.hypixelcry.HypixelCry.LUA_MANAGER
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua

class ModulesLib(val L: Lua) {

    fun register() {
        L.newTable() // Создаем таблицу 'modules'

        // Регистрируем функцию getLoadedScripts
        L.push(JFunction { getLoadedScripts(it) })
        L.setField(-2, "getLoadedScripts")

        // Регистрируем объект pathFinder
        // Мы используем .push(), так как PathFinderRendererObject — это SimpleLuaWrapper
        val pathFinder = PathFinderRendererObject(L)
        pathFinder.push()
        L.setField(-2, "pathFinder")

        // Делаем таблицу глобальной
        L.setGlobal("modules")
    }

    private fun getLoadedScripts(l: Lua): Int {
        val scripts = LUA_MANAGER.getLoadedScripts()

        l.newTable() // Создаем таблицу-результат
        scripts.forEachIndexed { index, script ->
            l.push(script.scriptName)
            // В Lua массивы начинаются с 1, используем index + 1
            l.rawSetI(-2, index + 1)
        }

        return 1
    }
}