package com.nekiplay.neoscripts.features.lua.objects.modules

import com.nekiplay.neoscripts.Main
import com.nekiplay.neoscripts.Main.LUA_MANAGER
import com.nekiplay.neoscripts.utils.Utils
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

    class ScreenshotFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return try {
                val bytes = captureMinecraftScreenshot()

                // Создаем пустую Lua-таблицу
                val bytesTable = tableOf()

                // Заполняем её байтами в виде чисел (0-255) с индексацией от 1
                for (i in bytes.indices) {
                    bytesTable.set(i + 1, valueOf(bytes[i].toInt() and 0xFF))
                }

                bytesTable
            } catch (e: Exception) {
                error("Failed to capture Minecraft screenshot: ${e.message}")
            }
        }

        private fun captureMinecraftScreenshot(): ByteArray {
            // 1. Находим класс Minecraft/MinecraftClient независимо от загрузчика (Fabric/Yarn или Forge/Mojang)
            val mcClass = try {
                Class.forName("net.minecraft.client.MinecraftClient") // Fabric/Yarn
            } catch (e: ClassNotFoundException) {
                Class.forName("net.minecraft.client.Minecraft") // Forge/Mojang
            }

            val getInstanceMethod = mcClass.getMethod("getInstance")
            val mcInstance = getInstanceMethod.invoke(null)

            // 2. Ищем метод execute(Runnable) для отправки задачи на основной поток рендеринга игры
            val executeMethod = mcClass.methods.firstOrNull {
                it.name == "execute" && it.parameterCount == 1 && Runnable::class.java.isAssignableFrom(it.parameterTypes[0])
            } ?: throw Exception("Main thread executor not found in Minecraft instance")

            val future = CompletableFuture<ByteArray>()

            // Выполняем захват в потоке OpenGL (это предотвратит черный экран и краши LWJGL)
            executeMethod.invoke(mcInstance, Runnable {
                try {
                    // 3. Получаем объект Framebuffer (или RenderTarget в новых версиях)
                    val framebufferMethod = mcClass.methods.firstOrNull {
                        val returnTypeName = it.returnType.name
                        returnTypeName.contains("Framebuffer") || returnTypeName.contains("RenderTarget")
                    } ?: throw Exception("Framebuffer / RenderTarget method not found")
                    val framebuffer = framebufferMethod.invoke(mcInstance)

                    // 4. Находим класс ScreenshotHelper / ScreenshotRecorder
                    val screenshotClass = try {
                        Class.forName("net.minecraft.client.util.ScreenshotRecorder") // Yarn
                    } catch (e: ClassNotFoundException) {
                        try {
                            Class.forName("net.minecraft.client.Screenshot") // Mojang 1.21+
                        } catch (e: ClassNotFoundException) {
                            Class.forName("net.minecraft.util.ScreenShotHelper") // Legacy / Старые версии
                        }
                    }

                    var nativeImage: Any? = null

                    // 5. Ищем метод takeScreenshot.
                    // В новых версиях он асинхронный и принимает (RenderTarget, Consumer<NativeImage>)
                    val asyncTakeMethod = screenshotClass.methods.firstOrNull {
                        it.name == "takeScreenshot" && it.parameterCount == 2 &&
                                it.parameterTypes[1] == Consumer::class.java
                    }

                    if (asyncTakeMethod != null) {
                        val futureImage = CompletableFuture<Any>()
                        val consumer = Consumer<Any> { img ->
                            futureImage.complete(img)
                        }
                        asyncTakeMethod.invoke(null, framebuffer, consumer)
                        nativeImage = futureImage.get()
                    } else {
                        // В более старых версиях он синхронный и возвращает NativeImage напрямую
                        val syncTakeMethod = screenshotClass.methods.firstOrNull {
                            it.name == "takeScreenshot" && it.parameterCount == 1
                        } ?: throw Exception("takeScreenshot method not found")
                        nativeImage = syncTakeMethod.invoke(null, framebuffer)
                    }

                    if (nativeImage == null) {
                        throw Exception("Failed to capture NativeImage from Framebuffer")
                    }

                    // 6. Получаем байты PNG изображения из NativeImage
                    val getBytesMethod = nativeImage.javaClass.getMethod("getBytes")
                    val bytes = getBytesMethod.invoke(nativeImage) as ByteArray

                    // Освобождаем выделенную нативную память
                    if (nativeImage is AutoCloseable) {
                        nativeImage.close()
                    }

                    future.complete(bytes)
                } catch (ex: Exception) {
                    future.completeExceptionally(ex)
                }
            })

            return future.get()
        }
    }

    private inner class GetHWID : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(Utils.getHWID8())
        }
    }

    private inner class GetLoadedMods : ZeroArgFunction() {
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

    private inner class GetModLoader : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf("Fabric")
        }
    }

    private inner class IsModLoaded : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(FabricLoader.getInstance().isModLoaded(arg.tojstring())) ?: FALSE
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