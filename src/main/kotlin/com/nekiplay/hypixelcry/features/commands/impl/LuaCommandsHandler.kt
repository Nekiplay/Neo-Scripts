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
import org.luaj.vm2.LuaValue
import java.util.concurrent.CompletableFuture

object LuaCommandsHandler {
    private val COMMAND_SUGGESTION_PROVIDER = SuggestionProvider<FabricClientCommandSource> { _, builder ->
        suggestRegisteredCommands(builder)
    }

    // Store the dispatcher for dynamic registration
    private var currentDispatcher: CommandDispatcher<FabricClientCommandSource>? = null
    private var currentRegistryAccess: CommandBuildContext? = null
    
    // Track registered commands for proper unregistration
    private val registeredCommands = mutableMapOf<String, Any>()

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>, registryAccess: CommandBuildContext) {
        // Store references for dynamic registration
        currentDispatcher = dispatcher
        currentRegistryAccess = registryAccess
        
        // Register all pending commands from Lua scripts
        val commandsToRegister = getAllPendingCommands()
        
        commandsToRegister.forEach { entry ->
            val commandName = entry.key
            val callback = entry.value
            try {
                registerCommandInternal(dispatcher, commandName, callback)
                // Mark command as registered in the script
                HypixelCry.LUA_MANAGER.scripts.values.forEach { script ->
                    if (script.getPendingCommands().contains(commandName)) {
                        script.markCommandAsRegistered(commandName)
                    }
                }
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
        
        val commandsToRegister = getAllPendingCommands()
        
        commandsToRegister.forEach { entry ->
            val commandName = entry.key
            val callback = entry.value
            try {
                registerCommandInternal(dispatcher, commandName, callback)
                // Mark command as registered in the script
                HypixelCry.LUA_MANAGER.scripts.values.forEach { script ->
                    if (script.getPendingCommands().contains(commandName)) {
                        script.markCommandAsRegistered(commandName)
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Failed to register Lua command dynamically: /$commandName", e)
            }
        }
    }


    // Internal method to register a single command
    private fun registerCommandInternal(dispatcher: CommandDispatcher<FabricClientCommandSource>, commandName: String, callback: LuaValue) {
        val command = ClientCommandManager.literal(commandName)
            .executes { context ->
                try {
                    val argsTable = LuaValue.listOf(arrayOf<LuaValue>())
                    callback.call(LuaValue.valueOf(commandName), argsTable, LuaValue.valueOf(context.source.player?.name?.string ?: ""))
                    1
                } catch (e: Exception) {
                    HypixelCry.LOGGER.error("Error executing Lua command: /$commandName", e)
                    context.source.sendError(Component.literal("Error executing command: ${e.message}"))
                    0
                }
            }
            .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                .executes { context ->
                    try {
                        val argsString = StringArgumentType.getString(context, "args")
                        val args = argsString.split(" ").map { LuaValue.valueOf(it) }.toTypedArray()
                        val argsTable = LuaValue.listOf(args)
                        callback.call(LuaValue.valueOf(commandName), argsTable, LuaValue.valueOf(context.source.player?.name?.string ?: ""))
                        1
                    } catch (e: Exception) {
                        HypixelCry.LOGGER.error("Error executing Lua command: /$commandName", e)
                        context.source.sendError(Component.literal("Error executing command: ${e.message}"))
                        0
                    }
                }
            )
        
        dispatcher.register(command)
    }

    // Internal method to unregister a single command
    private fun unregisterCommandInternal(dispatcher: CommandDispatcher<FabricClientCommandSource>, commandName: String) {
        try {
            // Use reflection to access the command map and remove the command
            val rootCommandField = dispatcher.javaClass.getDeclaredField("root")
            rootCommandField.isAccessible = true
            val rootCommand = rootCommandField.get(dispatcher)
            
            val childrenField = rootCommand.javaClass.getDeclaredField("children")
            childrenField.isAccessible = true
            val children = childrenField.get(rootCommand) as MutableMap<String, Any>
            
            // Remove the command from the children map
            children.remove(commandName)
            
            // Also remove from our tracking map
            registeredCommands.remove(commandName)
            
            HypixelCry.LOGGER.info("${HypixelCry.LOG_PREFIX}Successfully unregistered Lua command: /$commandName")
        } catch (e: Exception) {
            HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Failed to unregister command /$commandName using reflection", e)
            
            // Fallback: mark as unregistered but don't actually remove from dispatcher
            registeredCommands.remove(commandName)
        }
    }

    // Get all pending commands from all loaded scripts
    private fun getAllPendingCommands(): Map<String, LuaValue> {
        val pendingCommands = mutableMapOf<String, LuaValue>()
        
        HypixelCry.LUA_MANAGER.scripts.values.forEach { script ->
            script.getPendingCommands().forEach { commandName ->
                script.getCommandCallback(commandName)?.let { callback ->
                    if (!pendingCommands.containsKey(commandName)) {
                        pendingCommands[commandName] = callback
                    }
                }
            }
        }
        
        return pendingCommands
    }

    private fun suggestRegisteredCommands(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val input = builder.remainingLowerCase

        // Get all registered commands from all scripts
        HypixelCry.LUA_MANAGER.scripts.values.forEach { script ->
            script.getRegisteredCommands().forEach { commandName ->
                if (commandName.lowercase().startsWith(input)) {
                    builder.suggest(commandName)
                }
            }
        }

        return builder.buildFuture()
    }

}
