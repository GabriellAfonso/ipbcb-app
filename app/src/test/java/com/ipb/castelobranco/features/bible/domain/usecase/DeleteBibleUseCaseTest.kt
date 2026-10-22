package com.ipb.castelobranco.features.bible.domain.usecase

import com.ipb.castelobranco.features.bible.domain.download.BibleDownloadScheduler
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteBibleUseCaseTest {

    private val repository: BibleRepository = mockk(relaxed = true)
    private val scheduler: BibleDownloadScheduler = mockk(relaxed = true)
    private val useCase = DeleteBibleUseCase(repository, scheduler)

    @Test
    fun `invoke cancels the download, pauses it and clears the Bible`() = runTest {
        useCase()

        coVerifyOrder {
            scheduler.cancel()
            repository.setAutoDownloadPaused(true)
            repository.clearAll()
        }
    }

    @Test
    fun `invoke never enqueues a new download`() = runTest {
        useCase()

        verify(exactly = 0) { scheduler.enqueueWifiOnly(any(), any()) }
        verify(exactly = 0) { scheduler.enqueueAnyNetwork(any()) }
        coVerify(exactly = 0) { repository.setAutoDownloadPaused(false) }
    }
}
