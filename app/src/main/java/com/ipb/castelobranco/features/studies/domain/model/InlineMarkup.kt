package com.ipb.castelobranco.features.studies.domain.model

/** A run of lesson text with its formatting. */
data class InlineSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
)

/**
 * The only markup lesson texts accept: `**bold**` and `*italic*`. A marker without its closing pair stays as
 * typed, and anything else is literal text.
 */
object InlineMarkup {

    private const val BOLD = "**"
    private const val ITALIC = "*"

    fun parse(text: String): List<InlineSpan> {
        val spans = mutableListOf<InlineSpan>()
        val plain = StringBuilder()
        var i = 0
        while (i < text.length) {
            val marker = when {
                text.startsWith(BOLD, i) -> BOLD
                text.startsWith(ITALIC, i) -> ITALIC
                else -> null
            }
            val close = marker?.let { closingIndex(text, i + it.length, it) }
            if (marker == null || close == null) {
                plain.append(text[i])
                i++
                continue
            }
            if (plain.isNotEmpty()) {
                spans += InlineSpan(plain.toString())
                plain.clear()
            }
            val inner = text.substring(i + marker.length, close)
            spans += InlineSpan(inner, bold = marker == BOLD, italic = marker == ITALIC)
            i = close + marker.length
        }
        if (plain.isNotEmpty()) spans += InlineSpan(plain.toString())
        return spans
    }

    /** Where [marker] closes after [from], with something in between; for `*`, a `**` is not a close. */
    private fun closingIndex(text: String, from: Int, marker: String): Int? {
        var j = from
        while (j < text.length) {
            val found = text.indexOf(marker, j)
            if (found < 0) return null
            val isPartOfBold = marker == ITALIC && text.startsWith(BOLD, found)
            if (isPartOfBold) {
                j = found + BOLD.length
                continue
            }
            return if (found > from) found else null
        }
        return null
    }
}
