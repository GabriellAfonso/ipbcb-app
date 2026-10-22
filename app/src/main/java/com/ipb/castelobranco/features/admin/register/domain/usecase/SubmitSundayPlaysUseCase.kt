package com.ipb.castelobranco.features.admin.register.domain.usecase

import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.features.admin.register.domain.mapper.dateIso
import com.ipb.castelobranco.features.admin.register.domain.mapper.toSundayPlayItems
import com.ipb.castelobranco.features.admin.register.domain.repository.WorshipRegisterRepository
import com.ipb.castelobranco.features.admin.register.domain.validation.MusicRegistrationValidator
import com.ipb.castelobranco.features.admin.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.core.domain.model.Song
import java.time.LocalDate
import timber.log.Timber
import javax.inject.Inject

class SubmitSundayPlaysUseCase @Inject constructor(
    private val repository: WorshipRegisterRepository
) {
    sealed interface Result {
        data class ValidationError(val errorsByPosition: Map<Int, String>) : Result
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    suspend operator fun invoke(
        rows: List<SundaySongRowState>,
        availableSongs: List<Song>,
        selectedDate: LocalDate?
    ): Result {
        val validation = MusicRegistrationValidator.validateSundayRows(rows, availableSongs)
        if (validation.errorsByPosition.isNotEmpty()) {
            return Result.ValidationError(validation.errorsByPosition)
        }

        val dateIso = dateIso(selectedDate)
        val plays = toSundayPlayItems(rows, availableSongs)

        return try {
            repository.pushSundayPlays(date = dateIso, plays = plays).getOrThrow()
            Result.Success
        } catch (t: Throwable) {
            Timber.w(t, "Failed to submit sunday plays")
            val msg = t.toAppError().userMessage?.trim().takeIf { !it.isNullOrBlank() }
                ?: "Erro inesperado ao enviar."
            Result.Failure(msg)
        }
    }
}
