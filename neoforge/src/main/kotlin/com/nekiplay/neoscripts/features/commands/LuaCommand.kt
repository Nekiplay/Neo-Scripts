package com.nekiplay.neoscripts.features.commands.impl

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.nekiplay.neoscripts.Main
import com.nekiplay.neoscripts.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent
import org.luaj.vm2.compiler.DumpState
import org.luaj.vm2.compiler.LuaC
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.CompletableFuture

@EventBusSubscriber(modid = Main.ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
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

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterClientCommandsEvent) {
        val dispatcher = event.dispatcher
        val luaCommand = Commands.literal("lua")
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
            .then(Commands.literal("hwid")
                .executes { context ->
                    context.source.sendSystemMessage(Component.literal("${Main.PREFIX}§7HWID: §a" + Utils.getHWID8()))
                    1
                }
            )
            .then(Commands.literal("list")
                .executes { context ->
                    listLuaFiles(context.source)
                    1
                }
            )
            .then(Commands.literal("loaded")
                .executes { context ->
                    listLoadedScripts(context.source, null)
                    1
                }
                .then(Commands.argument("scriptName", StringArgumentType.string())
                    .suggests(LOADED_SCRIPT_SUGGESTION_PROVIDER)
                    .executes { context ->
                        val name = StringArgumentType.getString(context, "scriptName")
                        listLoadedScripts(context.source, name)
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
            .then(Commands.literal("folder")
                .executes { context ->
                    openScriptsFolder(context.source)
                    1
                }
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

        dispatcher.register(luaCommand)
    }

    private fun openScriptsFolder(source: CommandSourceStack) {
        val gameDir = Minecraft.getInstance().gameDirectory
        val scriptsDir = File(gameDir, "config/neoscripts/scripts")

        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
        }

        val os = System.getProperty("os.name").lowercase()
        val absolutePath = scriptsDir.absolutePath

        try {
            val command = when {
                os.contains("win") -> arrayOf("explorer.exe", absolutePath)
                os.contains("mac") -> arrayOf("open", absolutePath)
                else -> arrayOf("xdg-open", absolutePath)
            }

            ProcessBuilder(*command).start()
            source.sendSystemMessage(Component.literal("${Main.PREFIX}§aOpening scripts folder..."))
        } catch (e: Exception) {
            if (os.contains("nix") || os.contains("nux")) {
                source.sendSystemMessage(Component.literal("${Main.PREFIX}§cCould not open folder. Path: §7$absolutePath"))
            } else {
                source.sendSystemMessage(Component.literal("${Main.PREFIX}§cError: ${e.message}"))
            }
            e.printStackTrace()
        }
    }

    private fun toggleScript(source: CommandSourceStack, name: String) {
        val luaManager = Main.LUA_MANAGER
        val loadedScripts = luaManager?.getLoadedScripts() ?: emptyList()

        val loadedInstance = loadedScripts.find { it.scriptName.equals(name, ignoreCase = true) }

        if (loadedInstance != null) {
            try {
                luaManager?.unloadScript(loadedInstance.scriptName)
                source.sendSystemMessage(Component.literal("${Main.PREFIX}§7Script §a${loadedInstance.scriptName} §7has been §cunloaded§7."))
            } catch (e: Exception) {
                source.sendSystemMessage(Component.literal("${Main.PREFIX}§cError unloading script: ${e.message}"))
            }
        } else {
            val scriptsDir = getScriptsDirectory()
            val scriptFile = File(scriptsDir, "$name.lua").let {
                if (it.exists()) it else File(scriptsDir, "$name.luac")
            }

            if (scriptFile.exists()) {
                try {
                    luaManager?.executeScript(scriptFile)
                    source.sendSystemMessage(Component.literal("${Main.PREFIX}§7Script §a$name §7is now §aloaded§7."))
                } catch (e: Exception) {
                    source.sendSystemMessage(Component.literal("${Main.PREFIX}§cFailed to load script §e$name§c: ${e.message}"))
                }
            } else {
                source.sendSystemMessage(Component.literal("${Main.PREFIX}§cFile §e$name.lua §cnot found in scripts directory."))
            }
        }
    }

    private fun suggestScriptFiles(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val scriptsDir = getScriptsDirectory()

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
        val scriptsDir = getScriptsDirectory()

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
        val luaManager = Main.LUA_MANAGER
        val loadedScripts = luaManager?.getLoadedScripts() ?: emptyList()
        val input = builder.remainingLowerCase

        loadedScripts.forEach { script ->
            if (script.scriptName.lowercase().startsWith(input)) {
                builder.suggest(script.scriptName)
            }
        }

        return builder.buildFuture()
    }

    private fun executeLuaFile(filename: String, source: CommandSourceStack) {
        val luaManager = Main.LUA_MANAGER
        val scriptsDir = getScriptsDirectory()

        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
            source.sendSystemMessage(Component.literal(Main.PREFIX + "§cDirectory for scripts: ${scriptsDir.path}"))
            return
        }

        val scriptFile = resolveScriptFile(scriptsDir, filename)

        if (!scriptFile.exists()) {
            source.sendSystemMessage(Component.literal(Main.PREFIX + "§cScript ${scriptFile.name} not found"))
            return
        }

        try {
            val wasLoaded = luaManager?.unloadScript(scriptFile.nameWithoutExtension)
            val result = luaManager?.executeScript(scriptFile)

            if (wasLoaded == true) {
                source.sendSystemMessage(Component.literal(Main.PREFIX + "§aScript '${scriptFile.nameWithoutExtension}' restarted successfully, result: '$result'"))
            } else {
                source.sendSystemMessage(Component.literal(Main.PREFIX + "§aScript '${scriptFile.nameWithoutExtension}' executed successfully, result: '$result'"))
            }
        } catch (e: Exception) {
            source.sendSystemMessage(Component.literal(Main.PREFIX + "§cScript execution error: ${e.message}"))
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
        val luaManager = Main.LUA_MANAGER
        val scriptName = when {
            filename.endsWith(".lua") -> filename.removeSuffix(".lua")
            filename.endsWith(".luac") -> filename.removeSuffix(".luac")
            else -> filename
        }

        if (luaManager?.unloadScript(scriptName) == true) {
            source.sendSystemMessage(Component.literal(Main.PREFIX + "§aScript '$scriptName' unloaded successfully"))
        } else {
            source.sendSystemMessage(Component.literal(Main.PREFIX + "§cScript '$scriptName' is not loaded or not found"))
        }
    }

    private fun listLuaFiles(source: CommandSourceStack) {
        val scriptsDir = getScriptsDirectory()

        if (!scriptsDir.exists() || scriptsDir.listFiles()?.isEmpty() != false) {
            source.sendSystemMessage(Component.literal(Main.PREFIX + "§7No scripts available. Create files in: ${scriptsDir.path}"))
            return
        }

        val scriptFiles = scriptsDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".lua") || file.name.endsWith(".luac"))
        }?.sortedBy { it.name }

        if (scriptFiles.isNullOrEmpty()) {
            source.sendSystemMessage(Component.literal(Main.PREFIX + "§7No .lua or .luac files in scripts directory"))
            return
        }

        source.sendSystemMessage(Component.literal(Main.PREFIX + "§6Available scripts:"))
        scriptFiles.forEach { file ->
            val fileType = if (file.name.endsWith(".luac")) "§9[compiled]§7" else "§a[source]§7"
            source.sendSystemMessage(Component.literal("  §7- §e${file.nameWithoutExtension} §7(${file.length()} bytes) $fileType"))
        }
    }

    private fun listLoadedScripts(source: CommandSourceStack, targetName: String?) {
        val luaManager = Main.LUA_MANAGER
        val loadedScripts = luaManager?.getLoadedScripts() ?: emptyList()

        if (loadedScripts.isEmpty()) {
            source.sendSystemMessage(Component.literal("${Main.PREFIX}§7No scripts currently loaded"))
            return
        }

        if (targetName != null) {
            val scriptsToDisplay = loadedScripts.filter { it.scriptName.equals(targetName, ignoreCase = true) }

            if (scriptsToDisplay.isEmpty()) {
                source.sendSystemMessage(Component.literal("${Main.PREFIX}§cScript '$targetName' is not loaded."))
                return
            }

            source.sendSystemMessage(Component.literal("${Main.PREFIX}§6Dependency tree for §a$targetName§6:"))
            source.sendSystemMessage(Component.literal(""))

            scriptsToDisplay.forEach { script ->
                val depCount = countUniqueDependencies(script.localDependencyGraph)
                val depInfo = if (depCount > 0) " §8(§7$depCount modules§8)" else ""
                source.sendSystemMessage(Component.literal("  §6▶ §a§l${script.scriptName}$depInfo"))

                if (script.localDependencyGraph.isEmpty()) {
                    source.sendSystemMessage(Component.literal("  §8  §7No dependencies"))
                } else {
                    renderBeautifulTree(source, script.scriptName, "  §8  ", script.localDependencyGraph, mutableSetOf(), 0)
                }
            }
            return
        }

        source.sendSystemMessage(Component.literal("${Main.PREFIX}§6Loaded scripts §7(${loadedScripts.size}):"))
        loadedScripts.forEach { script ->
            val depCount = countUniqueDependencies(script.localDependencyGraph)
            source.sendSystemMessage(Component.literal("  §7- §a${script.scriptName} §8(§7$depCount modules§8) §8[ID: ${script.hashCode().toString(16).take(4)}]"))
        }
        source.sendSystemMessage(Component.literal("§7Tip: Use §e/lua loaded <name> §7to see dependencies"))
    }

    private fun countUniqueDependencies(graph: Map<String, Set<String>>): Int {
        if (graph.isEmpty()) return 0

        val allModules = mutableSetOf<String>()
        allModules.addAll(graph.keys)
        graph.values.forEach { dependencies ->
            allModules.addAll(dependencies)
        }

        return allModules.size
    }

    private fun compileLuaScript(source: CommandSourceStack, name: String) {
        val scriptsDir = getScriptsDirectory()

        if (!scriptsDir.exists()) {
            source.sendSystemMessage(Component.literal("${Main.PREFIX}§cScripts directory does not exist."))
            return
        }

        val sourceFile = if (name.endsWith(".lua")) File(scriptsDir, name)
        else File(scriptsDir, "$name.lua")

        if (!sourceFile.exists()) {
            source.sendSystemMessage(Component.literal("${Main.PREFIX}§cSource file §e${sourceFile.name} §cnot found."))
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

            source.sendSystemMessage(Component.literal("${Main.PREFIX}§aCompiled §e${sourceFile.name} §a→ §e${outputFile.name} §7(${outputFile.length()} bytes)"))
        } catch (e: Exception) {
            source.sendSystemMessage(Component.literal("${Main.PREFIX}§cCompilation error: ${e.message}"))
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

            source.sendSystemMessage(Component.literal("$prefix$branchSymbol $nameColor$depName"))

            if (depName in visited) {
                val circularPrefix = prefix + (if (isLast) "     " else "┃    ")
                source.sendSystemMessage(Component.literal("$circularPrefix§c┗━ [Circular]"))
                return@forEachIndexed
            }

            val nextVisited = visited.toMutableSet()
            nextVisited.add(name)
            val nextPrefix = prefix + (if (isLast) "     " else "┃    ")

            renderBeautifulTree(source, depName, nextPrefix, graph, nextVisited, depth + 1)
        }
    }

    private fun getScriptsDirectory(): File {
        // Используем стандартный путь NeoForge к папке config
        val configDir = FMLPaths.CONFIGDIR.get()
        return configDir.resolve("neoscripts/scripts").toFile()
    }
}