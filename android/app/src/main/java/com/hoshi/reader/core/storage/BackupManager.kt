package com.hoshi.reader.core.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun exportBackup(outputUri: Uri) {
        val booksDir = File(context.filesDir, "Books")
        val dictsDir = File(context.filesDir, "Dictionaries")
        val ankiConfig = File(context.filesDir, "anki_config.json")

        context.contentResolver.openOutputStream(outputUri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                if (booksDir.exists()) {
                    addDirectoryToZip(zip, booksDir, "Books")
                }
                if (dictsDir.exists()) {
                    val configFile = File(dictsDir, "config.json")
                    if (configFile.exists()) {
                        addFileToZip(zip, configFile, "Dictionaries/config.json")
                    }
                }
                if (ankiConfig.exists()) {
                    addFileToZip(zip, ankiConfig, "anki_config.json")
                }
            }
        }
    }

    fun importBackup(inputUri: Uri) {
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val targetFile = File(context.filesDir, entry.name)
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { output ->
                            zip.copyTo(output)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }

    private fun addDirectoryToZip(zip: ZipOutputStream, dir: File, prefix: String) {
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relativePath = "$prefix/${file.relativeTo(dir).path}"
                addFileToZip(zip, file, relativePath)
            }
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }
}
