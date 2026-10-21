package com.ipb.castelobranco.features.events.domain.repository

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.events.domain.model.Event
import kotlinx.coroutines.flow.Flow

/** The public events list, kept on disk so the carousel shows it offline. */
interface EventsRepository {
    fun observeEvents(): Flow<SnapshotState<List<Event>>>

    suspend fun preload()

    /** A call made while another one is running is skipped. */
    suspend fun refreshEvents(): RefreshResult

    /** Forgets the saved list, on disk and in memory. */
    suspend fun clear()
}
