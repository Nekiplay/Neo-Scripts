package com.nekiplay.hypixelcry.features.lua.objects.datatypes.phys

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaBox(l: Lua?, val box: AABB) : SimpleLuaWrapper(l) {
    override fun push(targetLua: Lua?) {
        super.push(targetLua)

        val lua = targetLua ?: L ?: return
        if (lua.getMetatable(-1) != 0) {
            lua.push(JFunction { l ->
                l.push("Box(${box.minX}, ${box.minY}, ${box.minZ} -> ${box.maxX}, ${box.maxY}, ${box.maxZ})")
                1
            })
            lua.setField(-2, "__tostring")
            lua.pop(1)
        }
    }

    override fun pushValue(targetLua: Lua?): LuaValue {
        push(targetLua)
        return (targetLua ?: L)!!.get()
    }

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            // --- Поля (Getters) ---
            "minX" -> box.minX
            "minY" -> box.minY
            "minZ" -> box.minZ
            "maxX" -> box.maxX
            "maxY" -> box.maxY
            "maxZ" -> box.maxZ

            // Таблицы min/max
            "min" -> vec3ToTable(l, box.minX, box.minY, box.minZ)
            "max" -> vec3ToTable(l, box.maxX, box.maxY, box.maxZ)

            // --- Размеры (Функции) ---
            "getSize" -> JFunction { s -> s.push(box.size); 1 }
            "getXSize" -> JFunction { s -> s.push(box.xsize); 1 }
            "getYSize" -> JFunction { s -> s.push(box.ysize); 1 }
            "getZSize" -> JFunction { s -> s.push(box.zsize); 1 }
            "getCenter" -> JFunction { s ->
                vec3ToTable(s, box.center.x, box.center.y, box.center.z)
                1
            }

            // --- Модификации (Возвращают новый LuaBox) ---
            "setMinX" -> JFunction { s -> LuaBox(l, box.setMinX(s.toNumber(1))).push(); 1 }
            "setMinY" -> JFunction { s -> LuaBox(l, box.setMinY(s.toNumber(1))).push(); 1 }
            "setMinZ" -> JFunction { s -> LuaBox(l, box.setMinZ(s.toNumber(1))).push(); 1 }
            "setMaxX" -> JFunction { s -> LuaBox(l, box.setMaxX(s.toNumber(1))).push(); 1 }
            "setMaxY" -> JFunction { s -> LuaBox(l, box.setMaxY(s.toNumber(1))).push(); 1 }
            "setMaxZ" -> JFunction { s -> LuaBox(l, box.setMaxZ(s.toNumber(1))).push(); 1 }

            "contract" -> JFunction { s ->
                LuaBox(l, box.contract(s.toNumber(1), s.toNumber(2), s.toNumber(3))).push()
                1
            }

            "expand" -> JFunction { s ->
                val result = if (s.isTable(1)) {
                    box.expandTowards(tableToVec3(s, 1))
                } else {
                    box.expandTowards(s.toNumber(1), s.toNumber(2), s.toNumber(3))
                }
                LuaBox(l, result).push()
                1
            }

            "inflate" -> JFunction { s ->
                val result = if (s.getTop() == 1) {
                    box.inflate(s.toNumber(1))
                } else {
                    box.inflate(s.toNumber(1), s.toNumber(2), s.toNumber(3))
                }
                LuaBox(l, result).push()
                1
            }

            "deflate" -> JFunction { s ->
                val result = if (s.getTop() == 1) {
                    box.deflate(s.toNumber(1))
                } else {
                    box.deflate(s.toNumber(1), s.toNumber(2), s.toNumber(3))
                }
                LuaBox(l, result).push()
                1
            }

            "intersect" -> JFunction { s ->
                val other = s.toJavaObject(1) as? LuaBox
                if (other != null) {
                    LuaBox(l, box.intersect(other.box)).push()
                    1
                } else 0
            }

            "union" -> JFunction { s ->
                val other = s.toJavaObject(1) as? LuaBox
                if (other != null) {
                    LuaBox(l, box.minmax(other.box)).push()
                    1
                } else 0
            }

            "move", "offset" -> JFunction { s ->
                val result = if (s.isTable(1)) {
                    box.move(tableToVec3(s, 1))
                } else {
                    box.move(s.toNumber(1), s.toNumber(2), s.toNumber(3))
                }
                LuaBox(l, result).push()
                1
            }

            // --- Проверки ---
            "intersects" -> JFunction { s ->
                val result = if (s.getTop() == 1) {
                    val other = s.toJavaObject(1) as? LuaBox
                    other?.let { box.intersects(it.box) } ?: false
                } else {
                    box.intersects(
                        s.toNumber(1), s.toNumber(2), s.toNumber(3),
                        s.toNumber(4), s.toNumber(5), s.toNumber(6)
                    )
                }
                s.push(result)
                1
            }

            "contains" -> JFunction { s ->
                val result = if (s.isTable(1)) {
                    box.contains(tableToVec3(s, 1))
                } else {
                    box.contains(s.toNumber(1), s.toNumber(2), s.toNumber(3))
                }
                s.push(result)
                1
            }

            "clip" -> JFunction { s ->
                val start = tableToVec3(s, 1)
                val end = tableToVec3(s, 2)
                val result = box.clip(start, end)
                if (result.isPresent) {
                    vec3ToTable(s, result.get().x, result.get().y, result.get().z)
                    1
                } else 0
            }

            else -> null
        }
    }

    // --- Помощники ---

    private fun vec3ToTable(l: Lua, x: Double, y: Double, z: Double): LuaValue {
        l.newTable()
        l.push(x); l.setField(-2, "x")
        l.push(y); l.setField(-2, "y")
        l.push(z); l.setField(-2, "z")
        return l.get()
    }

    private fun tableToVec3(l: Lua, index: Int): Vec3 {
        l.getField(index, "x"); val x = l.toNumber(-1); l.pop(1)
        l.getField(index, "y"); val y = l.toNumber(-1); l.pop(1)
        l.getField(index, "z"); val z = l.toNumber(-1); l.pop(1)
        return Vec3(x, y, z)
    }
}