package com.ipb.castelobranco.core.domain.worship

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWorshipAccessUseCase @Inject constructor(
    private val repository: WorshipAccessRepository,
) {
    operator fun invoke(): Flow<WorshipAccess> = repository.worshipAccess
}
