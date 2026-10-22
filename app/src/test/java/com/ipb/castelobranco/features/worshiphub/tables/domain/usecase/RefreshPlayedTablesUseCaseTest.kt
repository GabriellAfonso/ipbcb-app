package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RefreshPlayedTablesUseCaseTest {

    private val repository = mockk<SongsRepository>(relaxed = true)

    @Test
    fun `re-reads the three tables built from the played songs`() = runTest {
        RefreshPlayedTablesUseCase(repository)()

        coVerify(exactly = 1) { repository.refreshSongsBySunday() }
        coVerify(exactly = 1) { repository.refreshTopSongs() }
        coVerify(exactly = 1) { repository.refreshTopTones() }
        coVerify(exactly = 0) { repository.refreshAllSongs() }
    }
}
