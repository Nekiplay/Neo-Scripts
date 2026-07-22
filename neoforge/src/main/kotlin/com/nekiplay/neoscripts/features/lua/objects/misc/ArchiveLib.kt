package com.nekiplay.neoscripts.features.lua.objects.misc

import org.luaj.vm2.*
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.OneArgFunction
import java.io.*
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile as ApacheZipFile
import org.apache.commons.compress.utils.IOUtils

class ArchiveLib : LuaValue() {
    override fun typename(): String = "archive"
    override fun tojstring(): String = "ArchiveObject"
    override fun isnil(): Boolean = false
    override fun type(): Int = TUSERDATA
    override fun call(): LuaValue = this

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "zip", "compress" -> Zip()
            "unzip", "extract" -> Unzip()
            "extractEntry" -> ExtractEntry()
            "add", "addToArchive" -> AddToArchive()
            "listEntries" -> ListEntries()
            else -> super.get(key)
        }
    }

    companion object {
        private fun addToZipRecursive(source: File, entryPath: String, zos: ZipArchiveOutputStream) {
            val name = entryPath.replace('\\', '/').removePrefix("/")

            if (source.isDirectory) {
                val dirName = if (name.endsWith("/")) name else "$name/"
                // Создаем запись для самой папки (важно для пустых папок)
                if (dirName.isNotEmpty()) {
                    val entry = ZipArchiveEntry(source, dirName)
                    zos.putArchiveEntry(entry)
                    zos.closeArchiveEntry()
                }

                source.listFiles()?.forEach { child ->
                    addToZipRecursive(child, "$dirName${child.name}", zos)
                }
            } else {
                val entry = ZipArchiveEntry(source, name)
                zos.putArchiveEntry(entry)
                FileInputStream(source).use { fis ->
                    IOUtils.copy(fis, zos)
                }
                zos.closeArchiveEntry()
            }
        }
    }
    /**
     * Создание нового архива.
     * archive:zip("папка_или_файл", "имя_архива.zip")
     */
    class Zip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val sourcePath = args.arg(1).checkjstring()
            val zipPath = args.arg(2).checkjstring()

            return try {
                val source = File(sourcePath)
                if (!source.exists()) return varargsOf(NIL, valueOf("Source not found"))

                ZipArchiveOutputStream(FileOutputStream(zipPath)).use { zos ->
                    // Поведение 7-zip: корневая папка включается в архив
                    addToZipRecursive(source, source.name, zos)
                }
                varargsOf(TRUE, NIL)
            } catch (e: Exception) {
                varargsOf(NIL, valueOf(e.message))
            }
        }
    }

    /**
     * Полная распаковка архива.
     * archive:unzip("архив.zip", "куда_распаковать")
     */
    class Unzip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val zipPath = args.arg(1).checkjstring()
            val destPath = args.arg(2).checkjstring()

            return try {
                val destDir = File(destPath)
                ApacheZipFile(File(zipPath)).use { zipFile ->
                    val entries = zipFile.entries
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val out = File(destDir, entry.name)
                        if (entry.isDirectory) {
                            out.mkdirs()
                        } else {
                            out.parentFile?.mkdirs()
                            zipFile.getInputStream(entry).use { input ->
                                FileOutputStream(out).use { output -> IOUtils.copy(input, output) }
                            }
                        }
                    }
                }
                varargsOf(TRUE, NIL)
            } catch (e: Exception) {
                varargsOf(NIL, valueOf(e.message))
            }
        }
    }

    /**
     * Извлечение конкретного файла или папки из архива.
     * archive:extractEntry("архив.zip", "путь/в/архиве", "путь/на/диске")
     */
    class ExtractEntry : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val zipPath = args.arg(1).checkjstring()
            val targetEntry = args.arg(2).checkjstring().replace('\\', '/').removeSuffix("/")
            val destPath = args.arg(3).checkjstring()

            return try {
                ApacheZipFile(File(zipPath)).use { zipFile ->
                    val entries = zipFile.entries
                    var found = false

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name.replace('\\', '/').removeSuffix("/")

                        if (name == targetEntry || name.startsWith("$targetEntry/")) {
                            found = true
                            // Рассчитываем относительный путь для распаковки
                            val relativePath = entry.name.substring(targetEntry.length).removePrefix("/")
                            val outFile = if (relativePath.isEmpty()) {
                                File(destPath, File(name).name)
                            } else {
                                File(destPath, relativePath)
                            }

                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                zipFile.getInputStream(entry).use { input ->
                                    FileOutputStream(outFile).use { output -> IOUtils.copy(input, output) }
                                }
                            }
                        }
                    }
                    if (!found) return varargsOf(NIL, valueOf("Entry not found in archive"))
                }
                varargsOf(TRUE, NIL)
            } catch (e: Exception) {
                varargsOf(NIL, valueOf(e.message))
            }
        }
    }

    /**
     * Добавление или обновление файла/папки в архиве.
     * archive:add("архив.zip", "что_добавить", "куда_в_архиве")
     */
    class AddToArchive : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val archiveFile = File(args.arg(1).checkjstring())
            val sourceFile = File(args.arg(2).checkjstring())
            // Если третий аргумент не указан, берем имя исходного файла
            val targetInZip = args.arg(3).optjstring(sourceFile.name).replace('\\', '/')

            if (!sourceFile.exists()) return varargsOf(NIL, valueOf("Source file/folder not found"))

            val tempFile = File.createTempFile("zip_update", ".tmp")

            return try {
                ZipArchiveOutputStream(FileOutputStream(tempFile)).use { zos ->
                    // 1. Если архив уже существует, переносим старые файлы
                    if (archiveFile.exists()) {
                        ApacheZipFile(archiveFile).use { oldZip ->
                            val entries = oldZip.entries
                            while (entries.hasMoreElements()) {
                                val entry = entries.nextElement()
                                // Пропускаем старую версию файла/папки, если пути совпадают
                                if (entry.name != targetInZip && !entry.name.startsWith("$targetInZip/")) {
                                    zos.addRawArchiveEntry(entry, oldZip.getRawInputStream(entry))
                                }
                            }
                        }
                    }

                    // 2. Добавляем новое содержимое
                    addToZipRecursive(sourceFile, targetInZip, zos)
                }

                // 3. Заменяем старый файл новым
                if (archiveFile.exists()) archiveFile.delete()
                tempFile.renameTo(archiveFile)

                varargsOf(TRUE, NIL)
            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                varargsOf(NIL, valueOf(e.message))
            }
        }
    }

    /**
     * Получение списка всех файлов в архиве.
     * local list = archive:listEntries("архив.zip")
     */
    class ListEntries : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val archivePath = arg.checkjstring()
            val archiveFile = File(archivePath)

            if (!archiveFile.exists()) return NIL

            val entriesTable = LuaTable()
            var index = 1

            return try {
                ApacheZipFile(archiveFile).use { zipFile ->
                    val entries = zipFile.entries
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()

                        val entryInfo = LuaTable()
                        entryInfo.set("name", valueOf(entry.name))
                        entryInfo.set("size", valueOf(entry.size.toDouble()))
                        entryInfo.set("is_directory", valueOf(entry.isDirectory))
                        entryInfo.set("mtime", valueOf((entry.time / 1000).toDouble()))

                        entriesTable.set(index++, entryInfo)
                    }
                }
                entriesTable
            } catch (e: Exception) {
                NIL
            }
        }
    }
}
