package com.nekiplay.hypixelcry.features.lua

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.text.LuaComponentBuilder
import com.nekiplay.hypixelcry.features.lua.objects.misc.Creator
import com.nekiplay.hypixelcry.features.lua.objects.misc.DJLLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.EncodingLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ImGuiLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.JsonLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.TCPLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.ThreadLib
import com.nekiplay.hypixelcry.features.lua.objects.misc.http.HttpClientLib
import com.nekiplay.hypixelcry.features.lua.objects.modules.ModulesLib
import com.nekiplay.hypixelcry.features.lua.objects.player.PlayerObject
import com.nekiplay.hypixelcry.features.lua.objects.render.TwoRenderObject
import com.nekiplay.hypixelcry.features.lua.objects.render.WorldRendererObject
import com.nekiplay.hypixelcry.features.lua.objects.world.WorldObject
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.utils.Location
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction
import kotlinx.atomicfu.locks.withLock
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import party.iroiro.luajava.ExternalLoader
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJit
import party.iroiro.luajava.value.LuaValue
import java.io.File
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

class LuaScript(val scriptName: String, private val luaManager: LuaManager) {
    // Локальный граф зависимостей для этого конкретного экземпляра скрипта
    // Ключ: имя файла, Значение: список имен, которые этот файл запросил через require
    val localDependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()

    // Events
    private val clientTickCallbacks = ArrayList<LuaValue>()
    private val clientPreTickCallbacks = ArrayList<LuaValue>()
    private val blockUpdateCallbacks = ArrayList<LuaValue>()
    private val renderWorldCallbacks = ArrayList<LuaValue>()
    private val render2DCallbacks = ArrayList<LuaValue>()
    private val keyEventCallbacks = ArrayList<LuaValue>()
    private val messageEventCallbacks = ArrayList<LuaValue>()
    private val onSendMessageEventCallbacks = ArrayList<LuaValue>()
    private val onSendCommandEventCallbacks = ArrayList<LuaValue>()
    private val useBlockCallbacks = ArrayList<LuaValue>()
    private val attackBlockCallbacks = ArrayList<LuaValue>()
    private val locationChangeCallbacks = ArrayList<LuaValue>()
    private val imguiRenderCallbacks = ArrayList<LuaValue>()
    private val inventoryItemChangeCallbacks = ArrayList<LuaValue>()
    private val particleCallbacks = ArrayList<LuaValue>()

    // Packet events
    private val serverSideRotationCallbacks = ArrayList<LuaValue>()
    private val serverSideTeleportCallbacks = ArrayList<LuaValue>()

    // Command events
    val commandCallbacks = ConcurrentHashMap<String, LuaValue>()
    val commandSuggestionsCallbacks = ConcurrentHashMap<String, LuaValue>()
    val commandDispatchers = ConcurrentHashMap<String, CommandDispatcher<FabricClientCommandSource>>()

    // Script events
    private val scriptUnloadCallbacks = ArrayList<LuaValue>()

    // Synchronize only when needed
    private val callbacksLock = ReentrantLock()

    // Script-specific globals
    var L = LuaJit()

    // Script-specific libraries
    private val djlLibrary = DJLLib(L)
    private val encoding = EncodingLib(L)
    private val creator = Creator(L)
    private val json = JsonLib(L)
    private val tcp = TCPLib(L)
    private val threads = ThreadLib(L)
    private val http = HttpClientLib(L)
    val imgui = ImGuiLib(L)
    private val modules = ModulesLib(L)

    // Dependency tracking for nested requires
    private val dependencies = ConcurrentHashMap<String, MutableList<String>>()

    init {
        L.openLibraries()
        L.setExternalLoader(object : ExternalLoader {
            override fun load(moduleName: String, l: Lua): ByteBuffer? {
                // 1. Normalize the path (handle both '.' and '/' in require)
                val normalizedName = moduleName.replace('.', File.separatorChar)
                                               .replace('/', File.separatorChar)
                val fileName = "$normalizedName.lua"
    
                // 2. Define the search folders
                // We search in the root scripts folder AND the libs folder
                val scriptsRoot = File(mc.gameDirectory, "config/hypixelcry/scripts")
                val libsFolder = File(scriptsRoot, "libs")
                
                val candidatePaths = listOf(
                    File(scriptsRoot, fileName),
                    File(libsFolder, fileName)
                )
    
                for (scriptFile in candidatePaths) {
                    // DEBUG: Uncomment this to see in console where it is looking
                    // println("LuaLoader: Checking path: ${scriptFile.absolutePath}")
    
                    if (scriptFile.exists()) {
                        try {
                            val bytes = scriptFile.readBytes()
                            val buffer = ByteBuffer.allocateDirect(bytes.size)
                            buffer.put(bytes)
                            buffer.flip()
                            return buffer
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return null
                        }
                    }
                }
    
                // If we reach here, the file wasn't found in any candidate path
                return null
            }
        })

        // Загрузка библиотек
        djlLibrary.register()
        encoding.register()
        creator.register()
        json.register()
        tcp.register()
        threads.register()
        http.register()
        imgui.register()
        modules.register()
        LuaComponentBuilder.register(L)

        registerCustomFunctions()

        // Register global objects
        WorldObject(L).register()
        PlayerObject(L).register()
    }

    private fun registerCustomFunctions() {
        // Register event registration functions
        registerEventRegistrationFunctions()

        // Register event unregistration functions
        registerEventUnregistrationFunctions()

        // Register command functions
        registerCommandFunctions()

        // Register other custom functions
        registerOtherCustomFunctions()
    }

    private fun registerEventRegistrationFunctions() {
        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    scriptUnloadCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerUnloadCallback")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    particleCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerSpawnParticle")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    inventoryItemChangeCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerInventoryItemChange")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    useBlockCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerUseBlock")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    attackBlockCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerAttackBlock")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    clientPreTickCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerClientTick")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    clientTickCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerClientTickPost")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    clientPreTickCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerClientTickPre")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    blockUpdateCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerBlockUpdate")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    renderWorldCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerWorldRenderer")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    render2DCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("register2DRenderer")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    keyEventCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerKeyEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    messageEventCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerMessageEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    onSendMessageEventCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerSendMessageEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    onSendCommandEventCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerSendCommandEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    locationChangeCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerLocationChangeEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    imguiRenderCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerImGuiRenderEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    serverSideRotationCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerServerSideRotationEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    serverSideTeleportCallbacks.add(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("registerServerSideTeleportEvent")
    }

    private fun registerEventUnregistrationFunctions() {
        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    particleCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterSpawnParticle")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    inventoryItemChangeCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterInventoryItemChange")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    useBlockCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterUseBlock")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    attackBlockCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterAttackBlock")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    clientPreTickCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterClientTick")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    clientTickCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterClientTickPost")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    clientPreTickCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterClientTickPre")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    blockUpdateCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterBlockUpdate")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    renderWorldCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterWorldRenderer")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    render2DCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregister2DRenderer")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    keyEventCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterKeyEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    messageEventCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterMessageEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    onSendMessageEventCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterSendMessageEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    onSendCommandEventCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterSendCommandEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    locationChangeCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterLocationChangeEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    imguiRenderCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterImGuiRenderEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    serverSideRotationCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterServerSideRotationEvent")

        L.push(JFunction { l ->
            // Проверяем, является ли первый аргумент функцией
            if (l.type(1) == Lua.LuaType.FUNCTION) {
                // Копируем функцию на вершину стека и забираем как LuaValue
                l.pushValue(1)
                val callback = l.get() // Теперь это LuaValue, он удерживает ссылку на функцию

                callbacksLock.withLock {
                    serverSideTeleportCallbacks.remove(callback)
                }
                l.push(true)
            } else {
                l.push(false)
            }
            1 // Количество возвращаемых значений
        })
        L.setGlobal("unregisterServerSideTeleportEvent")
    }

    private fun registerOtherCustomFunctions() {
        L.pushJavaObject(HypixelCry.getInstance())
        L.setGlobal("HypixelCry")

        L.push(scriptName)
        L.setGlobal("currentScriptName")


        // Register print function
        L.push(JFunction { l ->
            val n = l.getTop() // Получаем количество переданных аргументов
            val message = StringBuilder()

            for (i in 1..n) {
                if (i > 1) message.append("\t") // В стандартном Lua print использует табуляцию

                // l.toString(i) преобразует значение в строку (аналог tojstring)
                // Если значение nil или его нельзя превратить в строку, вернется null
                val str = l.toString(i) ?: "nil"
                message.append(str)
            }

            val finalMessage = message.toString()

            // Вывод в лог Minecraft
            HypixelCry.LOGGER.info("${HypixelCry.LOG_PREFIX}$finalMessage")

            // print в Lua обычно ничего не возвращает
            0
        })
        L.setGlobal("print")
    }

    fun addCommandCallback(commandName: String, callback: LuaValue, suggestionsCallback: LuaValue?): Boolean {
        // В iroiro LuaValue.type() возвращает LuaType
        if (callback.type() != Lua.LuaType.FUNCTION) return false
        if (commandName.isBlank()) return false
        if (suggestionsCallback != null && suggestionsCallback.type() != Lua.LuaType.FUNCTION) return false

        synchronized(callbacksLock) {
            if (commandCallbacks.containsKey(commandName)) {
                return false
            }
            // Сохранение LuaValue предотвращает удаление функции со стороны Lua GC
            commandCallbacks[commandName] = callback
            if (suggestionsCallback != null) {
                commandSuggestionsCallbacks[commandName] = suggestionsCallback
            }

            // Ваша логика регистрации команды в Minecraft
            registerMinecraftCommand(commandName)
            return true
        }
    }

    private fun registerCommandFunctions() {
        // Функция: registerCommand(name, callback, suggestionsCallback)
        L.push(JFunction { l ->
            val nArgs = l.getTop()

            // 1. Извлекаем имя команды
            val commandName = l.toString(1) ?: ""

            // 2. Извлекаем основной коллбэк (аргумент 2)
            // Нам нужно получить LuaValue, поэтому копируем его наверх и вызываем get()
            if (l.type(2) != Lua.LuaType.FUNCTION) {
                l.push(false)
                return@JFunction 1
            }
            l.pushValue(2)
            val callbackValue = l.get()

            // 3. Извлекаем опциональный коллбэк подсказок (аргумент 3)
            var suggestionsValue: LuaValue? = null
            if (nArgs >= 3 && l.type(3) == Lua.LuaType.FUNCTION) {
                l.pushValue(3)
                suggestionsValue = l.get()
            }

            val success = addCommandCallback(commandName, callbackValue, suggestionsValue)
            l.push(success)
            1
        })
        L.setGlobal("registerCommand")

        // Функция: unregisterCommand(name)
        L.push(JFunction { l ->
            val commandName = l.toString(1) ?: ""
            val success = removeCommandCallback(commandName)
            l.push(success)
            1
        })
        L.setGlobal("unregisterCommand")
    }

    private fun registerMinecraftCommand(commandName: String) {
        try {
            // 1. Регистрируем коллбэк для будущих обновлений диспетчера (например, при смене сервера)
            ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
                commandDispatchers[commandName] = dispatcher
                actualRegister(dispatcher, commandName)
            }

            // 2. Попытка немедленной регистрации, если диспетчер уже доступен
            val client = HypixelCry.mc
            val networkHandler = client.connection
            if (networkHandler != null) {
                val currentDispatcher = networkHandler.commands

                client.execute {
                    @Suppress("UNCHECKED_CAST")
                    val fabricDispatcher = currentDispatcher as CommandDispatcher<FabricClientCommandSource>

                    commandDispatchers[commandName] = fabricDispatcher
                    actualRegister(fabricDispatcher, commandName)
                    HypixelCry.LOGGER.info("${HypixelCry.LOG_PREFIX}Registered Minecraft command: /$commandName")
                }
            }
        } catch (e: Exception) {
            HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Failed to register Minecraft command: /$commandName", e)
        }
    }

    private fun actualRegister(dispatcher: CommandDispatcher<FabricClientCommandSource>, commandName: String) {
        // Удаляем старую команду, если она была
        val root = dispatcher.root
        if (root.getChild(commandName) != null) {
            try {
                val childrenField = root.javaClass.getDeclaredField("children")
                childrenField.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                (childrenField.get(root) as MutableMap<String, *>).remove(commandName)

                val literalsField = root.javaClass.getDeclaredField("literals")
                literalsField.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                (literalsField.get(root) as MutableMap<String, *>).remove(commandName)
            } catch (e: Exception) {
                // Ignore
            }
        }

        val commandBuilder = ClientCommandManager.literal(commandName)
            .executes { context ->
                executeLuaCommand(commandName, emptyArray(), context.source)
                1
            }

        // Проверяем, есть ли callback для автодополнения
        val suggestionsCallback = commandSuggestionsCallbacks[commandName]

        if (suggestionsCallback != null) {
            // С автодополнением
            commandBuilder.then(
                ClientCommandManager.argument("args", StringArgumentType.greedyString())
                    .suggests { context, builder ->
                        getSuggestionsFromLua(commandName, context, builder, suggestionsCallback)
                    }
                    .executes { context ->
                        val args = StringArgumentType.getString(context, "args").split(" ").toTypedArray()
                        executeLuaCommand(commandName, args, context.source)
                        1
                    }
            )
        } else {
            // Без автодополнения
            commandBuilder.then(
                ClientCommandManager.argument("args", StringArgumentType.greedyString())
                    .executes { context ->
                        val args = StringArgumentType.getString(context, "args").split(" ").toTypedArray()
                        executeLuaCommand(commandName, args, context.source)
                        1
                    }
            )
        }

        dispatcher.register(commandBuilder)

        if (dispatcher.root.getChild(commandName) == null) {
            HypixelCry.LOGGER.error("Failed to inject node into dispatcher root!")
        } else {
            HypixelCry.LOGGER.info("Successfully injected command: $commandName with suggestions: ${suggestionsCallback != null}")
        }
    }

    private fun getSuggestionsFromLua(
        commandName: String,
        context: CommandContext<FabricClientCommandSource>,
        builder: com.mojang.brigadier.suggestion.SuggestionsBuilder,
        suggestionsCallback: LuaValue
    ): java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> {
        return java.util.concurrent.CompletableFuture.supplyAsync {
            synchronized(callbacksLock) {
                try {
                    val input = builder.remaining
                    val fullInput = builder.input

                    val l = L

                    // 1. Создаем таблицу аргументов {input=..., fullInput=...}
                    l.newTable()
                    l.push(input); l.setField(-2, "input")
                    l.push(fullInput); l.setField(-2, "fullInput")
                    val infoTable = l.get()

                    // 2. Вызываем коллбэк
                    val resultArray = suggestionsCallback.call(infoTable)

                    // Если массив пустой, значит функция ничего не вернула
                    if (resultArray == null || resultArray.isEmpty()) {
                        return@supplyAsync builder.build()
                    }

                    // Берем первый (и скорее всего единственный) результат
                    val result = resultArray[0]

                    // 3. Проверяем тип результата
                    l.push(result) // Теперь это один LuaValue, push его примет!
                    val resultIsTable = l.isTable(-1)
                    val resultIsString = l.isString(-1) || l.isNumber(-1)
                    l.pop(1)

                    if (resultIsTable) {
                        var i = 1
                        while (true) {
                            // Метод get у LuaValue (таблицы) возвращает один LuaValue
                            val suggestion = result.get(i)

                            l.push(suggestion) // Опять же, push примет одиночный LuaValue
                            val isNil = l.isNil(-1)

                            if (isNil) {
                                l.pop(1)
                                break
                            }

                            if (l.isString(-1) || l.isNumber(-1)) {
                                builder.suggest(suggestion.toString())
                            } else if (l.isTable(-1)) {
                                val textValue = suggestion.get("text")
                                val text = if (textValue.type() == Lua.LuaType.NIL) "" else textValue.toString()

                                val tooltipValue = suggestion.get("tooltip")

                                l.push(tooltipValue)
                                val hasTooltip = !l.isNil(-1)
                                l.pop(1)

                                if (hasTooltip) {
                                    builder.suggest(text, Component.literal(tooltipValue.toString()))
                                } else {
                                    builder.suggest(text)
                                }
                            }

                            l.pop(1)
                            i++
                        }
                    } else if (resultIsString) {
                        builder.suggest(result.toString())
                    }

                } catch (e: Exception) {
                    HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in suggestions for /$commandName", e)
                }
            }
            builder.build()
        }
    }

    private fun unregisterCommandInternal(
        dispatcher: CommandDispatcher<FabricClientCommandSource>,
        commandName: String
    ) {
        try {
            val root = dispatcher.root // В Kotlin это вызывает getRoot()

            // В Brigadier у узла (CommandNode) есть три карты, в которых хранятся команды.
            // Названия полей в самой библиотеке Brigadier (она не обфусцирована так, как MC):
            // "children", "literals", "arguments"
            val nodeClass = root.javaClass.superclass // CommandNode — родитель для RootCommandNode

            val fieldsToClear = arrayOf("children", "literals", "arguments")

            for (fieldName in fieldsToClear) {
                try {
                    // Ищем поле в классе CommandNode
                    val field = com.mojang.brigadier.tree.CommandNode::class.java.getDeclaredField(fieldName)
                    field.isAccessible = true

                    @Suppress("UNCHECKED_CAST")
                    val map = field.get(root) as MutableMap<String, *>
                    map.remove(commandName)
                } catch (e: NoSuchFieldException) {
                    // Если вдруг библиотека Brigadier в вашей среде имеет другие названия полей
                    HypixelCry.LOGGER.warn("Field $fieldName not found in CommandNode")
                }
            }

            // Удаляем из наших внутренних списков
            commandDispatchers.remove(commandName)
            commandCallbacks.remove(commandName)

            HypixelCry.LOGGER.info("${HypixelCry.LOG_PREFIX}Successfully unregistered Lua command: /$commandName")
        } catch (e: Exception) {
            HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Failed to unregister command /$commandName", e)
        }
    }

    private fun executeLuaCommand(commandName: String, args: Array<String>, source: FabricClientCommandSource?) {
        val callback = commandCallbacks[commandName] ?: return

        // Синхронизация для потокобезопасности JNI
        synchronized(callbacksLock) {
            try {
                val l = L

                // 1. Проверяем, является ли коллбэк функцией
                l.push(callback)
                val isFunction = l.isFunction(-1)
                l.pop(1)
                if (!isFunction) return

                // 2. Преобразуем массив строк в таблицу Lua
                l.newTable() // Создаем таблицу на вершине стека
                args.forEachIndexed { index, arg ->
                    l.push(arg) // Кладем строку
                    l.rawSetI(-2, index + 1) // Кладем в таблицу под индексом (Lua-style: 1-based)
                }
                // Забираем готовую таблицу как LuaValue (она удалится со стека)
                val argsTable = l.get()

                // 3. Подготавливаем остальные параметры
                val playerName = source?.player?.name?.string ?: ""

                // 4. Вызываем коллбэк: callback(commandName, argsTable, playerName)
                // Метод call(Object...) автоматически обработает String и LuaValue
                callback.call(commandName, argsTable, playerName)

            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error executing Lua command: /$commandName", e)
                source?.sendError(Component.literal("Error executing command: ${e.message}"))
            }
        }
    }

    fun removeCommandCallback(commandName: String): Boolean {
        synchronized(callbacksLock) {
            val dispatcher = commandDispatchers[commandName]
            if (dispatcher != null) {
                unregisterCommandInternal(dispatcher, commandName)
            }
            commandSuggestionsCallbacks.remove(commandName)
            return commandCallbacks.remove(commandName) != null
        }
    }

    // Event handlers
    fun onInventoryItemAChange(slot: Int, stack: ItemStack): Boolean {
        var allow = true

        // 1. Копируем список колбэков, чтобы избежать ConcurrentModificationException
        val callbacks = synchronized(callbacksLock) {
            inventoryItemChangeCallbacks.toList()
        }

        for (callback in callbacks) {
            try {
                // Обязательно синхронизируемся при работе с нативным стеком L
                synchronized(callbacksLock) {
                    // 2. Создаем обертку предмета (метод .push() возвращает LuaValue)
                    val luaStack = LuaItemStack(L, stack).push()

                    // 3. Вызываем функцию Lua: callback(slot, item)
                    // В iroiro метод call принимает (Object...)
                    val resultArray = callback.call(slot.toDouble(), luaStack)

                    // 4. Проверяем результат (первый элемент массива LuaValue[])
                    val result = resultArray?.getOrNull(0)
                    if (result != null) {
                        // Кладём результат на стек для проверки типа
                        L.push(result)

                        // Если вернули булево значение
                        if (L.isBoolean(-1)) {
                            // Если результат — false, то отменяем действие
                            if (!L.toBoolean(-1)) {
                                allow = false
                            }
                        }

                        // Удаляем результат со стека
                        L.pop(1)
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in inventory change callback", e)
            }
        }
        return allow
    }

    fun onAttackBlock(pos: BlockPos, direction: Direction, hand: InteractionHand): Boolean {
        var allow = true

        // 1. Копируем список колбэков для безопасной итерации
        val callbacks = synchronized(callbacksLock) {
            attackBlockCallbacks.toList()
        }

        for (callback in callbacks) {
            try {
                // Обязательно синхронизируемся при работе с нативным стеком L
                synchronized(callbacksLock) {
                    // 2. Создаем таблицу аргументов {x, y, z, direction, hand}
                    L.newTable() // Таблица на индексе -1

                    L.push(pos.x.toDouble()); L.setField(-2, "x")
                    L.push(pos.y.toDouble()); L.setField(-2, "y")
                    L.push(pos.z.toDouble()); L.setField(-2, "z")

                    // Создаем обертку направления и пушим её в таблицу
                    LuaDirection(L, direction).push()
                    L.setField(-2, "direction")

                    L.push(hand.name); L.setField(-2, "hand")

                    // Забираем готовую таблицу как LuaValue для передачи в функцию
                    val argTable = L.get()

                    // 3. Вызываем функцию Lua: callback(t)
                    val resultArray = callback.call(argTable)

                    // 4. Проверяем результат
                    val result = resultArray?.getOrNull(0)
                    if (result != null) {
                        L.push(result) // Кладем результат на стек для проверки

                        // Если Lua вернула false (явно), то запрещаем действие
                        if (L.isBoolean(-1) && !L.toBoolean(-1)) {
                            allow = false
                        }

                        L.pop(1) // Очищаем стек
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in attack block callback", e)
            }
        }
        return allow
    }

    fun onUseBlock(pos: BlockPos, hand: InteractionHand): Boolean {
        var allow = true

        // 1. Копируем список колбэков для безопасной итерации
        val callbacks = synchronized(callbacksLock) {
            useBlockCallbacks.toList()
        }

        for (callback in callbacks) {
            try {
                // Обязательно синхронизируемся при работе с нативным состоянием L
                synchronized(callbacksLock) {
                    // 2. Создаем таблицу аргументов {x, y, z, hand} на стеке L
                    L.newTable() // Таблица теперь на индексе -1

                    L.push(pos.x.toDouble()); L.setField(-2, "x")
                    L.push(pos.y.toDouble()); L.setField(-2, "y")
                    L.push(pos.z.toDouble()); L.setField(-2, "z")
                    L.push(hand.name); L.setField(-2, "hand")

                    // Метод get() БЕЗ параметров (согласно вашему Lua.java)
                    // забирает таблицу со стека и возвращает её как LuaValue
                    val argTable = L.get()

                    // 3. Вызываем функцию Lua: callback(argTable)
                    // Результат — массив LuaValue[], так как Lua может возвращать несколько значений
                    val resultArray = callback.call(argTable)

                    // 4. Проверяем результат (первый элемент массива)
                    val res = resultArray?.getOrNull(0)
                    if (res != null) {
                        L.push(res) // Кладем результат на стек для проверки типа

                        // Если Lua вернула false (явно), то запрещаем действие
                        // Используем методы из вашего интерфейса Lua
                        if (L.isBoolean(-1) && !L.toBoolean(-1)) {
                            allow = false
                        }

                        L.pop(1) // Очищаем стек после проверки, чтобы не было утечек
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in use block callback", e)
            }
        }
        return allow
    }

    fun onClientTick() {
        val callbacks = synchronized(callbacksLock) {
            clientTickCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in client tick callback in ${scriptName}", e)
            }
        }
    }

    fun onClientTickPre() {
        val callbacks = synchronized(callbacksLock) {
            clientPreTickCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error(
                    "${HypixelCry.LOG_PREFIX}Error in client pre tick callback in ${scriptName}",
                    e
                )
            }
        }
    }

    fun onRenderTick(wrapper: WorldRendererObject) {
        // 1. Быстрая проверка на наличие колбэков
        val callbacks = synchronized(callbacksLock) {
            if (renderWorldCallbacks.isEmpty()) return
            renderWorldCallbacks.toList()
        }
        val initialTop = L.getTop()
        // 2. Синхронизация для JNI (рендеринг идет в другом потоке!)
        synchronized(callbacksLock) {
            try {
                for (callback in callbacks) {
                    try {
                        L.push(callback)
                        wrapper.push()

                        val result = L.pCall(1, 0)
                        if (result != 0.toLong()) { // В некоторых версиях iroiro это Long/Int
                            val errorMsg = L.toString(-1) ?: "Unknown Lua Error"
                            HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in world render callback: $errorMsg")
                            L.pop(1) // ОБЯЗАТЕЛЬНО удаляем сообщение об ошибке
                        }
                    } catch (e: Exception) {
                        HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in world render callback: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in world render callback: ${e.message}")
                L.pop(1)
            } finally {
                val currentTop = L.getTop()
                if (currentTop > initialTop) {
                    L.pop(currentTop - initialTop)
                }
            }
        }
    }

    fun on2DRenderTick(context: GuiGraphics?) {
        val callbacks = synchronized(callbacksLock) {
            render2DCallbacks.toTypedArray()
        }
        val initialTop = L.getTop()
        val renderContext = TwoRenderObject(L, context, scriptName)
        for (callback in callbacks) {
            try {
                L.push(callback)
                renderContext.push()

                val result = L.pCall(1, 0)
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in 2D render callback in ${scriptName}: ${e.message}")
                L.pop(1)
            } finally {
                val currentTop = L.getTop()
                if (currentTop > initialTop) {
                    L.pop(currentTop - initialTop)
                }
            }
        }
    }

    fun onKeyEvent(key: Int, type: KeyAction): Boolean {
        var allow = true

        // 1. Копируем список колбэков для безопасной итерации
        val callbacks = synchronized(callbacksLock) {
            keyEventCallbacks.toList()
        }

        for (callback in callbacks) {
            try {
                // Обязательно синхронизируемся при работе с нативным состоянием L
                synchronized(callbacksLock) {
                    // 2. Вызываем функцию Lua: callback(key, typeName)
                    // В Lua числа передаем как Double, строки как String
                    val resultArray = callback.call(key.toDouble(), type.name)

                    // 3. Проверяем результат (первый элемент массива LuaValue[])
                    val res = resultArray?.getOrNull(0)
                    if (res != null) {
                        // Кладем результат на стек глобального L для проверки типа
                        L.push(res)

                        // Проверяем: если это булево значение и оно равно false
                        // Используем методы из вашего интерфейса Lua
                        if (L.isBoolean(-1) && !L.toBoolean(-1)) {
                            allow = false
                        }

                        // Обязательно удаляем результат со стека после проверки
                        L.pop(1)
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in key callback: ${e.message}")
            }
        }
        return allow
    }


    fun onChatMessageEvent(text: String, overlay: Boolean, json: String): Boolean {
        var allow = true

        // 1. Копируем список колбэков для безопасной итерации
        val callbacks = synchronized(callbacksLock) {
            messageEventCallbacks.toList()
        }

        for (callback in callbacks) {
            try {
                // Обязательно синхронизируемся при работе с нативным состоянием L
                synchronized(callbacksLock) {
                    // 2. Вызываем функцию Lua: callback(text, overlay, json)
                    // Библиотека автоматически сконвертирует String и Boolean в типы Lua
                    val resultArray = callback.call(text, overlay, json)

                    // 3. Проверяем результат (первый элемент массива LuaValue[])
                    val res = resultArray?.getOrNull(0)
                    if (res != null) {
                        // Кладем результат на стек глобального L для проверки типа
                        L.push(res)

                        // Проверяем: если это булево значение и оно равно false
                        // Используем методы из вашего интерфейса Lua (L.java)
                        if (L.isBoolean(-1) && !L.toBoolean(-1)) {
                            allow = false
                        }

                        // Обязательно удаляем результат со стека после проверки
                        L.pop(1)
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in chat message callback: ${e.message}")
            }
        }
        return allow
    }

    fun onSendChatMessageEvent(text: String): Boolean {
        var allow = true

        // 1. Копируем список колбэков для безопасной итерации
        val callbacks = synchronized(callbacksLock) {
            onSendMessageEventCallbacks.toList()
        }

        for (callback in callbacks) {
            try {
                // Обязательно синхронизируемся при работе с нативным состоянием L
                synchronized(callbacksLock) {
                    // 2. Вызываем функцию Lua: callback(text)
                    // Строка 'text' будет автоматически сконвертирована в тип Lua
                    val resultArray = callback.call(text)

                    // 3. Проверяем результат (первый элемент массива LuaValue[])
                    val res = resultArray?.getOrNull(0)
                    if (res != null) {
                        // Кладем результат на стек глобального L для проверки типа
                        L.push(res)

                        // Проверяем: если это булево значение и оно равно false
                        // Используем методы из вашего интерфейса Lua (L.java)
                        if (L.isBoolean(-1) && !L.toBoolean(-1)) {
                            allow = false
                        }

                        // Обязательно удаляем результат со стека после проверки
                        L.pop(1)
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in send message callback: ${e.message}")
            }
        }
        return allow
    }

    fun onSendChatCommandEvent(text: String): Boolean {
        var allow = true

        // 1. Копируем список колбэков для безопасной итерации
        val callbacks = synchronized(callbacksLock) {
            onSendCommandEventCallbacks.toList()
        }

        for (callback in callbacks) {
            try {
                // Обязательно синхронизируемся при работе с нативным состоянием L
                synchronized(callbacksLock) {
                    // 2. Вызываем функцию Lua: callback(text)
                    // Результат в этой библиотеке всегда возвращается как массив LuaValue[]
                    val resultArray = callback.call(text)

                    // 3. Проверяем результат (первый элемент массива)
                    val res = resultArray?.getOrNull(0)
                    if (res != null) {
                        // Кладем результат на стек глобального L для проверки типа
                        L.push(res)

                        // Проверяем: если это булево значение и оно равно false
                        // Используем методы из вашего интерфейса Lua (L.java)
                        if (L.isBoolean(-1) && !L.toBoolean(-1)) {
                            allow = false
                        }

                        // Обязательно удаляем результат со стека после проверки
                        L.pop(1)
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in send command callback: ${e.message}")
            }
        }
        return allow
    }

    fun onBlockUpdateEvent(pos: BlockPos, old: BlockState?, new: BlockState?): Boolean {
        // 1. Сначала проверяем, есть ли вообще колбэки, чтобы не делать лишнюю работу
        val callbacks = synchronized(callbacksLock) {
            if (blockUpdateCallbacks.isEmpty()) return true
            blockUpdateCallbacks.toList()
        }

        var allow = true

        // 2. Синхронизируемся для работы с нативным стеком L
        synchronized(callbacksLock) {
            try {
                // Создаем ОДНУ таблицу для всех колбэков этого скрипта
                L.newTable() // Таблица на индексе -1

                L.push(pos.x.toDouble()); L.setField(-2, "x")
                L.push(pos.y.toDouble()); L.setField(-2, "y")
                L.push(pos.z.toDouble()); L.setField(-2, "z")

                if (old != null) {
                    // LuaBlockState(L, old).push() создает LuaValue с метатаблицей
                    LuaBlockState(L, old).push()
                    L.setField(-2, "old")
                }

                if (new != null) {
                    LuaBlockState(L, new).push()
                    L.setField(-2, "new")
                }

                // Забираем готовую таблицу как LuaValue для передачи в call()
                val eventTable = L.get()

                // 3. Проходим по колбэкам
                for (callback in callbacks) {
                    try {
                        // Вызываем функцию Lua: callback(eventTable)
                        val resultArray = callback.call(eventTable)

                        // Проверяем результат (первый элемент массива)
                        val res = resultArray?.getOrNull(0)
                        if (res != null) {
                            L.push(res) // Кладем на стек для проверки
                            if (L.isBoolean(-1) && !L.toBoolean(-1)) {
                                allow = false
                            }
                            L.pop(1) // Очищаем стек после проверки
                        }
                    } catch (e: Exception) {
                        HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in block update callback: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Critical error in onBlockUpdateEvent: ${e.message}")
            }
        }

        return allow
    }

    fun onLocationChangeEvent(location: Location): Boolean {
        val callbacks = synchronized(callbacksLock) {
            locationChangeCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call(location.toString())
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in location change callback in ${scriptName}: ${e.message}")
            }
        }
        return true
    }

    fun onImGuiRenderEvent(): Boolean {
        val callbacks = synchronized(callbacksLock) {
            imguiRenderCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in imgui callback in ${scriptName}: ${e.message}")
            }
        }
        return true
    }

    // Packet events
    fun onSpawnParticleEvent(id: Int, x: Double, y: Double, z: Double, xDist: Float, yDist: Float, zDist: Float, maxSpeed: Float, count: Int): Boolean {
        var allow = true

        // 1. Копируем список колбэков для безопасной итерации
        val callbacks = synchronized(callbacksLock) {
            particleCallbacks.toList()
        }

        for (callback in callbacks) {
            try {
                // Обязательно синхронизируемся при работе с нативным состоянием L
                synchronized(callbacksLock) {
                    // 2. Создаем таблицу параметров {id, x, y, z, x_dist, ...} на стеке L
                    L.newTable() // Таблица теперь на индексе -1

                    L.push(id.toDouble()); L.setField(-2, "id")
                    L.push(x); L.setField(-2, "x")
                    L.push(y); L.setField(-2, "y")
                    L.push(z); L.setField(-2, "z")

                    L.push(xDist.toDouble()); L.setField(-2, "x_dist")
                    L.push(yDist.toDouble()); L.setField(-2, "y_dist")
                    L.push(zDist.toDouble()); L.setField(-2, "z_dist")

                    L.push(maxSpeed.toDouble()); L.setField(-2, "max_speed")
                    L.push(count.toDouble()); L.setField(-2, "count")

                    // Метод get() БЕЗ параметров забирает таблицу со стека и возвращает её как LuaValue
                    val argTable = L.get()

                    // 3. Вызываем функцию Lua: callback(argTable)
                    // В вашем оригинальном коде результат не проверялся, но если нужно —
                    // вы можете добавить проверку resultArray аналогично предыдущим ивентам.
                    callback.call(argTable)
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in particle callback: ${e.message}")
            }
        }
        return allow
    }

    fun onServerSideRotationEvent(yaw: Float, pitch: Float): Boolean {
        var allow = true

        // 1. Копируем список колбэков для безопасной итерации
        val callbacks = synchronized(callbacksLock) {
            serverSideRotationCallbacks.toList()
        }

        for (callback in callbacks) {
            try {
                // Обязательно синхронизируемся при работе с нативным состоянием L
                synchronized(callbacksLock) {
                    // 2. Вызываем функцию Lua: callback(yaw, pitch)
                    // Передаем как Double, так как в Lua это стандарт для чисел
                    val resultArray = callback.call(yaw.toDouble(), pitch.toDouble())

                    // 3. Проверяем результат (первый элемент массива LuaValue[])
                    val res = resultArray?.getOrNull(0)
                    if (res != null) {
                        // Кладем результат на стек глобального L для проверки типа
                        L.push(res)

                        // Проверяем: если это булево значение и оно равно false
                        // Используем методы из вашего интерфейса Lua (L.java)
                        if (L.isBoolean(-1) && !L.toBoolean(-1)) {
                            allow = false
                        }

                        // Обязательно удаляем результат со стека после проверки
                        L.pop(1)
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in server side rotation callback: ${e.message}")
            }
        }
        return allow
    }

    fun onServerSideTeleportEvent(x: Double, y: Double, z: Double): Boolean {
        var allow = true

        // 1. Копируем список колбэков для безопасной итерации
        val callbacks = synchronized(callbacksLock) {
            serverSideTeleportCallbacks.toList()
        }

        for (callback in callbacks) {
            try {
                // Обязательно синхронизируемся при работе с нативным состоянием L (JNI)
                synchronized(callbacksLock) {
                    // 2. Вызываем функцию Lua: callback(x, y, z)
                    // Библиотека автоматически упакует Double в числа Lua
                    val resultArray = callback.call(x, y, z)

                    // 3. Проверяем результат (первый элемент массива LuaValue[])
                    val res = resultArray?.getOrNull(0)
                    if (res != null) {
                        // Кладем результат на стек глобального L для проверки типа
                        L.push(res)

                        // Проверяем: если это булево значение и оно равно false
                        // Используем методы из вашего интерфейса Lua (L.java)
                        if (L.isBoolean(-1) && !L.toBoolean(-1)) {
                            allow = false
                        }

                        // Обязательно удаляем результат со стека после проверки,
                        // чтобы не засорять память нативной стороны
                        L.pop(1)
                    }
                }
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in server side teleport callback: ${e.message}")
            }
        }
        return allow
    }

    // Cleanup method
    fun cleanup() {
        // Вызываем все callback'и выгрузки скрипта
        scriptUnloadCallbacks.forEach { callback ->
            try {
                callback.call()
            } catch (e: Exception) {
                HypixelCry.LOGGER.error("${HypixelCry.LOG_PREFIX}Error in script unload callback in ${scriptName}", e)
            }
        }

        // Очищаем библиотеки
        threads.stopAllThreads()
        tcp.cleanup()

        for (command in commandCallbacks.keys) {
            val dispatcher = commandDispatchers[command]
            if (dispatcher != null) {
                unregisterCommandInternal(dispatcher, command)
            }
        }

        // Очищаем все коллбэки
        synchronized(callbacksLock) {
            scriptUnloadCallbacks.clear()
            inventoryItemChangeCallbacks.clear()
            useBlockCallbacks.clear()
            attackBlockCallbacks.clear()
            clientTickCallbacks.clear()
            clientPreTickCallbacks.clear()
            blockUpdateCallbacks.clear()
            renderWorldCallbacks.clear()
            render2DCallbacks.clear()
            keyEventCallbacks.clear()
            messageEventCallbacks.clear()
            onSendMessageEventCallbacks.clear()
            onSendCommandEventCallbacks.clear()
            locationChangeCallbacks.clear()
            imguiRenderCallbacks.clear()
            serverSideRotationCallbacks.clear()
            serverSideTeleportCallbacks.clear()
            commandCallbacks.clear()
            commandSuggestionsCallbacks.clear()
        }
        imgui.queue.clear()
        djlLibrary.models.clear()
        djlLibrary.predictors.clear()
        djlLibrary.inputShapes.clear()
        djlLibrary.modelModes.clear()
        //ffi.loadedLibraries.forEach { lib ->
        //    lib.value.dispose()
        //}
        //ffi.loadedLibraries.clear()

        commandDispatchers.clear()

        localDependencyGraph.remove(scriptName)
        // Очищаем зависимости
        dependencies.clear()
        L.close()
    }
}
