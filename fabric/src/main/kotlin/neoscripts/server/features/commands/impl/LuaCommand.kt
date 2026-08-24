package com.nekiplay.neoscripts.server.features.commands.impl

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.ServerMain
import com.nekiplay.neoscripts.client.features.lua.LuaClientScript
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.Permissions
import org.luaj.vm2.compiler.DumpState
import org.luaj.vm2.compiler.LuaC
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.CompletableFuture


object LuaCommand {
    private val SCRIPT_SUGGESTION_PROVIDER = SuggestionProvider<CommandSourceStack> { _, builder ->
        suggestScriptFiles(builder)
    }

    private val SOURCE_SCRIPT_SUGGESTION_PROVIDER = SuggestionProvider<CommandSourceStack> { _, builder ->
        suggestSourceFiles(builder)
    }

    private val LOADED_SCRIPT_SUGGESTION_PROVIDER = SuggestionProvider<CommandSourceStack> { _, builder ->
        suggestLoadedScripts(builder)
    }

    private fun scriptsDir(): File {
        return ServerMain.scriptsDir
            ?: return File(ServerMain.CONFIG_DIR.toFile(), "neoscripts/scripts")
    }

    private fun reply(source: CommandSourceStack, message: String) {
        source.sendSuccess({ Component.literal(message) }, false)
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext) {
        val sluaCommand = Commands.literal("slua")
            .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN) }
            .then(Commands.literal("load")
                .then(Commands.argument("filename", StringArgumentType.string())
                    .suggests(SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val filename = StringArgumentType.getString(context, "filename")
                        executeLuaFile(filename, context.source)
                        1
                    }
                )
            )
            .then(Commands.literal("unload")
                .then(Commands.argument("filename", StringArgumentType.string())
                    .suggests(LOADED_SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val filename = StringArgumentType.getString(context, "filename")
                        unloadLuaScript(filename, context.source)
                        1
                    }
                )
            )
            .then(Commands.literal("list")
                .executes { context ->
                    listLuaFiles(context.source)
                    1
                }
            )
            .then(Commands.literal("loaded")
                .executes { context ->
                    listLoadedScripts(context.source, null) // Вызов без аргумента
                    1
                }
                .then(Commands.argument("scriptName", StringArgumentType.string())
                    .suggests(LOADED_SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val name = StringArgumentType.getString(context, "scriptName")
                        listLoadedScripts(context.source, name) // Вызов с именем скрипта
                        1
                    }
                )
            )
            .then(Commands.literal("toggle")
                .then(Commands.argument("scriptName", StringArgumentType.string())
                    .suggests(SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val name = StringArgumentType.getString(context, "scriptName")
                        toggleScript(context.source, name)
                        1
                    }
                )
            )
            .then(Commands.literal("compile")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(SOURCE_SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val name = StringArgumentType.getString(context, "name")
                        compileLuaScript(context.source, name)
                        1
                    }
                )
            )

        dispatcher.register(sluaCommand)
    }

    private fun toggleScript(source: CommandSourceStack, name: String) {
        val luaManager = ServerMain.LUA_MANAGER
        val loadedScripts = luaManager?.getLoadedScripts() ?: emptyList()

        // 1. Пытаемся найти уже загруженный скрипт по имени
        val loadedInstance = loadedScripts.find { it.scriptName.equals(name, ignoreCase = true) }

        if (loadedInstance != null) {
            // Если скрипт загружен — ВЫГРУЖАЕМ
            try {
                luaManager?.unloadScript(loadedInstance.scriptName)
                reply(source, "${ServerMain.PREFIX}§7Script §a${loadedInstance.scriptName} §7has been §cunloaded§7.")
            } catch (e: Exception) {
                source.sendFailure(Component.literal("${ServerMain.PREFIX}§cError unloading script: ${e.message}"))
            }
        } else {
            // Если скрипт не загружен — ЗАГРУЖАЕМ
            val dir = scriptsDir()

            // Ищем файл с расширением .lua или .luac
            val scriptFile = File(dir, "$name.lua").let {
                if (it.exists()) it else File(dir, "$name.luac")
            }

            if (scriptFile.exists()) {
                try {
                    val script = luaManager?.getScript(scriptFile, true, source.server)
                    if (script != null) {
                        luaManager?.executeScript(scriptFile, script)
                        reply(source, "${ServerMain.PREFIX}§7Script §a$name §7is now §aloaded§7.")
                    }
                    else {
                        source.sendFailure(Component.literal("${ServerMain.PREFIX}§cFailed to load script §e$name§c: server not found"))
                    }
                } catch (e: Exception) {
                    source.sendFailure(Component.literal("${ServerMain.PREFIX}§cFailed to load script §e$name§c: ${e.message}"))
                }
            } else {
                source.sendFailure(Component.literal("${ServerMain.PREFIX}§cFile §e$name.lua §cnot found in scripts directory."))
            }
        }
    }

    private fun suggestScriptFiles(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val dir = scriptsDir()

        if (!dir.exists()) {
            return builder.buildFuture()
        }

        val input = builder.remainingLowerCase
        val scriptFiles = dir.listFiles { file ->
            file.isFile && (file.name.endsWith(".lua") || file.name.endsWith(".luac"))
        } ?: emptyArray()

        scriptFiles.forEach { file ->
            val name = file.nameWithoutExtension
            if (name.lowercase().startsWith(input)) {
                builder.suggest(name)
            }
        }

        return builder.buildFuture()
    }

    private fun suggestSourceFiles(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val dir = scriptsDir()

        if (!dir.exists()) {
            return builder.buildFuture()
        }

        val input = builder.remainingLowerCase
        val scriptFiles = dir.listFiles { file ->
            file.isFile && file.name.endsWith(".lua")
        } ?: emptyArray()

        scriptFiles.forEach { file ->
            val name = file.nameWithoutExtension
            if (name.lowercase().startsWith(input)) {
                builder.suggest(name)
            }
        }

        return builder.buildFuture()
    }

    private fun suggestLoadedScripts(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val luaManager = ServerMain.LUA_MANAGER
        val loadedScripts = luaManager?.getLoadedScripts() ?: emptyList()
        val input = builder.remainingLowerCase

        loadedScripts.forEach { scriptName ->
            if (scriptName.scriptName.lowercase().startsWith(input)) {
                builder.suggest(scriptName.scriptName)
            }
        }

        return builder.buildFuture()
    }

    private fun executeLuaFile(filename: String, source: CommandSourceStack) {
        val luaManager = ServerMain.LUA_MANAGER

        if (luaManager == null) {
            source.sendFailure(Component.literal(ServerMain.PREFIX + "§cNo world is loaded yet"))
            return
        }

        val dir = scriptsDir()

        if (!dir.exists()) {
            dir.mkdirs()
            source.sendFailure(Component.literal(ServerMain.PREFIX + "§cDirectory for scripts: ${dir.path}"))
            return
        }

        val scriptFile = resolveScriptFile(dir, filename)

        if (!scriptFile.exists()) {
            source.sendFailure(Component.literal(ServerMain.PREFIX + "§cScript ${scriptFile.name} not found"))
            return
        }

        try {
            // Сначала выгружаем существующий скрипт, если он загружен
            val wasLoaded = luaManager.unloadScript(scriptFile.nameWithoutExtension)

            // Затем загружаем скрипт
            val script = luaManager.getScript(scriptFile, true, source.server)
            if (script != null) {
                val result = luaManager.executeScript(scriptFile, script)

                if (wasLoaded) {
                    reply(
                        source,
                        ServerMain.PREFIX + "§aScript '${scriptFile.nameWithoutExtension}' restarted successfully, result: '${result}'"
                    )
                } else {
                    reply(
                        source,
                        ServerMain.PREFIX + "§aScript '${scriptFile.nameWithoutExtension}' executed successfully, result: '${result}'"
                    )
                }
            }
            else {
                source.sendFailure(Component.literal("${ServerMain.PREFIX}§cFailed to load script §e${scriptFile.nameWithoutExtension}§c: server not found"))
            }
        } catch (e: Exception) {
            source.sendFailure(Component.literal(ServerMain.PREFIX + "§cScript execution error: ${e.message}"))
            e.printStackTrace()
        }
    }

    private fun resolveScriptFile(scriptsDir: File, filename: String): File {
        return when {
            filename.endsWith(".lua") || filename.endsWith(".luac") -> File(scriptsDir, filename)
            else -> {
                val luaFile = File(scriptsDir, "$filename.lua")
                if (luaFile.exists()) luaFile else File(scriptsDir, "$filename.luac")
            }
        }
    }

    private fun unloadLuaScript(filename: String, source: CommandSourceStack) {
        val luaManager = ServerMain.LUA_MANAGER
        val scriptName = when {
            filename.endsWith(".lua") -> filename.removeSuffix(".lua")
            filename.endsWith(".luac") -> filename.removeSuffix(".luac")
            else -> filename
        }

        if (luaManager?.unloadScript(scriptName) == true) {
            reply(source, ServerMain.PREFIX + "§aScript '$scriptName' unloaded successfully")
        } else {
            source.sendFailure(Component.literal(ServerMain.PREFIX + "§cScript '$scriptName' is not loaded or not found"))
        }
    }

    private fun listLuaFiles(source: CommandSourceStack) {
        val dir = scriptsDir()

        if (!dir.exists() || dir.listFiles()?.isEmpty() != false) {
            reply(source, ServerMain.PREFIX + "§7No scripts available. Create files in: ${dir.path}")
            return
        }

        val scriptFiles = dir.listFiles { file ->
            file.isFile && (file.name.endsWith(".lua") || file.name.endsWith(".luac"))
        }?.sortedBy { it.name }

        if (scriptFiles.isNullOrEmpty()) {
            reply(source, ServerMain.PREFIX + "§7No .lua or .luac files in scripts directory")
            return
        }

        reply(source, ServerMain.PREFIX + "§6Available scripts:")
        scriptFiles.forEach { file ->
            val fileType = if (file.name.endsWith(".luac")) "§9[compiled]§7" else "§a[source]§7"
            reply(source, "  §7- §e${file.nameWithoutExtension} §7(${file.length()} bytes) $fileType")
        }
    }

    private fun listLoadedScripts(source: CommandSourceStack, targetName: String?) {
        val luaManager = ServerMain.LUA_MANAGER
        val loadedScripts = luaManager?.getLoadedScripts() ?: emptyList()

        if (loadedScripts.isEmpty()) {
            reply(source, "${ServerMain.PREFIX}§7No scripts currently loaded")
            return
        }

        // РЕЖИМ 1: Показ дерева для конкретного скрипта
        if (targetName != null) {
            val scriptsToDisplay = loadedScripts.filter { it.scriptName.equals(targetName, ignoreCase = true) }

            if (scriptsToDisplay.isEmpty()) {
                source.sendFailure(Component.literal("${ServerMain.PREFIX}§cScript '$targetName' is not loaded."))
                return
            }

            reply(source, "${ServerMain.PREFIX}§6Dependency tree for §a$targetName§6:")
            reply(source, "")

            scriptsToDisplay.forEach { script ->
                if (script is LuaClientScript) {
                    val depCount = countUniqueDependencies(script.localDependencyGraph)
                    val depInfo = if (depCount > 0) " §8(§7$depCount modules§8)" else ""
                    reply(source, "  §6▶ §a§l${script.scriptName}$depInfo")

                    if (script.localDependencyGraph.isEmpty()) {
                        reply(source, "  §8  §7No dependencies")
                    } else {
                        renderBeautifulTree(
                            source,
                            script.scriptName,
                            "  §8  ",
                            script.localDependencyGraph,
                            mutableSetOf(),
                            0
                        )
                    }
                }
            }
            return
        }

        // РЕЖИМ 2: Просто краткий список всех загруженных скриптов
        reply(source, "${ServerMain.PREFIX}§6Loaded scripts §7(${loadedScripts.size}):")
        loadedScripts.forEach { script ->
            if (script is LuaClientScript) {
                val depCount = countUniqueDependencies(script.localDependencyGraph)
                // Стиль как в вашем listLuaFiles: §7- §aИмя §7(доп инфо)
                reply(
                    source,
                    "  §7- §a${script.scriptName} §8(§7$depCount modules§8) §8[ID: ${
                        script.hashCode().toString(16).take(4)
                    }]"
                )
            }
        }
        reply(source, "§7Tip: Use §e/slua loaded <name> §7to see dependencies")
    }

    private fun countUniqueDependencies(graph: Map<String, Set<String>>): Int {
        if (graph.isEmpty()) return 0

        val allModules = mutableSetOf<String>()

        // Добавляем все ключи (модули, которые имеют зависимости)
        allModules.addAll(graph.keys)

        // Добавляем все значения (сами зависимости)
        graph.values.forEach { dependencies ->
            allModules.addAll(dependencies)
        }

        return allModules.size
    }

    /**
     * Рекурсивная отрисовка дерева зависимостей
     */
    private fun compileLuaScript(source: CommandSourceStack, name: String) {
        val dir = scriptsDir()

        if (!dir.exists()) {
            source.sendFailure(Component.literal("${ServerMain.PREFIX}§cScripts directory does not exist."))
            return
        }

        val sourceFile = if (name.endsWith(".lua")) File(dir, name)
        else File(dir, "$name.lua")

        if (!sourceFile.exists()) {
            source.sendFailure(Component.literal("${ServerMain.PREFIX}§cSource file §e${sourceFile.name} §cnot found."))
            return
        }

        val outputFile = File(dir, "${sourceFile.nameWithoutExtension}_compiled.luac")

        try {
            val inputStream = FileInputStream(sourceFile)
            val proto = LuaC.instance.compile(inputStream, sourceFile.name)
            inputStream.close()

            val outputStream = FileOutputStream(outputFile)
            DumpState.dump(proto, outputStream, false)
            outputStream.close()

            reply(source, "${ServerMain.PREFIX}§aCompiled §e${sourceFile.name} §a→ §e${outputFile.name} §7(${outputFile.length()} bytes)")
        } catch (e: Exception) {
            source.sendFailure(Component.literal("${ServerMain.PREFIX}§cCompilation error: ${e.message}"))
            e.printStackTrace()
        }
    }

    private fun renderBeautifulTree(
        source: CommandSourceStack,
        name: String,
        prefix: String,
        graph: Map<String, Set<String>>,
        visited: MutableSet<String>,
        depth: Int
    ) {
        val dependencies = graph[name]?.toList() ?: return

        dependencies.forEachIndexed { index, depName ->
            val isLast = index == dependencies.size - 1
            val branchSymbol = if (isLast) "┗━" else "┣━"
            val nameColor = if (depth == 0) "§e" else "§7"

            reply(source, "$prefix$branchSymbol $nameColor$depName")

            if (depName in visited) {
                val circularPrefix = prefix + (if (isLast) "     " else "┃    ")
                reply(source, "$circularPrefix§c┗━ [Circular]")
                return@forEachIndexed
            }

            val nextVisited = visited.toMutableSet()
            nextVisited.add(name)
            val nextPrefix = prefix + (if (isLast) "     " else "┃    ")

            renderBeautifulTree(source, depName, nextPrefix, graph, nextVisited, depth + 1)
        }
    }
}
