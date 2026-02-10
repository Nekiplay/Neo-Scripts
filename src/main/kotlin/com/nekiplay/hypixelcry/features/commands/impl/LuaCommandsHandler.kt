package com.nekiplay.hypixelcry.features.commands.impl

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.nekiplay.hypixelcry.HypixelCry
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.CommandBuildContext
import net.minecraft.network.chat.Component
import java.util.concurrent.CompletableFuture

object LuaCommandsHandler {
    private val COMMAND_SUGGESTION_PROVIDER = SuggestionProvider<FabricClientCommandSource> { _, builder ->
        suggestRegisteredCommands(builder)
    }

    // Store the dispatcher for dynamic registration
    private var currentDispatcher: CommandDispatcher<FabricClientCommandSource>? = null
    private var currentRegistryAccess: CommandBuildContext? = null

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>, registryAccess: CommandBuildContext) {
        // Store references for dynamic registration
        currentDispatcher = dispatcher
        currentRegistryAccess = registryAccess
        
        // Register all pending commands from Lua scripts
        val commandsToRegister = HypixelCry.LUA_MANAGER.registerPendingCommands()
        
        commandsToRegister.forEach { (commandName, callback) ->
            try {
                registerCommandInternal(commandName, callback, dispatcher, registryAccess)
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Failed to register Lua command: /$commandName", e)
            }
        }
    }

    // Method to register commands dynamically when scripts are loaded at runtime
    fun registerPendingCommandsDynamically() {
        val dispatcher = currentDispatcher
        val registryAccess = currentRegistryAccess
        
        if (dispatcher == null || registryAccess == null) {
            HypixelCry.LOGGER.warn("${HypixelCry.LOG_PREFIX}Cannot register commands dynamically - dispatcher not available")
            return
        }
        
        val commandsToRegister = HypixelCry.LUA_MANAGER.registerPendingCommands()
        
        commandsToRegister.forEach { (commandName, callback) ->
            try {
                registerCommandInternal(commandName, callback, dispatcher, registryAccess)
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Failed to register Lua command dynamically: /$commandName", e)
            }
        }
    }

    // Internal method to register a single command
    private fun registerCommandInternal(
        commandName: String, 
        callback: org.luaj.vm2.LuaValue, 
        dispatcher: CommandDispatcher<FabricClientCommandSource>, 
        registryAccess: CommandBuildContext
    ) {
        val command = ClientCommandManager.literal(commandName)
            .executes { context ->
                executeLuaCommand(commandName, emptyArray(), context.source, callback)
                1
            }
            .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                .executes { context ->
                    val args = StringArgumentType.getString(context, "args").split(" ").toTypedArray()
                    executeLuaCommand(commandName, args, context.source, callback)
                    1
                }
            )
        
        dispatcher.register(command)
        HypixelCry.LOGGER.info("${HypixelCry.LOG_PREFIX}Registered Lua command: /$commandName")
    }

    private fun suggestRegisteredCommands(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val registeredCommands = HypixelCry.LUA_MANAGER.getRegisteredCommands()
        val input = builder.remainingLowerCase

        registeredCommands.keys.forEach { commandName ->
            if (commandName.lowercase().startsWith(input)) {
                builder.suggest(commandName)
            }
        }

        return builder.buildFuture()
    }

    private fun executeLuaCommand(commandName: String, args: Array<String>, source: FabricClientCommandSource, callback: org.luaj.vm2.LuaValue) {
        try {
            // Преобразуем аргументы в Lua таблицу
            val argsTable = org.luaj.vm2.LuaValue.listOf(args.map { org.luaj.vm2.LuaValue.valueOf(it) }.toTypedArray())
            callback.call(org.luaj.vm2.LuaValue.valueOf(commandName), argsTable, org.luaj.vm2.LuaValue.valueOf(source.player?.name?.string ?: ""))
        } catch (e: Exception) {
            HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error executing Lua command: /$commandName", e)
            source.sendError(Component.literal("Error executing command: ${e.message}"))
        }
    }
}
