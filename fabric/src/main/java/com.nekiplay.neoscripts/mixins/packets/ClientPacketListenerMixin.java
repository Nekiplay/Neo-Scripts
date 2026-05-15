package com.nekiplay.neoscripts.mixins.packets;

import com.nekiplay.neoscripts.Main;
import com.nekiplay.neoscripts.features.lua.LuaScript;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.luaj.vm2.LuaValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String command, CallbackInfo ci) {
        String cmdName = command.split(" ")[0];
        boolean allow = true;

        if (cmdName.equalsIgnoreCase("lua")) {
            ci.cancel();
            return;
        }

        // 1. Lua-скрипты могут запретить команду
        if (Main.LUA_MANAGER != null && Main.LUA_MANAGER.getScripts() != null) {
            for (LuaScript script : Main.LUA_MANAGER.getScripts().values()) {
                try {
                    if (!script.onSendChatCommandEvent(command)) {
                        allow = false;
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (!allow) {
            ci.cancel();
            return;
        }

        // 2. Ищем скрипт с зарегистрированной командой и выполняем Lua-колбэк
        if (Main.LUA_MANAGER != null && Main.LUA_MANAGER.getScripts() != null) {
            for (LuaScript script : Main.LUA_MANAGER.getScripts().values()) {
                if (script.getCommandCallbacks().containsKey(cmdName)
                        && script.getCommandDispatchers().containsKey(cmdName)) {

                    LuaValue callback = script.getCommandCallbacks().get(cmdName);
                    if (callback != null && callback.isfunction()) {
                        // Разбираем аргументы
                        String[] args = command.contains(" ")
                                ? command.substring(command.indexOf(" ") + 1).split(" ")
                                : new String[0];

                        // Формируем Lua-таблицу аргументов
                        LuaValue[] luaArgs = new LuaValue[args.length];
                        for (int i = 0; i < args.length; i++) {
                            luaArgs[i] = LuaValue.valueOf(args[i]);
                        }
                        LuaValue argsTable = LuaValue.listOf(luaArgs);

                        // Получаем имя игрока (если доступно)
                        String playerName = "";
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            playerName = mc.player.getName().getString();
                        }

                        try {
                            callback.call(
                                    LuaValue.valueOf(cmdName),
                                    argsTable,
                                    LuaValue.valueOf(playerName)
                            );
                            Main.LOGGER.info(Main.LOG_PREFIX + "Executing command: " + command);
                        } catch (Exception e) {
                            Main.LOGGER.error(Main.LOG_PREFIX + "Error executing Lua command: " + command, e);
                        }
                    }

                    ci.cancel(); // команда выполнена локально, серверу не отправляем
                    return;
                }
            }
        }
        // Если команда не найдена – она уйдёт на сервер (миксин не отменяет вызов)
    }
}