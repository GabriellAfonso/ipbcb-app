package com.ipb.castelobranco.features.admin.register.domain.usecase

import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import com.ipb.castelobranco.features.admin.register.domain.repository.SetlistConfirmationRepository
import java.time.LocalDate
import javax.inject.Inject

/** The setlist that pre-fills "Registrar domingo" for [date]. */
class GetSetlistForDateUseCase @Inject constructor(
    private val repository: SetlistConfirmationRepository,
) {
    suspend operator fun invoke(date: LocalDate): Result<SundaySetlist> = repository.byDate(date)
}

/** Sundays whose played songs are still to be registered, newest first. */
class GetPendingConfirmationsUseCase @Inject constructor(
    private val repository: SetlistConfirmationRepository,
) {
    suspend operator fun invoke(): Result<List<LocalDate>> = repository.pending().map { setlists ->
        setlists.map { it.date }.distinct().sortedDescending()
    }
}
