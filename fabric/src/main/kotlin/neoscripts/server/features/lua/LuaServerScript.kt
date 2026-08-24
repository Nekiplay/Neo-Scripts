package com.nekiplay.neoscripts.server.features.lua

import com.nekiplay.neoscripts.ClientMain
import com.nekiplay.neoscripts.common.features.lua.objects.misc.ArchiveLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.ThreadLib
import com.nekiplay.neoscripts.common.features.lua.LuaManager
import com.nekiplay.neoscripts.common.features.lua.MinecraftLuajavaLib
import com.nekiplay.neoscripts.common.features.lua.Script
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponentBuilder
import com.nekiplay.neoscripts.common.features.lua.objects.misc.Blocks
import com.nekiplay.neoscripts.common.features.lua.objects.misc.CatboostLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.Creator
import com.nekiplay.neoscripts.common.features.lua.objects.misc.DJLLuaTrainer
import com.nekiplay.neoscripts.common.features.lua.objects.misc.EncodingLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.Entities
import com.nekiplay.neoscripts.common.features.lua.objects.misc.FFILib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.Items
import com.nekiplay.neoscripts.common.features.lua.objects.misc.JsonLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.TCPLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.UDPLib
import com.nekiplay.neoscripts.common.features.lua.objects.misc.http.HttpClientLib
import com.nekiplay.neoscripts.server.features.lua.objects.LuaServer
import com.nekiplay.neoscripts.server.features.lua.objects.ServerWorldObject
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaClosure
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.luajc.LuaJC
import java.util.Collections
import java.util.Stack
import java.util.concurrent.ConcurrentHashMap

class LuaServerScript(val name: String, mgr: LuaManager, val server: MinecraftServer?): Script(name, mgr) {
    private val loadingStack = Stack<String>()
    private val systemModuleCache = ConcurrentHashMap<String, LuaValue>()
    val localDependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()
    val requireCache = ConcurrentHashMap<String, LuaValue>()

    // Synchronize only when needed
    private val callbacksLock = Any()

    // Script events
    private val scriptUnloadCallbacks = ArrayList<LuaValue>()

    // Events
    private val serverTickCallbacks = ArrayList<LuaValue>()
    private val serverPreTickCallbacks = ArrayList<LuaValue>()
    private val serverWorldTickCallbacks = ArrayList<LuaValue>()
    private val serverWorldPreTickCallbacks = ArrayList<LuaValue>()
    private val serverStoppingCallbacks = ArrayList<LuaValue>()

    // Interaction events
    private val attackBlockCallbacks = ArrayList<LuaValue>()
    private val useBlockCallbacks = ArrayList<LuaValue>()
    private val useItemOnBlockCallbacks = ArrayList<LuaValue>()
    private val useWithoutItemCallbacks = ArrayList<LuaValue>()
    private val breakBlockBeforeCallbacks = ArrayList<LuaValue>()
    private val breakBlockAfterCallbacks = ArrayList<LuaValue>()
    private val breakBlockCancelCallbacks = ArrayList<LuaValue>()
    private val attackEntityCallbacks = ArrayList<LuaValue>()
    private val useEntityCallbacks = ArrayList<LuaValue>()
    private val useItemCallbacks = ArrayList<LuaValue>()
    private val useItemOnCallbacks = ArrayList<LuaValue>()
    private val pickItemFromBlockCallbacks = ArrayList<LuaValue>()
    private val pickItemFromEntityCallbacks = ArrayList<LuaValue>()

    // Script-specific libraries
    private var tcpLib: TCPLib? = null
    private var udpLib: UDPLib? = null
    private var threadLib: ThreadLib? = null
    private var djlLibrary: DJLLuaTrainer? = null
    private var ffi: FFILib? = null
    private var http: HttpClientLib? = null

    init {
        // Register standard libraries
        scriptGlobals.load(MinecraftLuajavaLib())

        // Compile scripts to Java bytecode (LuaJC), fall back to interpreter on failure
        scriptGlobals.loader = Globals.Loader { proto, name, env ->
            try {
                LuaJC.instance.load(proto, name, env)
            } catch (t: Throwable) {
                LuaClosure(proto, env)
            }
        }

        registerCustomFunctions()
    }

    private fun registerCustomFunctions() {
        // Register event registration functions
        registerEventRegistrationFunctions()

        // Register event unregistration functions
        registerEventUnregistrationFunctions()

        // Register require function with module loading prevention
        registerRequireFunction()

        // Register other custom functions
        registerOtherCustomFunctions()
    }

    private fun registerEventRegistrationFunctions() {
        scriptGlobals.set("registerUnloadCallback", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(scriptUnloadCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerServerTick", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverPreTickCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerServerTickPost", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverTickCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerServerTickPre", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverPreTickCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerServerWorldTick", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverWorldPreTickCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerServerWorldTickPost", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverWorldTickCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerServerWorldTickPre", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverWorldPreTickCallbacks.add(callback))
                }
            }
        })

        scriptGlobals.set("registerServerStoppingCallback", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverStoppingCallbacks.add(callback))
                }
            }
        })

        registerInteractionEventFunction("registerAttackBlockCallback", attackBlockCallbacks)
        registerInteractionEventFunction("registerUseBlockCallback", useBlockCallbacks)
        registerInteractionEventFunction("registerUseItemOnBlockCallback", useItemOnBlockCallbacks)
        registerInteractionEventFunction("registerUseWithoutItemCallback", useWithoutItemCallbacks)
        registerInteractionEventFunction("registerBreakBlockBeforeCallback", breakBlockBeforeCallbacks)
        registerInteractionEventFunction("registerBreakBlockAfterCallback", breakBlockAfterCallbacks)
        registerInteractionEventFunction("registerBreakBlockCancelCallback", breakBlockCancelCallbacks)
        registerInteractionEventFunction("registerAttackEntityCallback", attackEntityCallbacks)
        registerInteractionEventFunction("registerUseEntityCallback", useEntityCallbacks)
        registerInteractionEventFunction("registerUseItemCallback", useItemCallbacks)
        registerInteractionEventFunction("registerUseItemOnCallback", useItemOnCallbacks)
        registerInteractionEventFunction("registerPickItemFromBlockCallback", pickItemFromBlockCallbacks)
        registerInteractionEventFunction("registerPickItemFromEntityCallback", pickItemFromEntityCallbacks)
    }

    private fun registerInteractionEventFunction(name: String, list: ArrayList<LuaValue>) {
        scriptGlobals.set(name, object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(list.add(callback))
                }
            }
        })
    }

    private fun unregisterInteractionEventFunction(name: String, list: ArrayList<LuaValue>) {
        scriptGlobals.set(name, object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(list.remove(callback))
                }
            }
        })
    }

    val hasAttackBlockCallbacks: Boolean get() = synchronized(callbacksLock) { attackBlockCallbacks.isNotEmpty() }
    val hasUseBlockCallbacks: Boolean get() = synchronized(callbacksLock) { useBlockCallbacks.isNotEmpty() }
    val hasUseItemOnBlockCallbacks: Boolean get() = synchronized(callbacksLock) { useItemOnBlockCallbacks.isNotEmpty() }
    val hasUseWithoutItemCallbacks: Boolean get() = synchronized(callbacksLock) { useWithoutItemCallbacks.isNotEmpty() }
    val hasBreakBlockBeforeCallbacks: Boolean get() = synchronized(callbacksLock) { breakBlockBeforeCallbacks.isNotEmpty() }
    val hasBreakBlockAfterCallbacks: Boolean get() = synchronized(callbacksLock) { breakBlockAfterCallbacks.isNotEmpty() }
    val hasBreakBlockCancelCallbacks: Boolean get() = synchronized(callbacksLock) { breakBlockCancelCallbacks.isNotEmpty() }
    val hasAttackEntityCallbacks: Boolean get() = synchronized(callbacksLock) { attackEntityCallbacks.isNotEmpty() }
    val hasUseEntityCallbacks: Boolean get() = synchronized(callbacksLock) { useEntityCallbacks.isNotEmpty() }
    val hasUseItemCallbacks: Boolean get() = synchronized(callbacksLock) { useItemCallbacks.isNotEmpty() }
    val hasUseItemOnCallbacks: Boolean get() = synchronized(callbacksLock) { useItemOnCallbacks.isNotEmpty() }
    val hasPickItemFromBlockCallbacks: Boolean get() = synchronized(callbacksLock) { pickItemFromBlockCallbacks.isNotEmpty() }
    val hasPickItemFromEntityCallbacks: Boolean get() = synchronized(callbacksLock) { pickItemFromEntityCallbacks.isNotEmpty() }
    val hasServerStoppingCallbacks: Boolean get() = synchronized(callbacksLock) { serverStoppingCallbacks.isNotEmpty() }

    private fun registerEventUnregistrationFunctions() {
        scriptGlobals.set("unregisterUnloadCallback", object : OneArgFunction() {
            override fun call(callback: LuaValue): LuaValue {
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(scriptUnloadCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterServerTick", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverPreTickCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterServerTickPost", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverTickCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterServerTickPre", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverPreTickCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterServerWorldTick", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverWorldPreTickCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterServerWorldTickPost", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverWorldTickCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterServerWorldTickPre", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverWorldPreTickCallbacks.remove(callback))
                }
            }
        })

        scriptGlobals.set("unregisterServerStoppingCallback", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val callback = args.arg(1)
                if (!callback.isfunction()) return FALSE
                synchronized(callbacksLock) {
                    return valueOf(serverStoppingCallbacks.remove(callback))
                }
            }
        })

        unregisterInteractionEventFunction("unregisterAttackBlockCallback", attackBlockCallbacks)
        unregisterInteractionEventFunction("unregisterUseBlockCallback", useBlockCallbacks)
        unregisterInteractionEventFunction("unregisterUseItemOnBlockCallback", useItemOnBlockCallbacks)
        unregisterInteractionEventFunction("unregisterUseWithoutItemCallback", useWithoutItemCallbacks)
        unregisterInteractionEventFunction("unregisterBreakBlockBeforeCallback", breakBlockBeforeCallbacks)
        unregisterInteractionEventFunction("unregisterBreakBlockAfterCallback", breakBlockAfterCallbacks)
        unregisterInteractionEventFunction("unregisterBreakBlockCancelCallback", breakBlockCancelCallbacks)
        unregisterInteractionEventFunction("unregisterAttackEntityCallback", attackEntityCallbacks)
        unregisterInteractionEventFunction("unregisterUseEntityCallback", useEntityCallbacks)
        unregisterInteractionEventFunction("unregisterUseItemCallback", useItemCallbacks)
        unregisterInteractionEventFunction("unregisterUseItemOnCallback", useItemOnCallbacks)
        unregisterInteractionEventFunction("unregisterPickItemFromBlockCallback", pickItemFromBlockCallbacks)
        unregisterInteractionEventFunction("unregisterPickItemFromEntityCallback", pickItemFromEntityCallbacks)
    }

    private fun registerRequireFunction() {
        scriptGlobals.set("require", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val moduleName = args.arg(1).checkjstring()
                val cache = args.arg(2).optboolean(false)

                val caller = if (loadingStack.isEmpty()) scriptName else loadingStack.peek()

                localDependencyGraph.getOrPut(caller) {
                    Collections.synchronizedSet(LinkedHashSet<String>())
                }.add(moduleName)

                if (requireCache.containsKey(moduleName) && cache) {
                    return requireCache.getOrDefault(moduleName, NIL)
                }

                val value = requireModule(moduleName)
                if (cache) {
                    requireCache[moduleName] = value
                }
                return value
            }
        })
    }

    private fun registerOtherCustomFunctions() {
        scriptGlobals.set("currentScriptName", LuaValue.valueOf(scriptName))

        // Register print function
        scriptGlobals.set("print", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val message = StringBuilder()

                for (i in 1..args.narg()) {
                    if (i > 1) message.append(" ")
                    message.append(args.arg(i).tojstring())
                }
                val messageStr = message.toString()
                ClientMain.LOGGER?.info(ClientMain.LOG_PREFIX + messageStr)
                return NIL
            }
        })
    }

    private fun getSystemModule(name: String): LuaValue? {
        systemModuleCache[name.lowercase()]?.let { return it }

        val module: LuaValue = when (name.lowercase()) {
            "server" -> {
                LuaServer(server)
            }
            "tcp" -> {
                if (tcpLib == null) tcpLib = TCPLib()
                tcpLib!!
            }
            "udp" -> {
                if (udpLib == null) udpLib = UDPLib()
                udpLib!!
            }
            "threads" -> {
                if (threadLib == null) threadLib = ThreadLib()
                threadLib!!
            }
            "ffi" -> {
                if (ffi == null) ffi = FFILib()
                ffi!!
            }
            "djl", -> {
                if (djlLibrary == null)
                    djlLibrary = DJLLuaTrainer(manager)
                djlLibrary!!
                //scriptGlobals.get("djl")
            }
            "json" -> {
                JsonLib()
            }
            "http" -> {
                if (http == null)
                    http = HttpClientLib()
                http!!
            }
            "creator" -> {
                Creator()
            }
            "blocks" -> {
                Blocks()
            }
            "entities" -> {
                Entities()
            }
            "items" -> {
                Items()
            }
            "encoding" -> {
                EncodingLib()
            }
            "archive" -> {
                ArchiveLib()
            }
            "catboost" -> {
                CatboostLib()
            }
            "text_builder", "textbuilder", "component_builder", "componentbuilder" -> {
                LuaComponentBuilder.createLibrary()
            }
            else -> return null
        }

        systemModuleCache[name.lowercase()] = module
        return module
    }

    fun requireModule(moduleName: String): LuaValue {
        val systemModule = getSystemModule(moduleName)
        if (systemModule != null) {
            return systemModule
        }

        val moduleFile = manager.findModuleFile(moduleName)
            ?: throw LuaError("module '$moduleName' not found")

        try {
            loadingStack.push(moduleName)

            val chunk = LuaManager.loadChunk(moduleFile, moduleName, scriptGlobals)

            val result = chunk.call()
            return result

        } catch (e: Exception) {
            throw LuaError("error loading module '$moduleName': ${e.message}")
        } finally {
            if (!loadingStack.isEmpty() && loadingStack.peek() == moduleName) {
                loadingStack.pop()
            }
        }
    }

    fun onServerTick() {
        val callbacks = synchronized(callbacksLock) {
            serverTickCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call()
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("${ClientMain.LOG_PREFIX}Error in client tick callback in ${scriptName}", e)
            }
        }
    }

    fun onServerTickPre() {
        val callbacks = synchronized(callbacksLock) {
            serverPreTickCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call()
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("${ClientMain.LOG_PREFIX}Error in client pre tick callback in ${scriptName}", e)
            }
        }
    }

    fun onServerWorldTick(world: ServerWorldObject) {
        val callbacks = synchronized(callbacksLock) {
            serverTickCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call(world)
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("${ClientMain.LOG_PREFIX}Error in client tick callback in ${scriptName}", e)
            }
        }
    }

    fun onServerWorldTickPre(world: ServerWorldObject) {
        val callbacks = synchronized(callbacksLock) {
            serverPreTickCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call(world)
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("${ClientMain.LOG_PREFIX}Error in client pre tick callback in ${scriptName}", e)
            }
        }
    }

    /**
     * Вызывается при остановке сервера/мира, пока уровни ещё загружены.
     */
    fun onServerStopping(world: ServerWorldObject) {
        val callbacks = synchronized(callbacksLock) {
            serverStoppingCallbacks.toTypedArray()
        }

        for (callback in callbacks) {
            try {
                callback.call(world)
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("${ClientMain.LOG_PREFIX}Error in server stopping callback in ${scriptName}", e)
            }
        }
    }


    private fun invokeCancellable(snapshot: Array<LuaValue>, table: LuaValue): Boolean {
        var allow = true
        for (callback in snapshot) {
            try {
                val res = callback.call(table)
                if (res.isboolean() && !res.toboolean()) {
                    allow = false
                }
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("${ClientMain.LOG_PREFIX}Error in server event callback in ${scriptName}", e)
            }
        }
        return allow
    }

    private fun invokeNotify(snapshot: Array<LuaValue>, table: LuaValue) {
        for (callback in snapshot) {
            try {
                callback.call(table)
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("${ClientMain.LOG_PREFIX}Error in server event callback in ${scriptName}", e)
            }
        }
    }

    private fun baseEventTable(player: Player?, level: Level?, hand: InteractionHand?): LuaValue {
        val t = LuaValue.tableOf()
        if (player != null) t.set("player", LuaEntity(player))
        if (level != null) t.set("world", ServerWorldObject(level as? ServerLevel))
        if (hand != null) t.set("hand", LuaValue.valueOf(hand.name))
        return t
    }


    fun onAttackBlock(player: Player, level: Level, hand: InteractionHand, pos: BlockPos, direction: Direction): Boolean {
        val callbacks = synchronized(callbacksLock) { attackBlockCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return true

        val t = baseEventTable(player, level, hand)
        t.set("blockpos", LuaBlockPos(pos))
        t.set("direction", LuaDirection(direction))
        return invokeCancellable(callbacks, t)
    }

    fun onUseBlock(player: Player, level: Level, hand: InteractionHand, hit: BlockHitResult): Boolean {
        val callbacks = synchronized(callbacksLock) { useBlockCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return true

        val t = baseEventTable(player, level, hand)
        t.set("blockpos", LuaBlockPos(hit.blockPos))
        t.set("direction", LuaDirection(hit.direction))
        return invokeCancellable(callbacks, t)
    }

    fun onUseItemOnBlock(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand
    ): Boolean {
        val callbacks = synchronized(callbacksLock) { useItemOnBlockCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return true

        val t = baseEventTable(player, level, hand)
        t.set("item", if (stack.isEmpty) LuaValue.NIL else LuaItemStack(stack))
        t.set("blockpos", LuaBlockPos(pos))
        t.set("blockstate", LuaBlockState(state))
        return invokeCancellable(callbacks, t)
    }

    fun onUseWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player): Boolean {
        val callbacks = synchronized(callbacksLock) { useWithoutItemCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return true

        val t = baseEventTable(player, level, null)
        t.set("blockpos", LuaBlockPos(pos))
        t.set("blockstate", LuaBlockState(state))
        return invokeCancellable(callbacks, t)
    }

    fun onBreakBlockBefore(level: Level, player: Player, pos: BlockPos, state: BlockState, blockEntity: BlockEntity?): Boolean {
        val callbacks = synchronized(callbacksLock) { breakBlockBeforeCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return true

        val t = baseEventTable(player, level, null)
        t.set("blockpos", LuaBlockPos(pos))
        t.set("blockstate", LuaBlockState(state))
        t.set("blockentity", if (blockEntity != null) LuaBlockEntity(blockEntity) else LuaValue.NIL)
        return invokeCancellable(callbacks, t)
    }

    fun onBreakBlockAfter(level: Level, player: Player, pos: BlockPos, state: BlockState, blockEntity: BlockEntity?) {
        val callbacks = synchronized(callbacksLock) { breakBlockAfterCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return

        val t = baseEventTable(player, level, null)
        t.set("blockpos", LuaBlockPos(pos))
        t.set("blockstate", LuaBlockState(state))
        t.set("blockentity", if (blockEntity != null) LuaBlockEntity(blockEntity) else LuaValue.NIL)
        invokeNotify(callbacks, t)
    }

    fun onBreakBlockCancel(level: Level, player: Player, pos: BlockPos, state: BlockState, blockEntity: BlockEntity?) {
        val callbacks = synchronized(callbacksLock) { breakBlockCancelCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return

        val t = baseEventTable(player, level, null)
        t.set("blockpos", LuaBlockPos(pos))
        t.set("blockstate", LuaBlockState(state))
        t.set("blockentity", if (blockEntity != null) LuaBlockEntity(blockEntity) else LuaValue.NIL)
        invokeNotify(callbacks, t)
    }

    fun onAttackEntity(player: Player, level: Level, hand: InteractionHand, target: Entity, hit: EntityHitResult?): Boolean {
        val callbacks = synchronized(callbacksLock) { attackEntityCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return true

        val t = baseEventTable(player, level, hand)
        t.set("entity", LuaEntity(target))
        if (hit != null) t.set("hit_pos", LuaVector3d(hit.location))
        return invokeCancellable(callbacks, t)
    }

    fun onUseEntity(player: Player, level: Level, hand: InteractionHand, target: Entity, hit: EntityHitResult?): Boolean {
        val callbacks = synchronized(callbacksLock) { useEntityCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return true

        val t = baseEventTable(player, level, hand)
        t.set("entity", LuaEntity(target))
        if (hit != null) t.set("hit_pos", LuaVector3d(hit.location))
        return invokeCancellable(callbacks, t)
    }

    fun onUseItem(player: Player, level: Level, hand: InteractionHand): Boolean {
        val callbacks = synchronized(callbacksLock) { useItemCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return true

        val t = baseEventTable(player, level, hand)
        val stack = player.getItemInHand(hand)
        t.set("item", if (stack.isEmpty) LuaValue.NIL else LuaItemStack(stack))
        return invokeCancellable(callbacks, t)
    }

    fun onUseItemOn(context: UseOnContext): Boolean {
        val callbacks = synchronized(callbacksLock) { useItemOnCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return true

        val stack = context.itemInHand
        val t = baseEventTable(context.player, context.level, context.hand)
        t.set("item", if (stack.isEmpty) LuaValue.NIL else LuaItemStack(stack))
        t.set("blockpos", LuaBlockPos(context.clickedPos))
        t.set("direction", LuaDirection(context.clickedFace))
        return invokeCancellable(callbacks, t)
    }

    fun onPickItemFromBlock(player: ServerPlayer, pos: BlockPos, state: BlockState, includeData: Boolean): ItemStack? {
        val callbacks = synchronized(callbacksLock) { pickItemFromBlockCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return null

        val t = baseEventTable(player, null, null)
        t.set("blockpos", LuaBlockPos(pos))
        t.set("blockstate", LuaBlockState(state))
        t.set("include_data", LuaValue.valueOf(includeData))

        var override: ItemStack? = null
        for (callback in callbacks) {
            try {
                val res = callback.call(t)
                if (override == null && res is LuaItemStack) override = res.stack.copy()
                else if (override == null && res.isuserdata(ItemStack::class.java)) override = (res.touserdata() as ItemStack).copy()
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("${ClientMain.LOG_PREFIX}Error in pick item callback in ${scriptName}", e)
            }
        }
        return override
    }

    fun onPickItemFromEntity(player: ServerPlayer, target: Entity, includeData: Boolean): ItemStack? {
        val callbacks = synchronized(callbacksLock) { pickItemFromEntityCallbacks.toTypedArray() }
        if (callbacks.isEmpty()) return null

        val t = baseEventTable(player, null, null)
        t.set("entity", LuaEntity(target))
        t.set("include_data", LuaValue.valueOf(includeData))

        var override: ItemStack? = null
        for (callback in callbacks) {
            try {
                val res = callback.call(t)
                if (override == null && res is LuaItemStack) override = res.stack.copy()
                else if (override == null && res.isuserdata(ItemStack::class.java)) override = (res.touserdata() as ItemStack).copy()
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("${ClientMain.LOG_PREFIX}Error in pick item callback in ${scriptName}", e)
            }
        }
        return override
    }

    override fun cleanup() {
        scriptUnloadCallbacks.forEach { callback ->
            try {
                callback.call()
            } catch (e: Exception) {
                ClientMain.LOGGER?.error("${ClientMain.LOG_PREFIX}Error in script unload callback in ${scriptName}", e)
            }
        }

        synchronized(callbacksLock) {
            scriptUnloadCallbacks.clear()
            attackBlockCallbacks.clear()
            useBlockCallbacks.clear()
            useItemOnBlockCallbacks.clear()
            useWithoutItemCallbacks.clear()
            breakBlockBeforeCallbacks.clear()
            breakBlockAfterCallbacks.clear()
            breakBlockCancelCallbacks.clear()
            attackEntityCallbacks.clear()
            useEntityCallbacks.clear()
            useItemCallbacks.clear()
            useItemOnCallbacks.clear()
            pickItemFromBlockCallbacks.clear()
            pickItemFromEntityCallbacks.clear()
        }

        scriptGlobals = null
    }
}
