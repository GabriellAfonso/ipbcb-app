package com.ipb.castelobranco.features.bible.domain.usecase

import com.ipb.castelobranco.features.bible.domain.download.BibleDownloadScheduler
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BibleAutoDownloadUseCaseTest {

    private val scheduler: BibleDownloadScheduler = mockk(relaxed = true)
    private val repository: BibleRepository = mockk(relaxed = true)
    private val useCase = BibleAutoDownloadUseCase(scheduler, repository)

    @Test
    fun `triggerIfNeeded enqueues when cache is missing some translation`() = runTest {
        every { repository.cachedTranslationsFlow } returns MutableStateFlow(setOf(BibleTranslation.NAA))

        useCase.triggerIfNeeded()

        verify(exactly = 1) { scheduler.enqueueWifiOnly(replaceExisting = false, force = false) }
    }

    @Test
    fun `triggerIfNeeded skips when both translations are cached`() = runTest {
        every { repository.cachedTranslationsFlow } returns MutableStateFlow(BibleTranslation.entries.toSet())

        useCase.triggerIfNeeded()

        verify(exactly = 0) { scheduler.enqueueWifiOnly(any(), any()) }
    }

    @Test
    fun `triggerIfNeeded skips while the Bible deleted by the user is paused`() = runTest {
        every { repository.cachedTranslationsFlow } returns MutableStateFlow(emptySet())
        coEvery { repository.isAutoDownloadPaused() } returns true

        useCase.triggerIfNeeded()

        verify(exactly = 0) { scheduler.enqueueWifiOnly(any(), any()) }
    }

    @Test
    fun `enqueueWifiOnly lifts the pause, then forwards replaceExisting and force`() = runTest {
        useCase.enqueueWifiOnly(replaceExisting = true, force = true)

        coVerifyOrder {
            repository.setAutoDownloadPaused(false)
            scheduler.enqueueWifiOnly(replaceExisting = true, force = true)
        }
    }

    @Test
    fun `enqueueAnyNetwork lifts the pause, then forwards force`() = runTest {
        useCase.enqueueAnyNetwork(force = true)

        coVerifyOrder {
            repository.setAutoDownloadPaused(false)
            scheduler.enqueueAnyNetwork(force = true)
        }
        coVerify(exactly = 0) { repository.setAutoDownloadPaused(true) }
    }
}
