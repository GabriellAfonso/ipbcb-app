package com.ipb.castelobranco.features.worshiphub.lyrics.data.repository

import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.lyrics.data.api.LyricsEditApi
import com.ipb.castelobranco.features.worshiphub.lyrics.data.dto.LyricsDto
import com.ipb.castelobranco.core.domain.error.AppError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class LyricsRepositoryImplTest {

    private lateinit var cache: SnapshotCache<List<LyricsDto>>
    private lateinit var fetcher: SnapshotFetcher<List<LyricsDto>>
    private lateinit var editApi: LyricsEditApi
    private lateinit var repository: LyricsRepositoryImpl

    @Before
    fun setup() {
        cache = mockk(relaxed = true)
        fetcher = mockk()
        editApi = mockk()
        repository = LyricsRepositoryImpl(cache, fetcher, Logger.Noop, editApi)
    }

    @Test
    fun `observe returns StateFlow starting with Loading`() {
        val state = repository.observe().value
        assertTrue(state is SnapshotState.Loading)
    }

    @Test
    fun `refresh on Success returns Updated and maps DTOs to domain`() = runTest {
        val dtos = listOf(
            LyricsDto(id = 1, songId = 10, content = "Verso 1\nRefrão", updatedAt = "2025-01-01")
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
        assertEquals("Verso 1\nRefrão", data[0].content)
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

    // region createLyrics

    @Test
    fun `createLyrics success calls refresh and returns success`() = runTest {
        val dto = LyricsDto(id = 5, songId = 1, content = "Nova letra", updatedAt = "2025-01-01")
        coEvery { editApi.createLyrics(any()) } returns Response.success(dto)
        coEvery { cache.loadETag() } returns null
        coEvery { fetcher.fetch(any()) } returns NetworkResult.Success(listOf(dto), "v1")
        coEvery { cache.save(any(), any()) } returns Unit

        val result = repository.createLyrics(songId = 1, content = "Nova letra")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `createLyrics failure returns AppError Server with correct code`() = runTest {
        coEvery { editApi.createLyrics(any()) } returns Response.error(400, "".toResponseBody())

        val result = repository.createLyrics(songId = 1, content = "Nova letra")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as AppError.Server
        assertEquals(400, error.code)
    }

    // endregion
}
