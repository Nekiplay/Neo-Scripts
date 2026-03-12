package com.nekiplay.hypixelcry.features.lua.objects.misc

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaLong
import com.sun.jna.Callback
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.CallbackReference
import com.sun.jna.Function as JnaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

class FFILib : TwoArgFunction() {
    // Реестр типов для sizeof и cast
    val structRegistry = mutableMapOf<String, LuaStructDefinition>()
    val loadedLibraries = mutableMapOf<String, NativeLibrary>()

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val ffi = LuaTable()

        ffi.set("load", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val name = arg.checkjstring()
                // Если библиотека уже загружена, возвращаем её, иначе загружаем новую
                val lib = loadedLibraries.getOrPut(name) {
                    NativeLibrary.getInstance(name)
                }
                return NativeLibWrapper(lib, name)
            }
        })
        ffi.set("new_struct", object : TwoArgFunction() {
            override fun call(name: LuaValue, layout: LuaValue): LuaValue {
                val definition = LuaStructDefinition(layout.checktable())
                structRegistry[name.checkjstring()] = definition
                return definition
            }
        })
        ffi.set("new", object : TwoArgFunction() {
            override fun call(type: LuaValue, count: LuaValue): LuaValue {
                val t = type.checkjstring()
                val n = count.optint(1)

                val structDef = structRegistry[t]
                if (structDef != null) {
                    val mem = Memory(structDef.totalSize.toLong() * n)
                    mem.clear()
                    return LuaStructInstance(structDef, mem)
                }

                val typeSize = getTypeSize(t)
                if (typeSize <= 0) error("FFI: Unknown type or struct: $t")

                val size = typeSize * n
                return LuaPointer(Memory(size.toLong()), t)
            }
        })
        ffi.set("cast", object : TwoArgFunction() {
            override fun call(type: LuaValue, ptr: LuaValue): LuaValue {
                val t = type.checkjstring()
                val p = (ptr as? LuaPointer)?.memory ?: return NIL
                val def = structRegistry[t] ?: return LuaPointer(p, t)
                return LuaStructInstance(def, p)
            }
        })
        ffi.set("string", object : TwoArgFunction() {
            override fun call(ptr: LuaValue, len: LuaValue): LuaValue {
                val p = (ptr as? LuaPointer)?.memory ?: return NIL
                return if (len.isnil()) valueOf(p.getString(0))
                else valueOf(p.getByteArray(0, len.checkint()))
            }
        })
        ffi.set("sizeof", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val t = arg.checkjstring()
                val size = structRegistry[t]?.totalSize ?: getTypeSize(t)
                return valueOf(size)
            }
        })
        ffi.set("callback", object : ThreeArgFunction() {
            override fun call(func: LuaValue, retType: LuaValue, argTypes: LuaValue): LuaValue {
                return createCallback(func, retType.checkjstring(), argTypes.checktable())
            }
        })

        env.set("ffi", ffi)
        return ffi
    }

    class LuaPointer(val memory: Pointer, val type: String) : LuaTable() {
        init {
            // Добавляем метод :to_table(size) прямо в объект указателя
            set("to_table", object : TwoArgFunction() {
                override fun call(self: LuaValue, size: LuaValue): LuaValue {
                    val n = size.checkint()
                    val table = LuaTable()
                    val elementSize = getTypeSize(type).toLong()

                    for (i in 0 until n) {
                        val offset = i * elementSize
                        val value = when (type) {
                            "int" -> valueOf(memory.getInt(offset))
                            "float" -> valueOf(memory.getFloat(offset).toDouble())
                            "double" -> valueOf(memory.getDouble(offset))
                            else -> LuaPointer(memory.getPointer(offset), "void")
                        }
                        table.set(i + 1, value) // В Lua индексы с 1
                    }
                    return table
                }
            })
        }

        override fun get(key: LuaValue): LuaValue {
            if (key.isint()) {
                val index = key.checkint()
                val offset = index * getTypeSize(type).toLong()
                return when (type) {
                    "int" -> valueOf(memory.getInt(offset))
                    "float" -> valueOf(memory.getFloat(offset).toDouble())
                    "double" -> valueOf(memory.getDouble(offset))
                    else -> LuaPointer(memory.getPointer(offset), "void")
                }
            }
            return super.get(key)
        }
    }

    class LuaStructDefinition(val layout: LuaTable) : LuaUserdata(layout) {
        val fields = mutableListOf<FieldInfo>()
        val totalSize: Int
        init {
            var offset = 0
            for (i in 1..layout.length()) {
                val pair = layout.get(i).checktable()
                val size = getTypeSize(pair.get(2).tojstring())
                if (offset % size != 0) offset += size - (offset % size)
                fields.add(FieldInfo(pair.get(1).tojstring(), pair.get(2).tojstring(), offset, size))
                offset += size
            }
            totalSize = offset
        }
        fun allocate() = LuaStructInstance(this, Memory(totalSize.toLong()))
    }

    data class FieldInfo(val name: String, val type: String, val offset: Int, val size: Int)

    private fun invokeLua(func: LuaValue, args: Array<Any?>, retType: String, argTypes: LuaTable): Any? {
        val luaArgs = Array<LuaValue>(args.size) { i ->
            val rawVal = args[i] as Long // JNA передает аргументы как Long
            val typeName = argTypes.get(i + 1).tojstring()

            val structDef = structRegistry[typeName]

            if (structDef != null) {
                LuaStructInstance(structDef, Pointer(rawVal))
            } else {
                when (typeName) {
                    "int" -> valueOf(rawVal.toInt())
                    "float" -> valueOf(java.lang.Float.intBitsToFloat(rawVal.toInt()).toDouble())
                    "double" -> valueOf(java.lang.Double.longBitsToDouble(rawVal))
                    "ptr", "callback" -> LuaPointer(Pointer(rawVal), "void")
                    "string" -> {
                        val p = Pointer(rawVal)
                        if (rawVal == 0L) NIL else valueOf(p.getString(0))
                    }
                    // Исправленный fallback: преобразуем Long в Double для Lua
                    else -> valueOf(rawVal.toDouble())
                }
            }
        }

        val result = func.invoke(LuaValue.varargsOf(luaArgs))
        if (retType == "void") return null
        return convertLuaToNative(result.arg1(), retType)
    }

    class LuaStructInstance(val definition: LuaStructDefinition, val memory: Pointer) : LuaTable() {
        override fun get(key: LuaValue): LuaValue {
            val f = definition.fields.find { it.name == key.tojstring() } ?: return super.get(key)
            return when (f.type) {
                "int" -> valueOf(memory.getInt(f.offset.toLong()))
                "float" -> valueOf(memory.getFloat(f.offset.toLong()).toDouble())
                "double" -> valueOf(memory.getDouble(f.offset.toLong()))
                "string" -> valueOf(memory.getString(f.offset.toLong()))
                else -> NIL
            }
        }
        override fun set(key: LuaValue, value: LuaValue) {
            val f = definition.fields.find { it.name == key.tojstring() }
            if (f != null) {
                when (f.type) {
                    "int" -> memory.setInt(f.offset.toLong(), value.checkint())
                    "float" -> memory.setFloat(f.offset.toLong(), value.checkdouble().toFloat())
                    "double" -> memory.setDouble(f.offset.toLong(), value.checkdouble())
                }
            } else super.set(key, value)
        }
    }

    private fun createCallback(func: LuaValue, retType: String, argTypes: LuaTable): LuaValue {
        val argCount = argTypes.length()

        // We create a specific interface implementation based on the number of arguments
        val callback = when (argCount) {
            0 -> object : Callback {
                fun callback(): Any? =
                    invokeLua(func, emptyArray(), retType, argTypes)
            }
            1 -> object : Callback {
                fun callback(a1: Any?): Any? =
                    invokeLua(func, arrayOf(a1), retType, argTypes)
            }
            2 -> object : Callback {
                fun callback(a1: Any?, a2: Any?): Any? =
                    invokeLua(func, arrayOf(a1, a2), retType, argTypes)
            }
            3 -> object : Callback {
                fun callback(a1: Any?, a2: Any?, a3: Any?): Any? =
                    invokeLua(func, arrayOf(a1, a2, a3), retType, argTypes)
            }
            4 -> object : Callback {
                fun callback(a1: Any?, a2: Any?, a3: Any?, a4: Any?): Any? =
                    invokeLua(func, arrayOf(a1, a2, a3, a4), retType, argTypes)
            }
            5 -> object : Callback {
                fun callback(a1: Any?, a2: Any?, a3: Any?, a4: Any?, a5: Any?): Any? =
                    invokeLua(func, arrayOf(a1, a2, a3, a4, a5), retType, argTypes)
            }
            6 -> object : Callback {
                fun callback(a1: Any?, a2: Any?, a3: Any?, a4: Any?, a5: Any?, a6: Any?): Any? =
                    invokeLua(func, arrayOf(a1, a2, a3, a4, a5, a6), retType, argTypes)
            }
            7 -> object : Callback {
                fun callback(a1: Any?, a2: Any?, a3: Any?, a4: Any?, a5: Any?, a6: Any?, a7: Any?): Any? =
                    invokeLua(func, arrayOf(a1, a2, a3, a4, a5, a6, a7), retType, argTypes)
            }
            else -> error("FFI: Unsupported callback argument count: $argCount")
        }

        return LuaUserdata(callback)
    }

    inner class NativeLibWrapper(val lib: NativeLibrary, val libName: String) : LuaTable() {
        init {
            set("bind", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    val self = args.checktable(1)
                    val name = args.checkjstring(2)
                    val ret = args.checkjstring(3)
                    val params = args.checktable(4)

                    val jnaFunc = lib.getFunction(name)
                    val luaFunc = NativeFunction(jnaFunc, ret, params)
                    self.set(name, luaFunc)
                    return luaFunc
                }
            })

            set("unload", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    loadedLibraries.remove(libName)
                    lib.dispose()
                    return NIL
                }
            })
        }
    }

    inner class NativeFunction(
        val jnaFunc: JnaFunction,
        val retType: String,
        val argTypes: LuaTable
    ) : VarArgFunction() {

        override fun invoke(args: Varargs): Varargs {
            val count = argTypes.length()
            val jnaArgs = arrayOfNulls<Any>(count)

            for (i in 0 until count) {
                val type = argTypes.get(i + 1).checkjstring()
                val luaVal = args.arg(i + 1)

                if (luaVal.isnil()) error("FFI: Argument ${i + 1} for '${jnaFunc.name}' is nil")

                jnaArgs[i] = when (luaVal) {
                    is LuaUserdata if luaVal.userdata() is Callback -> {
                        CallbackReference.getFunctionPointer(luaVal.userdata() as Callback)
                    }
                    is LuaStructInstance -> {
                        luaVal.memory
                    }
                    is LuaPointer -> {
                        luaVal.memory
                    }
                    else -> {
                        convertLuaToNative(luaVal, type)
                    }
                }
            }

            return try {
                val result = jnaFunc.invoke(mapType(retType), jnaArgs)
                convertNativeToLua(result, retType)
            } catch (e: Exception) {
                error("FFI: Error calling '${jnaFunc.name}': ${e.message}")
            }
        }
    }

    companion object {
        fun getTypeSize(type: String) = when (type) {
            "int", "float" -> 4
            "long", "double", "ptr", "callback", "string" -> 8
            else -> 0
        }

        fun mapType(type: String): Class<*> = when (type) {
            "int" -> Integer.TYPE
            "long" -> java.lang.Long.TYPE
            "double" -> java.lang.Double.TYPE
            "float" -> java.lang.Float.TYPE
            "bool" -> java.lang.Boolean.TYPE
            "string" -> String::class.java
            "void" -> java.lang.Void.TYPE
            else -> Pointer::class.java
        }

        // Чтение из памяти с учетом типа
        fun readTypeAt(memory: Pointer, offset: Long, type: String): LuaValue {
            return when (type) {
                "int" -> valueOf(memory.getInt(offset))
                "long" -> LuaLong(memory.getLong(offset))
                "float" -> valueOf(memory.getFloat(offset).toDouble())
                "double" -> valueOf(memory.getDouble(offset))
                "string" -> valueOf(memory.getString(offset))
                "ptr" -> LuaPointer(memory.getPointer(offset), "void")
                else -> NIL
            }
        }

        // Конвертация Long из стека каллбэка в LuaValue
        fun readTypeFromLong(rawVal: Long, type: String): LuaValue {
            return when (type) {
                "int" -> valueOf(rawVal.toInt())
                "long" -> LuaLong(rawVal)
                "float" -> valueOf(java.lang.Float.intBitsToFloat(rawVal.toInt()).toDouble())
                "double" -> valueOf(java.lang.Double.longBitsToDouble(rawVal))
                "ptr", "callback" -> if (rawVal == 0L) NIL else LuaPointer(Pointer(rawVal), "void")
                "string" -> if (rawVal == 0L) NIL else valueOf(Pointer(rawVal).getString(0))
                else -> LuaLong(rawVal)
            }
        }

        fun convertLuaToNative(v: LuaValue, type: String): Any? {
            if (v.isnil()) return null
            return when (type) {
                "int" -> v.toint()
                "long" -> if (v is LuaLong) v.value else v.tolong()
                "double" -> v.todouble()
                "float" -> v.todouble().toFloat()
                "bool" -> v.toboolean()
                "string" -> v.tojstring()
                "ptr" -> when(v) {
                    is LuaPointer -> v.memory
                    is LuaStructInstance -> v.memory
                    is LuaUserdata -> v.userdata() as? Pointer
                    else -> null
                }
                "callback" -> if (v is LuaUserdata && v.userdata() is Callback)
                    CallbackReference.getFunctionPointer(v.userdata() as Callback)
                else null
                else -> null
            }
        }

        fun convertNativeToLua(v: Any?, type: String): LuaValue {
            if (v == null) return NIL
            return when (v) {
                is Int -> valueOf(v)
                is Long -> LuaLong(v)
                is Double -> valueOf(v)
                is Float -> valueOf(v.toDouble())
                is Boolean -> valueOf(v)
                is String -> valueOf(v)
                is Pointer -> LuaPointer(v, "void")
                else -> NIL
            }
        }
    }
}