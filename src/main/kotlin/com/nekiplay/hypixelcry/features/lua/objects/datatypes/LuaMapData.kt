package com.nekiplay.hypixelcry.features.lua.objects.datatypes

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import kotlin.jvm.optionals.getOrNull

class LuaMapData(L: Lua, val mapData: MapItemSavedData) : SimpleLuaWrapper(L) {

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "scale" -> mapData.scale.toDouble()
            "locked" -> mapData.locked
            "tracking_position" -> mapData.trackingPosition
            "unlimited_tracking" -> mapData.unlimitedTracking
            "dimension" -> mapData.dimension.identifier().toString()
            "center_x" -> mapData.centerX.toDouble()
            "center_z" -> mapData.centerZ.toDouble()
            "is_exploration_map" -> mapData.isExplorationMap
            "tracked_decoration_count" -> mapData.trackedDecorationCount.toDouble()

            "banners" -> getBanners(l)
            "decorations" -> getDecorations(l)
            "frame_markers" -> getFrameMarkers(l)
            "color_data" -> getColorData(l)
            "colors" -> getColorsArray(l)

            // Функции регистрируем как JFunction
            "getColor" -> JFunction { stack ->
                val x = stack.toInteger(1).toInt()
                val y = stack.toInteger(2).toInt()
                if (x in 0..127 && y in 0..127) {
                    stack.push(mapData.colors[x + y * 128].toInt().toDouble())
                    1
                } else 0
            }
            "updateColor" -> JFunction { stack ->
                val x = stack.toInteger(1).toInt()
                val y = stack.toInteger(2).toInt()
                val color = stack.toInteger(3).toByte()
                stack.push(mapData.updateColor(x, y, color))
                1
            }
            else -> null
        }
    }

    private fun getBanners(l: Lua): LuaValue {
        l.newTable()
        var index = 1
        mapData.banners.forEach { banner ->
            l.newTable()
            l.push(banner.id); l.setField(-2, "id")
            // В новых версиях MC работа с компонентами текста может отличаться
            l.push(banner.name?.getOrNull()?.string ?: ""); l.setField(-2, "name")
            l.push(banner.color.getName()); l.setField(-2, "color")
            l.push(banner.pos.x.toDouble()); l.setField(-2, "x")
            l.push(banner.pos.y.toDouble()); l.setField(-2, "y")
            l.push(banner.pos.z.toDouble()); l.setField(-2, "z")

            l.rawSetI(-2, index++)
        }
        return l.get()
    }

    private fun getDecorations(l: Lua): LuaValue {
        l.newTable()
        var index = 1
        mapData.decorations.forEach { decoration ->
            l.newTable()
            l.push(decoration.type().unwrapKey().get().identifier().toString()); l.setField(-2, "type")
            l.push(decoration.x().toDouble()); l.setField(-2, "x")
            l.push(decoration.y().toDouble()); l.setField(-2, "y")
            l.push(decoration.rot().toDouble()); l.setField(-2, "rot")
            decoration.name().ifPresent { name ->
                l.push(name.string); l.setField(-2, "name")
            }

            l.rawSetI(-2, index++)
        }
        return l.get()
    }

    private fun getFrameMarkers(l: Lua): LuaValue {
        l.newTable()
        var index = 1
        mapData.frameMarkers.forEach { (_, frame) ->
            l.newTable()
            l.push(frame.id); l.setField(-2, "id")
            l.push(frame.entityId.toDouble()); l.setField(-2, "entity_id")
            l.push(frame.pos.x.toDouble()); l.setField(-2, "x")
            l.push(frame.pos.y.toDouble()); l.setField(-2, "y")
            l.push(frame.pos.z.toDouble()); l.setField(-2, "z")
            l.push(frame.rotation.toDouble()); l.setField(-2, "rotation")

            l.rawSetI(-2, index++)
        }
        return l.get()
    }

    private fun getColorData(l: Lua): LuaValue {
        l.newTable() // Главная таблица [128][128]
        for (x in 0..127) {
            l.newTable() // Таблица ряда
            for (z in 0..127) {
                val color = mapData.colors[x + z * 128]
                if (color != 0.toByte()) {
                    l.newTable()
                    l.push(color.toDouble()); l.setField(-2, "value")
                    l.rawSetI(-2, z + 1)
                } else {
                    l.pushNil()
                    l.rawSetI(-2, z + 1)
                }
            }
            l.rawSetI(-2, x + 1)
        }
        return l.get()
    }

    private fun getColorsArray(l: Lua): LuaValue {
        l.newTable()
        for (i in mapData.colors.indices) {
            l.push(mapData.colors[i].toDouble())
            l.rawSetI(-2, i + 1)
        }
        return l.get()
    }

    override fun toString(): String {
        return "LuaMapData(centerX=${mapData.centerX}, centerZ=${mapData.centerZ}, scale=${mapData.scale}, dimension=${mapData.dimension})"
    }
}