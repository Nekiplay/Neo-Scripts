package com.nekiplay.neoscripts.features.lua.objects.modules

import com.nekiplay.neoscripts.Main.LUA_MANAGER
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class ModulesObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getLoadedScripts" -> GetLoadedScriptsFunction()
            "getScriptRequirements" -> GetScriptRequirements ()
            else -> NIL
        } as LuaValue
    }

    private inner class GetScriptRequirements : OneArgFunction() {
        override fun call(stringName: LuaValue): LuaValue {
            val name = stringName.checkjstring()

            // Используем Set для отслеживания посещенных скриптов (защита от бесконечной рекурсии)
            return buildDependencyTree(name, mutableSetOf())
        }

        private fun buildDependencyTree(scriptName: String, visited: MutableSet<String>): LuaValue {
            val result = tableOf()
            result.set("name", valueOf(scriptName))

            // Если мы уже обрабатывали этот скрипт в этой ветке, выходим (циклическая ссылка)
            if (visited.contains(scriptName)) {
                result.set("circular", TRUE)
                return result
            }
            visited.add(scriptName)

            val dependenciesTable = tableOf()
            var index = 1

            // 1. Находим объект скрипта в менеджере
            val scriptObject = LUA_MANAGER?.getLoadedScripts()?.find { it.scriptName == scriptName }

            // 2. Если нашли, берем его граф зависимостей
            // Предполагаем, что localDependencyGraph: Map<String, Set<String>>
            scriptObject?.localDependencyGraph?.get(scriptName)?.forEach { depName ->
                // РЕКУРСИЯ: вызываем эту же функцию для каждой зависимости
                // Передаем копию visited, чтобы не блокировать зависимости на разных ветках
                val subTree = buildDependencyTree(depName, visited.toMutableSet())
                dependenciesTable.set(index++, subTree)
            }

            result.set("dependencies", dependenciesTable)
            return result
        }
    }

    private inner class GetLoadedScriptsFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = tableOf()
            var index = 1
            for (script in LUA_MANAGER?.getLoadedScripts() ?: emptyList()) {
                table.set(index, script.scriptName)
                index++
            }
            return table
        }
    }

    override fun typename(): String = "modules"
    override fun tojstring(): String = "ModulesObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}