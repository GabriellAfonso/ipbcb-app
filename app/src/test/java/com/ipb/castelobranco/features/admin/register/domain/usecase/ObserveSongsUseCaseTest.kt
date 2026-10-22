package com.ipb.castelobranco.features.admin.register.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.domain.repository.AllSongsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObserveSongsUseCaseTest {

    private lateinit var repository: AllSongsRepository
    private lateinit var useCase: ObserveSongsUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ObserveSongsUseCase(repository)
    }

    @Test
    fun `observe returns flow from repository`() = runTest {
        val songs = listOf(Song(id = 1, title = "A", artist = "X", categoryName = "Cat"))
        val flow = MutableStateFlow<SnapshotState<List<Song>>>(SnapshotState.Data(songs))
        every { repository.observeAllSongs() } returns flow

        val state = useCase.observe().first()

        assertTrue(state is SnapshotState.Data)
        assertEquals(songs, (state as SnapshotState.Data).value)
    }

    @Test
    fun `refresh delegates to repository`() = runTest {
        coEvery { repository.refreshAllSongs() } returns RefreshResult.Updated

        useCase.refresh()

        coVerify { repository.refreshAllSongs() }
    }
}
