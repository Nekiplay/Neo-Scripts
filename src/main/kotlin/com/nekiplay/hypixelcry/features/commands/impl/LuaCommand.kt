package com.nekiplay.hypixelcry.features.commands.impl

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.nekiplay.hypixelcry.HypixelCry
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandBuildContext
import net.minecraft.network.chat.Component
import org.luaj.vm2.compiler.DumpState
import org.luaj.vm2.compiler.LuaC
import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.CompletableFuture


object LuaCommand {
    private val SCRIPT_SUGGESTION_PROVIDER = SuggestionProvider<FabricClientCommandSource> { _, builder ->
        suggestScriptFiles(builder)
    }

    private val SOURCE_SCRIPT_SUGGESTION_PROVIDER = SuggestionProvider<FabricClientCommandSource> { _, builder ->
        suggestSourceFiles(builder)
    }

    private val LOADED_SCRIPT_SUGGESTION_PROVIDER = SuggestionProvider<FabricClientCommandSource> { _, builder ->
        suggestLoadedScripts(builder)
    }

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>, registryAccess: CommandBuildContext) {
        val luaCommand = ClientCommandManager.literal("lua")
            .then(ClientCommandManager.literal("load")
                .then(ClientCommandManager.argument("filename", StringArgumentType.string())
                    .suggests(SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val filename = StringArgumentType.getString(context, "filename")
                        executeLuaFile(filename, context.source)
                        1
                    }
                )
            )
            .then(ClientCommandManager.literal("unload")
                .then(ClientCommandManager.argument("filename", StringArgumentType.string())
                    .suggests(LOADED_SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val filename = StringArgumentType.getString(context, "filename")
                        unloadLuaScript(filename, context.source)
                        1
                    }
                )
            )
            .then(ClientCommandManager.literal("list")
                .executes { context ->
                    listLuaFiles(context.source)
                    1
                }
            )
            .then(ClientCommandManager.literal("loaded")
                .executes { context ->
                    listLoadedScripts(context.source, null) // Вызов без аргумента
                    1
                }
                .then(ClientCommandManager.argument("scriptName", StringArgumentType.string())
                    .suggests(LOADED_SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val name = StringArgumentType.getString(context, "scriptName")
                        listLoadedScripts(context.source, name) // Вызов с именем скрипта
                        1
                    }
                )
            )
            .then(ClientCommandManager.literal("toggle")
                .then(ClientCommandManager.argument("scriptName", StringArgumentType.string())
                    .suggests(SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val name = StringArgumentType.getString(context, "scriptName")
                        toggleScript(context.source, name)
                        1
                    }
                )
            )
            .then(ClientCommandManager.literal("folder")
                .executes { context ->
                    openScriptsFolder(context.source)
                    1
                }
            )
            .then(ClientCommandManager.literal("compile")
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                    .suggests(SOURCE_SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val name = StringArgumentType.getString(context, "name")
                        compileLuaScript(context.source, name)
                        1
                    }
                )
            )

        dispatcher.register(luaCommand)
    }

    private fun openScriptsFolder(source: FabricClientCommandSource) {
        val gameDir = Minecraft.getInstance().gameDirectory
        val scriptsDir = File(gameDir, "config/hypixelcry/scripts")

        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
        }

        val os = System.getProperty("os.name").lowercase()
        val absolutePath = scriptsDir.absolutePath

        try {
            val command = when {
                os.contains("win") -> arrayOf("explorer.exe", absolutePath)
                os.contains("mac") -> arrayOf("open", absolutePath)
                else -> arrayOf("xdg-open", absolutePath) // Для большинства дистрибутивов Linux
            }

            // Используем ProcessBuilder вместо Runtime.exec
            // Это гарантирует, что путь с пробелами будет передан как ОДИН аргумент
            ProcessBuilder(*command).start()

            source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§aOpening scripts folder..."))
        } catch (e: Exception) {
            // Если xdg-open не найден (например, в минималистичных сборках Linux)
            if (os.contains("nix") || os.contains("nux")) {
                source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§cCould not open folder. Path: §7$absolutePath"))
            } else {
                source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§cError: ${e.message}"))
            }
            e.printStackTrace()
        }
    }

    private fun toggleScript(source: FabricClientCommandSource, name: String) {
        val luaManager = HypixelCry.LUA_MANAGER
        val loadedScripts = luaManager.getLoadedScripts()

        // 1. Пытаемся найти уже загруженный скрипт по имени
        val loadedInstance = loadedScripts.find { it.scriptName.equals(name, ignoreCase = true) }

        if (loadedInstance != null) {
            // Если скрипт загружен — ВЫГРУЖАЕМ
            try {
                luaManager.unloadScript(loadedInstance.scriptName)
                source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§7Script §a${loadedInstance.scriptName} §7has been §cunloaded§7."))
            } catch (e: Exception) {
                source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§cError unloading script: ${e.message}"))
            }
        } else {
            // Если скрипт не загружен — ЗАГРУЖАЕМ
            val scriptsDir = File("config/hypixelcry/scripts")

            // Ищем файл с расширением .lua или .luac
            val scriptFile = File(scriptsDir, "$name.lua").let {
                if (it.exists()) it else File(scriptsDir, "$name.luac")
            }

            if (scriptFile.exists()) {
                try {
                    // Предполагается, что в LuaManager есть метод loadScript(File)
                    luaManager.executeScript(scriptFile)
                    source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§7Script §a$name §7is now §aloaded§7."))
                } catch (e: Exception) {
                    source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§cFailed to load script §e$name§c: ${e.message}"))
                }
            } else {
                source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§cFile §e$name.lua §cnot found in scripts directory."))
            }
        }
    }

    private fun suggestScriptFiles(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val scriptsDir = File("config/hypixelcry/scripts")

        if (!scriptsDir.exists()) {
            return builder.buildFuture()
        }

        val input = builder.remainingLowerCase
        val scriptFiles = scriptsDir.listFiles { file ->
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
        val scriptsDir = File("config/hypixelcry/scripts")

        if (!scriptsDir.exists()) {
            return builder.buildFuture()
        }

        val input = builder.remainingLowerCase
        val scriptFiles = scriptsDir.listFiles { file ->
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
        val luaManager = HypixelCry.LUA_MANAGER
        val loadedScripts = luaManager.getLoadedScripts()
        val input = builder.remainingLowerCase

        loadedScripts.forEach { scriptName ->
            if (scriptName.scriptName.lowercase().startsWith(input)) {
                builder.suggest(scriptName.scriptName)
            }
        }

        return builder.buildFuture()
    }

    private fun executeLuaFile(filename: String, source: FabricClientCommandSource) {
        val luaManager = HypixelCry.LUA_MANAGER
        val scriptsDir = File("config/hypixelcry/scripts")

        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§cDirectory for scripts: ${scriptsDir.path}"))
            return
        }

        val scriptFile = resolveScriptFile(scriptsDir, filename)

        if (!scriptFile.exists()) {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§cScript ${scriptFile.name} not found"))
            return
        }

        try {
            // Сначала выгружаем существующий скрипт, если он загружен
            val wasLoaded = luaManager.unloadScript(scriptFile.nameWithoutExtension)
            
            // Затем загружаем скрипт
            val result = luaManager.executeScript(scriptFile)

            if (wasLoaded) {
                source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§aScript '${scriptFile.nameWithoutExtension}' restarted successfully, result: '${result}'"))
            } else {
                source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§aScript '${scriptFile.nameWithoutExtension}' executed successfully, result: '${result}'"))
            }
        } catch (e: Exception) {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§cScript execution error: ${e.message}"))
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

    private fun unloadLuaScript(filename: String, source: FabricClientCommandSource) {
        val luaManager = HypixelCry.LUA_MANAGER
        val scriptName = when {
            filename.endsWith(".lua") -> filename.removeSuffix(".lua")
            filename.endsWith(".luac") -> filename.removeSuffix(".luac")
            else -> filename


        }

        if (luaManager.unloadScript(scriptName)) {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§aScript '$scriptName' unloaded successfully"))


        } else {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§cScript '$scriptName' is not loaded or not found"))
        }
    }

    private fun listLuaFiles(source: FabricClientCommandSource) {
        val scriptsDir = File("config/hypixelcry/scripts")

        if (!scriptsDir.exists() || scriptsDir.listFiles()?.isEmpty() != false) {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§7No scripts available. Create files in: ${scriptsDir.path}"))
            return
        }

        val scriptFiles = scriptsDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".lua") || file.name.endsWith(".luac"))
        }?.sortedBy { it.name }

        if (scriptFiles.isNullOrEmpty()) {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§7No .lua or .luac files in scripts directory"))
            return
        }

        source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§6Available scripts:"))
        scriptFiles.forEach { file ->
            val fileType = if (file.name.endsWith(".luac")) "§9[compiled]§7" else "§a[source]§7"
            source.sendFeedback(Component.literal("  §7- §e${file.nameWithoutExtension} §7(${file.length()} bytes) $fileType"))
        }
    }

    private fun listLoadedScripts(source: FabricClientCommandSource, targetName: String?) {
        val luaManager = HypixelCry.LUA_MANAGER
        val loadedScripts = luaManager.getLoadedScripts()

        if (loadedScripts.isEmpty()) {
            source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§7No scripts currently loaded"))
            return
        }

        // РЕЖИМ 1: Показ дерева для конкретного скрипта
        if (targetName != null) {
            val scriptsToDisplay = loadedScripts.filter { it.scriptName.equals(targetName, ignoreCase = true) }

            if (scriptsToDisplay.isEmpty()) {
                source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§cScript '$targetName' is not loaded."))
                return
            }

            source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§6Dependency tree for §a$targetName§6:"))
            source.sendFeedback(Component.literal(""))

            scriptsToDisplay.forEach { script ->
                val depCount = countUniqueDependencies(script.localDependencyGraph)
                val depInfo = if (depCount > 0) " §8(§7$depCount modules§8)" else ""
                source.sendFeedback(Component.literal("  §6▶ §a§l${script.scriptName}$depInfo"))

                if (script.localDependencyGraph.isEmpty()) {
                    source.sendFeedback(Component.literal("  §8  §7No dependencies"))
                } else {
                    renderBeautifulTree(source, script.scriptName, "  §8  ", script.localDependencyGraph, mutableSetOf(), 0)
                }
            }
            return
        }

        // РЕЖИМ 2: Просто краткий список всех загруженных скриптов
        source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§6Loaded scripts §7(${loadedScripts.size}):"))
        loadedScripts.forEach { script ->
            val depCount = countUniqueDependencies(script.localDependencyGraph)
            // Стиль как в вашем listLuaFiles: §7- §aИмя §7(доп инфо)
            source.sendFeedback(Component.literal("  §7- §a${script.scriptName} §8(§7$depCount modules§8) §8[ID: ${script.hashCode().toString(16).take(4)}]"))
        }
        source.sendFeedback(Component.literal("§7Tip: Use §e/lua loaded <name> §7to see dependencies"))
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
     * Рекурсивная отрисовка (остается без изменений из предыдущего ответа)
     */
    private fun compileLuaScript(source: FabricClientCommandSource, name: String) {
        val scriptsDir = File("config/hypixelcry/scripts")

        if (!scriptsDir.exists()) {
            source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§cScripts directory does not exist."))
            return
        }

        val sourceFile = if (name.endsWith(".lua")) File(scriptsDir, name)
        else File(scriptsDir, "$name.lua")

        if (!sourceFile.exists()) {
            source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§cSource file §e${sourceFile.name} §cnot found."))
            return
        }

        val outputFile = File(scriptsDir, "${sourceFile.nameWithoutExtension}_compiled.luac")

        try {
            val inputStream = FileInputStream(sourceFile)
            val proto = LuaC.instance.compile(inputStream, sourceFile.name)
            inputStream.close()

            val outputStream = FileOutputStream(outputFile)
            DumpState.dump(proto, outputStream, false)
            outputStream.close()

            source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§aCompiled §e${sourceFile.name} §a→ §e${outputFile.name} §7(${outputFile.length()} bytes)"))
        } catch (e: Exception) {
            source.sendFeedback(Component.literal("${HypixelCry.PREFIX}§cCompilation error: ${e.message}"))
            e.printStackTrace()
        }
    }

    private fun renderBeautifulTree(
        source: FabricClientCommandSource,
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

            source.sendFeedback(Component.literal("$prefix$branchSymbol $nameColor$depName"))

            if (depName in visited) {
                val circularPrefix = prefix + (if (isLast) "     " else "┃    ")
                source.sendFeedback(Component.literal("$circularPrefix§c┗━ [Circular]"))
                return@forEachIndexed
            }

            val nextVisited = visited.toMutableSet()
            nextVisited.add(name)
            val nextPrefix = prefix + (if (isLast) "     " else "┃    ")

            renderBeautifulTree(source, depName, nextPrefix, graph, nextVisited, depth + 1)
        }
    }
}
