package com.nekiplay.hypixelcry.features.lua.objects.misc.catboost

import ai.catboost.CatBoostPredictions
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class CatBoostPredictionsLua(val predictions: CatBoostPredictions) : LuaUserdata(predictions) {
    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "get" -> Get()
            "getObjectCount" -> GetObjectCount()
            else -> super.get(key)
        }
    }
    inner class Get : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val onjectIndex = args.checkint(1)
            val predictionIndex = args.checkint(2)

            predictions.objectCount

            return LuaValue.valueOf(predictions.get(onjectIndex, predictionIndex))
        }
    }

    inner class GetObjectCount : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return LuaValue.valueOf(predictions.objectCount)
        }
    }
}