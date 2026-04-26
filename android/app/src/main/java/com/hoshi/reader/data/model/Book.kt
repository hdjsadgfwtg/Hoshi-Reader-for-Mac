package com.hoshi.reader.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BookMetadata(
    val id: String,
    val title: String,
    val creator: String? = null,
    val language: String? = null,
    val coverImagePath: String? = null,
    val epubPath: String,
    val contentDirectory: String,
    val shelfId: String? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val totalCharacters: Int = 0,
    val readCharacters: Int = 0
)

@Serializable
data class Bookmark(
    val chapterIndex: Int = 0,
    val progress: Double = 0.0,
    val characterOffset: Int = 0
)

@Serializable
data class BookInfo(
    val chapters: List<ChapterInfo> = emptyList()
)

@Serializable
data class ChapterInfo(
    val index: Int,
    val href: String,
    val title: String? = null,
    val characterCount: Int = 0
)

@Serializable
data class BookShelf(
    val id: String,
    val name: String,
    val sortOrder: Int = 0
)

@Serializable
data class ShelvesData(
    val shelves: List<BookShelf> = emptyList()
)
