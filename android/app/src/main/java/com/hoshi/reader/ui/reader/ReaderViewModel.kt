package com.hoshi.reader.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshi.reader.core.epub.EpubParser
import com.hoshi.reader.core.storage.BookStorage
import com.hoshi.reader.core.storage.UserConfig
import com.hoshi.reader.data.model.Bookmark
import com.hoshi.reader.data.model.BookMetadata
import com.hoshi.reader.data.model.ChapterInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookStorage: BookStorage,
    private val epubParser: EpubParser,
    val userConfig: UserConfig
) : ViewModel() {

    private val bookId: String = savedStateHandle.get<String>("bookId") ?: ""

    private val _metadata = MutableStateFlow<BookMetadata?>(null)
    val metadata: StateFlow<BookMetadata?> = _metadata

    private val _chapters = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chapters: StateFlow<List<ChapterInfo>> = _chapters

    private val _currentChapter = MutableStateFlow(0)
    val currentChapter: StateFlow<Int> = _currentChapter

    private val _currentProgress = MutableStateFlow(0.0)
    val currentProgress: StateFlow<Double> = _currentProgress

    private val _chapterHref = MutableStateFlow<String?>(null)
    val chapterHref: StateFlow<String?> = _chapterHref

    val contentDir: File?
        get() = _metadata.value?.contentDirectory?.let { File(it) }

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch(Dispatchers.IO) {
            val meta = bookStorage.metadataStore(bookId).read()
            _metadata.value = meta

            val bookInfo = bookStorage.bookInfoStore(bookId).read()
            _chapters.value = bookInfo.chapters

            val bookmark = bookStorage.bookmarkStore(bookId).read()
            _currentChapter.value = bookmark.chapterIndex
            _currentProgress.value = bookmark.progress

            bookStorage.metadataStore(bookId).update {
                it.copy(lastAccessedAt = System.currentTimeMillis())
            }

            loadChapterHref(bookmark.chapterIndex)
        }
    }

    private fun loadChapterHref(index: Int) {
        val chapters = _chapters.value
        if (index in chapters.indices) {
            _chapterHref.value = chapters[index].href
        }
    }

    fun navigateToChapter(index: Int) {
        if (index < 0 || index >= _chapters.value.size) return
        _currentChapter.value = index
        _currentProgress.value = 0.0
        loadChapterHref(index)
        saveBookmark()
    }

    fun nextChapter() {
        navigateToChapter(_currentChapter.value + 1)
    }

    fun previousChapter() {
        navigateToChapter(_currentChapter.value - 1)
    }

    fun updateProgress(progress: Double) {
        _currentProgress.value = progress
        saveBookmark()
    }

    private fun saveBookmark() {
        viewModelScope.launch(Dispatchers.IO) {
            bookStorage.bookmarkStore(bookId).write(
                Bookmark(
                    chapterIndex = _currentChapter.value,
                    progress = _currentProgress.value
                )
            )
        }
    }
}
