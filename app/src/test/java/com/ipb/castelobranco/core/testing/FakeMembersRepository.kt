package com.ipb.castelobranco.core.testing

import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.repository.MembersRepository
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMembersRepository(
    initial: SnapshotState<List<Birthday>> = SnapshotState.Loading,
) : MembersRepository {

    val state = MutableStateFlow(initial)
    var refreshCalls = 0
        private set

    override fun observeBirthdays(): Flow<SnapshotState<List<Birthday>>> = state
    override fun getCurrentSnapshot(): SnapshotState<List<Birthday>> = state.value
    override suspend fun preload() = Unit
    override suspend fun refreshBirthdays(): RefreshResult {
        refreshCalls++
        return RefreshResult.Updated
    }
    override suspend fun clearBirthdaysCache() {
        state.value = SnapshotState.Loading
    }
}
