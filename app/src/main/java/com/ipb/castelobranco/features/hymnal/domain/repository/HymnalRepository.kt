package com.ipb.castelobranco.features.hymnal.domain.repository

import com.ipb.castelobranco.core.domain.repository.HymnCatalogRepository
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import kotlinx.coroutines.flow.Flow

/**
 * Owns the hymnal for the reader experience, and also serves the catalogue to the rest of the app
 * through [HymnCatalogRepository] — the same arrangement `SongsRepository` has with
 * `AllSongsRepository`, so no other feature has to import this one.
 */
interface HymnalRepository : HymnCatalogRepository {
    fun observeHymnal(): Flow<SnapshotState<List<Hymn>>>
    suspend fun refreshHymnal(): RefreshResult
    suspend fun preload()
}