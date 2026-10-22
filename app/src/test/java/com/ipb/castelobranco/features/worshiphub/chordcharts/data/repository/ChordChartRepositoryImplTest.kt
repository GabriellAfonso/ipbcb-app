package com.ipb.castelobranco.features.worshiphub.chordcharts.data.repository

import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.chordcharts.data.api.ChordChartsEditApi
import com.ipb.castelobranco.features.worshiphub.chordcharts.data.dto.ChordChartDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ChordChartRepositoryImplTest {

    private lateinit var cache: SnapshotCache<List<ChordChartDto>>
    private lateinit var fetcher: SnapshotFetcher<List<ChordChartDto>>
    private lateinit var editApi: ChordChartsEditApi
    private lateinit var repository: ChordChartRepositoryImpl

    @Before
    fun setup() {
        cache = mockk(relaxed = true)
        fetcher = mockk()
        editApi = mockk()
        repository = ChordChartRepositoryImpl(cache, fetcher, Logger.Noop, editApi)
    }

    @Test
    fun `observe returns StateFlow starting with Loading`() {
        val state = repository.observe().value
        assertTrue(state is SnapshotState.Loading)
    }

    @Test
    fun `refresh on Success returns Updated and maps DTOs to domain`() = runTest {
        val dtos = listOf(
            ChordChartDto(id = 1, songId = 10, content = "{t:Test}", tone = "G", instrument = "Violão", updatedAt = "2025-01-01")
        )
        coEvery { cache.loadETag() } returns null
        coEvery { fetcher.fetch(any()) } returns NetworkResult.Success(dtos, "v1")
        coEvery { cache.save(any(), any()) } returns Unit

        val result = repository.refresh()

        assertEquals(RefreshResult.Updated, result)
        val state = repository.getCurrentState()
        assertTrue(state is SnapshotState.Data)
        val data = (state as SnapshotState.Data).value
        assertEquals(1, data.size)
        assertEquals(1, data[0].id)
        assertEquals("G", data[0].tone)
    }

    @Test
    fun `refresh on Failure with no cache returns Error`() = runTest {
        val error = RuntimeException("network error")
        coEvery { cache.loadETag() } returns null
        coEvery { fetcher.fetch(any()) } returns NetworkResult.Failure(error)
        coEvery { cache.load() } returns null

        val result = repository.refresh()

        assertTrue(result is RefreshResult.Error)
    }

    @Test
    fun `createChordChart success refreshes and returns success`() = runTest {
        val dto = ChordChartDto(id = 5, songId = 1, content = "[G]Test", tone = "G", instrument = "Violão", updatedAt = "2025-01-01")
        coEvery { editApi.createChordChart(any()) } returns Response.success(dto)
        coEvery { cache.loadETag() } returns null
        coEvery { fetcher.fetch(any()) } returns NetworkResult.Success(listOf(dto), "v1")
        coEvery { cache.save(any(), any()) } returns Unit

        val result = repository.createChordChart(1, "[G]Test", "G", "Violão")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `createChordChart failure returns error`() = runTest {
        coEvery { editApi.createChordChart(any()) } returns Response.error(400, "bad request".toResponseBody())

        val result = repository.createChordChart(1, "[G]Test", "G", "Violão")

        assertTrue(result.isFailure)
    }
}
