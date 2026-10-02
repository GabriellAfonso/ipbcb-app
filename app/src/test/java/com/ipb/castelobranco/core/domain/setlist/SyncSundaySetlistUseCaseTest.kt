package com.ipb.castelobranco.core.domain.setlist

import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.worship.WorshipAccess
import com.ipb.castelobranco.core.testing.FakeSundaySetlistRepository
import com.ipb.castelobranco.core.testing.FakeWorshipAccessRepository
import com.ipb.castelobranco.core.testing.WORSHIP_MEMBER
import com.ipb.castelobranco.core.testing.setlistOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SyncSundaySetlistUseCaseTest {

    private val stored = setlistOf(LocalDate.of(2026, 10, 4), 12)

    private fun useCase(
        repository: FakeSundaySetlistRepository,
        loggedIn: Boolean,
        access: WorshipAccess,
    ) = SyncSundaySetlistUseCase(
        repository = repository,
        sessionPresence = SessionPresenceProvider { loggedIn },
        worshipAccess = FakeWorshipAccessRepository(access),
    )

    @Test
    fun `a worship member with a session reads the current setlist`() = runTest {
        val repository = FakeSundaySetlistRepository(stored)

        val result = useCase(repository, loggedIn = true, access = WORSHIP_MEMBER)()

        assertTrue(result.isSuccess)
        assertEquals(1, repository.refreshCalls)
        assertEquals(0, repository.clearCalls)
    }

    @Test
    fun `not a worship member clears without reading`() = runTest {
        val repository = FakeSundaySetlistRepository(stored)

        useCase(repository, loggedIn = true, access = WorshipAccess.NONE)()

        assertEquals(0, repository.refreshCalls)
        assertNull(repository.state.value)
    }

    @Test
    fun `logged out clears without reading`() = runTest {
        val repository = FakeSundaySetlistRepository(stored)

        useCase(repository, loggedIn = false, access = WORSHIP_MEMBER)()

        assertEquals(0, repository.refreshCalls)
        assertNull(repository.state.value)
    }

    @Test
    fun `a failed read is returned and keeps the stored copy`() = runTest {
        val repository = FakeSundaySetlistRepository(stored).apply {
            refreshResult = Result.failure(AppError.Network())
        }

        val result = useCase(repository, loggedIn = true, access = WORSHIP_MEMBER)()

        assertTrue(result.exceptionOrNull() is AppError.Network)
        assertEquals(stored, repository.state.value)
    }
}
