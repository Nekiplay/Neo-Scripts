package com.nekiplay.neoscripts.server.features.lua.objects

import com.nekiplay.neoscripts.client.sugar.toComponent
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LuaServer(val server: MinecraftServer?) : LuaValue() {

    companion object {
        // Отдельный поток только для задержек; сам колбэк всегда выполняется на главном потоке сервера
        private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "NeoScripts-Scheduler").apply { isDaemon = true }
        }
    }

    override fun typename(): String = "luaserver"
    override fun tojstring(): String = "LuaServer"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getLevel", "getWorld" -> GetLevel()
            "getLevels", "getWorlds" -> GetLevels()
            "getOnlinePlayers" -> GetOnlinePlayers()
            "getPlayer" -> GetPlayer()
            "broadcast" -> Broadcast()
            "executeCommand", "runCommand" -> ExecuteConsoleCommand()
            "schedule" -> Schedule()
            "runTask" -> RunTask()
            "getTps", "getTPS" -> GetTps()
            "getMspt", "getMSPT" -> GetMspt()
            "gameRule", "gamerule", "getGameRule", "setGameRule" -> GameRuleFunction()
            "difficulty", "getDifficulty", "setDifficulty" -> DifficultyFunction()
            "getRootLevelPath" -> GetRootLevelPath()
            "saveAll" -> SaveAll()
            "stop" -> Stop()
            else -> super.get(key)
        }
    }

    inner class GetRootLevelPath : ZeroArgFunction() {
        override fun call(): LuaValue {
            val worldRoot = server?.getWorldPath(LevelResource.ROOT)
            return valueOf(worldRoot.toString())
        }
    }

    inner class GetOnlinePlayers : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = tableOf()
            server?.playerList?.players?.forEachIndexed { index, player ->
                table.set(index + 1, LuaEntity(player))
            }
            return table
        }
    }

    /**
     * server.getPlayer(nameOrUuid) — ищет игрока по нику (без учёта регистра) или UUID.
     */
    inner class GetPlayer : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val list = server?.playerList ?: return NIL
            if (arg == null || !arg.isstring()) return NIL
            val query = arg.tojstring()

            val player = runCatching { list.getPlayer(UUID.fromString(query)) }.getOrNull()
                ?: list.getPlayer(query)
                ?: list.players.firstOrNull { it.gameProfile.name.equals(query, ignoreCase = true) }

            return player?.let { LuaEntity(it) } ?: NIL
        }
    }

    inner class GetLevel : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val world = arg?.optjstring("minecraft:overworld") ?: return NIL
            val dim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(world));

            return ServerWorldObject(server?.getLevel(dim))
        }
    }

    // Все загруженные измерения сервера
    inner class GetLevels : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = tableOf()
            server?.allLevels?.forEachIndexed { index, level ->
                table.set(index + 1, ServerWorldObject(level))
            }
            return table
        }
    }

    /**
     * server.broadcast(message) — отправляет сообщение всем игрокам.
     * Принимает строку или текстовый компонент (LuaComponent / LuaComponentBuilder).
     */
    inner class Broadcast : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val list = server?.playerList ?: return NIL
            val component = arg?.toComponent() ?: return FALSE

            list.broadcastSystemMessage(component, false)
            return TRUE
        }
    }

    /**
     * server.executeCommand(command [, x, y, z]) — выполняет команду от имени консоли
     * с полными правами (PermissionSet.ALL_PERMISSIONS). Вывод подавлен.
     * Необязательная позиция задаёт точку для относительных координат (~).
     *
     * Возвращает несколько значений:
     *   1) результат команды: LuaEntity (одна заспавненная сущность), таблица LuaEntity
     *      (несколько), TRUE (без новых сущностей) или FALSE при ошибке;
     *   2) числовой результат команды или NIL;
     *   3) сообщение об ошибке или NIL.
     */
    inner class ExecuteConsoleCommand : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val srv = server ?: return NIL

            if (args.narg() < 1 || !args.arg1().isstring()) {
                return error("Invalid arguments: expected command string")
            }

            val command = args.arg1().tojstring().removePrefix("/")

            var position: Vec3? = null
            if (args.narg() >= 4 && args.arg(2).isnumber() && args.arg(3).isnumber() && args.arg(4).isnumber()) {
                position = Vec3(args.arg(2).todouble(), args.arg(3).todouble(), args.arg(4).todouble())
            }

            // Снимок сущностей во всех измерениях до выполнения
            val entitiesBefore = HashSet<Entity>()
            srv.allLevels.forEach { lvl -> lvl.allEntities.forEach { entitiesBefore.add(it) } }

            var source = srv.createCommandSourceStack()
                .withPermission(PermissionSet.ALL_PERMISSIONS)
                .withSuppressedOutput()
            if (position != null) {
                source = source.withPosition(position)
            }

            val dispatcher = srv.commands.dispatcher
            return try {
                val parseResults = dispatcher.parse(command, source)
                val result = dispatcher.execute(parseResults)

                val spawned = ArrayList<Entity>()
                srv.allLevels.forEach { lvl ->
                    lvl.allEntities.forEach { if (it !in entitiesBefore) spawned.add(it) }
                }

                when {
                    spawned.size == 1 ->
                        LuaValue.varargsOf(LuaEntity(spawned[0]), valueOf(result))

                    spawned.isNotEmpty() -> {
                        val entitiesTable = tableOf()
                        spawned.forEachIndexed { index, entity ->
                            entitiesTable.set(index + 1, LuaEntity(entity))
                        }
                        LuaValue.varargsOf(entitiesTable, valueOf(result))
                    }

                    else ->
                        LuaValue.varargsOf(TRUE, valueOf(result))
                }
            } catch (e: CommandSyntaxExceptionAlias) {
                LuaValue.varargsOf(FALSE, NIL, valueOf(e.message ?: "Unknown command error"))
            }
        }
    }

    /**
     * server.schedule(ticks, fn) — вызывает функцию через указанное количество тиков
     * (1 тик = 50 мс). Колбэк выполняется на главном потоке сервера.
     */
    inner class Schedule : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            val srv = server ?: return NIL
            val fn = arg2?.checkfunction() ?: return error("Invalid arguments: expected (ticks, function)")
            val delayTicks = arg1?.optlong(0)?.coerceAtLeast(0L) ?: 0L

            scheduler.schedule({
                srv.executeIfPossible { fn.call() }
            }, delayTicks * 50L, TimeUnit.MILLISECONDS)

            return TRUE
        }
    }

    /**
     * server.runTask(fn) — выполняет функцию на главном потоке сервера
     * (полезно из других потоков).
     */
    inner class RunTask : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val srv = server ?: return NIL
            val fn = arg?.checkfunction() ?: return error("Invalid arguments: expected function")
            srv.executeIfPossible { fn.call() }
            return TRUE
        }
    }

    /**
     * server.getTps() — текущий TPS сервера (не выше 20).
     */
    inner class GetTps : ZeroArgFunction() {
        override fun call(): LuaValue {
            val mspt = server?.averageTickTimeNanos?.div(1_000_000.0) ?: 0.0
            val tps = if (mspt <= 0.0) 20.0 else minOf(20.0, 1000.0 / mspt)
            return valueOf(tps)
        }
    }

    /**
     * server.getMspt() — среднее время тика в миллисекундах.
     */
    inner class GetMspt : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(server?.averageTickTimeNanos?.div(1_000_000.0) ?: 0.0)
        }
    }

    /**
     * server.gameRule(name) — читает значение правила (boolean/number).
     * server.gameRule(name, value) — устанавливает значение правила.
     */
    inner class GameRuleFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val srv = server ?: return NIL
            if (args.narg() < 1 || !args.arg1().isstring()) {
                return error("Invalid arguments: expected gamerule name")
            }

            val rules = srv.globalGameRules
            val rule = rules.availableRules().toList().firstOrNull {
                it.id().equals(args.arg1().tojstring(), ignoreCase = true)
            } ?: return error("Unknown gamerule: ${args.arg1().tojstring()}")

            // Чтение
            if (args.narg() < 2 || args.arg(2).isnil()) {
                return when (val value = rules.get(rule)) {
                    is Boolean -> valueOf(value)
                    is Int -> valueOf(value)
                    is String -> valueOf(value)
                    else -> NIL
                }
            }

            // Запись
            val parsed: Any = when (rule.valueClass()) {
                java.lang.Boolean.TYPE, java.lang.Boolean::class.java -> args.arg(2).toboolean()
                java.lang.Integer.TYPE, java.lang.Integer::class.java -> args.arg(2).toint()
                java.lang.String::class.java -> args.arg(2).tojstring()
                else -> return FALSE
            }
            @Suppress("UNCHECKED_CAST")
            rules.set(rule as GameRule<Any>, parsed, srv)
            return valueOf(parsed.toString())
        }
    }

    /**
     * server.difficulty() — возвращает сложность ("peaceful", "easy", "normal", "hard").
     * server.difficulty(value) — устанавливает сложность по строке или числу (0-3).
     */
    inner class DifficultyFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val srv = server ?: return NIL
            if (arg == null || arg.isnil()) {
                return valueOf(srv.worldData.difficulty.getSerializedName())
            }

            val difficulty: Difficulty = when {
                arg.isnumber() -> Difficulty.byId(arg.toint())
                arg.isstring() -> runCatching {
                    Difficulty.valueOf(arg.tojstring().uppercase(Locale.ROOT))
                }.getOrElse { return error("Unknown difficulty: ${arg.tojstring()}") }

                else -> return error("Invalid arguments: expected \"peaceful\"|\"easy\"|\"normal\"|\"hard\" or 0-3")
            }

            srv.setDifficulty(difficulty, true)
            return valueOf(difficulty.getSerializedName())
        }
    }

    /**
     * server.saveAll() — сохраняет мир и данные игроков. Возвращает boolean успеха.
     */
    inner class SaveAll : ZeroArgFunction() {
        override fun call(): LuaValue {
            val srv = server ?: return NIL
            return valueOf(srv.saveEverything(true, false, true))
        }
    }

    /**
     * server.stop() — останавливает сервер (предварительно сохранив мир штатным путём).
     */
    inner class Stop : ZeroArgFunction() {
        override fun call(): LuaValue {
            val srv = server ?: return NIL
            srv.halt(false)
            return TRUE
        }
    }
}

// Псевдоним, чтобы не тащить полный импорт brigadier в сигнатуру catch выше
private typealias CommandSyntaxExceptionAlias = com.mojang.brigadier.exceptions.CommandSyntaxException
