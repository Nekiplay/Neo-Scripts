package com.nekiplay.neoscripts.common.features.lua.objects.misc.catboost

import ai.catboost.CatBoostModel
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class CatBoostModelLua(val model: CatBoostModel) : LuaUserdata(model) {
    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "predict" -> Predict()
            "getFeatureNames" -> GetFeatureNames()
            "getFeatures" -> GetFeatures()
            else -> super.get(key)
        }
    }

    inner class GetFeatures : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val list = model.features

            val table = tableOf()

            list.forEachIndexed { index, feature ->
                val t = tableOf()
                t.set("name", valueOf(feature.name))
                t.set("index", valueOf(feature.featureIndex))
                t.set("flatIndex", valueOf(feature.flatFeatureIndex))
                table.set(index + 1, t)
            }
            return table
        }
    }

    inner class GetFeatureNames : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val list = model.featureNames

            val table = tableOf()

            list.forEachIndexed { index, string ->
                table.set(index + 1, valueOf(string))
            }
            return table
        }
    }

    inner class Predict : VarArgFunction() {
        override fun invoke(args: Varargs): LuaValue {
            val featuresTable = args.checktable(1)
            val features = ArrayList<Float>()
            featuresTable.keys().forEachIndexed { index, value ->
                run {
                    features.add(value.checkdouble().toFloat())
                }
            }

            val labelsTable = args.checktable(2)
            val labelsFeatures = ArrayList<String>()
            labelsTable.keys().forEachIndexed { index, value ->
                run {
                    labelsFeatures.add(value.tojstring())
                }
            }

            return CatBoostPredictionsLua(model.predict(features.toFloatArray(), labelsFeatures.toTypedArray()))
        }
    }
}
