package com.ipb.castelobranco.features.worshiphub.chordcharts.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.model.ChordChart
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.repository.ChordChartRepository
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

class GetChordChartsUseCaseTest {

    private lateinit var repository: ChordChartRepository
    private lateinit var useCase: GetChordChartsUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetChordChartsUseCase(repository)
    }

    @Test
    fun `observe returns flow from repository`() = runTest {
        val charts = listOf(ChordChart(id = 1, songId = 10, content = "{t:Test}", tone = "G", instrument = "Violão"))
        val flow = MutableStateFlow<SnapshotState<List<ChordChart>>>(SnapshotState.Data(charts))
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
