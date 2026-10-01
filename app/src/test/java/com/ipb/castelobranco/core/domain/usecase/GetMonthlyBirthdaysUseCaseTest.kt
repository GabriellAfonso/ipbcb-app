package com.ipb.castelobranco.core.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.testing.FakeMembersRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GetMonthlyBirthdaysUseCaseTest {

    private val currentMonth = LocalDate.now().monthValue
    private val otherMonth = currentMonth % 12 + 1

    @Test
    fun `keeps only the current month from the year snapshot`() = runTest {
        val repository = FakeMembersRepository(
            SnapshotState.Data(
                listOf(
                    Birthday(name = "Now", month = currentMonth, day = 4),
                    Birthday(name = "Later", month = otherMonth, day = 4),
                )
            )
        )

        val state = GetMonthlyBirthdaysUseCase(repository).observe().first()

        assertEquals(listOf("Now"), (state as SnapshotState.Data).value.map { it.name })
    }

    @Test
    fun `passes errors through`() = runTest {
        val repository = FakeMembersRepository(SnapshotState.Error(AppError.Network()))

        val state = GetMonthlyBirthdaysUseCase(repository).observe().first()

        assertTrue(state is SnapshotState.Error)
    }
}
