package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.core.domain.util.normalize
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import javax.inject.Inject

/** A Sunday with its date, titles, artists and tones already normalized for search. */
data class SearchableSunday(
    val sunday: SundaySet,
    val fields: List<String>,
)

/**
 * Searches the "Últimos Domingos" list. [index] normalizes once per data load, so each query only runs
 * `contains` over prepared strings instead of normalizing the whole history on every keystroke.
 */
class SearchSundaysUseCase @Inject constructor() {

    fun index(sundays: List<SundaySet>): List<SearchableSunday> = sundays.map { sunday ->
        val fields = buildList {
            add(sunday.date.normalize())
            sunday.songs.forEach { song ->
                add(song.title.normalize())
                add(song.artist.normalize())
                add(song.tone.normalize())
            }
        }
        SearchableSunday(sunday, fields)
    }

    /** The Sundays where any field contains [query]; all of them when [query] is blank. */
    operator fun invoke(index: List<SearchableSunday>, query: String): List<SundaySet> {
        val nq = query.trim().normalize()
        if (nq.isBlank()) return index.map { it.sunday }
        return index
            .filter { entry -> entry.fields.any { it.contains(nq, ignoreCase = true) } }
            .map { it.sunday }
    }
}
