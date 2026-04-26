package com.hoshi.reader.core.anki

object Handlebars {

    private val PATTERN = Regex("\\{\\{(.+?)\\}\\}")

    fun render(template: String, variables: Map<String, String>): String {
        return PATTERN.replace(template) { match ->
            val key = match.groupValues[1].trim()
            variables[key] ?: ""
        }
    }

    fun buildVariables(
        expression: String = "",
        reading: String = "",
        matched: String = "",
        furiganaPlain: String = "",
        glossary: String = "",
        glossaryFirst: String = "",
        singleGlossaries: Map<String, String> = emptyMap(),
        frequenciesHtml: String = "",
        freqHarmonicRank: String = "",
        pitchPositions: String = "",
        pitchCategories: String = "",
        sentence: String = "",
        audio: String = "",
        popupSelectionText: String = "",
        bookTitle: String = "",
        chapterTitle: String = ""
    ): Map<String, String> {
        val vars = mutableMapOf(
            "expression" to expression,
            "reading" to reading,
            "matched" to matched,
            "furigana-plain" to furiganaPlain,
            "glossary" to glossary,
            "glossary-first" to glossaryFirst,
            "frequencies" to frequenciesHtml,
            "frequency-harmonic-rank" to freqHarmonicRank,
            "pitch-accent-positions" to pitchPositions,
            "pitch-accent-categories" to pitchCategories,
            "sentence" to sentence,
            "cloze-prefix" to "",
            "cloze-body" to matched,
            "cloze-suffix" to "",
            "popup-selection-text" to popupSelectionText,
            "audio" to audio,
            "book-title" to bookTitle,
            "chapter-title" to chapterTitle,
        )

        if (sentence.isNotEmpty() && matched.isNotEmpty()) {
            val idx = sentence.indexOf(matched)
            if (idx >= 0) {
                vars["cloze-prefix"] = sentence.substring(0, idx)
                vars["cloze-body"] = matched
                vars["cloze-suffix"] = sentence.substring(idx + matched.length)
                vars["sentence-cloze"] = "${vars["cloze-prefix"]}<b>${matched}</b>${vars["cloze-suffix"]}"
            } else {
                vars["sentence-cloze"] = sentence
            }
        }

        for ((dictName, html) in singleGlossaries) {
            vars["glossary-$dictName"] = html
        }

        return vars
    }
}
