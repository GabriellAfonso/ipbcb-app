package com.ipb.castelobranco.features.bible.domain.usecase

import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAndRedownloadBibleUseCaseTest {

    private val repository: BibleRepository = mockk(relaxed = true)
    private val autoDownload: BibleAutoDownloadUseCase = mockk(relaxed = true)
    private val useCase = DeleteAndRedownloadBibleUseCase(repository, autoDownload)

    @Test
    fun `invoke clears repository before enqueuing download`() = runTest {
        coEvery { repository.clearAll() } just runs

        useCase()

        coVerifyOrder {
            repository.clearAll()
            autoDownload.enqueueAnyNetwork(force = true)
        }
    }

    @Test
    fun `invoke calls clearAll on repository`() = runTest {
        useCase()

        coVerify(exactly = 1) { repository.clearAll() }
    }

    @Test
    fun `invoke enqueues with force true`() = runTest {
        useCase()

        coVerify(exactly = 1) { autoDownload.enqueueAnyNetwork(force = true) }
    }
}
