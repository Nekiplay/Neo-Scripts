package com.nekiplay.hypixelcry.features.commands.impl

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.nekiplay.hypixelcry.HypixelCry
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.text.Text
import java.io.File

object LuaFileCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>, registryAccess: CommandRegistryAccess) {
        // Используем CommandManager.literal() без типа, так как он будет inferred
        val luaFileCommand = ClientCommandManager.literal("luaFile")
            .then(ClientCommandManager.argument("filename", StringArgumentType.string())
                .executes { context ->
                    val filename = StringArgumentType.getString(context, "filename")
                    executeLuaFile(filename, context.source)
                    1
                }
            )

        val luaListCommand = ClientCommandManager.literal("luaList")
            .executes { context ->
                listLuaFiles(context.source)
                1
            }

        // Регистрируем команды
        dispatcher.register(luaFileCommand)
        dispatcher.register(luaListCommand)
    }

    private fun executeLuaFile(filename: String, source: FabricClientCommandSource) {
        val luaManager = HypixelCry.LUA_MANAGER
        val scriptsDir = File("config/hypixelcry/scripts")

        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
            source.sendFeedback(Text.literal("§cДиректория скриптов создана: ${scriptsDir.path}"))
            return
        }

        val scriptFile = File(scriptsDir, if (filename.endsWith(".lua")) filename else "$filename.lua")

        if (!scriptFile.exists()) {
            source.sendFeedback(Text.literal("§cФайл скрипта не найден: ${scriptFile.name}"))
            return
        }

        try {
            val scriptContent = scriptFile.readText()
            val result = luaManager.executeScript(scriptContent)

            source.sendFeedback(Text.literal("§aСкрипт '${scriptFile.name}' выполнен успешно!"))
            if (result != null) {
                source.sendFeedback(Text.literal("§7Результат: $result"))
            }
        } catch (e: Exception) {
            source.sendFeedback(Text.literal("§cОшибка выполнения скрипта: ${e.message}"))
            e.printStackTrace()
        }
    }

    private fun listLuaFiles(source: FabricClientCommandSource) {
        val scriptsDir = File("config/hypixelcry/scripts")

        if (!scriptsDir.exists() || scriptsDir.listFiles()?.isEmpty() != false) {
            source.sendFeedback(Text.literal("§7Нет доступных скриптов. Создайте файлы в: ${scriptsDir.path}"))
            return
        }

        val luaFiles = scriptsDir.listFiles { file ->
            file.isFile && file.name.endsWith(".lua")
        }?.sortedBy { it.name }

        if (luaFiles.isNullOrEmpty()) {
            source.sendFeedback(Text.literal("§7Нет .lua файлов в директории скриптов"))
            return
        }

        source.sendFeedback(Text.literal("§6Доступные скрипты:"))
        luaFiles.forEach { file ->
            source.sendFeedback(Text.literal("§7- §e${file.name} §7(${file.length()} bytes)"))
        }
    }
}