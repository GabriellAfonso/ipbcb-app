package com.ipb.castelobranco.features.worshiphub.lyrics.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.model.Lyrics
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.repository.LyricsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLyricsUseCase @Inject constructor(
    private val repository: LyricsRepository,
) {
    fun observe(): Flow<SnapshotState<List<Lyrics>>> = repository.observe()
    suspend fun refresh(): RefreshResult = repository.refresh()
}
