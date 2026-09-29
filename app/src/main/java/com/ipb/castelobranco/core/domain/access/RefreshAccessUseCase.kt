package com.ipb.castelobranco.core.domain.access

import javax.inject.Inject

/** Re-reads the profile after the backend refused a scoped request, so screens hide what was lost. */
class RefreshAccessUseCase @Inject constructor(
    private val repository: AccessRepository,
) {
    suspend operator fun invoke() = repository.refresh()
}
