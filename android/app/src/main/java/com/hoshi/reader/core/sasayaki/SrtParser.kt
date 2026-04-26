package com.hoshi.reader.core.sasayaki

import com.hoshi.reader.data.model.SasayakiCue

object SrtParser {

    fun parse(srtContent: String): List<SasayakiCue> {
        val cues = mutableListOf<SasayakiCue>()
        val blocks = srtContent.replace("\r\n", "\n").trim().split("\n\n")

        for (block in blocks) {
            val lines = block.trim().split("\n")
            if (lines.size < 3) continue

            val timeLine = lines[1]
            val arrow = timeLine.indexOf("-->")
            if (arrow < 0) continue

            val startMs = parseTimestamp(timeLine.substring(0, arrow).trim()) ?: continue
            val endMs = parseTimestamp(timeLine.substring(arrow + 3).trim()) ?: continue
            val text = lines.drop(2).joinToString("\n").trim()

            if (text.isNotEmpty()) {
                cues.add(SasayakiCue(
                    startMs = startMs,
                    endMs = endMs,
                    text = text
                ))
            }
        }

        return cues
    }

    private fun parseTimestamp(ts: String): Long? {
        val parts = ts.replace(",", ".").split(":")
        if (parts.size != 3) return null
        return try {
            val hours = parts[0].trim().toLong()
            val minutes = parts[1].trim().toLong()
            val secAndMs = parts[2].trim().split(".")
            val seconds = secAndMs[0].toLong()
            val millis = if (secAndMs.size > 1) {
                secAndMs[1].padEnd(3, '0').take(3).toLong()
            } else 0L
            hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
        } catch (_: Exception) {
            null
        }
    }
}
