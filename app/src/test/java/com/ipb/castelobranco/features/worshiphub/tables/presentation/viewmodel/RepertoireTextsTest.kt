package com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistFailure
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RepertoireTextsTest {

    private val sunday = LocalDate.of(2026, 10, 4)

    @Test
    fun `dates are shown as day and month`() {
        assertEquals("Salvar repertório de domingo 04/10?", RepertoireTexts.confirmTitle(sunday))
        assertEquals("Repertório de domingo 04/10 salvo.", RepertoireTexts.saved(sunday))
    }

    @Test
    fun `each failure has its text`() {
        assertEquals(
            "Você não tem permissão para salvar o repertório.",
            RepertoireTexts.failure(SaveSetlistFailure.NoPermission),
        )
        assertEquals("Sem conexão. Tente novamente.", RepertoireTexts.failure(SaveSetlistFailure.NoConnection))
        assertEquals(
            "1 música não foi encontrada. Atualize a lista e gere o repertório de novo.",
            RepertoireTexts.failure(SaveSetlistFailure.MissingSongs(1)),
        )
        assertEquals(
            "2 músicas não foram encontradas. Atualize a lista e gere o repertório de novo.",
            RepertoireTexts.failure(SaveSetlistFailure.MissingSongs(2)),
        )
    }

    @Test
    fun `invalid shows the server detail`() {
        val error = AppError.Server(code = 400, userMessage = "Data deve ser um domingo.")
        assertEquals("Data deve ser um domingo.", RepertoireTexts.failure(SaveSetlistFailure.Invalid(error)))
    }
}
