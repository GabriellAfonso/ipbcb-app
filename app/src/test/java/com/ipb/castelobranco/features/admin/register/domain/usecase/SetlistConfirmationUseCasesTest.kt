package com.ipb.castelobranco.features.admin.register.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.setlistOf
import com.ipb.castelobranco.features.admin.register.domain.FakeSetlistConfirmationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SetlistConfirmationUseCasesTest {

    private val older = LocalDate.of(2026, 9, 27)
    private val newer = LocalDate.of(2026, 10, 4)

    @Test
    fun `the setlist of a date is returned`() = runTest {
        val repository = FakeSetlistConfirmationRepository(byDateResult = { Result.success(setlistOf(it, 12)) })

        assertEquals(newer, GetSetlistForDateUseCase(repository)(newer).getOrThrow().date)
    }

    @Test
    fun `a failed read is passed on`() = runTest {
        val repository = FakeSetlistConfirmationRepository(byDateResult = { Result.failure(AppError.Network()) })

        assertTrue(GetSetlistForDateUseCase(repository)(newer).exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `pending dates come newest first`() = runTest {
        val repository = FakeSetlistConfirmationRepository(
            pendingResult = Result.success(listOf(setlistOf(older, 1), setlistOf(newer, 2))),
        )

        assertEquals(listOf(newer, older), GetPendingConfirmationsUseCase(repository)().getOrThrow())
    }

    @Test
    fun `a failed pending read is passed on`() = runTest {
        val repository = FakeSetlistConfirmationRepository(pendingResult = Result.failure(AppError.Network()))

        assertTrue(GetPendingConfirmationsUseCase(repository)().exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `deleting a setlist succeeds`() = runTest {
        val repository = FakeSetlistConfirmationRepository()

        assertTrue(DeletePendingSetlistUseCase(repository)(newer).isSuccess)
        assertEquals(listOf(newer), repository.deletedDates)
    }

    @Test
    fun `a setlist already gone counts as deleted`() = runTest {
        val repository = FakeSetlistConfirmationRepository(
            deleteResult = { Result.failure(AppError.Server(code = 404)) },
        )

        assertTrue(DeletePendingSetlistUseCase(repository)(newer).isSuccess)
    }

    @Test
    fun `a failed delete is passed on`() = runTest {
        val repository = FakeSetlistConfirmationRepository(deleteResult = { Result.failure(AppError.Network()) })

        assertTrue(DeletePendingSetlistUseCase(repository)(newer).exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `a server error on delete is passed on`() = runTest {
        val repository = FakeSetlistConfirmationRepository(
            deleteResult = { Result.failure(AppError.Server(code = 500)) },
        )

        val error = DeletePendingSetlistUseCase(repository)(newer).exceptionOrNull()

        assertEquals(500, (error as AppError.Server).code)
    }
}
