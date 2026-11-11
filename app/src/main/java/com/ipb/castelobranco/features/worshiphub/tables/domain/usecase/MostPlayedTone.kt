package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySetItem
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/**
 * The tone [song] was played in most often across these Sundays, or "" when it was never played. A tie goes to the
 * tone that appears first, i.e. the most recent Sunday.
 */
fun List<SundaySet>.mostPlayedToneOf(song: Song): String =
    asSequence()
        .flatMap { it.songs.asSequence() }
        .filter { it.isSameSongAs(song) && it.tone.isNotBlank() }
        .groupingBy { it.tone }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        .orEmpty()

/** By id when the history has one; older entries without an id fall back to title and artist. */
private fun SundaySetItem.isSameSongAs(song: Song): Boolean =
    if (songId > 0) songId == song.id else title == song.title && artist == song.artist

/** The played Sundays (offline snapshot) that [mostPlayedToneOf] reads; loading and errors keep the last list. */
class ObservePlayedSundaysUseCase @Inject constructor(
    private val repository: SongsRepository,
) {
    operator fun invoke(): Flow<List<SundaySet>> = repository.observeSongsBySunday()
        .filterIsInstance<SnapshotState.Data<List<SundaySet>>>()
        .map { it.value }
}
