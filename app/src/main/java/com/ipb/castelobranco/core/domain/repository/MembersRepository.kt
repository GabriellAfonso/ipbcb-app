package com.ipb.castelobranco.core.domain.repository

import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import kotlinx.coroutines.flow.Flow

interface MembersRepository {

    fun observeBirthdays(): Flow<SnapshotState<List<Birthday>>>

    fun getCurrentSnapshot(): SnapshotState<List<Birthday>>

    /** Like [getCurrentSnapshot], but reads the disk cache first when memory is still empty. */
    suspend fun loadCurrentSnapshot(): SnapshotState<List<Birthday>>

    suspend fun preload()

    suspend fun refreshBirthdays(): RefreshResult

    suspend fun clearBirthdaysCache()
}
