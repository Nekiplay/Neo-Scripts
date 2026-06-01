package com.nekiplay.neoscripts.features.lua.objects.modules

import com.nekiplay.neoscripts.Main
import com.nekiplay.neoscripts.Main.LUA_MANAGER
import com.nekiplay.neoscripts.utils.Utils
import net.neoforged.fml.ModList
import org.luaj.vm2.LuaTable
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
            "screenshot" -> ScreenshotFunction()
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

    class ScreenshotFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (GraphicsEnvironment.isHeadless()) {
                throw LuaError("Cannot take screenshot: GraphicsEnvironment is headless")
            }

            // Если передан аргумент, используем его как путь для сохранения файла
            val path = if (args.arg1().isnil()) null else args.arg1().checkjstring()

            return try {
                // Ищем активное и показываемое (isShowing) на экране окно, либо просто первое видимое
                val window = Window.getWindows().firstOrNull { it.isShowing && it.isActive }
                    ?: Window.getWindows().firstOrNull { it.isShowing }
                    ?: throw LuaError("No showing Java windows found to screenshot")

                // Получаем координаты и размеры окна
                val loc = window.locationOnScreen
                val rect = Rectangle(loc.x, loc.y, window.width, window.height)

                // Захватываем область экрана
                val robot = Robot()
                val image = robot.createScreenCapture(rect)

                if (path != null) {
                    val file = File(path)
                    val parent = file.parentFile
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs()
                    }
                    val success = ImageIO.write(image, "png", file)
                    if (!success) {
                        throw LuaError("Failed to write image to path: $path")
                    }
                    return TRUE
                } else {
                    // Конвертируем изображение в PNG байты
                    val baos = ByteArrayOutputStream()
                    ImageIO.write(image, "png", baos)
                    val bytes = baos.toByteArray()

                    // Возвращаем таблицу байтов (аналогично toLuaTable)
                    val bytesTable = tableOf()
                    for (i in bytes.indices) {
                        bytesTable.set(i + 1, valueOf(bytes[i].toInt() and 0xFF))
                    }
                    bytesTable
                }
            } catch (e: Exception) {
                throw LuaError("Failed to capture screenshot: ${e.message}")
            }
        }
    }

    private inner class GetHWID : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(Utils.getHWID8())
        }
    }

    private inner class GetLoadedMods : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = LuaTable()
            val modList = ModList.get()

            val mods = modList.mods

            mods.forEachIndexed { index, modInfo ->
                val modTable = LuaTable()

                // Базовые строки
                modTable.set("name", modInfo.displayName)
                modTable.set("id", modInfo.modId)
                modTable.set("description", modInfo.description ?: "")
                modTable.set("version", modInfo.version.toString())
                modTable.set("loader", modInfo.loader.name() ?: "")
                modTable.set("loader_version", modInfo.loader.version() ?: "")

                // Иконка (путь к файлу внутри JAR, может быть null)
                modInfo.logoFile?.let { logoPath ->
                    modTable.set("iconPath", logoPath.toString())
                }

                // Зависимости — Collection<? extends IModDependency>
                val depsTable = LuaTable()
                modInfo.dependencies.forEach { dep ->
                    val depId = dep.modId
                    val depData = LuaTable()
                    // kind — тип зависимости: DEPENDENCY, RECOMMENDED, INCOMPATIBLE, OPTIONAL?
                    depData.set("kind", dep.type.name) // или dep.type.toString()
                    depData.set("modId", dep.modId)
                    // versionRequirements — строка или список
                    val versionReqs = dep.versionRange?.toString() ?: ""
                    depData.set("versionRequirements", versionReqs)
                    depsTable.set(depId, depData)
                }
                modTable.set("dependencies", depsTable)

                table.set(index + 1, modTable)
            }
            return table
        }
    }

    private inner class GetModLoader : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf("NeoForge")
        }
    }

    private inner class IsModLoaded : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(ModList.get().isLoaded(arg.tojstring())) ?: FALSE
        }
    }

    private inner class UnLoadScript : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(Main.LUA_MANAGER?.unloadScript(arg.tojstring()) ?: false)
        }
    }

    private inner class LoadScript : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val file = File(arg.tojstring())
            if (file.exists()) {
                Main.LUA_MANAGER?.unloadScript(file.nameWithoutExtension)
                Main.LUA_MANAGER?.executeScript(file)
                return TRUE
            }
            return FALSE
        }
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