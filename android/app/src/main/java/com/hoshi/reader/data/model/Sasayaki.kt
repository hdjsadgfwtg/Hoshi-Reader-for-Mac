package com.hoshi.reader.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SasayakiCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

@Serializable
data class SasayakiMatch(
    val cueStartMs: Long,
    val cueEndMs: Long,
    val textStart: Int,
    val textEnd: Int,
    val cueText: String
)

@Serializable
data class SasayakiMatchData(
    val matches: List<SasayakiMatch> = emptyList(),
    val unmatchedCount: Int = 0
)

@Serializable
data class SasayakiConfig(
    val audioPath: String = "",
    val srtPath: String = "",
    val enabled: Boolean = false
)

data class SasayakiPlaybackData(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)
