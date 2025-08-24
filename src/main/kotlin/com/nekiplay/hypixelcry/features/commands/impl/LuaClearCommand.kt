package com.nekiplay.hypixelcry.features.commands.impl

import com.mojang.brigadier.CommandDispatcher
import com.nekiplay.hypixelcry.HypixelCry
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.text.Text

object LuaClearCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>, registryAccess: CommandRegistryAccess) {
        // Команда для очистки всех callback'ов
        dispatcher.register(
            ClientCommandManager.literal("luaClear")
                .executes { context ->
                    HypixelCry.LUA_MANAGER.clearAllCallbacks()
                    context.source.sendFeedback(Text.literal("§aВсе Lua callback'ы очищены"))
                    1
                }
        )
    }
}