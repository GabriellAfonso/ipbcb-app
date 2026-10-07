package com.ipb.castelobranco.features.admin.register.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
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

/** Deletes the setlist of a pending Sunday. One the server no longer has counts as deleted. */
class DeletePendingSetlistUseCase @Inject constructor(
    private val repository: SetlistConfirmationRepository,
) {
    suspend operator fun invoke(date: LocalDate): Result<Unit> = repository.delete(date).recoverCatching { error ->
        val appError = error.toAppError()
        if (appError !is AppError.Server || appError.code != HTTP_NOT_FOUND) throw appError
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
