package com.ipb.castelobranco.core.domain.worship

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWorshipAccessUseCase @Inject constructor(
    private val repository: WorshipAccessRepository,
) {
    operator fun invoke(): Flow<WorshipAccess> = repository.worshipAccess

    /** The flags in memory now, for a screen's first frame. */
    fun current(): WorshipAccess = repository.peek()
}
