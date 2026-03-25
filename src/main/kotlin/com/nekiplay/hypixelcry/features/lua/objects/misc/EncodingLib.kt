package com.nekiplay.hypixelcry.features.lua.objects.misc

import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

class EncodingLib(val L: Lua) {
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

    fun register() {
        L.newTable() // Создаем таблицу encoding

        L.push(JFunction { stringToBytes(it) })
        L.setField(-2, "stringToBytes")

        L.push(JFunction { bytesToString(it) })
        L.setField(-2, "bytesToString")

        L.push(JFunction { getSupportedEncodings(it) })
        L.setField(-2, "getSupportedEncodings")

        L.push(JFunction { isValidEncoding(it) })
        L.setField(-2, "isValidEncoding")

        L.push(JFunction { detectEncoding(it) })
        L.setField(-2, "detectEncoding")

        L.push(JFunction { convertEncoding(it) })
        L.setField(-2, "convertEncoding")

        L.push(JFunction { hexEncode(it) })
        L.setField(-2, "hexEncode")

        L.push(JFunction { hexDecode(it) })
        L.setField(-2, "hexDecode")

        L.push(JFunction { base64Encode(it) })
        L.setField(-2, "base64Encode")

        L.push(JFunction { base64Decode(it) })
        L.setField(-2, "base64Decode")

        L.setGlobal("encoding")
    }

    private fun getCharset(encoding: String): Charset {
        return supportedCharsets[encoding] ?: throw IllegalArgumentException("Unsupported encoding: $encoding")
    }

    private fun stringToBytes(l: Lua): Int {
        val text = l.toString(1) ?: return 0
        val encoding = if (l.isString(2)) l.toString(2)!! else "UTF-8"

        return try {
            val charset = getCharset(encoding)
            val bytes = text.toByteArray(charset)

            l.newTable()
            for (i in bytes.indices) {
                l.push((bytes[i].toInt() and 0xFF).toDouble())
                l.rawSetI(-2, i + 1)
            }
            l.pushNil() // Ошибки нет
            2 // Возвращаем (table, nil)
        } catch (e: Exception) {
            l.pushNil()
            l.push(e.message ?: "Conversion failed")
            2
        }
    }

    private fun bytesToString(l: Lua): Int {
        if (!l.isTable(1)) {
            l.pushNil()
            l.push("Table required")
            return 2
        }
        val encoding = if (l.isString(2)) l.toString(2)!! else "UTF-8"
        val startIndex = if (l.isNumber(3)) l.toNumber(3).toInt() else 1
        val length = if (l.isNumber(4)) l.toNumber(4).toInt() else -1

        return try {
            val charset = getCharset(encoding)
            val tableLength = l.rawLength(1)
            val actualLength = if (length == -1) tableLength - startIndex + 1 else length

            if (startIndex < 1 || startIndex > tableLength) {
                l.pushNil(); l.push("Start index out of range"); return 2
            }

            val bytes = ByteArray(actualLength)
            for (i in 0 until actualLength) {
                l.rawGetI(1, startIndex + i)
                val byteValue = l.toNumber(-1).toInt()
                l.pop(1)
                bytes[i] = byteValue.toByte()
            }

            l.push(String(bytes, charset))
            l.pushNil()
            2
        } catch (e: Exception) {
            l.pushNil(); l.push(e.message ?: "Error"); return 2
        }
    }

    private fun getSupportedEncodings(l: Lua): Int {
        l.newTable()
        supportedCharsets.keys.sorted().forEachIndexed { index, name ->
            l.push(name)
            l.rawSetI(-2, index + 1)
        }
        return 1
    }

    private fun isValidEncoding(l: Lua): Int {
        val name = l.toString(1) ?: ""
        l.push(supportedCharsets.containsKey(name))
        return 1
    }

    private fun detectEncoding(l: Lua): Int {
        val text = l.toString(1) ?: return 0
        val bytes = text.toByteArray()

        // Упрощенная логика из оригинала
        val result = when {
            bytes.all { it in 0..127 } -> "ASCII"
            isUTF8(bytes) -> "UTF-8"
            else -> "UTF-8"
        }
        l.push(result)
        return 1
    }

    private fun isUTF8(bytes: ByteArray): Boolean = try {
        String(bytes, StandardCharsets.UTF_8).toByteArray(StandardCharsets.UTF_8).contentEquals(bytes)
    } catch (e: Exception) { false }

    private fun convertEncoding(l: Lua): Int {
        val text = l.toString(1) ?: return 0
        val fromEnc = l.toString(2) ?: "UTF-8"
        val toEnc = l.toString(3) ?: "UTF-8"

        return try {
            val bytes = text.toByteArray(getCharset(fromEnc))
            l.push(String(bytes, getCharset(toEnc)))
            l.pushNil()
            2
        } catch (e: Exception) {
            l.pushNil(); l.push(e.message ?: "Error"); 2
        }
    }

    private fun hexEncode(l: Lua): Int {
        val text = l.toString(1) ?: return 0
        val hex = text.toByteArray().joinToString("") { "%02x".format(it) }
        l.push(hex)
        return 1
    }

    private fun hexDecode(l: Lua): Int {
        val hex = l.toString(1) ?: ""
        return try {
            val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            l.push(String(bytes))
            l.pushNil()
            2
        } catch (e: Exception) {
            l.pushNil(); l.push(e.message ?: "Error"); 2
        }
    }

    private fun base64Encode(l: Lua): Int {
        val text = l.toString(1) ?: return 0
        l.push(Base64.getEncoder().encodeToString(text.toByteArray()))
        return 1
    }

    private fun base64Decode(l: Lua): Int {
        val b64 = l.toString(1) ?: ""
        return try {
            val bytes = Base64.getDecoder().decode(b64)
            l.push(String(bytes))
            l.pushNil()
            2
        } catch (e: Exception) {
            l.pushNil(); l.push(e.message ?: "Error"); 2
        }
    }
}