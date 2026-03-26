package com.nekiplay.hypixelcry.features.lua.objects.misc

import ai.catboost.CatBoostModel
import com.nekiplay.hypixelcry.features.lua.objects.misc.catboost.CatBoostModelLua
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

class CatboostLib : TwoArgFunction() {
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val library = LuaTable()
        library.set("loadModel", LoadModel())
        //env.set("catboost", library)
        return library
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