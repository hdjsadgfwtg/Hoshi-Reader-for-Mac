package com.hoshi.reader.core.epub

import io.documentnode.epub4j.domain.Resource
import io.documentnode.epub4j.epub.EpubReader
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

class BookProcessor @Inject constructor() {

    fun extractChapterText(epubFile: File, chapterIndex: Int): String {
        val book = EpubReader().readEpub(FileInputStream(epubFile))
        val resource = book.spine.spineReferences.getOrNull(chapterIndex)?.resource
            ?: return ""
        return extractText(resource)
    }

    fun extractAllText(epubFile: File): List<String> {
        val book = EpubReader().readEpub(FileInputStream(epubFile))
        return book.spine.spineReferences.mapNotNull { ref ->
            ref.resource?.let { extractText(it) }
        }
    }

    private fun extractText(resource: Resource): String {
        return try {
            val html = String(resource.data, Charsets.UTF_8)
            val bodyMatch = Regex("<body[^>]*>(.*?)</body>", RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.getOrNull(1) ?: html

            bodyMatch
                .replace(Regex("<rt[^>]*>.*?</rt>"), "")
                .replace(Regex("<rp[^>]*>.*?</rp>"), "")
                .replace(Regex("<(p|div|h[1-6]|br)[^>]*/?>"), "\n")
                .replace(Regex("</?(p|div|h[1-6])[^>]*>"), "\n")
                .replace(Regex("<[^>]+>"), "")
                .replace(Regex("&nbsp;"), " ")
                .replace(Regex("&[a-zA-Z]+;"), "")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
        } catch (_: Exception) {
            ""
        }
    }
}
