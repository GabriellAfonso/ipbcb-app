package com.ipb.castelobranco.features.bible.domain.model

/**
 * Última posição lida pelo usuário. Persiste em [BiblePreferences].
 * Default: Gênesis 1:1.
 */
data class BibleReadingPosition(
    val bookAbbrev: String,
    val chapter: Int,
    val verse: Int,
) {
    companion object {
        const val DEFAULT_BOOK_ABBREV: String = "gn"
        const val DEFAULT_CHAPTER: Int = 1
        const val DEFAULT_VERSE: Int = 1

        fun default(): BibleReadingPosition =
            BibleReadingPosition(DEFAULT_BOOK_ABBREV, DEFAULT_CHAPTER, DEFAULT_VERSE)
    }
}
