package com.ipb.castelobranco.core.domain.usecase

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.core.domain.startup.Refreshable
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.hymnal.domain.repository.HymnalRepository
import com.ipb.castelobranco.features.schedule.domain.repository.ScheduleRepository
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.repository.ChordChartRepository
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.repository.LyricsRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreloadDataUseCaseTest {

    private lateinit var songsRepository: SongsRepository
    private lateinit var hymnalRepository: HymnalRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var galleryRepository: GalleryRepository
    private lateinit var chordChartRepository: ChordChartRepository
    private lateinit var lyricsRepository: LyricsRepository

    private lateinit var useCase: PreloadDataUseCase

    @Before
    fun setup() {
        songsRepository = mockk()
        hymnalRepository = mockk()
        scheduleRepository = mockk()
        galleryRepository = mockk()
        chordChartRepository = mockk()
        lyricsRepository = mockk()

        coEvery { songsRepository.preload() } just runs
        coEvery { scheduleRepository.preload() } just runs
        coEvery { galleryRepository.preload() } just runs
        coEvery { chordChartRepository.preload() } just runs
        coEvery { lyricsRepository.preload() } just runs

        coEvery { songsRepository.refreshAllSongs() } returns RefreshResult.Updated
        coEvery { songsRepository.refreshSongsBySunday() } returns RefreshResult.Updated
        coEvery { songsRepository.refreshTopSongs() } returns RefreshResult.Updated
        coEvery { songsRepository.refreshTopTones() } returns RefreshResult.Updated
        coEvery { songsRepository.refreshSuggestedSongs() } returns RefreshResult.Updated
        coEvery { hymnalRepository.refreshHymnal() } returns RefreshResult.Updated
        coEvery { scheduleRepository.refreshMonthSchedule() } returns RefreshResult.Updated
        coEvery { chordChartRepository.refresh() } returns RefreshResult.Updated
        coEvery { lyricsRepository.refresh() } returns RefreshResult.Updated

        val preloadables = setOf(
            Preloadable { songsRepository.preload() },
            Preloadable { scheduleRepository.preload() },
            Preloadable { galleryRepository.preload() },
            Preloadable { chordChartRepository.preload() },
            Preloadable { lyricsRepository.preload() },
        )
        val refreshables = setOf(
            Refreshable { songsRepository.refreshAllSongs() },
            Refreshable { songsRepository.refreshSongsBySunday() },
            Refreshable { songsRepository.refreshTopSongs() },
            Refreshable { songsRepository.refreshTopTones() },
            Refreshable { songsRepository.refreshSuggestedSongs() },
            Refreshable { hymnalRepository.refreshHymnal() },
            Refreshable { scheduleRepository.refreshMonthSchedule() },
            Refreshable { chordChartRepository.refresh() },
            Refreshable { lyricsRepository.refresh() },
        )

        useCase = PreloadDataUseCase(preloadables, refreshables, Dispatchers.Unconfined)
    }

    // region preload phase — all repos called

    @Test
    fun `invoke calls songsRepository preload`() = runTest {
        useCase()
        coVerify { songsRepository.preload() }
    }

    @Test
    fun `invoke calls scheduleRepository preload`() = runTest {
        useCase()
        coVerify { scheduleRepository.preload() }
    }

    @Test
    fun `invoke calls galleryRepository preload`() = runTest {
        useCase()
        coVerify { galleryRepository.preload() }
    }

    @Test
    fun `invoke calls chordChartRepository preload`() = runTest {
        useCase()
        coVerify { chordChartRepository.preload() }
    }

    @Test
    fun `invoke calls lyricsRepository preload`() = runTest {
        useCase()
        coVerify { lyricsRepository.preload() }
    }

    // endregion

    // region refresh phase — all repos called

    @Test
    fun `invoke calls songsRepository refreshAllSongs`() = runTest {
        useCase()
        coVerify { songsRepository.refreshAllSongs() }
    }

    @Test
    fun `invoke calls songsRepository refreshSongsBySunday`() = runTest {
        useCase()
        coVerify { songsRepository.refreshSongsBySunday() }
    }

    @Test
    fun `invoke calls songsRepository refreshTopSongs`() = runTest {
        useCase()
        coVerify { songsRepository.refreshTopSongs() }
    }

    @Test
    fun `invoke calls songsRepository refreshTopTones`() = runTest {
        useCase()
        coVerify { songsRepository.refreshTopTones() }
    }

    @Test
    fun `invoke calls songsRepository refreshSuggestedSongs`() = runTest {
        useCase()
        coVerify { songsRepository.refreshSuggestedSongs() }
    }

    @Test
    fun `invoke calls hymnalRepository refreshHymnal`() = runTest {
        useCase()
        coVerify { hymnalRepository.refreshHymnal() }
    }

    @Test
    fun `invoke calls scheduleRepository refreshMonthSchedule`() = runTest {
        useCase()
        coVerify { scheduleRepository.refreshMonthSchedule() }
    }

    @Test
    fun `invoke calls chordChartRepository refresh`() = runTest {
        useCase()
        coVerify { chordChartRepository.refresh() }
    }

    @Test
    fun `invoke calls lyricsRepository refresh`() = runTest {
        useCase()
        coVerify { lyricsRepository.refresh() }
    }

    // endregion

    // region error isolation — preload phase

    @Test
    fun `invoke completes even when songsRepository preload throws`() = runTest {
        coEvery { songsRepository.preload() } throws RuntimeException("disk error")

        useCase()

        coVerify { scheduleRepository.preload() }
        coVerify { galleryRepository.preload() }
        coVerify { chordChartRepository.preload() }
        coVerify { lyricsRepository.preload() }
    }

    @Test
    fun `invoke completes even when all preloads throw`() = runTest {
        coEvery { songsRepository.preload() } throws RuntimeException()
        coEvery { scheduleRepository.preload() } throws RuntimeException()
        coEvery { galleryRepository.preload() } throws RuntimeException()
        coEvery { chordChartRepository.preload() } throws RuntimeException()
        coEvery { lyricsRepository.preload() } throws RuntimeException()

        useCase()

        // If supervisorScope isolation works, refresh phase must still run
        coVerify { songsRepository.refreshAllSongs() }
    }

    // endregion

    // region error isolation — refresh phase

    @Test
    fun `invoke completes even when one refresh throws`() = runTest {
        coEvery { songsRepository.refreshAllSongs() } throws RuntimeException("network error")

        useCase()

        coVerify { songsRepository.refreshSongsBySunday() }
        coVerify { songsRepository.refreshTopSongs() }
        coVerify { hymnalRepository.refreshHymnal() }
        coVerify { scheduleRepository.refreshMonthSchedule() }
        coVerify { chordChartRepository.refresh() }
        coVerify { lyricsRepository.refresh() }
    }

    @Test
    fun `invoke does not throw even when all refreshes throw`() = runTest {
        coEvery { songsRepository.refreshAllSongs() } throws RuntimeException()
        coEvery { songsRepository.refreshSongsBySunday() } throws RuntimeException()
        coEvery { songsRepository.refreshTopSongs() } throws RuntimeException()
        coEvery { songsRepository.refreshTopTones() } throws RuntimeException()
        coEvery { songsRepository.refreshSuggestedSongs() } throws RuntimeException()
        coEvery { hymnalRepository.refreshHymnal() } throws RuntimeException()
        coEvery { scheduleRepository.refreshMonthSchedule() } throws RuntimeException()
        coEvery { chordChartRepository.refresh() } throws RuntimeException()
        coEvery { lyricsRepository.refresh() } throws RuntimeException()

        // Must not throw
        useCase()
    }

    // endregion

    // region phase ordering

    @Test
    fun `preload phase completes before refresh phase starts`() = runTest {
        val callOrder = mutableListOf<String>()

        coEvery { songsRepository.preload() } answers { callOrder.add("preload:songs") }
        coEvery { scheduleRepository.preload() } answers { callOrder.add("preload:schedule") }
        coEvery { galleryRepository.preload() } answers { callOrder.add("preload:gallery") }
        coEvery { chordChartRepository.preload() } answers { callOrder.add("preload:chordChart") }
        coEvery { lyricsRepository.preload() } answers { callOrder.add("preload:lyrics") }

        coEvery { songsRepository.refreshAllSongs() } answers { callOrder.add("refresh:allSongs"); RefreshResult.Updated }
        coEvery { songsRepository.refreshSongsBySunday() } answers { callOrder.add("refresh:bySunday"); RefreshResult.Updated }
        coEvery { songsRepository.refreshTopSongs() } answers { callOrder.add("refresh:topSongs"); RefreshResult.Updated }
        coEvery { songsRepository.refreshTopTones() } answers { callOrder.add("refresh:topTones"); RefreshResult.Updated }
        coEvery { songsRepository.refreshSuggestedSongs() } answers { callOrder.add("refresh:suggested"); RefreshResult.Updated }
        coEvery { hymnalRepository.refreshHymnal() } answers { callOrder.add("refresh:hymnal"); RefreshResult.Updated }
        coEvery { scheduleRepository.refreshMonthSchedule() } answers { callOrder.add("refresh:schedule"); RefreshResult.Updated }
        coEvery { chordChartRepository.refresh() } answers { callOrder.add("refresh:chordChart"); RefreshResult.Updated }
        coEvery { lyricsRepository.refresh() } answers { callOrder.add("refresh:lyrics"); RefreshResult.Updated }

        useCase()

        val lastPreloadIndex = callOrder.indexOfLast { it.startsWith("preload:") }
        val firstRefreshIndex = callOrder.indexOfFirst { it.startsWith("refresh:") }

        assert(lastPreloadIndex < firstRefreshIndex) {
            "Expected all preloads before any refresh, but got: $callOrder"
        }
    }

    // endregion
}
