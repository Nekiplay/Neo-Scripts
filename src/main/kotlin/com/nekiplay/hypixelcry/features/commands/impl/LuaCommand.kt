package com.nekiplay.hypixelcry.features.commands.impl

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.nekiplay.hypixelcry.HypixelCry
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.text.Text
import java.io.File

object LuaCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>, registryAccess: CommandRegistryAccess) {
        val luaCommand = ClientCommandManager.literal("lua")
            .then(ClientCommandManager.literal("clear")
                .executes { context ->
                    HypixelCry.LUA_MANAGER.clearAllCallbacks()
                    context.source.sendFeedback(Text.literal("§aAll Lua callback's cleared"))
                    1
                }
            )
            .then(ClientCommandManager.literal("load")
                .then(ClientCommandManager.argument("filename", StringArgumentType.string())
                    .executes { context ->
                        val filename = StringArgumentType.getString(context, "filename")
                        executeLuaFile(filename, context.source)
                        1
                    }
                )
            )
            .then(ClientCommandManager.literal("unload")
                .then(ClientCommandManager.argument("filename", StringArgumentType.string())
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

    private fun executeLuaFile(filename: String, source: FabricClientCommandSource) {
        val luaManager = HypixelCry.LUA_MANAGER
        val scriptsDir = File("config/hypixelcry/scripts")

        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
            source.sendFeedback(Text.literal("§cDirectory for scripts: ${scriptsDir.path}"))
            return
        }

        val scriptFile = File(scriptsDir, if (filename.endsWith(".lua")) filename else "$filename.lua")

        if (!scriptFile.exists()) {
            source.sendFeedback(Text.literal("§cScript ${scriptFile.name} not found"))
            return
        }

        try {
            val scriptContent = scriptFile.readText()
            val result = luaManager.executeScript(scriptContent, scriptFile.nameWithoutExtension, scriptsDir.path)
            source.sendFeedback(Text.literal("§aScript '${scriptFile.name}' executed successfully, result: '${result}'"))
        } catch (e: Exception) {
            source.sendFeedback(Text.literal("§cScript execution error: ${e.message}"))
            e.printStackTrace()
        }
    }

    private fun unloadLuaScript(filename: String, source: FabricClientCommandSource) {
        val luaManager = HypixelCry.LUA_MANAGER
        val scriptName = if (filename.endsWith(".lua")) filename.removeSuffix(".lua") else filename

        if (luaManager.unloadScript(scriptName)) {
            source.sendFeedback(Text.literal("§aScript '$scriptName' unloaded successfully"))
        } else {
            source.sendFeedback(Text.literal("§cScript '$scriptName' is not loaded or not found"))
        }
    }

    private fun listLuaFiles(source: FabricClientCommandSource) {
        val scriptsDir = File("config/hypixelcry/scripts")

        if (!scriptsDir.exists() || scriptsDir.listFiles()?.isEmpty() != false) {
            source.sendFeedback(Text.literal("§7No scripts available. Create files in: ${scriptsDir.path}"))
            return
        }

        val luaFiles = scriptsDir.listFiles { file ->
            file.isFile && file.name.endsWith(".lua")
        }?.sortedBy { it.name }

        if (luaFiles.isNullOrEmpty()) {
            source.sendFeedback(Text.literal("§7No .lua files in scripts directory"))
            return
        }

        source.sendFeedback(Text.literal("§6Available scripts:"))
        luaFiles.forEach { file ->
            source.sendFeedback(Text.literal("§7- §e${file.nameWithoutExtension} §7(${file.length()} bytes)"))
        }
    }

    private fun listLoadedScripts(source: FabricClientCommandSource) {
        val luaManager = HypixelCry.LUA_MANAGER
        val loadedScripts = luaManager.getLoadedScripts()

        if (loadedScripts.isEmpty()) {
            source.sendFeedback(Text.literal("§7No scripts currently loaded"))
            return
        }

        source.sendFeedback(Text.literal("§6Loaded scripts:"))
        loadedScripts.forEach { scriptName ->
            source.sendFeedback(Text.literal("§7- §a$scriptName"))
        }
    }
}