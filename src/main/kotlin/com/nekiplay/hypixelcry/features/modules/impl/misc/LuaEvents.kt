package com.nekiplay.hypixelcry.features.modules.impl.misc

import com.nekiplay.hypixelcry.HypixelCry.LUA_MANAGER
import com.nekiplay.hypixelcry.features.esp.mining.dwarvenmines.DarkMonolithESP
import com.nekiplay.hypixelcry.features.modules.ClientModule
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AfterTranslucent

object LuaEvents: ClientModule() {
    override fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            LUA_MANAGER.onClientTick()
        }

        WorldRenderEvents.AFTER_TRANSLUCENT.register(AfterTranslucent { context: WorldRenderContext? ->
            LUA_MANAGER.onRenderTick(context)
        })
    }

    override fun get_name(): String {
        return "Lua_Events";
    }
}