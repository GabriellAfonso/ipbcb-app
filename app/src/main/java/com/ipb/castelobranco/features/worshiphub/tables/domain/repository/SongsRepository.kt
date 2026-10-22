package com.ipb.castelobranco.features.worshiphub.tables.domain.repository

import com.ipb.castelobranco.core.domain.repository.AllSongsRepository
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SuggestedSong
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopSong
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopTone
import kotlinx.coroutines.flow.Flow

interface SongsRepository : AllSongsRepository {

    fun observeSongsBySunday(): Flow<SnapshotState<List<SundaySet>>>
    suspend fun refreshSongsBySunday(): RefreshResult

    fun observeTopSongs(): Flow<SnapshotState<List<TopSong>>>
    suspend fun refreshTopSongs(): RefreshResult

    fun observeTopTones(): Flow<SnapshotState<List<TopTone>>>
    suspend fun refreshTopTones(): RefreshResult

    fun observeSuggestedSongs(): Flow<SnapshotState<List<SuggestedSong>>>
    suspend fun refreshSuggestedSongs(): RefreshResult

    /**
     * @param fixedByPosition position to Played id (`id` of the suggested item).
     * @param excludedByPosition position to Song ids (`songId`) not to draw there again.
     */
    suspend fun refreshSuggestedSongs(
        fixedByPosition: Map<Int, Int>,
        excludedByPosition: Map<Int, Set<Int>>,
    ): RefreshResult

    suspend fun preload()
}