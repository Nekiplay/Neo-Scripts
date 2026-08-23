package com.nekiplay.neoscripts.client.features.lua.objects.modules

import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.ClientMain.LUA_MANAGER
import com.nekiplay.neoscripts.client.features.lua.LuaClientScript
import com.nekiplay.neoscripts.client.utils.Utils
import net.fabricmc.loader.api.FabricLoader
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.File

class ModulesObject: LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getLoadedScripts" -> GetLoadedScriptsFunction()
            "getScriptRequirements" -> GetScriptRequirements()
            "loadScript" -> LoadScript()
            "unloadScript" -> UnLoadScript()
            "getModLoader" -> GetModLoader()
            "isModLoaded" -> IsModLoaded()
            "getLoadedMods" -> GetLoadedMods()
            "getHWID" -> GetHWID()
            else -> NIL
        } as LuaValue
    }


    private class GetHWID : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(Utils.getHWID8())
        }
    }

    private class GetLoadedMods : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = tableOf()
            val fabricLoader = FabricLoader.getInstance()
            fabricLoader.allMods.forEachIndexed { index, mod ->

                val modTable = tableOf()
                val meta = mod.metadata

                // Базовые строки
                modTable.set("name", meta.name)
                modTable.set("id", meta.id)
                modTable.set("description", meta.description)
                modTable.set("version", meta.version.friendlyString)   // версия как строка
                modTable.set("license", meta.license.first() ?: "")

                // Авторы
                val authorsTable = tableOf()
                meta.authors.forEachIndexed { i, person ->
                    authorsTable.set(i + 1, person.name)
                }
                modTable.set("authors", authorsTable)

                // Зависимости (простая версия: id -> массив строк условий)
                val depsTable = tableOf()
                meta.dependencies.forEach { dep ->
                    val depData = tableOf()
                    depData.set("kind", dep.kind.name)
                    depData.set("modId", dep.modId)
                    val versionReqs = dep.versionRequirements.joinToString(" || ") { it.toString() }
                    depData.set("versionRequirements", versionReqs)
                    depsTable.set(dep.modId, depData)
                }
                modTable.set("dependencies", depsTable)

                // Вкладчики (contributors) – если есть
                if (meta.contributors.isNotEmpty()) {
                    val contribTable = tableOf()
                    meta.contributors.forEachIndexed { i, person ->
                        contribTable.set(i + 1, person.name)
                    }
                    modTable.set("contributors", contribTable)
                }

                table.set(index + 1, modTable)
            }
            return table
        }
    }

    private class GetModLoader : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf("Fabric")
        }
    }

    private class IsModLoaded : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(FabricLoader.getInstance().isModLoaded(arg.tojstring())) ?: FALSE
        }
    }

    private class UnLoadScript : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(ClientMain.LUA_MANAGER?.unloadScript(arg.tojstring()) ?: false)
        }
    }

    private class LoadScript : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val file = File(arg.tojstring())
            if (file.exists()) {
                ClientMain.LUA_MANAGER?.unloadScript(file.nameWithoutExtension)
                val script = ClientMain.LUA_MANAGER?.getScript(file, false, null)
                if (script != null) {
                    ClientMain.LUA_MANAGER?.executeScript(file, script)
                    return TRUE
                }
                return FALSE
            }
            return FALSE
        }
    }

    private class GetScriptRequirements : OneArgFunction() {
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

            if (scriptObject is LuaClientScript) {
                // 2. Если нашли, берем его граф зависимостей
                // Предполагаем, что localDependencyGraph: Map<String, Set<String>>
                scriptObject.localDependencyGraph.get(scriptName)?.forEach { depName ->
                    // РЕКУРСИЯ: вызываем эту же функцию для каждой зависимости
                    // Передаем копию visited, чтобы не блокировать зависимости на разных ветках
                    val subTree = buildDependencyTree(depName, visited.toMutableSet())
                    dependenciesTable.set(index++, subTree)
                }
            }

            result.set("dependencies", dependenciesTable)
            return result
        }
    }

    private class GetLoadedScriptsFunction : ZeroArgFunction() {
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