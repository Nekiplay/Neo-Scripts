package com.nekiplay.neoscripts.features.lua.objects.misc

import org.luaj.vm2.*
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.*
import java.util.zip.*
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
            "extractFile" -> ExtractFile()
            "listEntries", "listZipEntries" -> ListEntries()
            "zipFile", "addFileToZip" -> ZipFile()
            "gzip", "compressGzip" -> Gzip()
            "gunzip", "decompressGzip" -> Gunzip()
            "tar", "createTar" -> Tar()
            "untar", "extractTar" -> Untar()
            "tarGzip", "createTarGz" -> TarGzip()
            "extractTarGz" -> ExtractTarGz()
            "extractFileFromTar" -> ExtractFileFromTar()
            "getSupportedFormats" -> GetSupportedFormats()
            else -> super.get(key)
        }
    }

    inner class Zip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val sourcePath = args.arg(1).checkjstring()
            val zipPath = args.arg(2).checkjstring()
            val recursive = if (args.narg() > 2) args.arg(3).toboolean() else true

            return try {
                val source = File(sourcePath)
                if (!source.exists()) {
                    return varargsOf(arrayOf(NIL, valueOf("Source path does not exist")))
                }

                ZipOutputStream(FileOutputStream(zipPath)).use { zos ->
                    if (source.isDirectory) {
                        zipDirectory(source, source.parentFile?.path ?: "", zos, recursive)
                    } else {
                        zipFile(source, zos, "")
                    }
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Zip compression failed")))
            }
        }

        private fun zipDirectory(directory: File, basePath: String, zos: ZipOutputStream, recursive: Boolean) {
            val files = directory.listFiles() ?: return
            if (files.isEmpty()) {
                val entryName = if (basePath.isEmpty()) directory.name + "/" else basePath.substringAfter(basePath.substringBeforeLast(File.separator)) + directory.name + "/"
                val entry = ZipEntry(entryName)
                zos.putNextEntry(entry)
                zos.closeEntry()
                return
            }
            
            for (file in files) {
                val entryName = if (basePath.isEmpty()) {
                    "${directory.name}/${file.name}"
                } else {
                    "$basePath/${file.name}"
                }
                
                if (file.isDirectory && recursive) {
                    zipDirectory(file, entryName, zos, recursive)
                } else if (!file.isDirectory) {
                    zipFile(file, zos, entryName)
                }
            }
        }

        private fun zipFile(file: File, zos: ZipOutputStream, entryName: String) {
            val name = if (entryName.isEmpty()) file.name else entryName
            zos.putNextEntry(ZipEntry(name))
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    inner class Unzip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val zipPath = args.arg(1).checkjstring()
            val destPath = args.arg(2).checkjstring()

            return try {
                val destDir = File(destPath)
                if (!destDir.exists()) {
                    destDir.mkdirs()
                }

                ZipFile(zipPath).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        val file = File(destDir, entry.name)
                        
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(file).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Zip extraction failed")))
            }
        }
    }

    inner class ExtractFile : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val archivePath = args.arg(1).checkjstring()
            val entryName = args.arg(2).checkjstring()
            val destPath = args.arg(3).checkjstring()

            return try {
                val destFile = File(destPath)
                destFile.parentFile?.mkdirs()

                if (archivePath.endsWith(".zip")) {
                    ZipFile(archivePath).use { zip ->
                        val entry = zip.getEntry(entryName)
                        if (entry == null) {
                            return varargsOf(arrayOf(NIL, valueOf("Entry not found: $entryName")))
                        }

                        if (entry.isDirectory) {
                            return varargsOf(arrayOf(NIL, valueOf("Entry is a directory: $entryName")))
                        }

                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                } else {
                    return varargsOf(arrayOf(NIL, valueOf("Unsupported archive format for single file extraction")))
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "File extraction failed")))
            }
        }
    }

    inner class ListEntries : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val archivePath = arg.checkjstring()

            return try {
                val entriesTable = LuaTable()
                var index = 1

                when {
                    archivePath.endsWith(".zip") -> {
                        ZipFile(archivePath).use { zip ->
                            zip.entries().asSequence().forEach { entry ->
                                val entryTable = createEntryTable(entry.name, entry.size, entry.isDirectory)
                                entriesTable.set(index, entryTable)
                                index++
                            }
                        }
                    }
                    archivePath.endsWith(".tar") -> {
                        @Suppress("DEPRECATION")
                        TarArchiveInputStream(FileInputStream(archivePath)).use { tis ->
                            var entry: TarArchiveEntry? = tis.nextTarEntry
                            while (entry != null) {
                                val entryTable = createEntryTable(entry.name, entry.size.toDouble(), entry.isDirectory)
                                entriesTable.set(index, entryTable)
                                index++
                                entry = tis.nextTarEntry
                            }
                        }
                    }
                    archivePath.endsWith(".tar.gz") || archivePath.endsWith(".tgz") -> {
                        @Suppress("DEPRECATION")
                        TarArchiveInputStream(GzipCompressorInputStream(FileInputStream(archivePath))).use { tis ->
                            var entry: TarArchiveEntry? = tis.nextTarEntry
                            while (entry != null) {
                                val entryTable = createEntryTable(entry.name, entry.size.toDouble(), entry.isDirectory)
                                entriesTable.set(index, entryTable)
                                index++
                                entry = tis.nextTarEntry
                            }
                        }
                    }
                    else -> {
                        return valueOf("Unsupported archive format")
                    }
                }

                entriesTable
            } catch (e: Exception) {
                NIL
            }
        }

        private fun createEntryTable(name: String, size: Double, isDirectory: Boolean): LuaTable {
            val entryTable = LuaTable()
            entryTable.set("name", valueOf(name))
            entryTable.set("size", valueOf(size))
            entryTable.set("is_directory", valueOf(isDirectory))
            return entryTable
        }
    }

    inner class ZipFile : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val zipPath = args.arg(1).checkjstring()
            val filePath = args.arg(2).checkjstring()
            val entryName = if (args.narg() > 2) args.arg(3).checkjstring() else File(filePath).name

            return try {
                val file = File(filePath)
                if (!file.exists()) {
                    return varargsOf(arrayOf(NIL, valueOf("File does not exist")))
                }

                val zipFile = File(zipPath)
                val exists = zipFile.exists()

                ZipOutputStream(FileOutputStream(zipFile, true)).use { zos ->
                    if (exists) {
                        ZipFile(zipFile).use { existingZip ->
                            existingZip.entries().asSequence().forEach { entry ->
                                zos.putNextEntry(ZipEntry(entry.name))
                                existingZip.getInputStream(entry).use { it.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                    
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Failed to add file to zip")))
            }
        }
    }

    inner class Gzip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val sourcePath = args.arg(1).checkjstring()
            val gzPath = args.arg(2).checkjstring()

            return try {
                val source = File(sourcePath)
                if (!source.exists()) {
                    return varargsOf(arrayOf(NIL, valueOf("Source file does not exist")))
                }

                GzipCompressorOutputStream(FileOutputStream(gzPath)).use { gzos ->
                    FileInputStream(source).use { fis ->
                        fis.copyTo(gzos)
                    }
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Gzip compression failed")))
            }
        }
    }

    inner class Gunzip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val gzPath = args.arg(1).checkjstring()
            val destPath = args.arg(2).checkjstring()

            return try {
                GzipCompressorInputStream(FileInputStream(gzPath)).use { gzis ->
                    FileOutputStream(destPath).use { fos ->
                        gzis.copyTo(fos)
                    }
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Gzip decompression failed")))
            }
        }
    }

    inner class Tar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val sourcePath = args.arg(1).checkjstring()
            val tarPath = args.arg(2).checkjstring()
            val recursive = if (args.narg() > 2) args.arg(3).toboolean() else true

            return try {
                val source = File(sourcePath)
                if (!source.exists()) {
                    return varargsOf(arrayOf(NIL, valueOf("Source path does not exist")))
                }

                TarArchiveOutputStream(FileOutputStream(tarPath)).use { tos ->
                    if (source.isDirectory) {
                        tarDirectory(source, "", tos, recursive)
                    } else {
                        tarFile(source, tos, source.name)
                    }
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Tar creation failed")))
            }
        }

        private fun tarDirectory(directory: File, basePath: String, tos: TarArchiveOutputStream, recursive: Boolean) {
            val files = directory.listFiles() ?: return
            for (file in files) {
                val entryName = if (basePath.isEmpty()) {
                    "${directory.name}/${file.name}"
                } else {
                    "$basePath/${file.name}"
                }
                
                if (file.isDirectory && recursive) {
                    tarDirectory(file, entryName, tos, recursive)
                } else if (!file.isDirectory) {
                    tarFile(file, tos, entryName)
                }
            }
        }

        private fun tarFile(file: File, tos: TarArchiveOutputStream, entryName: String) {
            val entry = TarArchiveEntry(file, entryName)
            entry.size = file.length()
            tos.putArchiveEntry(entry)
            FileInputStream(file).use { fis ->
                fis.copyTo(tos)
            }
            tos.closeArchiveEntry()
        }
    }

    inner class Untar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val tarPath = args.arg(1).checkjstring()
            val destPath = args.arg(2).checkjstring()

            return try {
                val destDir = File(destPath)
                if (!destDir.exists()) {
                    destDir.mkdirs()
                }

                @Suppress("DEPRECATION")
                TarArchiveInputStream(FileInputStream(tarPath)).use { tis ->
                    var entry: TarArchiveEntry? = tis.nextTarEntry
                    while (entry != null) {
                        val file = File(destDir, entry.name)
                        
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { fos ->
                                tis.copyTo(fos)
                            }
                        }
                        
                        entry = tis.nextTarEntry
                    }
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Tar extraction failed")))
            }
        }
    }

    inner class TarGzip : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val sourcePath = args.arg(1).checkjstring()
            val tarGzPath = args.arg(2).checkjstring()
            val recursive = if (args.narg() > 2) args.arg(3).toboolean() else true

            return try {
                val source = File(sourcePath)
                if (!source.exists()) {
                    return varargsOf(arrayOf(NIL, valueOf("Source path does not exist")))
                }

                GzipCompressorOutputStream(FileOutputStream(tarGzPath)).use { gzos ->
                    TarArchiveOutputStream(gzos).use { tos ->
                        if (source.isDirectory) {
                            tarDirectory(source, "", tos, recursive)
                        } else {
                            tarFile(source, tos, source.name)
                        }
                    }
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Tar.gz creation failed")))
            }
        }

        private fun tarDirectory(directory: File, basePath: String, tos: TarArchiveOutputStream, recursive: Boolean) {
            val files = directory.listFiles() ?: return
            for (file in files) {
                val entryName = if (basePath.isEmpty()) {
                    "${directory.name}/${file.name}"
                } else {
                    "$basePath/${file.name}"
                }
                
                if (file.isDirectory && recursive) {
                    tarDirectory(file, entryName, tos, recursive)
                } else if (!file.isDirectory) {
                    tarFile(file, tos, entryName)
                }
            }
        }

        private fun tarFile(file: File, tos: TarArchiveOutputStream, entryName: String) {
            val entry = TarArchiveEntry(file, entryName)
            entry.size = file.length()
            tos.putArchiveEntry(entry)
            FileInputStream(file).use { fis ->
                fis.copyTo(tos)
            }
            tos.closeArchiveEntry()
        }
    }

    inner class ExtractTarGz : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val tarGzPath = args.arg(1).checkjstring()
            val destPath = args.arg(2).checkjstring()

            return try {
                val destDir = File(destPath)
                if (!destDir.exists()) {
                    destDir.mkdirs()
                }

                GzipCompressorInputStream(FileInputStream(tarGzPath)).use { gzis ->
                    @Suppress("DEPRECATION")
                    TarArchiveInputStream(gzis).use { tis ->
                        var entry: TarArchiveEntry? = tis.nextTarEntry
                        while (entry != null) {
                            val file = File(destDir, entry.name)
                            
                            if (entry.isDirectory) {
                                file.mkdirs()
                            } else {
                                file.parentFile?.mkdirs()
                                FileOutputStream(file).use { fos ->
                                    tis.copyTo(fos)
                                }
                            }
                            
                            entry = tis.nextTarEntry
                        }
                    }
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "Tar.gz extraction failed")))
            }
        }
    }

    inner class ExtractFileFromTar : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val archivePath = args.arg(1).checkjstring()
            val entryName = args.arg(2).checkjstring()
            val destPath = args.arg(3).checkjstring()

            return try {
                val destFile = File(destPath)
                destFile.parentFile?.mkdirs()

                val tis: TarArchiveInputStream = if (archivePath.endsWith(".tar.gz") || archivePath.endsWith(".tgz")) {
                    TarArchiveInputStream(GzipCompressorInputStream(FileInputStream(archivePath)))
                } else {
                    TarArchiveInputStream(FileInputStream(archivePath))
                }

                @Suppress("DEPRECATION")
                var found = false
                tis.use { 
                    var entry: TarArchiveEntry? = it.nextTarEntry
                    while (entry != null) {
                        if (entry.name == entryName && !entry.isDirectory) {
                            FileOutputStream(destFile).use { fos ->
                                it.copyTo(fos)
                            }
                            found = true
                            break
                        }
                        entry = it.nextTarEntry
                    }
                }

                if (!found) {
                    return varargsOf(arrayOf(NIL, valueOf("Entry not found: $entryName")))
                }

                varargsOf(arrayOf(TRUE, NIL))
            } catch (e: Exception) {
                varargsOf(arrayOf(NIL, valueOf(e.message ?: "File extraction from tar failed")))
            }
        }
    }

    inner class GetSupportedFormats : ZeroArgFunction() {
        override fun call(): LuaValue {
            val formatsTable = LuaTable()
            formatsTable.set(1, valueOf("zip"))
            formatsTable.set(2, valueOf("gzip"))
            formatsTable.set(3, valueOf("tar"))
            formatsTable.set(4, valueOf("tar.gz"))
            return formatsTable
        }
    }
}
