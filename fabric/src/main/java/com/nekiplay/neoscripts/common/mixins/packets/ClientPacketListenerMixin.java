package com.nekiplay.neoscripts.common.mixins.packets;

import com.nekiplay.neoscripts.ClientMain;
import com.nekiplay.neoscripts.client.features.lua.LuaClientScript;
import com.nekiplay.neoscripts.common.features.lua.Script;
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
        if (ClientMain.LUA_MANAGER != null) {
            for (Script script : ClientMain.LUA_MANAGER.getScripts().values()) {
                try {
                    if (script instanceof LuaClientScript clientScript && !clientScript.onSendChatCommandEvent(command)) {
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
        if (ClientMain.LUA_MANAGER != null && ClientMain.LUA_MANAGER.getScripts() != null) {
            for (Script script : ClientMain.LUA_MANAGER.getScripts().values()) {
                if (script instanceof LuaClientScript clientScript) {
                    if (clientScript.getCommandCallbacks().containsKey(cmdName)
                            && clientScript.getCommandDispatchers().containsKey(cmdName)) {

                        LuaValue callback = clientScript.getCommandCallbacks().get(cmdName);
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
                                ClientMain.LOGGER.info(ClientMain.LOG_PREFIX + "Executing command: " + command);
                            } catch (Exception e) {
                                ClientMain.LOGGER.error(ClientMain.LOG_PREFIX + "Error executing Lua command: " + command, e);
                            }
                        }

                        ci.cancel(); // команда выполнена локально, серверу не отправляем
                        return;
                    }
                }
            }
        }
        // Если команда не найдена – она уйдёт на сервер (миксин не отменяет вызов)
    }
}