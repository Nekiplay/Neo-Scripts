package com.nekiplay.neoscripts.common.features.lua

import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.common.features.lua.objects.misc.ArchiveLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.Blocks
import com.nekiplay.neoscripts.common.features.lua.objects.misc.CatboostLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.Creator
import com.nekiplay.neoscripts.common.features.lua.objects.misc.DJLLuaTrainer
import com.nekiplay.neoscripts.common.features.lua.objects.misc.EncodingLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.Entities
import com.nekiplay.neoscripts.common.features.lua.objects.misc.FFILib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.Items
import com.nekiplay.neoscripts.common.features.lua.objects.misc.JsonLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.TCPLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.ThreadLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.UDPLib
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponentBuilder
import com.nekiplay.neoscripts.common.features.lua.objects.misc.http.HttpClientLib
import net.fabricmc.loader.api.FabricLoader
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaClosure
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.luajc.LuaJC
import java.util.Stack
import java.util.concurrent.ConcurrentHashMap

/**
 * Базовый класс Lua-скрипта, доступный на обеих сторонах (клиент и сервер).
 *
 * Содержит всё общее:
 *  - инфраструктуру require (поиск файлов модулей, граф зависимостей, кэш);
 *  - компиляцию в Java-байткод через LuaJC с фолбэком на интерпретатор;
 *  - стандартные функции globals: print, currentScriptName, modVersion;
 *  - ленивые инстансы всех общих библиотек и их раздачу через require:
 *    creator, items, blocks, entities, json, http, tcp, udp, threads, ffi,
 *    djl, encoding, archive, catboost, text_builder.
 *
 * Сторонние классы (LuaClientScript / LuaServerScript) добавляют свои
 * библиотеки и события поверх этого базового класса.
 */
open class CommonLuaScript(scriptName: String, manager: LuaManager) : Script(scriptName, manager) {

    // Локальный стек загрузки для этого конкретного экземпляра скрипта
    protected val loadingStack = Stack<String>()

    // Кэш уже созданных системных модулей
    protected val systemModuleCache = ConcurrentHashMap<String, LuaValue>()

    // Локальный граф зависимостей для этого конкретного экземпляра скрипта
    // Ключ: имя файла, Значение: список имен, которые этот файл запросил через require
    val localDependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()
    val requireCache = ConcurrentHashMap<String, LuaValue>()

    // Synchronize only when needed
    val callbacksLock = Any()

    // Dependency tracking for nested requires
    protected val dependencies = ConcurrentHashMap<String, MutableList<String>>()

    // Общие библиотеки (ленивая инициализация)
    protected var tcpLib: TCPLib? = null
    protected var udpLib: UDPLib? = null
    protected var threadLib: ThreadLib? = null
    protected var djlLibrary: DJLLuaTrainer? = null
    protected var ffi: FFILib? = null
    protected var http: HttpClientLib? = null

    init {
        // Register standard libraries
        scriptGlobals.load(MinecraftLuajavaLib())

        // Compile scripts to Java bytecode (LuaJC), fall back to interpreter on failure
        scriptGlobals.loader = Globals.Loader { proto, name, env ->
            try {
                LuaJC.instance.load(proto, name, env)
            } catch (t: Throwable) {
                LuaClosure(proto, env)
            }
        }

        registerRequireFunction()
        registerOtherCustomFunctions()

        // Подклассы регистрируют свои события/библиотеки после общей инициализации
        initializeScript()
    }

    /**
     * Переопределяется в подклассах для регистрации событий/библиотек.
     */
    protected open fun initializeScript() {}

    private fun registerRequireFunction() {
        scriptGlobals.set("require", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val moduleName = args.arg(1).checkjstring()
                val cache = args.arg(2).optboolean(false)

                // 1. Определяем "родителя" (кто вызвал require)
                val caller = if (loadingStack.isEmpty()) scriptName else loadingStack.peek()

                // 2. Записываем связь в локальное дерево этого объекта скрипта
                localDependencyGraph.getOrPut(caller) {
                    java.util.Collections.synchronizedSet(LinkedHashSet<String>())
                }.add(moduleName)

                // 3. Загружаем и выполняем (без кэша)
                if (requireCache.containsKey(moduleName) && cache) {
                    return requireCache.getOrDefault(moduleName, NIL)
                }

                val value = requireModule(moduleName)
                if (cache) {
                    requireCache[moduleName] = value
                }
                return value
            }
        })
    }

    private fun registerOtherCustomFunctions() {
        scriptGlobals.set("currentScriptName", LuaValue.valueOf(scriptName))

        // Текущая версия мода из метаданных Fabric
        scriptGlobals.set("modVersion", LuaValue.valueOf(
            FabricLoader.getInstance().getModContainer("neoscripts")
                .map { it.metadata.version.friendlyString }
                .orElse("unknown")
        ))

        // Register print function
        scriptGlobals.set("print", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val message = StringBuilder()

                // Обрабатываем все переданные аргументы
                for (i in 1..args.narg()) {
                    if (i > 1) message.append(" ")
                    message.append(args.arg(i).tojstring())
                }
                val messageStr = message.toString()
                ClientMain.LOGGER?.info(ClientMain.LOG_PREFIX + messageStr)
                return NIL
            }
        })
    }

    /**
     * Общие для обеих сторон системные модули. Возвращает null, если модуль
     * не общий — тогда его ищет подкласс.
     */
    protected fun resolveCommonModule(nameLower: String): LuaValue? {
        return when (nameLower) {
            "tcp" -> {
                if (tcpLib == null) tcpLib = TCPLib()
                tcpLib!!
            }
            "udp" -> {
                if (udpLib == null) udpLib = UDPLib()
                udpLib!!
            }
            "threads" -> {
                if (threadLib == null) threadLib = ThreadLib()
                threadLib!!
            }
            "ffi" -> {
                if (ffi == null) ffi = FFILib()
                ffi!!
            }
            "djl" -> {
                if (djlLibrary == null)
                    djlLibrary = DJLLuaTrainer(manager)
                djlLibrary!!
            }
            "json" -> JsonLib()
            "http" -> {
                if (http == null)
                    http = HttpClientLib()
                http!!
            }
            "encoding" -> EncodingLib()
            "archive" -> ArchiveLib()
            "creator" -> Creator()
            "catboost" -> CatboostLib()
            "blocks" -> Blocks()
            "entities" -> Entities()
            "items" -> Items()
            "text_builder", "textbuilder", "component_builder", "componentbuilder" ->
                LuaComponentBuilder.createLibrary()
            else -> null
        }
    }

    open fun requireModule(moduleName: String): LuaValue {
        val moduleFile = manager.findModuleFile(moduleName)
            ?: throw org.luaj.vm2.LuaError("module '$moduleName' not found")

        return try {
            // Добавляем в стек перед выполнением
            loadingStack.push(moduleName)

            // Загружаем код из файла
            val chunk = LuaManager.loadChunk(moduleFile, moduleName, scriptGlobals)

            // Выполняем. Результат не сохраняем в кэш, просто возвращаем
            chunk.call()
        } catch (e: Exception) {
            throw org.luaj.vm2.LuaError("error loading module '$moduleName': ${e.message}")
        } finally {
            // Обязательно убираем из стека после завершения
            if (!loadingStack.isEmpty() && loadingStack.peek() == moduleName) {
                loadingStack.pop()
            }
        }
    }
}
