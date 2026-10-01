package com.ipb.castelobranco.core.domain.usecase

import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.repository.MembersRepository
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Every birthday of the year, ordered by month then day. Same snapshot as the home card. */
class GetYearBirthdaysUseCase @Inject constructor(
    private val repository: MembersRepository,
) {
    fun observe(): Flow<SnapshotState<List<Birthday>>> = repository.observeBirthdays()

    fun current(): SnapshotState<List<Birthday>> = repository.getCurrentSnapshot()

    suspend fun refresh(): RefreshResult = repository.refreshBirthdays()
}
