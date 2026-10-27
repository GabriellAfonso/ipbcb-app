package com.ipb.castelobranco.features.studies.domain.model

/** The verses field of the reference editor: "1-3,5" ⇄ spans. Blank = the whole chapter (no spans). */
object VerseRangesInput {

    private const val SEPARATOR = ","
    private const val DASH = "-"

    /** Null when the text is not a list of verses or ranges (ranges must not go backwards). */
    fun parse(text: String): List<VerseSpan>? {
        if (text.isBlank()) return emptyList()
        return text.split(SEPARATOR).map { part ->
            val bounds = part.trim().split(DASH).map { it.trim().toIntOrNull() ?: return null }
            val span = when (bounds.size) {
                1 -> VerseSpan(bounds[0], bounds[0])
                2 -> VerseSpan(bounds[0], bounds[1])
                else -> return null
            }
            if (span.start < 1 || span.end < span.start) return null
            span
        }
    }

    fun format(spans: List<VerseSpan>): String = spans.joinToString(SEPARATOR) { span ->
        if (span.start == span.end) span.start.toString() else "${span.start}$DASH${span.end}"
    }
}
