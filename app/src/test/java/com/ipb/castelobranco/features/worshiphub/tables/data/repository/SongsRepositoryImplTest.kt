package com.ipb.castelobranco.features.worshiphub.tables.data.repository

import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.SuggestedSongDto
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.SuggestedSongInnerDto
import com.ipb.castelobranco.features.worshiphub.tables.data.fetcher.SuggestedSongsFetcher
import com.ipb.castelobranco.features.worshiphub.tables.data.snapshot.AllSongsSnapshotRepository
import com.ipb.castelobranco.features.worshiphub.tables.data.snapshot.SongsBySundaySnapshotRepository
import com.ipb.castelobranco.features.worshiphub.tables.data.snapshot.TopSongsSnapshotRepository
import com.ipb.castelobranco.features.worshiphub.tables.data.snapshot.TopTonesSnapshotRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SongsRepositoryImplTest {

    private lateinit var songsBySundaySnapshot: SongsBySundaySnapshotRepository
    private lateinit var topSongsSnapshot: TopSongsSnapshotRepository
    private lateinit var topTonesSnapshot: TopTonesSnapshotRepository
    private lateinit var allSongsSnapshot: AllSongsSnapshotRepository
    private lateinit var suggestedSongsCache: SnapshotCache<List<SuggestedSongDto>>
    private lateinit var suggestedSongsFetcher: SuggestedSongsFetcher
    private lateinit var repository: SongsRepositoryImpl

    private fun suggestedDto(id: Int, songId: Int, title: String, position: Int) =
        SuggestedSongDto(
            id = id,
            song = SuggestedSongInnerDto(id = songId, title = title, artist = "Artist"),
            date = "04/05/2025",
            tone = "G",
            position = position
        )

    @Before
    fun setup() {
        songsBySundaySnapshot = mockk(relaxed = true)
        topSongsSnapshot = mockk(relaxed = true)
        topTonesSnapshot = mockk(relaxed = true)
        allSongsSnapshot = mockk(relaxed = true)
        suggestedSongsCache = mockk()
        suggestedSongsFetcher = mockk()

        // Default stubs for cache
        coEvery { suggestedSongsCache.load() } returns null
        coEvery { suggestedSongsCache.save(any(), any()) } returns Unit
        coEvery { suggestedSongsCache.loadETag() } returns null
        coEvery { suggestedSongsCache.clear() } returns Unit

        repository = SongsRepositoryImpl(
            songsBySundaySnapshot, topSongsSnapshot, topTonesSnapshot,
            allSongsSnapshot, suggestedSongsCache, suggestedSongsFetcher
        )
    }

    // region delegation

    @Test
    fun `refreshSongsBySunday delegates to snapshot`() = runTest {
        coEvery { songsBySundaySnapshot.refresh() } returns RefreshResult.Updated

        val result = repository.refreshSongsBySunday()

        assertEquals(RefreshResult.Updated, result)
        coVerify { songsBySundaySnapshot.refresh() }
    }

    @Test
    fun `refreshTopSongs delegates to snapshot`() = runTest {
        coEvery { topSongsSnapshot.refresh() } returns RefreshResult.Updated

        val result = repository.refreshTopSongs()

        assertEquals(RefreshResult.Updated, result)
    }

    @Test
    fun `refreshTopTones delegates to snapshot`() = runTest {
        coEvery { topTonesSnapshot.refresh() } returns RefreshResult.Updated

        val result = repository.refreshTopTones()

        assertEquals(RefreshResult.Updated, result)
    }

    @Test
    fun `refreshAllSongs delegates to snapshot`() = runTest {
        coEvery { allSongsSnapshot.refresh() } returns RefreshResult.Updated

        val result = repository.refreshAllSongs()

        assertEquals(RefreshResult.Updated, result)
    }

    @Test
    fun `preload delegates to allSongsSnapshot`() = runTest {
        coEvery { allSongsSnapshot.preload() } returns Unit

        repository.preload()

        coVerify { allSongsSnapshot.preload() }
    }

    // endregion

    // region suggestedSongs — Success

    @Test
    fun `refreshSuggestedSongs on Success returns Updated`() = runTest {
        val dtos = listOf(suggestedDto(1, 10, "Song A", 1))
        coEvery { suggestedSongsFetcher.fetch(any(), any()) } returns NetworkResult.Success(dtos, "v1")

        val result = repository.refreshSuggestedSongs()

        assertEquals(RefreshResult.Updated, result)
    }

    @Test
    fun `refreshSuggestedSongs on Success updates state with mapped domain`() = runTest {
        val dtos = listOf(
            suggestedDto(1, 10, "B", 2),
            suggestedDto(2, 20, "A", 1),
        )
        coEvery { suggestedSongsFetcher.fetch(any(), any()) } returns NetworkResult.Success(dtos, "v1")

        repository.refreshSuggestedSongs()

        val state = repository.observeSuggestedSongs().first()
        assertTrue(state is SnapshotState.Data)
        val data = (state as SnapshotState.Data).value
        // toDomain() on list sorts by position
        assertEquals(listOf(1, 2), data.map { it.position })
    }

    @Test
    fun `refreshSuggestedSongs on Success saves to cache`() = runTest {
        val dtos = listOf(suggestedDto(1, 10, "Song", 1))
        coEvery { suggestedSongsFetcher.fetch(any(), any()) } returns NetworkResult.Success(dtos, "v1")

        repository.refreshSuggestedSongs()

        coVerify { suggestedSongsCache.save(dtos, "v1") }
    }

    // endregion

    // region suggestedSongs — NotModified

    @Test
    fun `refreshSuggestedSongs on NotModified returns NotModified`() = runTest {
        coEvery { suggestedSongsFetcher.fetch(any(), any()) } returns NetworkResult.NotModified

        val result = repository.refreshSuggestedSongs()

        assertEquals(RefreshResult.NotModified, result)
    }

    // endregion

    // region suggestedSongs — Failure

    @Test
    fun `refreshSuggestedSongs on Failure with cache returns CacheUsed`() = runTest {
        val error = RuntimeException("network error")
        coEvery { suggestedSongsFetcher.fetch(any(), any()) } returns NetworkResult.Failure(error)
        coEvery { suggestedSongsCache.load() } returns listOf(suggestedDto(1, 10, "Cached", 1))

        val result = repository.refreshSuggestedSongs()

        assertEquals(RefreshResult.CacheUsed, result)
    }

    @Test
    fun `refreshSuggestedSongs on Failure without cache returns Error`() = runTest {
        val error = RuntimeException("network error")
        coEvery { suggestedSongsFetcher.fetch(any(), any()) } returns NetworkResult.Failure(error)
        coEvery { suggestedSongsCache.load() } returns null

        val result = repository.refreshSuggestedSongs()

        assertTrue(result is RefreshResult.Error)
        assertEquals(error, (result as RefreshResult.Error).throwable.cause)
    }

    @Test
    fun `refreshSuggestedSongs on Failure sets Error state`() = runTest {
        val error = RuntimeException("fail")
        coEvery { suggestedSongsFetcher.fetch(any(), any()) } returns NetworkResult.Failure(error)
        coEvery { suggestedSongsCache.load() } returns null

        repository.refreshSuggestedSongs()

        val state = repository.observeSuggestedSongs().first()
        assertTrue(state is SnapshotState.Error)
    }

    // endregion

    // region suggestedSongs — fixedByPosition

    @Test
    fun `refreshSuggestedSongs passes fixed and excluded maps to fetcher`() = runTest {
        val fixed = mapOf(1 to 42, 2 to 99)
        val excluded = mapOf(3 to setOf(12, 34))
        coEvery { suggestedSongsFetcher.fetch(fixed, excluded) } returns NetworkResult.NotModified

        repository.refreshSuggestedSongs(fixed, excluded)

        coVerify { suggestedSongsFetcher.fetch(fixed, excluded) }
    }

    @Test
    fun `refreshSuggestedSongs without params passes empty map`() = runTest {
        coEvery { suggestedSongsFetcher.fetch(emptyMap(), emptyMap()) } returns NetworkResult.NotModified

        repository.refreshSuggestedSongs()

        coVerify { suggestedSongsFetcher.fetch(emptyMap(), emptyMap()) }
    }

    // endregion

    // region observeSuggestedSongs initial state

    @Test
    fun `observeSuggestedSongs starts with Loading`() = runTest {
        val state = repository.observeSuggestedSongs().first()
        assertTrue(state is SnapshotState.Loading)
    }

    // endregion
}
