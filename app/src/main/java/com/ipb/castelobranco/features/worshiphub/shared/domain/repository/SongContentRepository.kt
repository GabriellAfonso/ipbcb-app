package com.ipb.castelobranco.features.worshiphub.shared.domain.repository

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import kotlinx.coroutines.flow.Flow

interface SongContentRepository<T> {
    fun observe(): Flow<SnapshotState<List<T>>>
    suspend fun preload()
    suspend fun refresh(): RefreshResult
}
