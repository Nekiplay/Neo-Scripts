package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponent
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponentBuilder
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.ScoreHolder
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JavaInstance

/**
 * Обёртка над Scoreboard (табло). Работает одинаково на клиенте и на сервере:
 *  - клиент: изменения локальны (видны только этому игроку);
 *  - сервер: изменения через ServerScoreboard автоматически синхронизируются клиентам.
 */
class LuaScoreboard(val scoreboard: Scoreboard) : LuaUserdata(scoreboard) {

    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "javaClass", "class" -> JavaInstance(scoreboard)

            // Список всех обьективов
            "objectives" -> {
                val table = tableOf()
                scoreboard.getObjectives().forEachIndexed { index, objective ->
                    table.set(index + 1, LuaObjective(scoreboard, objective))
                }
                table
            }

            // Доступные имена слотов отображения ("list", "sidebar", "below_name", "sidebar.team.gold", ...)
            "display_slots", "displaySlots" -> {
                val table = tableOf()
                DisplaySlot.values().forEachIndexed { index, slot ->
                    table.set(index + 1, valueOf(slot.getSerializedName()))
                }
                table
            }

            "get_objective", "getObjective" -> GetObjectiveFunction()
            "create_objective", "add_objective", "createObjective", "addObjective" -> CreateObjectiveFunction()
            "remove_objective", "delete_objective", "removeObjective", "deleteObjective" -> RemoveObjectiveFunction()

            "get_score", "getScore" -> GetScoreFunction()
            "set_score", "setScore" -> SetScoreFunction()
            "add_score", "addScore" -> AddScoreFunction()
            "reset_score", "remove_score", "resetScore", "removeScore" -> ResetScoreFunction()
            "list_scores", "listScores" -> ListScoresFunction()

            "get_display", "getDisplay" -> GetDisplayFunction()
            "set_display", "setDisplay" -> SetDisplayFunction()

            else -> super.get(key)
        }
    }

    override fun typename(): String = "scoreboard"
    override fun tojstring(): String = "LuaScoreboard"

    private fun holder(name: String): ScoreHolder = ScoreHolder.forNameOnly(name)

    private fun findObjective(arg: LuaValue?): Objective? {
        if (arg == null || !arg.isstring()) return null
        return scoreboard.getObjective(arg.tojstring())
    }

    private fun parseDisplaySlot(arg: LuaValue?): DisplaySlot? {
        if (arg == null) return null
        if (arg.isnumber() && arg.isint()) {
            val id = arg.toint()
            if (id >= 0 && id < DisplaySlot.values().size) {
                return DisplaySlot.entries[id]
            }
            return null
        }
        if (!arg.isstring()) return null
        val name = arg.tojstring()
        for (slot in DisplaySlot.entries) {
            if (slot.serializedName.equals(name, ignoreCase = true) ||
                slot.name.equals(name, ignoreCase = true)
            ) {
                return slot
            }
        }
        return null
    }

    private fun parseCriteria(name: String): ObjectiveCriteria {
        return ObjectiveCriteria.byName(name).orElse(ObjectiveCriteria.DUMMY)
    }

    /**
     * sb:get_objective("имя") -> objective | nil
     */
    private inner class GetObjectiveFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val objective = findObjective(arg) ?: return NIL
            return LuaObjective(scoreboard, objective)
        }
    }

    /**
     * sb:create_objective("имя"[, criteria][, displayName][, renderType]) -> objective | false
     * criteria по умолчанию "dummy", renderType — "integer".
     */
    private inner class CreateObjectiveFunction : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            if (!args.isstring(1)) return NIL
            val name = args.arg1().tojstring()
            val criteriaName = args.optjstring(2, "dummy")
            val displayName = args.optjstring(3, name)
            val renderTypeName = args.optjstring(4, "integer")

            val criteria = parseCriteria(criteriaName)
            val renderType = ObjectiveCriteria.RenderType.byId(renderTypeName)

            return try {
                val objective = scoreboard.addObjective(
                    name,
                    criteria,
                    Component.literal(displayName),
                    renderType,
                    true,
                    null
                )
                LuaObjective(scoreboard, objective)
            } catch (_: Exception) {
                FALSE
            }
        }
    }

    /**
     * sb:remove_objective("имя") -> boolean
     */
    private inner class RemoveObjectiveFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val objective = findObjective(arg) ?: return FALSE
            return try {
                scoreboard.removeObjective(objective)
                TRUE
            } catch (_: Exception) {
                FALSE
            }
        }
    }

    /**
     * sb:get_score("обьектив", "держатель") -> number | nil
     */
    private inner class GetScoreFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            val objective = findObjective(arg1) ?: return NIL
            if (arg2 == null || !arg2.isstring()) return NIL
            val info = scoreboard.getPlayerScoreInfo(holder(arg2.tojstring()), objective) ?: return NIL
            return valueOf(info.value())
        }
    }

    /**
     * sb:set_score("обьектив", "держатель", значение) -> boolean
     */
    private inner class SetScoreFunction : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val objective = findObjective(args.arg1()) ?: return NIL
            if (!args.isstring(2) || !args.isnumber(3)) return NIL
            return try {
                scoreboard.getOrCreatePlayerScore(holder(args.arg(2).tojstring()), objective)
                    .set(args.arg(3).toint())
                TRUE
            } catch (_: Exception) {
                FALSE
            }
        }
    }

    /**
     * sb:add_score("обьектив", "держатель", дельта) -> boolean
     */
    private inner class AddScoreFunction : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val objective = findObjective(args.arg1()) ?: return NIL
            if (!args.isstring(2) || !args.isnumber(3)) return NIL
            return try {
                scoreboard.getOrCreatePlayerScore(holder(args.arg(2).tojstring()), objective)
                    .add(args.arg(3).toint())
                TRUE
            } catch (_: Exception) {
                FALSE
            }
        }
    }

    /**
     * sb:reset_score("обьектив", "держатель") -> boolean
     */
    private inner class ResetScoreFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            val objective = findObjective(arg1) ?: return NIL
            if (arg2 == null || !arg2.isstring()) return NIL
            return try {
                scoreboard.resetSinglePlayerScore(holder(arg2.tojstring()), objective)
                TRUE
            } catch (_: Exception) {
                FALSE
            }
        }
    }

    /**
     * sb:list_scores("обьектив") -> { { owner = "...", value = n, display = "..." }, ... }
     */
    private inner class ListScoresFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val objective = findObjective(arg) ?: return NIL
            val table = tableOf()
            var index = 1
            for (entry in scoreboard.listPlayerScores(objective)) {
                table.set(index++, scoreEntryToTable(entry))
            }
            return table
        }
    }

    /**
     * sb:get_display("sidebar") -> objective | nil
     */
    private inner class GetDisplayFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            val slot = parseDisplaySlot(arg) ?: return NIL
            val objective = scoreboard.getDisplayObjective(slot) ?: return NIL
            return LuaObjective(scoreboard, objective)
        }
    }

    /**
     * sb:set_display("sidebar", "обьектив") / sb:set_display("sidebar", nil) -> boolean
     */
    private inner class SetDisplayFunction : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val slot = parseDisplaySlot(args.arg1()) ?: return NIL
            return try {
                val objective = if (args.isnil(2)) null else findObjective(args.arg(2))
                scoreboard.setDisplayObjective(slot, objective)
                TRUE
            } catch (_: Exception) {
                FALSE
            }
        }
    }

    companion object {
        internal fun scoreEntryToTable(entry: net.minecraft.world.scores.PlayerScoreEntry): LuaValue {
            val entryTable = tableOf()
            entryTable.set("owner", valueOf(entry.owner()))
            entryTable.set("value", valueOf(entry.value()))
            entryTable.set(
                "display",
                entry.display()?.let { valueOf(it.string) } ?: NIL
            )
            return entryTable
        }
    }
}

/**
 * Обёртка над отдельным обьективом табло.
 */
class LuaObjective(val scoreboard: Scoreboard, val objective: Objective) : LuaUserdata(objective) {

    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "javaClass", "class" -> JavaInstance(objective)

            "name" -> valueOf(objective.name)
            "display_name" -> valueOf(objective.displayName.string)
            "criteria" -> valueOf(objective.criteria.getName())
            "render_type" -> valueOf(objective.renderType.getId())

            "get_score", "getScore" -> GetScoreFunction()
            "set_score", "setScore" -> SetScoreFunction()
            "add_score", "addScore" -> AddScoreFunction()
            "increment_score", "incrementScore" -> IncrementScoreFunction()
            "reset_score", "remove_score", "resetScore", "removeScore" -> ResetScoreFunction()
            "list_scores", "listScores" -> ListScoresFunction()

            else -> super.get(key)
        }
    }

    override fun set(key: LuaValue, value: LuaValue) {
        when (key.tojstring()) {
            "display_name", "displayName" -> when {
                value.isstring() -> objective.displayName = Component.literal(value.tojstring())
                value is LuaComponent -> objective.displayName = value.component.copy()
                value is LuaComponentBuilder -> objective.displayName = value.buildComponent()
            }
            "render_type", "renderType" -> {
                if (value.isstring()) {
                    objective.renderType = ObjectiveCriteria.RenderType.byId(value.tojstring())
                }
            }
        }
    }

    override fun typename(): String = "objective"
    override fun tojstring(): String = "LuaObjective(${objective.name})"

    private fun holder(name: String): ScoreHolder = ScoreHolder.forNameOnly(name)

    private inner class GetScoreFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            if (arg == null || !arg.isstring()) return NIL
            val info = scoreboard.getPlayerScoreInfo(holder(arg.tojstring()), objective) ?: return NIL
            return valueOf(info.value())
        }
    }

    private inner class SetScoreFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            if (arg1 == null || !arg1.isstring() || arg2 == null || !arg2.isnumber()) return NIL
            return try {
                scoreboard.getOrCreatePlayerScore(holder(arg1.tojstring()), objective).set(arg2.toint())
                TRUE
            } catch (_: Exception) {
                FALSE
            }
        }
    }

    private inner class AddScoreFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            if (arg1 == null || !arg1.isstring() || arg2 == null || !arg2.isnumber()) return NIL
            return try {
                scoreboard.getOrCreatePlayerScore(holder(arg1.tojstring()), objective).add(arg2.toint())
                TRUE
            } catch (_: Exception) {
                FALSE
            }
        }
    }

    private inner class IncrementScoreFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            if (arg == null || !arg.isstring()) return NIL
            return try {
                valueOf(scoreboard.getOrCreatePlayerScore(holder(arg.tojstring()), objective).increment())
            } catch (_: Exception) {
                NIL
            }
        }
    }

    private inner class ResetScoreFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            if (arg == null || !arg.isstring()) return FALSE
            return try {
                scoreboard.resetSinglePlayerScore(holder(arg.tojstring()), objective)
                TRUE
            } catch (_: Exception) {
                FALSE
            }
        }
    }

    private inner class ListScoresFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = tableOf()
            var index = 1
            for (entry in scoreboard.listPlayerScores(objective)) {
                table.set(index++, LuaScoreboard.scoreEntryToTable(entry))
            }
            return table
        }
    }
}
