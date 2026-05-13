package com.nekiplay.neoscripts.mixins.minecraft.packets;

import com.mojang.brigadier.CommandDispatcher;
import com.nekiplay.neoscripts.Main;
import com.nekiplay.neoscripts.features.lua.LuaScript;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String command, CallbackInfo ci) {
        // Имя команды — первое слово
        String cmdName = command.split(" ")[0];
        boolean allow = true;

        // 1. Lua-скрипты могут запретить команду
        for (LuaScript script : Main.LUA_MANAGER.getScripts().values()) {
            try {
                if (!script.onSendChatCommandEvent(command)) {
                    allow = false;
                    break; // одного запрета достаточно
                }
            } catch (Exception ignored) {
            }
        }

        if (!allow) {
            ci.cancel(); // команда не уходит на сервер
            return;
        }

        // 2. Ищем скрипт, зарегистрировавший эту команду
        for (LuaScript script : Main.LUA_MANAGER.getScripts().values()) {
            if (script.getCommandCallbacks().containsKey(cmdName)
                    && script.getCommandDispatchers().containsKey(cmdName)) {

                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) break;

                try {
                    CommandDispatcher<ClientSuggestionProvider> dispatcher =
                            (CommandDispatcher<ClientSuggestionProvider>) script.getCommandDispatchers().get(cmdName);
                    ClientSuggestionProvider source = mc.player.connection.getSuggestionsProvider();
                    int result = dispatcher.execute(command, source);
                    if (result >= 1) {
                        Main.LOGGER.info(Main.LOG_PREFIX + "Executing command: " + command);
                    }
                } catch (Exception e) {
                    Main.LOGGER.error(Main.LOG_PREFIX + "Error executing command " + command, e);
                }

                ci.cancel(); // команда выполнена локально, на сервер не отправляем
                return;
            }
        }
        // Если команда не найдена, она уходит на сервер (миксин не отменяет вызов)
    }
}