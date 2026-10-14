package com.ipb.castelobranco.features.worshiphub.tables.domain.model

/**
 * Song ids "Gerar" already showed in each free position during this generation session, sent as `exclude`
 * so the backend draws something new. Lives only while the screen is open — never persisted.
 */
data class ShownSuggestions(private val songIdsByPosition: Map<Int, Set<Int>> = emptyMap()) {

    /**
     * Pairs to exclude, only for [freePositions], plus [everywhere] in each of them; positions left with
     * nothing to exclude are left out.
     */
    fun excludedFor(freePositions: Set<Int>, everywhere: Set<Int> = emptySet()): Map<Int, Set<Int>> =
        freePositions
            .associateWith { position -> songIdsByPosition[position].orEmpty() + everywhere }
            .filterValues { it.isNotEmpty() }

    /**
     * A song that comes back to a position where it was already shown means the backend ran out of options
     * there and ignored the exclusion, so the position starts over with just that song.
     */
    fun record(position: Int, songId: Int): ShownSuggestions {
        val shown = songIdsByPosition[position].orEmpty()
        val next = if (songId in shown) setOf(songId) else shown + songId
        return copy(songIdsByPosition = songIdsByPosition + (position to next))
    }

    /** A fixed position is no longer drawn, so its list is dropped. */
    fun fix(position: Int): ShownSuggestions = copy(songIdsByPosition = songIdsByPosition - position)

    /** An unfixed position starts over with only the song it holds now. */
    fun unfix(position: Int, currentSongId: Int?): ShownSuggestions =
        copy(songIdsByPosition = songIdsByPosition + (position to setOfNotNull(currentSongId)))
}
