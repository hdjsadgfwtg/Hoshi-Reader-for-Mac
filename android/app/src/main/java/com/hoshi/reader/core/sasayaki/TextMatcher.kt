package com.hoshi.reader.core.sasayaki

import com.hoshi.reader.data.model.SasayakiCue
import com.hoshi.reader.data.model.SasayakiMatch

object TextMatcher {

    private val WHITESPACE = Regex("[\\s\\u3000]+")
    private val PUNCTUATION = Regex("[。、！？「」『』（）…―\\-–—\\[\\]【】〈〉《》〔〕｛｝・:;,.]")

    fun matchCuesToText(cues: List<SasayakiCue>, chapterText: String): List<SasayakiMatch> {
        val normalizedText = normalize(chapterText)
        val matches = mutableListOf<SasayakiMatch>()
        var searchStart = 0

        for (cue in cues) {
            val normalizedCue = normalize(cue.text)
            if (normalizedCue.isEmpty()) continue

            val idx = findBestMatch(normalizedText, normalizedCue, searchStart)
            if (idx >= 0) {
                val originalStart = mapToOriginal(chapterText, normalizedText, idx)
                val originalEnd = mapToOriginal(chapterText, normalizedText, idx + normalizedCue.length)

                matches.add(SasayakiMatch(
                    cueStartMs = cue.startMs,
                    cueEndMs = cue.endMs,
                    textStart = originalStart,
                    textEnd = originalEnd,
                    cueText = cue.text
                ))
                searchStart = idx + normalizedCue.length
            }
        }

        return matches
    }

    private fun normalize(text: String): String {
        return text
            .replace(WHITESPACE, "")
            .replace(PUNCTUATION, "")
            .lowercase()
    }

    private fun findBestMatch(text: String, pattern: String, startFrom: Int): Int {
        val directMatch = text.indexOf(pattern, startFrom)
        if (directMatch >= 0) return directMatch

        val minLen = (pattern.length * 0.6).toInt().coerceAtLeast(3)
        for (len in pattern.length downTo minLen) {
            val sub = pattern.substring(0, len)
            val idx = text.indexOf(sub, startFrom)
            if (idx >= 0) return idx
        }

        return -1
    }

    private fun mapToOriginal(original: String, @Suppress("UNUSED_PARAMETER") normalized: String, normalizedIndex: Int): Int {
        var ni = 0
        for (i in original.indices) {
            if (ni >= normalizedIndex) return i
            val ch = original[i].toString()
            val norm = normalize(ch)
            if (norm.isNotEmpty()) {
                ni += norm.length
            }
        }
        return original.length
    }
}
