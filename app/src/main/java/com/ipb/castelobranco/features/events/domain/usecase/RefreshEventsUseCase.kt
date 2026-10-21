package com.ipb.castelobranco.features.events.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.features.events.domain.repository.EventsRepository
import javax.inject.Inject

class RefreshEventsUseCase @Inject constructor(
    private val repository: EventsRepository,
) {
    suspend operator fun invoke(): RefreshResult = repository.refreshEvents()
}
