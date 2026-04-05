package com.nekiplay.neoscripts.features.lua.objects.datatypes.phys

import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class LuaBox(val box: AABB) : LuaUserdata(box) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // --- Fields (Getters) ---
            "minX" -> valueOf(box.minX)
            "minY" -> valueOf(box.minY)
            "minZ" -> valueOf(box.minZ)
            "maxX" -> valueOf(box.maxX)
            "maxY" -> valueOf(box.maxY)
            "maxZ" -> valueOf(box.maxZ)

            // Tables for min/max positions
            "min" -> {
                val t = tableOf()
                t.set("x", valueOf(box.minX))
                t.set("y", valueOf(box.minY))
                t.set("z", valueOf(box.minZ))
                t
            }
            "max" -> {
                val t = tableOf()
                t.set("x", valueOf(box.maxX))
                t.set("y", valueOf(box.maxY))
                t.set("z", valueOf(box.maxZ))
                t
            }

            // --- Size Info ---
            "getSize" -> object : ZeroArgFunction() {
                override fun call(): LuaValue = valueOf(box.size)
            }
            "getXSize" -> object : ZeroArgFunction() {
                override fun call(): LuaValue = valueOf(box.xsize)
            }
            "getYSize" -> object : ZeroArgFunction() {
                override fun call(): LuaValue = valueOf(box.ysize)
            }
            "getZSize" -> object : ZeroArgFunction() {
                override fun call(): LuaValue = valueOf(box.zsize)
            }
            "getCenter" -> object : ZeroArgFunction() {
                override fun call(): LuaValue = vec3ToTable(box.center)
            }

            // --- Modification (Withers) ---
            "setMinX" -> object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue = LuaBox(box.setMinX(arg.checkdouble()))
            }
            "setMinY" -> object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue = LuaBox(box.setMinY(arg.checkdouble()))
            }
            "setMinZ" -> object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue = LuaBox(box.setMinZ(arg.checkdouble()))
            }
            "setMaxX" -> object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue = LuaBox(box.setMaxX(arg.checkdouble()))
            }
            "setMaxY" -> object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue = LuaBox(box.setMaxY(arg.checkdouble()))
            }
            "setMaxZ" -> object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue = LuaBox(box.setMaxZ(arg.checkdouble()))
            }

            // --- Operations ---

            // contract(x, y, z)
            "contract" -> object : ThreeArgFunction() {
                override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
                    return LuaBox(box.contract(arg1.checkdouble(), arg2.checkdouble(), arg3.checkdouble()))
                }
            }

            // expand(x, y, z) or expand(Vec3Table)
            "expand" -> object : VarArgFunction() {
                override fun invoke(args: Varargs): LuaValue {
                    if (args.narg() == 1 && args.arg1().istable()) {
                        val vec = tableToVec3(args.arg1().checktable())
                        return LuaBox(box.expandTowards(vec))
                    } else if (args.narg() >= 3) {
                        return LuaBox(box.expandTowards(args.arg(1).checkdouble(), args.arg(2).checkdouble(), args.arg(3).checkdouble()))
                    }
                    return NIL
                }
            }

            // inflate(value) or inflate(x, y, z)
            "inflate" -> object : VarArgFunction() {
                override fun invoke(args: Varargs): LuaValue {
                    return if (args.narg() == 1) {
                        LuaBox(box.inflate(args.arg1().checkdouble()))
                    } else {
                        LuaBox(box.inflate(args.arg(1).checkdouble(), args.arg(2).checkdouble(), args.arg(3).checkdouble()))
                    }
                }
            }

            // deflate(value) or deflate(x, y, z)
            "deflate" -> object : VarArgFunction() {
                override fun invoke(args: Varargs): LuaValue {
                    return if (args.narg() == 1) {
                        LuaBox(box.deflate(args.arg1().checkdouble()))
                    } else {
                        LuaBox(box.deflate(args.arg(1).checkdouble(), args.arg(2).checkdouble(), args.arg(3).checkdouble()))
                    }
                }
            }

            // intersect(LuaBox) -> returns new LuaBox representing the intersection
            "intersect" -> object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    if (arg is LuaBox) {
                        return LuaBox(box.intersect(arg.box))
                    }
                    return NIL
                }
            }

            // union(LuaBox) -> maps to minmax (bounding box of both)
            "union" -> object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    if (arg is LuaBox) {
                        return LuaBox(box.minmax(arg.box))
                    }
                    return NIL
                }
            }

            // move(x, y, z) or move(Vec3Table)
            "move" -> object : VarArgFunction() {
                override fun invoke(args: Varargs): LuaValue {
                    if (args.narg() == 1 && args.arg1().istable()) {
                        val vec = tableToVec3(args.arg1().checktable())
                        return LuaBox(box.move(vec))
                    } else if (args.narg() >= 3) {
                        return LuaBox(box.move(args.arg(1).checkdouble(), args.arg(2).checkdouble(), args.arg(3).checkdouble()))
                    }
                    return NIL
                }
            }

            // offset -> alias for move
            "offset" -> get("move")

            // --- Checks ---

            // intersects(LuaBox) or intersects(x1, y1, z1, x2, y2, z2)
            "intersects" -> object : VarArgFunction() {
                override fun invoke(args: Varargs): LuaValue {
                    if (args.narg() == 1 && args.arg1() is LuaBox) {
                        return valueOf(box.intersects((args.arg1() as LuaBox).box))
                    } else if (args.narg() >= 6) {
                        return valueOf(box.intersects(
                            args.arg(1).checkdouble(), args.arg(2).checkdouble(), args.arg(3).checkdouble(),
                            args.arg(4).checkdouble(), args.arg(5).checkdouble(), args.arg(6).checkdouble()
                        ))
                    }
                    return FALSE
                }
            }

            // contains(x, y, z) or contains(Vec3Table)
            "contains" -> object : VarArgFunction() {
                override fun invoke(args: Varargs): LuaValue {
                    if (args.narg() == 1 && args.arg1().istable()) {
                        val vec = tableToVec3(args.arg1().checktable())
                        return valueOf(box.contains(vec))
                    } else if (args.narg() >= 3) {
                        return valueOf(box.contains(args.arg(1).checkdouble(), args.arg(2).checkdouble(), args.arg(3).checkdouble()))
                    }
                    return FALSE
                }
            }

            // --- Calculation ---

            // clip(startVecTable, endVecTable) -> returns Optional hit Vec3Table or nil
            "clip" -> object : VarArgFunction() {
                override fun invoke(args: Varargs): LuaValue {
                    if (args.narg() >= 2) {
                        val start = tableToVec3(args.arg(1).checktable())
                        val end = tableToVec3(args.arg(2).checktable())
                        val result = box.clip(start, end)
                        if (result.isPresent) {
                            return vec3ToTable(result.get())
                        }
                    }
                    return NIL
                }
            }
            else -> super.get(key)
        }
    }

    // --- Helpers ---

    private fun vec3ToTable(vec: Vec3): LuaTable {
        val t = tableOf()
        t.set("x", valueOf(vec.x))
        t.set("y", valueOf(vec.y))
        t.set("z", valueOf(vec.z))
        return t
    }

    private fun tableToVec3(table: LuaTable): Vec3 {
        val x = table.get("x").checkdouble()
        val y = table.get("y").checkdouble()
        val z = table.get("z").checkdouble()
        return Vec3(x, y, z)
    }

    override fun typename(): String = "box"
}