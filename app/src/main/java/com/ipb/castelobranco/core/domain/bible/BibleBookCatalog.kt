package com.ipb.castelobranco.core.domain.bible

import kotlinx.coroutines.flow.Flow

/** A Bible book as other features need it: to pick a passage, not to read it. */
data class BibleBookInfo(
    /** As references carry it: "Gn", "1Tn". */
    val abbrev: String,
    val name: String,
    /** Verses of each chapter; position `i` = chapter `i + 1`. */
    val verseCounts: List<Int>,
) {
    val chapterCount: Int get() = verseCounts.size

    fun verseCount(chapter: Int): Int? = verseCounts.getOrNull(chapter - 1)
}

/**
 * The Bible's book list for features that cannot import the Bible feature (the Sunday school reference editor).
 * Implemented by `features/bible`.
 */
interface BibleBookCatalog {
    /** Canonical order; empty until the Bible index is on the device. */
    val books: Flow<List<BibleBookInfo>>

    /** Loads the index from the device when nothing asked for it yet. */
    suspend fun load()
}
