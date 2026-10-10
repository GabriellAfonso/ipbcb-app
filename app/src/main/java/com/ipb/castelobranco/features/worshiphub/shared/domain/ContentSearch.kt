package com.ipb.castelobranco.features.worshiphub.shared.domain

import com.ipb.castelobranco.core.domain.util.normalize

/** Chord chart or lyrics content prepared once for "Buscar na letra". */
class SearchableContent internal constructor(
    private val lines: List<SearchableLine>,
    private val normalizedText: String,
) {
    /**
     * The line to show when every word of [query] appears somewhere in the content, `null` otherwise.
     * The line shown is the one holding the most query words (the first one on a tie).
     */
    fun findSnippet(query: String): String? {
        val words = queryWords(query)
        if (words.isEmpty() || words.any { it !in normalizedText }) return null
        return lines.maxByOrNull { line -> words.count { it in line.normalized } }?.original
    }
}

internal class SearchableLine(val original: String, val normalized: String)

object ContentSearch {

    /** Strips ChordPro chords and directives, then normalizes each line. Run once per data load. */
    fun index(content: String): SearchableContent {
        val lines = content.lines()
            .filterNot { DIRECTIVE.matches(it.trim()) }
            .map { CHORD.replace(it, "").trim() }
            .filter { it.isNotEmpty() }
            .map { SearchableLine(original = it, normalized = it.searchForm()) }
        return SearchableContent(lines, lines.joinToString("\n") { it.normalized })
    }

    private val CHORD = Regex("""\[[^\]]*\]""")
    private val DIRECTIVE = Regex("""\{.*\}""")
}

/** Whether [title] contains the whole [query], the same rule the name search always used. */
fun matchesTitle(title: String, query: String): Boolean =
    title.normalize().contains(query.normalize(), ignoreCase = true)

private fun queryWords(query: String): List<String> =
    query.searchForm().split(WHITESPACE).filter { it.isNotEmpty() }

private fun String.searchForm(): String = normalize().lowercase()

private val WHITESPACE = Regex("""\s+""")
