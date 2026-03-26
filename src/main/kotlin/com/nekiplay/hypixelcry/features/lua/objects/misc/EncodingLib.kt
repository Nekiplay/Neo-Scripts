package com.nekiplay.hypixelcry.features.lua.objects.misc

import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

class EncodingLib : TwoArgFunction() {
    private val supportedCharsets = mapOf(
        "UTF-8" to StandardCharsets.UTF_8,
        "UTF-16" to StandardCharsets.UTF_16,
        "UTF-16BE" to StandardCharsets.UTF_16BE,
        "UTF-16LE" to StandardCharsets.UTF_16LE,
        "UTF-32" to Charset.forName("UTF-32"),
        "UTF-32BE" to Charset.forName("UTF-32BE"),
        "UTF-32LE" to Charset.forName("UTF-32LE"),
        "ASCII" to StandardCharsets.US_ASCII,
        "ISO-8859-1" to StandardCharsets.ISO_8859_1,
        "CP1251" to Charset.forName("CP1251"),
        "CP1252" to Charset.forName("CP1252"),
        "KOI8-R" to Charset.forName("KOI8-R"),
        "KOI8-U" to Charset.forName("KOI8-U"),
        "WINDOWS-1251" to Charset.forName("WINDOWS-1251")
    )

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val library = LuaTable()
        library.set("stringToBytes", StringToBytes())
        library.set("bytesToString", BytesToString())
        library.set("getSupportedEncodings", GetSupportedEncodings())
        library.set("isValidEncoding", IsValidEncoding())
        library.set("detectEncoding", DetectEncoding())
        library.set("convertEncoding", ConvertEncoding())
        library.set("hexEncode", HexEncode())
        library.set("hexDecode", HexDecode())
        library.set("base64Encode", Base64Encode())
        library.set("base64Decode", Base64Decode())
        //env.set("encoding", library)

        return library
    }

    inner class StringToBytes : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val text = args.arg(1).checkjstring()
            val encoding = if (args.narg() > 1) args.arg(2).checkjstring() else "UTF-8"

            return try {
                val charset = getCharset(encoding)
                val bytes = text.toByteArray(charset)
                
                val bytesTable = LuaValue.tableOf()
                for (i in bytes.indices) {
                    bytesTable.set(i + 1, LuaValue.valueOf(bytes[i].toInt() and 0xFF))
                }
                
                LuaValue.varargsOf(arrayOf(bytesTable, LuaValue.NIL))
            } catch (e: Exception) {
                LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "String to bytes conversion failed")))
            }
        }
    }

    inner class BytesToString : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val bytesTable = args.arg(1).checktable()
            val encoding = if (args.narg() > 1) args.arg(2).checkjstring() else "UTF-8"
            val startIndex = if (args.narg() > 2) args.arg(3).checkint() else 1
            val length = if (args.narg() > 3) args.arg(4).checkint() else -1

            return try {
                val charset = getCharset(encoding)
                val tableLength = bytesTable.length()
                val actualLength = if (length == -1) tableLength - startIndex + 1 else length
                
                if (startIndex < 1 || startIndex > tableLength) {
                    return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("Start index out of range")))
                }
                
                if (actualLength < 0 || startIndex + actualLength - 1 > tableLength) {
                    return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("Length out of range")))
                }

                val bytes = ByteArray(actualLength)
                for (i in 0 until actualLength) {
                    val byteValue = bytesTable.get(startIndex + i).checkint()
                    if (byteValue < 0 || byteValue > 255) {
                        return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("Invalid byte value: $byteValue")))
                    }
                    bytes[i] = byteValue.toByte()
                }

                val result = String(bytes, charset)
                LuaValue.varargsOf(arrayOf(LuaValue.valueOf(result), LuaValue.NIL))
            } catch (e: Exception) {
                LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "Bytes to string conversion failed")))
            }
        }
    }

    inner class GetSupportedEncodings : ZeroArgFunction() {
        override fun call(): LuaValue {
            val encodingsTable = LuaValue.tableOf()
            supportedCharsets.keys.sorted().forEachIndexed { index, encoding ->
                encodingsTable.set(index + 1, LuaValue.valueOf(encoding))
            }
            return encodingsTable
        }
    }

    inner class IsValidEncoding : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val encoding = arg.checkjstring()
            return LuaValue.valueOf(supportedCharsets.containsKey(encoding))
        }
    }

    inner class DetectEncoding : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            if (!arg.isstring()) return LuaValue.NIL
            
            val text = arg.tojstring()
            return try {
                // Простая эвристика для определения кодировки
                val bytes = text.toByteArray()
                
                // Проверяем UTF-8
                if (isUTF8(bytes)) {
                    return LuaValue.valueOf("UTF-8")
                }
                
                // Проверяем ASCII
                if (isASCII(bytes)) {
                    return LuaValue.valueOf("ASCII")
                }
                
                // Проверяем CP1251 (часто используется для русского текста)
                if (isCP1251(text.toByteArray(StandardCharsets.UTF_8))) {
                    return LuaValue.valueOf("CP1251")
                }
                
                // По умолчанию UTF-8
                LuaValue.valueOf("UTF-8")
            } catch (e: Exception) {
                LuaValue.NIL
            }
        }

        private fun isUTF8(bytes: ByteArray): Boolean {
            return try {
                String(bytes, StandardCharsets.UTF_8).toByteArray(StandardCharsets.UTF_8).contentEquals(bytes)
            } catch (e: Exception) {
                false
            }
        }

        private fun isASCII(bytes: ByteArray): Boolean {
            return bytes.all { it in 0..127 }
        }

        private fun isCP1251(bytes: ByteArray): Boolean {
            return try {
                val text = String(bytes, StandardCharsets.UTF_8)
                val cp1251Bytes = text.toByteArray(Charset.forName("CP1251"))
                String(cp1251Bytes, Charset.forName("CP1251")) == text
            } catch (e: Exception) {
                false
            }
        }
    }

    inner class ConvertEncoding : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val text = args.arg(1).checkjstring()
            val fromEncoding = args.arg(2).checkjstring()
            val toEncoding = args.arg(3).checkjstring()

            return try {
                val fromCharset = getCharset(fromEncoding)
                val toCharset = getCharset(toEncoding)
                
                val bytes = text.toByteArray(fromCharset)
                val result = String(bytes, toCharset)
                
                LuaValue.varargsOf(arrayOf(LuaValue.valueOf(result), LuaValue.NIL))
            } catch (e: Exception) {
                LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "Encoding conversion failed")))
            }
        }
    }

    inner class HexEncode : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return try {
                val text = arg.checkjstring()
                val bytes = text.toByteArray()
                val hexString = bytes.joinToString("") { "%02x".format(it) }
                LuaValue.valueOf(hexString)
            } catch (e: Exception) {
                LuaValue.NIL
            }
        }
    }

    inner class HexDecode : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return try {
                val hexString = arg.checkjstring()
                if (hexString.length % 2 != 0) {
                    return varargsOf(arrayOf(NIL, valueOf("Hex string must have even length"))) as LuaValue
                }

                val bytes = hexString.chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray()

                val result = String(bytes)
                varargsOf(arrayOf(valueOf(result), NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Hex decode failed")))
            } as LuaValue
        }
    }

    inner class Base64Encode : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return try {
                val text = arg.checkjstring()
                val bytes = text.toByteArray()
                val base64String = Base64.getEncoder().encodeToString(bytes)
                LuaValue.valueOf(base64String)
            } catch (e: Exception) {
                LuaValue.NIL
            }
        }
    }

    inner class Base64Decode : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return try {
                val base64String = arg.checkjstring()
                val bytes = Base64.getDecoder().decode(base64String)
                val result = String(bytes)
                varargsOf(arrayOf(valueOf(result), NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Base64 decode failed")))
            } as LuaValue
        }
    }

    private fun getCharset(encoding: String): Charset {
        return supportedCharsets[encoding] ?: throw IllegalArgumentException("Unsupported encoding: $encoding")
    }
}