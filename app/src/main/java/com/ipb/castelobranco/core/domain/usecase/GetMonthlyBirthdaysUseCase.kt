package com.ipb.castelobranco.core.domain.usecase

import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.repository.MembersRepository
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** The current month's slice of the year snapshot. */
class GetMonthlyBirthdaysUseCase @Inject constructor(
    private val repository: MembersRepository,
) {
    fun observe(): Flow<SnapshotState<List<Birthday>>> = repository.observeBirthdays().map { state ->
        if (state is SnapshotState.Data) {
            val month = LocalDate.now().monthValue
            SnapshotState.Data(state.value.filter { it.month == month })
        } else {
            state
        }
    }

    suspend fun refresh(): RefreshResult = repository.refreshBirthdays()
}
