package com.nekiplay.neoscripts.features.lua.objects.datatypes

import com.nekiplay.neoscripts.sugar.getFormattedString
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.jvm.optionals.getOrNull

class LuaMapData(val mapData: MapItemSavedData) : LuaUserdata(mapData) {

    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "scale" -> valueOf(mapData.scale.toDouble())
            "locked" -> valueOf(mapData.locked)
            "dimension" -> valueOf(mapData.dimension.identifier().toString())
            "center_x" -> valueOf(mapData.centerX.toDouble())
            "center_z" -> valueOf(mapData.centerZ.toDouble())
            "is_exploration_map" -> valueOf(mapData.isExplorationMap)
            "banners" -> getBanners()
            "decorations" -> getDecorations()
            "color_data" -> getColorData()
            "colors" -> getColorsArray()
            "getColor" -> GetColor()
            "updateColor" -> UpdateColor()
            else -> super.get(key)
        }
    }

    private fun getBanners(): LuaValue {
        val bannersTable = tableOf()
        var index = 1

        mapData.banners.forEach { banner ->
            val bannerTable = tableOf()
            bannerTable.set("id", banner.id)
            bannerTable.set("name", banner.name?.getOrNull()?.getFormattedString() ?: "")
            bannerTable.set("color", banner.color.getName())
            bannerTable.set("x", banner.pos.x.toDouble())
            bannerTable.set("z", banner.pos.z.toDouble())
            bannerTable.set("y", banner.pos.y.toDouble())
            bannersTable.set(index, bannerTable)
            index++
        }

        return bannersTable
    }

    private fun getDecorations(): LuaValue {
        val decorationsTable = tableOf()
        var index = 1

        mapData.decorations.forEach { decoration ->
            val decorationTable = tableOf()
            decorationTable.set("type", decoration.type().unwrapKey().get().identifier().toString())
            decorationTable.set("x", decoration.x().toDouble())
            decorationTable.set("y", decoration.y().toDouble())
            decorationTable.set("rot", decoration.rot().toDouble())
            decoration.name().ifPresent { name ->
                decorationTable.set("name", name.string)
            }
            decorationsTable.set(index, decorationTable)
            index++
        }

        return decorationsTable
    }

    private fun getColorData(): LuaValue {
        val colorsTable = tableOf()

        for (x in 0..127) {
            val rowTable = tableOf()
            for (z in 0..127) {
                val color = mapData.colors[x + z * 128]
                if (color != 0.toByte()) {
                    val colorTable = tableOf()
                    colorTable.set("value", color.toDouble())
                    rowTable.set(z + 1, colorTable)
                } else {
                    rowTable.set(z + 1, NIL)
                }
            }
            colorsTable.set(x + 1, rowTable)
        }

        return colorsTable
    }

    private fun getColorsArray(): LuaValue {
        val colorsArray = tableOf()

        for (i in mapData.colors.indices) {
            colorsArray.set(i + 1, valueOf(mapData.colors[i].toDouble()))
        }

        return colorsArray
    }

    private inner class GetColor : TwoArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?
        ): LuaValue {
            if (arg1?.isnumber() == true && arg2?.isnumber() == true) {
                return valueOf(mapData.colors[arg1.toint() + arg2.toint() * 128].toInt())
            }
            return NIL
        }

    }

    private inner class UpdateColor : ThreeArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue?,
            arg3: LuaValue?
        ): LuaValue {
            if (arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true) {
                return valueOf(mapData.updateColor(arg1.toint(), arg2.toint(), arg3.tobyte()))
            }
            return NIL
        }

    }

    override fun toString(): String {
        return "LuaMapData(centerX=${mapData.centerX}, centerZ=${mapData.centerZ}, scale=${mapData.scale}, dimension=${mapData.dimension})"
    }

    override fun typename(): String = "map"
}