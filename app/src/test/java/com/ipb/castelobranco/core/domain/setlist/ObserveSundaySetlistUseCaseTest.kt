package com.ipb.castelobranco.core.domain.setlist

import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.core.testing.FakeSundaySetlistRepository
import com.ipb.castelobranco.core.testing.setlistOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ObserveSundaySetlistUseCaseTest {

    private val sunday = LocalDate.of(2026, 10, 4)
    private val setlist = setlistOf(sunday, 12, 55)

    private fun useCase(today: LocalDate) =
        ObserveSundaySetlistUseCase(FakeSundaySetlistRepository(setlist), DateProvider { today })

    @Test
    fun `shown before and on its Sunday`() = runTest {
        assertEquals(setlist, useCase(sunday.minusDays(3))().first())
        assertEquals(setlist, useCase(sunday)().first())
    }

    @Test
    fun `hidden from the Monday after`() = runTest {
        assertNull(useCase(sunday.plusDays(1))().first())
    }

    @Test
    fun `nothing stored, nothing shown`() = runTest {
        val useCase = ObserveSundaySetlistUseCase(FakeSundaySetlistRepository(), DateProvider { sunday })
        assertNull(useCase().first())
    }
}
