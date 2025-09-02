package com.nekiplay.hypixelcry.features.lua

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.modules.ModulesObject
import com.nekiplay.hypixelcry.features.lua.objects.player.PlayerObject
import com.nekiplay.hypixelcry.features.lua.objects.modules.PathFinderRendererObject
import com.nekiplay.hypixelcry.features.lua.objects.render.WorldRendererObject
import com.nekiplay.hypixelcry.features.lua.objects.world.WorldObject
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import net.minecraft.client.MinecraftClient
import org.luaj.vm2.lib.OneArgFunction
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class LuaManager() {
    private val globals: Globals = JsePlatform.standardGlobals()
    private val persistentGlobals = ConcurrentHashMap<String, LuaValue>()

    private val clientTickCallbacks = CopyOnWriteArrayList<LuaValue>()
    private val renderWorldCallbacks = CopyOnWriteArrayList<LuaValue>()
    private val keyEvetCallbacks = CopyOnWriteArrayList<LuaValue>()

    init {
        registerCustomFunctions()
        registerGlobalObjects()
    }

    private fun registerCustomFunctions() {
        globals.set("print", object : OneArgFunction() {
            override fun call(message: LuaValue): LuaValue {
                HypixelCry.LOGGER.info(message.tojstring());
                return NIL
            }
        })

        globals.set("registerClientTick", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addClientTickCallback(callback))
            }
        })
        globals.set("registerWorldRenderer", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(addWorldRendererCallback(callback))
            }
        })

        globals.set("unregisterClientTick", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeClientTickCallback(callback))
            }
        })
        globals.set("unregisterWorldRenderer", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                return LuaValue.valueOf(removeWorldRendererCallback(callback))
            }
        })
    }

    // Methods for adding callbacks
    fun addClientTickCallback(callback: LuaValue): Boolean {
        if (callback.isfunction()) {
            clientTickCallbacks.add(callback)
            return true
        }
        return false
    }

    fun addWorldRendererCallback(callback: LuaValue): Boolean {
        if (callback.isfunction()) {
            renderWorldCallbacks.add(callback)
            return true
        }
        return false
    }

    fun addKeyEventCallback(callback: LuaValue): Boolean {
        if (callback.isfunction()) {
            keyEvetCallbacks.add(callback)
            return true
        }
        return false
    }

    // Methods for removing callbacks
    fun removeClientTickCallback(callback: LuaValue): Boolean {
        return clientTickCallbacks.remove(callback)
    }
    fun removeWorldRendererCallback(callback: LuaValue): Boolean {
        return renderWorldCallbacks.remove(callback)
    }

    fun removeKeyEventCallback(callback: LuaValue): Boolean {
        return keyEvetCallbacks.remove(callback)
    }

    // Methods to clear all callbacks
    fun clearAllCallbacks() {
        clientTickCallbacks.clear()
        renderWorldCallbacks.clear()
        keyEvetCallbacks.clear()
    }

    private fun registerGlobalObjects() {
        // Register global objects
        globals.set("player", PlayerObject())
        globals.set("world", WorldObject())
        globals.set("modules", ModulesObject())
    }

    // Callback methods
    // for multiple handlers
    fun onClientTick() {
        clientTickCallbacks.forEach { callback ->
            try {
                callback.call()
            } catch (e: Exception) {
                println("Error in client tick callback: ${e.message}")
            }
        }
    }

    fun onRenderTick(context: WorldRenderContext?) {
        renderWorldCallbacks.forEach { callback ->
            val renderContext = WorldRendererObject(context)
            try {
                callback.call(renderContext)
            } catch (e: Exception) {
                println("Error in world render callback: ${e.message}")
            }
        }
    }

    fun onKeyEvent(key: Int, type: KeyAction) {
        keyEvetCallbacks.forEach { callback ->
            try {
                callback.call(LuaValue.valueOf(key), LuaValue.valueOf(type.name))
            } catch (e: Exception) {
                println("Error in key callback: ${e.message}")
            }
        }
    }

    fun executeScript(script: String): Any {
        val chunk = globals.load(script)
        val result = chunk.call()
        restoreGlobals()
        return result
    }

    private fun restoreGlobals() {
        persistentGlobals.forEach { (name, value) ->
            globals.set(name, value)
        }
    }
}