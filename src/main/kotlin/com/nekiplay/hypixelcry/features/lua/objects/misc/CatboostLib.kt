package com.nekiplay.hypixelcry.features.lua.objects.misc

import ai.catboost.CatBoostModel
import com.nekiplay.hypixelcry.features.lua.objects.misc.catboost.CatBoostModelLua
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

class CatboostLib : LuaValue() {
    override fun typename(): String = "catboost"
    override fun tojstring(): String = "CatBoostObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "loadModel" -> LoadModel()
            else -> super.get(key)
        }
    }

    inner class LoadModel : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue {
            if (arg?.isstring() == true) {
                return CatBoostModelLua(CatBoostModel.loadModel(arg.tojstring()))
            }
            return NIL
        }
    }
}