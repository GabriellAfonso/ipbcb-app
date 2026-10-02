package com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel

import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistFailure
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Texts of the repertoire save, in one place so they are tested together. */
object RepertoireTexts {

    private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

    fun dayMonth(date: LocalDate): String = date.format(DAY_MONTH)

    fun confirmTitle(date: LocalDate) = "Salvar repertório de domingo ${dayMonth(date)}?"

    const val CONFIRM_BODY = "O repertório salvo é enviado ao ministério de Louvor e substitui o que já " +
        "estiver salvo para esse domingo."

    fun saved(date: LocalDate) = "Repertório de domingo ${dayMonth(date)} salvo."

    fun failure(failure: SaveSetlistFailure): String = when (failure) {
        SaveSetlistFailure.NoPermission -> "Você não tem permissão para salvar o repertório."
        is SaveSetlistFailure.Invalid -> failure.error.toUserMessage()
        is SaveSetlistFailure.MissingSongs -> missingSongs(failure.count)
        SaveSetlistFailure.NoConnection -> "Sem conexão. Tente novamente."
        is SaveSetlistFailure.Other -> failure.error.toUserMessage()
    }

    private fun missingSongs(count: Int) = if (count == 1) {
        "1 música não foi encontrada. Atualize a lista e gere o repertório de novo."
    } else {
        "$count músicas não foram encontradas. Atualize a lista e gere o repertório de novo."
    }
}
