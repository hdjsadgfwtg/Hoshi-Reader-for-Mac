package com.hoshi.reader.ui.bookshelf

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshi.reader.core.epub.EpubParser
import com.hoshi.reader.core.storage.BookStorage
import com.hoshi.reader.data.model.BookInfo
import com.hoshi.reader.data.model.BookMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookStorage: BookStorage,
    private val epubParser: EpubParser
) : ViewModel() {

    private val _books = MutableStateFlow<List<BookMetadata>>(emptyList())
    val books: StateFlow<List<BookMetadata>> = _books

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            _books.value = bookStorage.getAllBooks()
        }
    }

    fun importEpub(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _importing.value = true
            _error.value = null
            try {
                Log.d("BookshelfVM", "importEpub: uri=$uri")
                val (bookId, contentDir) = bookStorage.importEpub(uri)
                Log.d("BookshelfVM", "importEpub: bookId=$bookId, contentDir=$contentDir")
                val epubFile = File(bookStorage.getBookDir(bookId), "book.epub")
                val result = epubParser.parse(epubFile, contentDir)
                Log.d("BookshelfVM", "importEpub: title=${result.title}, chapters=${result.chapters.size}")

                val metadata = BookMetadata(
                    id = bookId,
                    title = result.title,
                    creator = result.creator,
                    language = result.language,
                    coverImagePath = result.coverImagePath,
                    epubPath = epubFile.absolutePath,
                    contentDirectory = result.opfDir,
                    totalCharacters = result.totalCharacters
                )

                bookStorage.metadataStore(bookId).write(metadata)
                bookStorage.bookInfoStore(bookId).write(BookInfo(chapters = result.chapters))

                loadBooks()
            } catch (e: Exception) {
                Log.e("BookshelfVM", "importEpub failed", e)
                _error.value = e.message ?: "Import failed"
            } finally {
                _importing.value = false
            }
        }
    }

    fun deleteBook(book: BookMetadata) {
        viewModelScope.launch(Dispatchers.IO) {
            bookStorage.deleteBook(book.id)
            loadBooks()
        }
    }

    fun clearError() {
        _error.value = null
    }
}
