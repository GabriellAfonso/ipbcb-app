package com.ipb.castelobranco.features.admin.register.domain

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import com.ipb.castelobranco.features.admin.register.domain.repository.SetlistConfirmationRepository
import java.time.LocalDate

class FakeSetlistConfirmationRepository(
    var byDateResult: (LocalDate) -> Result<SundaySetlist> = { Result.failure(AppError.Server(code = 404)) },
    var pendingResult: Result<List<SundaySetlist>> = Result.success(emptyList()),
    var deleteResult: (LocalDate) -> Result<Unit> = { Result.success(Unit) },
) : SetlistConfirmationRepository {

    var byDateCalls = 0
        private set
    var pendingCalls = 0
        private set
    val deletedDates = mutableListOf<LocalDate>()

    override suspend fun byDate(date: LocalDate): Result<SundaySetlist> {
        byDateCalls++
        return byDateResult(date)
    }

    override suspend fun pending(): Result<List<SundaySetlist>> {
        pendingCalls++
        return pendingResult
    }

    override suspend fun delete(date: LocalDate): Result<Unit> {
        deletedDates += date
        return deleteResult(date)
    }
}
