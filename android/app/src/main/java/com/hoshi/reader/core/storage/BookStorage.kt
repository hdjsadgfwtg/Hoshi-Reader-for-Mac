package com.hoshi.reader.core.storage

import android.content.Context
import android.net.Uri
import com.hoshi.reader.data.model.Bookmark
import com.hoshi.reader.data.model.BookInfo
import com.hoshi.reader.data.model.BookMetadata
import com.hoshi.reader.data.model.Highlight
import com.hoshi.reader.data.model.SasayakiMatchData
import com.hoshi.reader.data.model.SasayakiConfig
import com.hoshi.reader.data.model.ShelvesData
import com.hoshi.reader.data.model.Statistics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val booksDir: File
        get() = File(context.filesDir, "Books").also { it.mkdirs() }

    val dictionariesDir: File
        get() = File(context.filesDir, "Dictionaries").also { it.mkdirs() }

    val fontsDir: File
        get() = File(context.filesDir, "Fonts").also { it.mkdirs() }

    fun getBookDir(bookId: String): File =
        File(booksDir, bookId).also { it.mkdirs() }

    // --- JSON stores per book ---

    fun metadataStore(bookId: String) = JsonStore(
        File(getBookDir(bookId), "metadata.json"),
        BookMetadata.serializer()
    ) { BookMetadata(id = bookId, title = "", epubPath = "", contentDirectory = "") }

    fun bookmarkStore(bookId: String) = JsonStore(
        File(getBookDir(bookId), "bookmark.json"),
        Bookmark.serializer()
    ) { Bookmark() }

    fun bookInfoStore(bookId: String) = JsonStore(
        File(getBookDir(bookId), "bookinfo.json"),
        BookInfo.serializer()
    ) { BookInfo() }

    fun highlightsStore(bookId: String) = JsonStore(
        File(getBookDir(bookId), "highlights.json"),
        ListSerializer(Highlight.serializer())
    ) { emptyList() }

    fun statisticsStore(bookId: String) = JsonStore(
        File(getBookDir(bookId), "statistics.json"),
        ListSerializer(Statistics.serializer())
    ) { emptyList() }

    fun sasayakiMatchStore(bookId: String) = JsonStore(
        File(getBookDir(bookId), "sasayaki_match.json"),
        SasayakiMatchData.serializer()
    ) { SasayakiMatchData() }

    fun sasayakiConfigStore(bookId: String) = JsonStore(
        File(getBookDir(bookId), "sasayaki_config.json"),
        SasayakiConfig.serializer()
    ) { SasayakiConfig() }

    // --- Global stores ---

    val shelvesStore = JsonStore(
        File(booksDir, "shelves.json"),
        ShelvesData.serializer()
    ) { ShelvesData() }

    // --- Book operations ---

    fun importEpub(uri: Uri): Pair<String, File> {
        val bookId = UUID.randomUUID().toString()
        val bookDir = getBookDir(bookId)
        val epubFile = File(bookDir, "book.epub")

        if (uri.scheme == "file") {
            val sourceFile = File(uri.path!!)
            sourceFile.inputStream().use { input ->
                FileOutputStream(epubFile).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(epubFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Cannot open EPUB file")
        }

        val contentDir = File(bookDir, "content")
        unzipEpub(epubFile, contentDir)

        return bookId to contentDir
    }

    private fun unzipEpub(epubFile: File, destDir: File) {
        destDir.mkdirs()
        ZipInputStream(epubFile.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out ->
                        zip.copyTo(out)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    fun deleteBook(bookId: String) {
        getBookDir(bookId).deleteRecursively()
    }

    suspend fun getAllBookIds(): List<String> {
        return booksDir.listFiles()
            ?.filter { it.isDirectory && File(it, "metadata.json").exists() }
            ?.map { it.name }
            ?: emptyList()
    }

    suspend fun getAllBooks(): List<BookMetadata> {
        return getAllBookIds().mapNotNull { id ->
            try { metadataStore(id).read() } catch (_: Exception) { null }
        }.sortedByDescending { it.lastAccessedAt }
    }
}
