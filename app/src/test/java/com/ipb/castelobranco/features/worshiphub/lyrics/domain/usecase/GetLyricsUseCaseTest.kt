package com.ipb.castelobranco.features.worshiphub.lyrics.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.model.Lyrics
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.repository.LyricsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetLyricsUseCaseTest {

    private lateinit var repository: LyricsRepository
    private lateinit var useCase: GetLyricsUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetLyricsUseCase(repository)
    }

    @Test
    fun `observe returns flow from repository`() = runTest {
        val lyrics = listOf(Lyrics(id = 1, songId = 10, content = "Verso 1"))
        val flow = MutableStateFlow<SnapshotState<List<Lyrics>>>(SnapshotState.Data(lyrics))
        every { repository.observe() } returns flow

        val state = useCase.observe().first()

        assertTrue(state is SnapshotState.Data)
        assertEquals(1, (state as SnapshotState.Data).value.size)
    }

    @Test
    fun `refresh returns Updated from repository`() = runTest {
        coEvery { repository.refresh() } returns RefreshResult.Updated

        val result = useCase.refresh()

        assertEquals(RefreshResult.Updated, result)
    }

    @Test
    fun `refresh propagates Error`() = runTest {
        val error = RuntimeException("fail")
        coEvery { repository.refresh() } returns RefreshResult.Error(error)

        val result = useCase.refresh()

        assertTrue(result is RefreshResult.Error)
    }
}
