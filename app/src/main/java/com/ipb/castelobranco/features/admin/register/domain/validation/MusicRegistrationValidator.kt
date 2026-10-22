package com.ipb.castelobranco.features.admin.register.domain.validation

import com.ipb.castelobranco.features.admin.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.core.domain.model.Song

object MusicRegistrationValidator {

    data class SundayRowsValidation(
        val errorsByPosition: Map<Int, String>,
        val incompletePositions: Set<Int>,
        val hasAtLeastOneCompleteValidRow: Boolean
    )

    /**
     * Regras:
     * - Se música e tom vazios: OK (linha ignorada).
     * - Se apenas um dos dois foi preenchido: ERRO (linha incompleta).
     * - Se ambos preenchidos: precisa ter selectedSongId válido e tom não vazio.
     */
    fun validateSundayRows(rows: List<SundaySongRowState>, availableSongs: List<Song>): SundayRowsValidation {
        val errors = mutableMapOf<Int, String>()
        val incomplete = mutableSetOf<Int>()
        var hasOneValidComplete = false

        rows.forEach { row ->
            val hasAnySongInput = row.songQuery.isNotBlank() || row.selectedSongId != null
            val hasTone = row.tone.isNotBlank()

            val bothEmpty = !hasAnySongInput && !hasTone
            if (bothEmpty) return@forEach

            val onlyOneFilled = (hasAnySongInput && !hasTone) || (!hasAnySongInput && hasTone)
            if (onlyOneFilled) {
                incomplete += row.position
                errors[row.position] = "Posição ${row.position} incompleta: preencha música e tom."
                return@forEach
            }


            // Tom já é não vazio aqui
            hasOneValidComplete = true
        }

        return SundayRowsValidation(
            errorsByPosition = errors,
            incompletePositions = incomplete,
            hasAtLeastOneCompleteValidRow = hasOneValidComplete
        )
    }
}