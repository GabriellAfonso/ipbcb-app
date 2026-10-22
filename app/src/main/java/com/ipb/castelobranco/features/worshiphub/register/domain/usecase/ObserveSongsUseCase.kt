package com.ipb.castelobranco.features.worshiphub.register.domain.usecase

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.domain.repository.AllSongsRepository
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSongsUseCase @Inject constructor(
    private val repository: AllSongsRepository
) {
    fun observe(): Flow<SnapshotState<List<Song>>> = repository.observeAllSongs()

    suspend fun refresh() {
        repository.refreshAllSongs()
    }
}
