package com.nekiplay.hypixelcry.features.lua.objects.misc

import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import java.io.File

class FileSystemLib : TwoArgFunction() {

    private var scriptDirectories: MutableMap<String, String> = mutableMapOf()

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val globals = env.checkglobals()

        // Регистрируем функции
        val fileSystem = LuaValue.tableOf()

        fileSystem.set("getScriptDirectory", object : OneArgFunction() {
            override fun call(scriptName: LuaValue): LuaValue {
                val name = if (scriptName.isnil()) null else scriptName.checkjstring()
                val dir = scriptDirectories[name] ?: ""
                return LuaValue.valueOf(dir)
            }
        })

        fileSystem.set("readFile", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                val filePath = path.checkjstring()
                return try {
                    val file = File(filePath)
                    if (file.exists() && file.isFile) {
                        LuaValue.valueOf(file.readText())
                    } else {
                        LuaValue.NIL
                    }
                } catch (e: Exception) {
                    LuaValue.NIL
                }
            }
        })

        fileSystem.set("fileExists", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                val filePath = path.checkjstring()
                val file = File(filePath)
                return LuaValue.valueOf(file.exists() && file.isFile)
            }
        })

        fileSystem.set("writeFile", object : TwoArgFunction() {
            override fun call(path: LuaValue, content: LuaValue): LuaValue {
                val filePath = path.checkjstring()
                val fileContent = content.checkjstring()
                return try {
                    File(filePath).writeText(fileContent)
                    LuaValue.TRUE
                } catch (e: Exception) {
                    LuaValue.FALSE
                }
            }
        })

        fileSystem.set("createDirectory", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                val dirPath = path.checkjstring()
                return try {
                    val dir = File(dirPath)
                    val result = dir.mkdirs()
                    LuaValue.valueOf(result)
                } catch (e: Exception) {
                    LuaValue.FALSE
                }
            }
        })

        fileSystem.set("listFiles", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                val dirPath = path.checkjstring()
                return try {
                    val dir = File(dirPath)
                    if (dir.exists() && dir.isDirectory) {
                        val files = dir.list() ?: emptyArray()
                        val luaTable = LuaValue.tableOf()
                        files.forEachIndexed { index, fileName ->
                            luaTable.set(index + 1, LuaValue.valueOf(fileName))
                        }
                        luaTable
                    } else {
                        LuaValue.NIL
                    }
                } catch (e: Exception) {
                    LuaValue.NIL
                }
            }
        })

        fileSystem.set("getAbsolutePath", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                val filePath = path.checkjstring()
                return try {
                    val file = File(filePath)
                    LuaValue.valueOf(file.absolutePath)
                } catch (e: Exception) {
                    LuaValue.NIL
                }
            }
        })

        fileSystem.set("isDirectory", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                val filePath = path.checkjstring()
                val file = File(filePath)
                return LuaValue.valueOf(file.exists() && file.isDirectory)
            }
        })

        fileSystem.set("deleteFile", object : OneArgFunction() {
            override fun call(path: LuaValue): LuaValue {
                val filePath = path.checkjstring()
                return try {
                    val file = File(filePath)
                    val result = file.delete()
                    LuaValue.valueOf(result)
                } catch (e: Exception) {
                    LuaValue.FALSE
                }
            }
        })

        // Регистрируем библиотеку
        globals.set("filesystem", fileSystem)
        return fileSystem
    }

    fun setScriptDirectory(scriptName: String, directory: String) {
        scriptDirectories[scriptName] = directory
    }

    fun removeScriptDirectory(scriptName: String) {
        scriptDirectories.remove(scriptName)
    }

    fun getScriptDirectory(scriptName: String): String? {
        return scriptDirectories[scriptName]
    }
}