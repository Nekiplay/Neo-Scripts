package com.nekiplay.hypixelcry.features.commands.impl

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.nekiplay.hypixelcry.HypixelCry
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.CommandBuildContext
import net.minecraft.network.chat.Component
import java.io.File
import java.util.concurrent.CompletableFuture

object LuaCommand {
    // Провайдер для предложений скриптов
    private val SCRIPT_SUGGESTION_PROVIDER = SuggestionProvider<FabricClientCommandSource> { context, builder ->
        suggestScriptFiles(builder)
    }
    
    // Провайдер для предложений загруженных скриптов
    private val LOADED_SCRIPT_SUGGESTION_PROVIDER = SuggestionProvider<FabricClientCommandSource> { context, builder ->
        suggestLoadedScripts(builder)
    }

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>, registryAccess: CommandBuildContext) {
        val luaCommand = ClientCommandManager.literal("lua")
            .then(ClientCommandManager.literal("load")
                .then(ClientCommandManager.argument("filename", StringArgumentType.string())
                    .suggests(SCRIPT_SUGGESTION_PROVIDER) // Добавляем авто-дополнение
                    .executes { context ->
                        val filename = StringArgumentType.getString(context, "filename")
                        executeLuaFile(filename, context.source)
                        1
                    }
                )
            )
            .then(ClientCommandManager.literal("unload")
                .then(ClientCommandManager.argument("filename", StringArgumentType.string())
                    .suggests(LOADED_SCRIPT_SUGGESTION_PROVIDER) // Добавляем авто-дополнение для загруженных скриптов
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
                    listLoadedScripts(context.source)
                    1
                }
            )

        dispatcher.register(luaCommand)
    }

    // Функция для предложения файлов скриптов
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
            val fileNameWithoutExtension = file.nameWithoutExtension
            if (fileNameWithoutExtension.lowercase().startsWith(input)) {
                builder.suggest(fileNameWithoutExtension)
            }
        }
        
        return builder.buildFuture()
    }
    
    // Функция для предложения загруженных скриптов
    private fun suggestLoadedScripts(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val luaManager = HypixelCry.LUA_MANAGER
        val loadedScripts = luaManager.getLoadedScripts()
        val input = builder.remainingLowerCase
        
        loadedScripts.forEach { scriptName ->
            if (scriptName.lowercase().startsWith(input)) {
                builder.suggest(scriptName)
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

        // Check for both .lua and .luac extensions
        val scriptFile = when {
            filename.endsWith(".lua") || filename.endsWith(".luac") -> File(scriptsDir, filename)
            else -> {
                // Try both extensions, preferring .lua first
                val luaFile = File(scriptsDir, "$filename.lua")
                if (luaFile.exists()) luaFile else File(scriptsDir, "$filename.luac")
            }
        }

        if (!scriptFile.exists()) {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§cScript ${scriptFile.name} not found"))
            return
        }

        try {
            val scriptFile = when {
                filename.endsWith(".lua") || filename.endsWith(".luac") -> File(scriptsDir, filename)
                else -> {
                    // Try both extensions, preferring .lua first
                    val luaFile = File(scriptsDir, "$filename.lua")
                    if (luaFile.exists()) luaFile else File(scriptsDir, "$filename.luac")
                }
            }
            val loaded = luaManager.unloadScript(scriptFile.nameWithoutExtension)
            val result = luaManager.executeScript(scriptFile)
            if (!loaded) {
                source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§aScript '${scriptFile.nameWithoutExtension}' executed successfully, result: '${result}'"))
            }
            else {
                source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§aScript '${scriptFile.nameWithoutExtension}' restarted successfully, result: '${result}'"))
            }
        } catch (e: Exception) {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§cScript execution error: ${e.message}"))
            e.printStackTrace()
        }
    }

    private fun unloadLuaScript(filename: String, source: FabricClientCommandSource) {
        val luaManager = HypixelCry.LUA_MANAGER
        // Remove either .lua or .luac extension for script name
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
            source.sendFeedback(Component.literal("§7- §e${file.nameWithoutExtension} §7(${file.length()} bytes) $fileType"))
        }
    }

    private fun listLoadedScripts(source: FabricClientCommandSource) {
        val luaManager = HypixelCry.LUA_MANAGER
        val loadedScripts = luaManager.getLoadedScripts()

        if (loadedScripts.isEmpty()) {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§7No scripts currently loaded"))
            return
        }

        source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§6Loaded scripts with dependency tree:"))
        
        // Собираем все зависимости для каждого скрипта
        loadedScripts.forEach { scriptName ->
            val dependencyTree = luaManager.getScriptDependencyTree(scriptName)
            if (dependencyTree.isNotEmpty()) {
                printScriptTree(source, scriptName, dependencyTree, 0)
            } else {
                // Если нет зависимостей, просто показываем имя скрипта
                source.sendFeedback(Component.literal("§a$scriptName"))
            }
        }
    }
    
    private fun printScriptTree(source: FabricClientCommandSource, scriptName: String, dependencyTree: Map<String, Set<String>>, depth: Int) {
        val indent = "   ".repeat(depth)
        val prefix = if (depth == 0) "├ " else "└ "
        
        source.sendFeedback(Component.literal("$indent$prefix§a$scriptName"))
        
        if (dependencyTree.isNotEmpty()) {
            var index = 0
            dependencyTree.forEach { moduleName, nestedDeps ->
                val isLast = index == dependencyTree.size - 1
                val nextIndent = if (depth == 0) "│ " else "  "
                val nextPrefix = if (isLast) "└ " else "├ "
                
                source.sendFeedback(Component.literal("$indent$nextIndent$nextPrefix§7$moduleName"))
                
                // Check if this module has nested dependencies
                if (nestedDeps.isNotEmpty()) {
                    printNestedDependencies(source, nestedDeps, depth + 2, isLast)
                }
                index++
            }
        }
    }
    
    private fun printNestedDependencies(source: FabricClientCommandSource, dependencies: Set<String>, depth: Int, isLastParent: Boolean) {
        var index = 0
        dependencies.forEach { dep ->
            val isLast = index == dependencies.size - 1
            val indent = if (depth > 0) "   ".repeat(depth - 1) + (if (isLastParent) "  " else "│ ") else ""
            val prefix = if (isLast) "└ " else "├ "
            
            source.sendFeedback(Component.literal("$indent$prefix§8$dep"))
            index++
        }
    }
}
