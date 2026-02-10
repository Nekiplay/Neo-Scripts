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
    private val SCRIPT_SUGGESTION_PROVIDER = SuggestionProvider<FabricClientCommandSource> { _, builder ->
        suggestScriptFiles(builder)
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
                    listLoadedScripts(context.source)
                    1
                }
            )

        dispatcher.register(luaCommand)
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

    private fun suggestLoadedScripts(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val luaManager = HypixelCry.LUA_MANAGER
        val loadedScripts = luaManager.getLoadedScripts()
        val input = builder.remainingLowerCase

        loadedScripts.forEach { file ->
            if (file.nameWithoutExtension.lowercase().startsWith(input)) {
                builder.suggest(file.nameWithoutExtension)
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
            val wasLoaded = luaManager.unloadScript(scriptFile)
            
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
        val scriptsDir = File("config/hypixelcry/scripts")

        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§cDirectory for scripts: ${scriptsDir.path}"))
            return
        }

        val scriptFile = resolveScriptFile(scriptsDir, filename)

        if (luaManager.unloadScript(scriptFile)) {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§aScript '${scriptFile.name}' unloaded successfully"))
        } else {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§cScript '${scriptFile.name}' is not loaded or not found"))
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

    private fun listLoadedScripts(source: FabricClientCommandSource) {
        val luaManager = HypixelCry.LUA_MANAGER
        val loadedScripts = luaManager.getLoadedScripts()

        if (loadedScripts.isEmpty()) {
            source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§7No scripts currently loaded"))
            return
        }

        source.sendFeedback(Component.literal(HypixelCry.PREFIX + "§6Loaded scripts:"))
        loadedScripts.forEach { file ->
            source.sendFeedback(Component.literal("§7- §a${file.nameWithoutExtension}"))
        }
    }
}
