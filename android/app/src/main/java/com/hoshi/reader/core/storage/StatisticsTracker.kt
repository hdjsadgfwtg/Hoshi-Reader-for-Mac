package com.hoshi.reader.core.storage

import com.hoshi.reader.data.model.Statistics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsTracker(
    private val store: JsonStore<List<Statistics>>
) {
    private var sessionStartMs: Long = 0
    private var sessionCharsRead: Int = 0

    fun startSession() {
        sessionStartMs = System.currentTimeMillis()
        sessionCharsRead = 0
    }

    fun addCharactersRead(count: Int) {
        sessionCharsRead += count
    }

    suspend fun endSession() {
        if (sessionStartMs == 0L) return
        val elapsedSeconds = (System.currentTimeMillis() - sessionStartMs) / 1000.0
        if (elapsedSeconds < 5) return

        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val speed = if (elapsedSeconds > 0) sessionCharsRead / (elapsedSeconds / 60.0) else 0.0

        store.update { stats ->
            val existing = stats.find { it.dateKey == dateKey }
            if (existing != null) {
                stats.map {
                    if (it.dateKey == dateKey) {
                        it.copy(
                            charactersRead = it.charactersRead + sessionCharsRead,
                            readingTimeSeconds = it.readingTimeSeconds + elapsedSeconds,
                            minSpeed = if (speed > 0 && (it.minSpeed == 0.0 || speed < it.minSpeed)) speed else it.minSpeed,
                            maxSpeed = maxOf(it.maxSpeed, speed),
                            lastSpeed = speed
                        )
                    } else it
                }
            } else {
                stats + Statistics(
                    dateKey = dateKey,
                    charactersRead = sessionCharsRead,
                    readingTimeSeconds = elapsedSeconds,
                    minSpeed = speed,
                    maxSpeed = speed,
                    lastSpeed = speed
                )
            }
        }

        sessionStartMs = 0
        sessionCharsRead = 0
    }
}
