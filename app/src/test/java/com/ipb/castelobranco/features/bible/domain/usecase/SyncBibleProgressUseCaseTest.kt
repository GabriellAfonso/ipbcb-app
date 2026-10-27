package com.ipb.castelobranco.features.bible.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.bible.domain.model.BibleProgress
import com.ipb.castelobranco.features.bible.domain.model.ProgressTarget
import com.ipb.castelobranco.features.bible.domain.repository.BibleProgressRepository
import com.ipb.castelobranco.features.bible.domain.repository.ProgressSyncOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncBibleProgressUseCaseTest {

    private class FakeRepository(var queued: Int, private val outcomes: ArrayDeque<ProgressSyncOutcome>) :
        BibleProgressRepository {
        var refreshes = 0
        override val progress: Flow<BibleProgress> = flowOf(BibleProgress.EMPTY)
        override suspend fun refresh() { refreshes++ }
        override suspend fun mark(targets: List<ProgressTarget>, done: Boolean) = Unit
        override suspend fun queuedCount(): Int = queued
        override suspend fun syncOnce(): ProgressSyncOutcome {
            val outcome = outcomes.removeFirst()
            if (outcome !is ProgressSyncOutcome.Deferred) queued = (queued - CHUNK).coerceAtLeast(0)
            return outcome
        }
        override suspend fun flushAndClear() = Unit
    }

    @Test
    fun `drains every chunk then fetches the account's state`() = runTest {
        val repository = FakeRepository(
            queued = 2 * CHUNK,
            outcomes = ArrayDeque(listOf(ProgressSyncOutcome.Delivered, ProgressSyncOutcome.Discarded)),
        )

        assertTrue(SyncBibleProgressUseCase(repository)())
        assertEquals(1, repository.refreshes)
    }

    @Test
    fun `offline keeps the queue and asks for a retry`() = runTest {
        val repository = FakeRepository(
            queued = 1,
            outcomes = ArrayDeque(listOf(ProgressSyncOutcome.Deferred(AppError.Network()))),
        )

        assertFalse(SyncBibleProgressUseCase(repository)())
        assertEquals(0, repository.refreshes)
        assertEquals(1, repository.queued)
    }

    @Test
    fun `an empty queue succeeds without calls`() = runTest {
        val repository = FakeRepository(queued = 0, outcomes = ArrayDeque())

        assertTrue(SyncBibleProgressUseCase(repository)())
        assertEquals(0, repository.refreshes)
    }

    private companion object {
        const val CHUNK = 500
    }
}
