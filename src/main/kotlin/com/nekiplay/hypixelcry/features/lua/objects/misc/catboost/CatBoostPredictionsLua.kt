package com.nekiplay.hypixelcry.features.lua.objects.misc.catboost

import ai.catboost.CatBoostPredictions
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua

class CatBoostPredictionsLua(L: Lua, val predictions: CatBoostPredictions) : SimpleLuaWrapper(L) {

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "get" -> JFunction { getPrediction(it) }
            "getObjectCount" -> JFunction { getObjectCount(it) }
            else -> null
        }
    }

    private fun getPrediction(l: Lua): Int {
        // Проверяем наличие двух числовых аргументов (objectIndex и predictionIndex)
        if (l.isNumber(1) && l.isNumber(2)) {
            val objectIndex = l.toInteger(1).toInt()
            val predictionIndex = l.toInteger(2).toInt()

            // Получаем значение и пушим его в стек
            l.push(predictions.get(objectIndex, predictionIndex))
        } else {
            l.pushNil()
        }
        return 1
    }

    private fun getObjectCount(l: Lua): Int {
        // Возвращаем количество объектов
        l.push(predictions.objectCount.toDouble())
        return 1
    }
}