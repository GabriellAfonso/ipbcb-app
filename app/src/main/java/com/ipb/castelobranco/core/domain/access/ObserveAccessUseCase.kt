package com.ipb.castelobranco.core.domain.access

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAccessUseCase @Inject constructor(
    private val repository: AccessRepository,
) {
    operator fun invoke(): Flow<Access> = repository.access
}
