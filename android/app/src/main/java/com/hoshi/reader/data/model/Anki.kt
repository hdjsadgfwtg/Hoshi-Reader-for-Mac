package com.hoshi.reader.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AnkiConfig(
    val ankiConnectUrl: String = "http://127.0.0.1:8765",
    val deck: String = "",
    val noteType: String = "",
    val fieldMappings: Map<String, String> = emptyMap(),
    val duplicateScope: String = "collection",
    val allowDuplicates: Boolean = false,
    val tags: List<String> = emptyList()
)

@Serializable
data class AnkiWord(
    val expression: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AnkiWordsData(
    val words: List<AnkiWord> = emptyList()
)

@Serializable
data class DictionaryConfig(
    val termOrder: List<DictionaryEntry> = emptyList(),
    val frequencyOrder: List<DictionaryEntry> = emptyList(),
    val pitchOrder: List<DictionaryEntry> = emptyList()
)

@Serializable
data class DictionaryEntry(
    val name: String,
    val enabled: Boolean = true
)

@Serializable
data class DictionaryIndex(
    val title: String,
    val revision: String? = null,
    val format: Int? = null
)
