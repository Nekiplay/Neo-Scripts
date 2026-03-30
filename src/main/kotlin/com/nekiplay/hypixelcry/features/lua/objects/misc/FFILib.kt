package com.nekiplay.hypixelcry.features.lua.objects.misc

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaLong
import com.sun.jna.Callback
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.CallbackReference
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.luaj.vm2.LuaNumber
import org.luaj.vm2.LuaString
import com.sun.jna.Function as JnaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class CField(
    val name: String,
    val type: CType,
    val offset: Int,
    val bitOffset: Int = 0,
    val bitSize: Int = 0
)

data class CType(
    val name: String,
    val size: Int,
    val nativeClass: Class<*>,
    val fields: Map<String, CField>? = null,
    val alignment: Int = -1,
    val isUnion: Boolean = false,
    val arraySize: Int = 0,
    val isPacked: Boolean = false,
    val enumValues: Map<String, Int>? = null,
    val isEnum: Boolean = false
)

data class CFunctionSignature(
    val name: String,
    val returnType: CType,
    val argTypes: List<CType>
)

class FFILib : LuaTable() {

    private val typeRegistry = ConcurrentHashMap<String, CType>()
    private val loadedLibraries = ConcurrentHashMap<String, NativeLibrary>()
    private val callbackCache = ConcurrentHashMap<Long, WeakReference<Callback>>()
    private val gcEntries = ConcurrentHashMap<Long, Pair<Pointer, LuaValue>>()
    private val metatypeRegistry = ConcurrentHashMap<String, LuaTable>()
    private val typeOfRegistry = ConcurrentHashMap<String, CType>()
    private val functionRegistry = ConcurrentHashMap<String, CFunctionSignature>()
    private var debugMode = AtomicBoolean(false)


    init {
        registerBasicTypes()

        set("cdef", CDefFunction())
        set("load", LoadFunction())
        set("new", NewFunction())
        set("cast", CastFunction())
        set("sizeof", SizeOfFunction())
        set("string", StringFunction())
        set("callback", CallbackFunction())
        set("gc", GcFunction())
        val osName = System.getProperty("os.name").lowercase()
        val osType = when {
            osName.contains("win") -> "Windows"
            osName.contains("mac") -> "OSX"
            osName.contains("bsd") -> "BSD"
            else -> "Linux"
        }
        set("os", valueOf(osType))

        set("copy", CopyFunction())
        set("fill", FillFunction())
        set("typeof", TypeOfFunction())
        set("metatype", MetatypeFunction())
        set("C", NativeLibWrapper(NativeLibrary.getProcess(), "C"))
    }

    private fun registerBasicTypes() {
        val types = listOf(
            CType("int", 4, Integer.TYPE),
            CType("uint32_t", 4, Integer.TYPE),
            CType("int32_t", 4, Integer.TYPE),
            CType("long", 8, java.lang.Long.TYPE),
            CType("int64_t", 8, java.lang.Long.TYPE),
            CType("uint64_t", 8, java.lang.Long.TYPE),
            CType("size_t", Native.SIZE_T_SIZE, java.lang.Long.TYPE),
            CType("float", 4, java.lang.Float.TYPE),
            CType("double", 8, java.lang.Double.TYPE),
            CType("char", 1, java.lang.Byte.TYPE),
            CType("bool", 1, java.lang.Boolean.TYPE),
            CType("void", 0, java.lang.Void.TYPE),
            CType("ptr", Native.POINTER_SIZE, Pointer::class.java),
            CType("uintptr_t", Native.POINTER_SIZE, Pointer::class.java)
        )
        types.forEach { typeRegistry[it.name] = it }
    }

    private fun registerEnum(name: String, values: Map<String, Int>, size: Int) {
        typeRegistry[name] = CType(name, size, Integer.TYPE, enumValues = values, isEnum = true)
    }

    private fun parseCDef(cdefString: String): CType? {
        // Очищаем строку от комментариев и лишних пробелов
        val cleanCdef = cdefString
            .replace(Regex("//.*"), "")
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .trim()

        log("parseCDef input: $cleanCdef")

        // 1. Парсинг функций: "int printf(const char* format, ...);"
        // Ищем паттерн: тип_возврата имя_функции ( аргументы );
        val funcRegex = Regex("""([\w\*]+)\s+(\w+)\s*\(([^)]*)\)\s*;""")
        val funcMatch = funcRegex.find(cleanCdef)
        if (funcMatch != null) {
            val retTypeName = funcMatch.groupValues[1].replace("*", "").trim()
            val funcName = funcMatch.groupValues[2]
            val argsStr = funcMatch.groupValues[3]

            val retType = typeRegistry[retTypeName] ?: typeRegistry["ptr"]!!
            val argTypes = mutableListOf<CType>()

            if (argsStr.trim().isNotEmpty() && argsStr.trim() != "void") {
                argsStr.split(",").forEach { arg ->
                    val parts = arg.trim().split(Regex("\\s+"))
                    val typePart = parts[0].replace("*", "").trim()
                    argTypes.add(typeRegistry[typePart] ?: typeRegistry["ptr"]!!)
                }
            }

            functionRegistry[funcName] = CFunctionSignature(funcName, retType, argTypes)
            log("Registered function signature: $funcName")
            return null // Функции не возвращают CType
        }

        // 2. Парсинг Typedef: "typedef int HWND;"
        if (cleanCdef.startsWith("typedef")) {
            val typedefRegex = Regex("""typedef\s+(.+?)\s+(\w+)\s*;""")
            val typedefMatch = typedefRegex.find(cleanCdef)
            if (typedefMatch != null) {
                val baseTypeStr = typedefMatch.groupValues[1].replace("*", "").trim()
                val newName = typedefMatch.groupValues[2]
                val base = typeRegistry[baseTypeStr] ?: typeRegistry["ptr"]!!
                val newType = base.copy(name = newName)
                typeRegistry[newName] = newType
                return newType
            }
        }

        // 3. Парсинг Enum
        val enumRegex = Regex("""enum\s+(\w+)?\s*\{([^}]+)\}""")
        val enumMatch = enumRegex.find(cleanCdef)
        if (enumMatch != null) {
            val enumName = enumMatch.groupValues[1] ?: "anonymous_enum_${System.currentTimeMillis()}"
            val valuesStr = enumMatch.groupValues[2]
            val values = mutableMapOf<String, Int>()
            var currentValue = 0
            valuesStr.split(",").forEach { pair ->
                if (pair.isBlank()) return@forEach
                val parts = pair.trim().split("=")
                val name = parts[0].trim()
                currentValue = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: currentValue else currentValue
                values[name] = currentValue++
            }
            val enumType = CType(enumName, 4, Integer.TYPE, enumValues = values, isEnum = true)
            typeRegistry[enumName] = enumType
            return enumType
        }

        // 4. Парсинг Struct / Union
        val structRegex = Regex("""(struct|union)\s+(\w+)\s*\{([\s\S]*?)\s*\}(?:\s*__attribute__\(\(packed\)\))?""")
        val structMatch = structRegex.find(cleanCdef)
        if (structMatch != null) {
            val kind = structMatch.groupValues[1]
            val structName = structMatch.groupValues[2]
            val body = structMatch.groupValues[3]
            val isPacked = cleanCdef.contains("__attribute__((packed))")
            val isUnion = kind == "union"

            val type = parseStructBody(structName, body, isPacked, isUnion)
            typeRegistry[structName] = type
            return type
        }

        log("parseCDef: no match found for '$cleanCdef'")
        return null
    }

    private fun parseStructBody(name: String, body: String, isPacked: Boolean, isUnion: Boolean = false): CType {
        val fields = mutableMapOf<String, CField>()
        var offset = 0
        var maxSize = 0

        val lines = body.split(";").filter { it.trim().isNotEmpty() }
        for (line in lines) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 2) continue

            val typeName = parts[0]
            val fieldNameWithArray = parts[1]
            
            val arrayMatch = Regex("""(\w+)\[(\d+)\]""").find(fieldNameWithArray)
            val fieldName = arrayMatch?.groupValues?.get(1) ?: fieldNameWithArray
            val arraySize = arrayMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

            var fieldType = typeRegistry[typeName]
            
            if (arraySize > 0 && fieldType != null) {
                fieldType = CType(
                    name = "${fieldType.name}[$arraySize]",
                    size = fieldType.size * arraySize,
                    nativeClass = fieldType.nativeClass,
                    arraySize = arraySize
                )
            }

            if (fieldType == null) continue

            val bitfieldMatch = Regex(""":(\d+)""").find(fieldNameWithArray)
            val bitSize = bitfieldMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            val actualFieldType = if (bitSize > 0) {
                fieldType.copy(name = fieldType.name)
            } else fieldType

            if (isUnion) {
                fields[fieldName] = CField(fieldName, actualFieldType, 0, if (bitSize > 0) 0 else 0, bitSize)
                if (actualFieldType.size > maxSize) maxSize = actualFieldType.size
            } else {
                var align = if (isPacked) 1 else actualFieldType.size
                if (offset % align != 0) offset += align - (offset % align)
                
                fields[fieldName] = CField(fieldName, actualFieldType, offset, 0, bitSize)
                offset += actualFieldType.size
                if (actualFieldType.size > maxSize) maxSize = actualFieldType.size
            }
        }

        var structSize = if (isUnion) maxSize else offset
        if (!isPacked && structSize % 8 != 0) structSize += 8 - (structSize % 8)

        return CType(name, structSize, Pointer::class.java, fields, isPacked = isPacked, isUnion = isUnion)
    }

    inner class CDefFunction : TwoArgFunction() {
        override fun call(arg: LuaValue, config: LuaValue?): LuaValue {
            if (config != null && !config.isnil()) {
                val name = config.get("name").checkjstring()
                val layout = config.get("layout").checktable()

                val fields = mutableMapOf<String, CField>()
                var offset = 0
                var maxSize = 0
                val isPacked = config.get("packed").toboolean()
                val isUnion = config.get("union").toboolean()

                for (i in 1..layout.length()) {
                    val f = layout.get(i).checktable()
                    val fName = f.get(1).checkjstring()
                    val fTypeName = f.get(2).checkjstring()
                    val fType = typeRegistry[fTypeName] ?: return error("FFI: Unknown type $fTypeName")

                    if (isUnion) {
                        fields[fName] = CField(fName, fType, 0)
                        if (fType.size > maxSize) maxSize = fType.size
                    } else {
                        if (!isPacked && offset % fType.size != 0) offset += fType.size - (offset % fType.size)
                        fields[fName] = CField(fName, fType, offset)
                        offset += fType.size
                        if (fType.size > maxSize) maxSize = fType.size
                    }
                }

                val structSize = if (isUnion) maxSize else offset
                typeRegistry[name] = CType(name, structSize, Pointer::class.java, fields, isPacked = isPacked, isUnion = isUnion)
                return valueOf(name)
            }

            val cdefString = arg.checkjstring()
            val parsed = parseCDef(cdefString)
            if (parsed != null) {
                typeRegistry[parsed.name] = parsed
                log("Registered type: ${parsed.name}")
                return valueOf(parsed.name)
            }
            return error("FFI: Failed to parse cdef")
        }
    }

    inner class NewFunction : TwoArgFunction() {
        override fun call(typeArg: LuaValue, count: LuaValue): LuaValue {
            val typeName = typeArg.checkjstring()
            var cType = typeRegistry[typeName] ?: typeOfRegistry[typeName] ?: return error("FFI: Type $typeName not found")
            
            val n = if (count.isnil()) 1 else count.checkint()

            val mem = Memory(cType.size.toLong() * n)
            mem.clear()
            return CData(mem as Pointer, cType, this@FFILib)
        }
    }

    inner class CastFunction : TwoArgFunction() {
        override fun call(typeArg: LuaValue, ptr: LuaValue): LuaValue {
            val typeName = typeArg.checkjstring()
            val cType = typeRegistry.get(typeName) ?: typeOfRegistry.get(typeName) ?: return error("FFI: Type $typeName not found")

            val p: Pointer = when (ptr) {
                is CData -> ptr.peer
                is LuaNumber -> Pointer(ptr.tolong())
                is LuaUserdata -> ptr.touserdata() as Pointer
                else -> Pointer.NULL
            }
            return CData(p, cType, this@FFILib)
        }
    }

    inner class CallbackFunction : TwoArgFunction() {
        override fun call(proto: LuaValue, func: LuaValue): LuaValue {
            val protoStr = proto.checkjstring()
            val signature = parseCallbackSignature(protoStr)

            // Создаем прокси-объект колбэка с поддержкой возвращаемого значения
            val callbackObj = object : Callback {
                // JNA ищет метод с именем "callback" или единственным public методом
                fun callback(vararg args: Any?): Any? {
                    val luaArgs = args.map { arg ->
                        when (arg) {
                            is Int -> valueOf(arg)
                            is Long -> valueOf(arg.toDouble())
                            is Pointer -> CData(arg, typeRegistry["ptr"]!!, this@FFILib)
                            else -> NIL
                        }
                    }

                    val res = if (luaArgs.isEmpty()) func.call() else func.invoke(LuaValue.varargsOf(luaArgs.toTypedArray())).arg1()

                    // Возвращаем результат обратно в C (Пункт 4)
                    return when (signature.returnType.nativeClass) {
                        Integer.TYPE -> res.toint()
                        java.lang.Long.TYPE -> res.tolong()
                        java.lang.Double.TYPE -> res.todouble()
                        Pointer::class.java -> (res as? CData)?.peer ?: Pointer.NULL
                        else -> null
                    }
                }
            }

            val ptr = CallbackReference.getFunctionPointer(callbackObj)
            callbackCache[ptr.hashCode().toLong()] = WeakReference(callbackObj)
            return CData(ptr, typeRegistry["ptr"]!!, this@FFILib)
        }

        private fun parseCallbackSignature(proto: String): CFunctionSignature {
            val regex = Regex("""(\w+)\s*\((.*)\)""")
            val match = regex.find(proto) ?: throw Exception("Invalid callback proto")
            val retType = typeRegistry[match.groupValues[1]] ?: typeRegistry["int"]!!
            return CFunctionSignature("cb", retType, emptyList())
        }
    }

    inner class GcFunction : TwoArgFunction() {
        override fun call(ptr: LuaValue, finalizer: LuaValue): LuaValue {
            if (ptr !is CData) return error("FFI.gc: expected cdata")
            val addr = ptr.peer.hashCode().toLong()
            gcEntries[addr] = Pair(ptr.peer, finalizer)
            return ptr
        }
    }

    inner class CopyFunction : ThreeArgFunction() {
        override fun call(dest: LuaValue, src: LuaValue, len: LuaValue): LuaValue {
            val destPtr = when (dest) {
                is CData -> dest.peer
                is LuaNumber -> Pointer(dest.tolong())
                else -> return NIL
            }
            
            when (src) {
                is LuaString -> {
                    val srcStr = src.tojstring()
                    val srcBytes = srcStr.toByteArray()
                    val copyLen = if (len.isnil()) srcBytes.size else minOf(len.checkint(), srcBytes.size)
                    destPtr.write(0, srcBytes, 0, copyLen)
                    destPtr.setByte(copyLen.toLong(), 0.toByte())
                    return NIL
                }
                is CData -> {
                    val srcPtr = src.peer
                    val length = if (len.isnil()) 1024 else len.checkint()
                    val srcArray = srcPtr.getByteArray(0, length)
                    destPtr.write(0, srcArray, 0, length)
                }
                is LuaNumber -> {
                    val srcPtr = Pointer(src.tolong())
                    val length = if (len.isnil()) 1024 else len.checkint()
                    val srcArray = srcPtr.getByteArray(0, length)
                    destPtr.write(0, srcArray, 0, length)
                }
                else -> return NIL
            }
            
            return NIL
        }
    }

    inner class FillFunction : ThreeArgFunction() {
        override fun call(dest: LuaValue, len: LuaValue, value: LuaValue): LuaValue {
            val destPtr = when (dest) {
                is CData -> dest.peer
                is LuaNumber -> Pointer(dest.tolong())
                else -> return NIL
            }
            val length = len.checkint()
            val fillByte = if (value.isnil()) 0 else value.checkint().toByte()

            for (i in 0 until length) {
                destPtr.setByte(i.toLong(), fillByte)
            }
            return NIL
        }
    }

    inner class TypeOfFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val cType = when (arg) {
                is CData -> arg.cType
                is LuaString -> typeRegistry[arg.tojstring()]
                else -> null
            } ?: return NIL
            
            typeOfRegistry[cType.name] = cType
            return LuaCType(cType, this@FFILib)
        }
    }

    inner class MetatypeFunction : TwoArgFunction() {
        override fun call(cdata: LuaValue, mt: LuaValue): LuaValue {
            val cType = when (cdata) {
                is LuaString -> typeRegistry[cdata.tojstring()]
                is LuaCType -> cdata.cType
                else -> null
            } ?: return error("metatype: expected ctype")
            
            metatypeRegistry[cType.name] = mt.checktable()
            return valueOf(cType.name)
        }
    }

    class LuaCType(val cType: CType, val ffi: FFILib) : LuaValue() {
        override fun type(): Int = TTABLE
        override fun typename(): String = "ctype"

        override fun get(key: LuaValue): LuaValue {
            return when (key.tojstring()) {
                "size" -> valueOf(cType.size)
                "name" -> valueOf(cType.name)
                "alignment" -> valueOf(if (cType.alignment > 0) cType.alignment else cType.size)
                else -> super.get(key)
            }
        }
    }

    class CData(
        val peer: Pointer,
        val cType: CType,
        val ffi: FFILib
    ) : LuaValue() {
        override fun type(): Int = TUSERDATA
        override fun typename(): String = "cdata"

        override fun get(key: LuaValue): LuaValue {
            if (key.isint()) {
                val index = key.toint()
                val offset = (index * cType.size).toLong()
                if (cType.arraySize in 1..index) {
                    return error("Array index out of bounds")
                }
                val sub = peer.getPointer(offset)
                if (sub != null) {
                    val pointedType = if (cType.fields != null) cType else cType.copy(name = "${cType.name}*")
                    return CData(sub, pointedType, ffi)
                }
                return getValueAt(peer, offset, cType)
            }

            val field = cType.fields?.get(key.tojstring())
            if (field != null) {
                if (field.type.arraySize > 0) {
                    val arrayPtr = peer.share(field.offset.toLong())
                    return CData(arrayPtr, field.type, ffi)
                }
                return getValueAt(peer, field.offset.toLong(), field.type)
            }

            val mt = ffi.metatypeRegistry[cType.name]
            if (mt != null) {
                val mtKey = mt.get(key)
                if (!mtKey.isnil()) return mtKey
            }

            return super.get(key)
        }

        override fun set(key: LuaValue, value: LuaValue) {
            if (key.isint()) {
                setValueAt(peer, (key.toint() * cType.size).toLong(), cType, value)
            } else {
                val field = cType.fields?.get(key.tojstring())
                if (field != null) setValueAt(peer, field.offset.toLong(), field.type, value)
            }
        }

        override fun add(value: LuaValue): LuaValue {
            return CData(peer.share(value.checklong() * cType.size), cType, ffi)
        }

        override fun tojstring(): String {
            return if (peer == Pointer.NULL) "NULL" else peer.toString()
        }

        private fun getValueAt(p: Pointer, offset: Long, t: CType): LuaValue {
            if (t.isEnum && t.enumValues != null) {
                val intVal = p.getInt(offset)
                for ((name, value) in t.enumValues) {
                    if (value == intVal) return valueOf(name)
                }
                return valueOf(intVal)
            }

            return when (t.name) {
                "int" -> valueOf(p.getInt(offset))
                "long" -> valueOf(p.getLong(offset).toDouble())
                "float" -> valueOf(p.getFloat(offset).toDouble())
                "double" -> valueOf(p.getDouble(offset))
                "char" -> valueOf(p.getByte(offset).toInt())
                "bool" -> valueOf(p.getByte(offset).toInt() != 0)
                else -> {
                    if (t.fields != null) CData(p.share(offset), t, ffi)
                    else {
                        val sub = p.getPointer(offset)
                        if (sub == null || sub == Pointer.NULL) NIL
                        else {
                            val pointedType = t.copy(name = "${t.name}*")
                            CData(sub, pointedType, ffi)
                        }
                    }
                }
            }
        }

        private fun setValueAt(p: Pointer, offset: Long, t: CType, v: LuaValue) {
            if (t.isEnum) {
                val enumVal = if (v.isstring()) {
                    t.enumValues?.get(v.tojstring()) ?: v.checkint()
                } else v.checkint()
                p.setInt(offset, enumVal)
                return
            }

            when (t.name) {
                "int" -> p.setInt(offset, v.checkint())
                "long" -> p.setLong(offset, v.checklong())
                "float" -> p.setFloat(offset, v.checkdouble().toFloat())
                "double" -> p.setDouble(offset, v.checkdouble())
                "char" -> p.setByte(offset, v.checkint().toByte())
                "bool" -> p.setByte(offset, if (v.checkboolean()) 1.toByte() else 0.toByte())
            }
        }
    }

    inner class SizeOfFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val t = when (arg) {
                is LuaString -> typeRegistry.get(arg.tojstring())
                is LuaCType -> arg.cType
                is CData -> arg.cType
                else -> null
            } ?: return valueOf(0)
            return valueOf(t.size)
        }
    }

    inner class LoadFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val name = arg.checkjstring()
            val lib = loadedLibraries.getOrPut(name) { NativeLibrary.getInstance(name) }
            return NativeLibWrapper(lib, name)
        }
    }

    inner class StringFunction : TwoArgFunction() {
        override fun call(ptr: LuaValue, len: LuaValue): LuaValue {
            log("StringFunction: ptr type=${ptr.typename()}")
            val p = (ptr as? CData)?.peer
            log("StringFunction: peer=$p, cType=${(ptr as? CData)?.cType?.name}")
            if (p == null || p == Pointer.NULL) return NIL
            val result = if (len.isnil()) p.getString(0) else String(p.getByteArray(0, len.checkint()))
            log("StringFunction: result='$result'")
            return valueOf(result)
        }
    }

    inner class NativeLibWrapper(val lib: NativeLibrary, val libName: String) : LuaTable() {
        override fun get(key: LuaValue): LuaValue {
            val name = key.tojstring()
            val sig = functionRegistry[name]

            return try {
                val func = lib.getFunction(name)
                NativeFunction(func, this@FFILib, sig)
            } catch (e: Exception) {
                super.get(key)
            }
        }
    }

    inner class NativeFunction(
        val jnaFunc: JnaFunction,
        val ffi: FFILib,
        val signature: CFunctionSignature? = null
    ) : VarArgFunction() {

        override fun invoke(args: Varargs): Varargs {
            val jnaArgs = arrayOfNulls<Any>(args.narg())
            for (i in 1..args.narg()) {
                val v = args.arg(i)
                // Конвертация Lua -> Java (улучшенная)
                jnaArgs[i-1] = when (v) {
                    is CData -> v.peer
                    is LuaNumber -> if (v.isint()) v.toint() else v.todouble()
                    is LuaString -> v.tojstring()
                    else -> null
                }
            }

            // Определяем класс возвращаемого значения на основе сигнатуры
            val returnClass = signature?.returnType?.nativeClass ?: Pointer::class.java

            return try {
                val result = jnaFunc.invoke(returnClass, jnaArgs)

                // Конвертация Java -> Lua на основе типа (Пункт 2)
                when {
                    result == null -> NIL
                    signature?.returnType?.name == "void" -> NIL
                    result is Int -> valueOf(result)
                    result is Long -> valueOf(result.toDouble())
                    result is Float -> valueOf(result.toDouble())
                    result is Double -> valueOf(result)
                    result is Boolean -> valueOf(result)
                    result is String -> valueOf(result)
                    result is Pointer -> {
                        if (result == Pointer.NULL) NIL
                        else CData(result, signature?.returnType ?: typeRegistry["ptr"]!!, ffi)
                    }
                    else -> NIL
                }
            } catch (e: Exception) {
                error("FFI Call Error [${jnaFunc.name}]: ${e.message}")
            }
        }
    }

    private fun log(msg: String) {
        println("[FFI] $msg")
    }

    fun setDebugMode(enabled: Boolean) {
        debugMode.set(enabled)
    }

    fun disposeLoadedLibraries() {
        loadedLibraries.forEach { it.value.dispose() }
        loadedLibraries.clear()
        functionRegistry.clear()
    }
}
