package com.ipb.castelobranco.features.hymnal.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.domain.repository.HymnalRepository
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

class ObserveHymnsUseCaseTest {

    private lateinit var repository: HymnalRepository
    private lateinit var useCase: ObserveHymnsUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ObserveHymnsUseCase(repository)
    }

    @Test
    fun `invoke returns flow from repository`() = runTest {
        val hymns = listOf(Hymn(id = null, number = "1", title = "Castelo Forte", lyrics = emptyList()))
        val flow = MutableStateFlow<SnapshotState<List<Hymn>>>(SnapshotState.Data(hymns))
        every { repository.observeHymnal() } returns flow

        val state = useCase().first()

        assertTrue(state is SnapshotState.Data)
        assertEquals(hymns, (state as SnapshotState.Data).value)
    }

    @Test
    fun `refresh returns result from repository`() = runTest {
        coEvery { repository.refreshHymnal() } returns RefreshResult.Updated

        val result = useCase.refresh()

        assertEquals(RefreshResult.Updated, result)
    }

    @Test
    fun `refresh propagates NotModified`() = runTest {
        coEvery { repository.refreshHymnal() } returns RefreshResult.NotModified

        val result = useCase.refresh()

        assertEquals(RefreshResult.NotModified, result)
    }
}
