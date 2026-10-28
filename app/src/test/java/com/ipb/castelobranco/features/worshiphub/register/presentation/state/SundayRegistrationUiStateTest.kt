package com.ipb.castelobranco.features.worshiphub.register.presentation.state

import com.ipb.castelobranco.core.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SundayRegistrationUiStateTest {

    private val agnus = Song(id = 1, title = "Agnus Dei", artist = "Michael W. Smith", categoryName = "")

    private fun state(vararg rows: SundaySongRowState) = SundayRegistrationUiState(
        availableSongs = listOf(agnus),
        selectedDate = LocalDate.of(2026, 10, 25),
        sundayRows = rows.toList(),
    )

    @Test
    fun `date title spells the weekday in portuguese`() {
        assertEquals("Domingo, 25/10/2026", state().dateTitle)
    }

    @Test
    fun `missing text names what blocks the register`() {
        assertEquals("escolha ao menos uma música", state(SundaySongRowState(1)).missingText)
        assertEquals(
            "complete música e tom da posição 2",
            state(SundaySongRowState(1), SundaySongRowState(2, tone = "G")).missingText,
        )
        assertEquals(
            "complete música e tom das posições 1 e 2",
            state(SundaySongRowState(1, tone = "A"), SundaySongRowState(2, tone = "G")).missingText,
        )
        assertNull(state(SundaySongRowState(1, "Agnus Dei [Michael W. Smith]", 1, "A")).missingText)
    }

    @Test
    fun `confirmation lists only the complete rows, numbered`() {
        val result = state(
            SundaySongRowState(1),
            SundaySongRowState(2, "Agnus Dei [Michael W. Smith]", 1, "A"),
        ).confirmLines

        assertEquals(listOf("1. Agnus Dei · A"), result)
    }

    @Test
    fun `a setlist song not in the catalog yet is read from its label`() {
        val row = SundaySongRowState(1, "Oceans [Hillsong]", 9, "D")

        val song = state(row).songOf(row)

        assertEquals("Oceans", song?.title)
        assertEquals("Hillsong", song?.artist)
    }
}
