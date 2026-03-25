package com.nekiplay.hypixelcry.features.lua.objects.misc.catboost

import ai.catboost.CatBoostModel
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua

class CatBoostModelLua(L: Lua, val model: CatBoostModel) : SimpleLuaWrapper(L) {

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "predict" -> JFunction { predict(it) }
            "getFeatureNames" -> JFunction { getFeatureNames(it) }
            "getFeatures" -> JFunction { getFeatures(it) }
            else -> null
        }
    }

    private fun getFeatures(l: Lua): Int {
        val list = model.features
        l.newTable() // Основная таблица

        list.forEachIndexed { index, feature ->
            l.newTable() // Таблица для конкретного feature

            l.push(feature.name)
            l.setField(-2, "name")

            l.push(feature.featureIndex.toDouble())
            l.setField(-2, "index")

            l.push(feature.flatFeatureIndex.toDouble())
            l.setField(-2, "flatIndex")

            // Кладем во внешнюю таблицу под индексом (index + 1)
            l.rawSetI(-2, (index + 1))
        }
        return 1
    }

    private fun getFeatureNames(l: Lua): Int {
        val list = model.featureNames
        l.newTable()

        list.forEachIndexed { index, name ->
            l.push(name)
            l.rawSetI(-2, (index + 1))
        }
        return 1
    }

    private fun predict(l: Lua): Int {
        // Проверяем наличие двух таблиц в аргументах
        if (!l.isTable(1) || !l.isTable(2)) {
            l.pushNil()
            return 1
        }

        // Логика из оригинала: извлекаем КЛЮЧИ первой таблицы как Float
        val features = ArrayList<Float>()
        l.pushNil() // Начальный ключ для итерации
        while (l.next(1) != 0) {
            // Ключ на -2, Значение на -1. Оригинал брал ключи (checkdouble).
            features.add(l.toNumber(-2).toFloat())
            l.pop(1) // Убираем значение, оставляем ключ для следующей итерации
        }

        // Логика из оригинала: извлекаем КЛЮЧИ второй таблицы как String
        val labelsFeatures = ArrayList<String>()
        l.pushNil()
        while (l.next(2) != 0) {
            labelsFeatures.add(l.toString(-2) ?: "")
            l.pop(1)
        }

        val result = model.predict(features.toFloatArray(), labelsFeatures.toTypedArray())

        // Предполагается, что CatBoostPredictionsLua тоже реализован через SimpleLuaWrapper
        CatBoostPredictionsLua(l, result).push()
        return 1
    }
}