package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.core.testing.FakeSundaySetlistRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.FakeSetlistSaveRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.DraftRow
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistFailure
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistResult
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SetlistEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SaveSundaySetlistUseCaseTest {

    private val sunday = LocalDate.of(2026, 10, 4)
    private val rows = listOf(
        DraftRow(1, 12, " G ", true),
        DraftRow(2, null, "", false),
        DraftRow(3, 55, "A#", false),
        DraftRow(4, null, "", false),
    )

    @Test
    fun `only filled rows are sent, in their positions, and the answer is stored`() = runTest {
        val repository = FakeSetlistSaveRepository()
        val stored = FakeSundaySetlistRepository()

        val result = SaveSundaySetlistUseCase(repository, stored)(sunday, rows)

        assertEquals(
            listOf(SetlistEntry(1, 12, "G"), SetlistEntry(3, 55, "A#")),
            repository.calls.single().second,
        )
        assertEquals(sunday, repository.calls.single().first)
        assertEquals((result as SaveSetlistResult.Saved).setlist, stored.state.value)
    }

    @Test
    fun `a refused save stores nothing`() = runTest {
        val repository = FakeSetlistSaveRepository(SaveSetlistResult.Failed(SaveSetlistFailure.NoPermission))
        val stored = FakeSundaySetlistRepository()

        val result = SaveSundaySetlistUseCase(repository, stored)(sunday, rows)

        assertEquals(SaveSetlistResult.Failed(SaveSetlistFailure.NoPermission), result)
        assertNull(stored.state.value)
    }
}
