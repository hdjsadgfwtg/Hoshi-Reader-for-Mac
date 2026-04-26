package com.hoshi.reader.core.epub

import com.hoshi.reader.data.model.BookMetadata
import com.hoshi.reader.data.model.ChapterInfo
import io.documentnode.epub4j.epub.EpubReader
import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.domain.Resource
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

class EpubParser @Inject constructor() {

    data class ParseResult(
        val title: String,
        val creator: String?,
        val language: String?,
        val coverImagePath: String?,
        val chapters: List<ChapterInfo>,
        val totalCharacters: Int,
        val opfDir: String
    )

    fun parse(epubFile: File, contentDir: File): ParseResult {
        val book = EpubReader().readEpub(FileInputStream(epubFile))
        val metadata = book.metadata

        val title = metadata.firstTitle?.ifBlank { null } ?: "Untitled"
        val creator = metadata.authors.firstOrNull()?.let { "${it.firstname} ${it.lastname}".trim() }
        val language = metadata.language

        val opfRelDir = findOpfDirectory(contentDir)
        val opfDir = if (opfRelDir.isNotEmpty()) File(contentDir, opfRelDir) else contentDir

        val coverPath = extractCover(book, opfDir)

        val chapters = buildChapterList(book, opfDir)
        val totalChars = chapters.sumOf { it.characterCount }

        return ParseResult(
            title = title,
            creator = creator,
            language = language,
            coverImagePath = coverPath,
            chapters = chapters,
            totalCharacters = totalChars,
            opfDir = opfDir.absolutePath
        )
    }

    private fun findOpfDirectory(contentDir: File): String {
        val containerFile = File(contentDir, "META-INF/container.xml")
        if (!containerFile.exists()) return ""
        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(containerFile)
            val rootfiles = doc.getElementsByTagName("rootfile")
            if (rootfiles.length > 0) {
                val fullPath = rootfiles.item(0).attributes.getNamedItem("full-path")?.nodeValue ?: return ""
                val parent = fullPath.substringBeforeLast('/', "")
                return parent
            }
        } catch (_: Exception) {}
        return ""
    }

    private fun extractCover(book: Book, contentDir: File): String? {
        val coverImage = book.coverImage ?: return null
        val coverFile = File(contentDir, "cover_extracted.${getExtension(coverImage.mediaType?.name)}")
        try {
            coverFile.parentFile?.mkdirs()
            coverFile.writeBytes(coverImage.data)
            return coverFile.absolutePath
        } catch (_: Exception) {
            return null
        }
    }

    private fun getExtension(mediaType: String?): String {
        return when {
            mediaType?.contains("png") == true -> "png"
            mediaType?.contains("gif") == true -> "gif"
            mediaType?.contains("webp") == true -> "webp"
            else -> "jpg"
        }
    }

    fun buildChapterList(book: Book, contentDir: File): List<ChapterInfo> {
        return book.spine.spineReferences.mapIndexedNotNull { index, ref ->
            val resource = ref.resource ?: return@mapIndexedNotNull null
            val href = resource.href ?: return@mapIndexedNotNull null
            val charCount = countCharacters(resource)

            val title = book.tableOfContents.tocReferences
                .find { it.resource?.href == href }
                ?.title

            ChapterInfo(
                index = index,
                href = href,
                title = title ?: "Chapter ${index + 1}",
                characterCount = charCount
            )
        }
    }

    fun buildChapterListFromFile(epubFile: File, contentDir: File): List<ChapterInfo> {
        val book = EpubReader().readEpub(FileInputStream(epubFile))
        return buildChapterList(book, contentDir)
    }

    private fun countCharacters(resource: Resource): Int {
        return try {
            val html = String(resource.data, Charsets.UTF_8)
            val text = html
                .replace(Regex("<rt[^>]*>.*?</rt>"), "")
                .replace(Regex("<rp[^>]*>.*?</rp>"), "")
                .replace(Regex("<[^>]+>"), "")
                .replace(Regex("&[a-zA-Z]+;"), " ")
            text.count { !it.isWhitespace() }
        } catch (_: Exception) {
            0
        }
    }

    fun getSpineHref(epubFile: File, chapterIndex: Int): String? {
        val book = EpubReader().readEpub(FileInputStream(epubFile))
        return book.spine.spineReferences.getOrNull(chapterIndex)?.resource?.href
    }
}
