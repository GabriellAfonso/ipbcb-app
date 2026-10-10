package com.ipb.castelobranco.features.bible.data.repository

import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.features.bible.data.dto.BibleBookDto
import com.ipb.castelobranco.features.bible.data.local.BiblePreferences
import com.ipb.castelobranco.features.bible.domain.model.BibleReadingPosition
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BibleRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private val naaCache: SnapshotCache<List<BibleBookDto>> = mockk(relaxed = true)
    private val araCache: SnapshotCache<List<BibleBookDto>> = mockk(relaxed = true)
    private val preferences: BiblePreferences = mockk(relaxed = true)
    private val storage: SnapshotStorage = mockk(relaxed = true)
    private val translation = MutableStateFlow(BibleTranslation.NAA)

    private val caches = mapOf(
        BibleTranslation.NAA to naaCache,
        BibleTranslation.ARA to araCache,
    )

    private val genesisDto = BibleBookDto(
        abbrev = "gn",
        name = "Gênesis",
        chapters = listOf(listOf("No princípio...", "A terra era sem forma...")),
    )

    private val exodusDto = BibleBookDto(abbrev = "ex", name = "Êxodo", chapters = listOf(listOf("Estes são...")))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { preferences.translationFlow } returns translation
        every { preferences.positionFlow } returns flowOf(BibleReadingPosition.default())
        every { preferences.fontSizeFlow } returns flowOf(BiblePreferences.DEFAULT_FONT_SIZE)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // The repository's scope runs on the test scheduler, so virtual time drives its sharing timeouts.
    private fun buildRepository(): BibleRepositoryImpl =
        BibleRepositoryImpl(caches, preferences, storage, testDispatcher)

    private fun onDisk(vararg translations: BibleTranslation) {
        coEvery { naaCache.exists() } returns (BibleTranslation.NAA in translations)
        coEvery { araCache.exists() } returns (BibleTranslation.ARA in translations)
    }

    /** Stands in for the Bible graph's ViewModel collecting the books. */
    private fun TestScope.collectBooks(repo: BibleRepositoryImpl) =
        backgroundScope.launch { repo.booksFlow.collect {} }

    // region cached translations

    @Test
    fun `cached translations are known without preload or decoding`() = runTest {
        onDisk(BibleTranslation.NAA, BibleTranslation.ARA)

        val repo = buildRepository()
        advanceUntilIdle()

        assertEquals(setOf(BibleTranslation.NAA, BibleTranslation.ARA), repo.cachedTranslationsFlow.value)
        coVerify(exactly = 0) { naaCache.load() }
        coVerify(exactly = 0) { araCache.load() }
    }

    @Test
    fun `preload refreshes cached translations after a download`() = runTest {
        onDisk()
        val repo = buildRepository()
        advanceUntilIdle()
        assertTrue(repo.cachedTranslationsFlow.value.isEmpty())

        onDisk(BibleTranslation.NAA)
        repo.preload()

        assertEquals(setOf(BibleTranslation.NAA), repo.cachedTranslationsFlow.value)
    }

    // endregion

    // region booksFlow

    @Test
    fun `books are not decoded while nobody collects`() = runTest {
        onDisk(BibleTranslation.NAA, BibleTranslation.ARA)
        coEvery { naaCache.load() } returns listOf(genesisDto)

        val repo = buildRepository()
        repo.preload()
        advanceUntilIdle()

        coVerify(exactly = 0) { naaCache.load() }
        assertNull(repo.booksFlow.value)
    }

    @Test
    fun `collecting decodes only the active translation, once`() = runTest {
        onDisk(BibleTranslation.NAA, BibleTranslation.ARA)
        coEvery { naaCache.load() } returns listOf(genesisDto)

        val repo = buildRepository()
        collectBooks(repo)
        repo.preload()
        advanceUntilIdle()

        assertEquals("gn", repo.booksFlow.value?.single()?.abbrev)
        coVerify(exactly = 1) { naaCache.load() }
        coVerify(exactly = 0) { araCache.load() }
    }

    @Test
    fun `books are empty when the active translation is not on disk`() = runTest {
        onDisk()

        val repo = buildRepository()
        collectBooks(repo)
        advanceUntilIdle()

        assertEquals(emptyList<Any>(), repo.booksFlow.value)
        coVerify(exactly = 0) { naaCache.load() }
    }

    @Test
    fun `books are released after the last collector leaves`() = runTest {
        onDisk(BibleTranslation.NAA)
        coEvery { naaCache.load() } returns listOf(genesisDto)

        val repo = buildRepository()
        val collector = collectBooks(repo)
        advanceUntilIdle()
        assertTrue(repo.booksFlow.value!!.isNotEmpty())

        collector.cancel()
        advanceTimeBy(6_000)
        runCurrent()

        assertNull(repo.booksFlow.value)
    }

    @Test
    fun `books survive a short gap between collectors`() = runTest {
        onDisk(BibleTranslation.NAA)
        coEvery { naaCache.load() } returns listOf(genesisDto)

        val repo = buildRepository()
        collectBooks(repo).also { advanceUntilIdle() }.cancel()
        advanceTimeBy(1_000)
        collectBooks(repo)
        advanceUntilIdle()

        coVerify(exactly = 1) { naaCache.load() }
    }

    @Test
    fun `changing translation decodes the new one`() = runTest {
        onDisk(BibleTranslation.NAA, BibleTranslation.ARA)
        coEvery { naaCache.load() } returns listOf(genesisDto)
        coEvery { araCache.load() } returns listOf(exodusDto)

        val repo = buildRepository()
        collectBooks(repo)
        advanceUntilIdle()
        translation.value = BibleTranslation.ARA
        advanceUntilIdle()

        assertEquals("ex", repo.booksFlow.value?.single()?.abbrev)
    }

    @Test
    fun `a download of the active translation fills the open reader`() = runTest {
        onDisk()
        coEvery { naaCache.load() } returns listOf(genesisDto)

        val repo = buildRepository()
        collectBooks(repo)
        advanceUntilIdle()
        assertTrue(repo.booksFlow.value!!.isEmpty())

        onDisk(BibleTranslation.NAA)
        repo.preload()
        advanceUntilIdle()

        assertEquals("gn", repo.booksFlow.value?.single()?.abbrev)
    }

    @Test
    fun `a corrupt active translation counts as missing`() = runTest {
        onDisk(BibleTranslation.NAA)
        // load() deletes a corrupt file and returns null.
        coEvery { naaCache.load() } answers {
            onDisk()
            null
        }

        val repo = buildRepository()
        collectBooks(repo)
        advanceUntilIdle()

        assertTrue(repo.booksFlow.value!!.isEmpty())
        assertTrue(repo.cachedTranslationsFlow.value.isEmpty())
    }

    // endregion

    // region preferences

    @Test
    fun `setActiveTranslation delegates to preferences`() = runTest {
        val repo = buildRepository()
        repo.setActiveTranslation(BibleTranslation.ARA)

        coVerify { preferences.setTranslation(BibleTranslation.ARA) }
    }

    @Test
    fun `savePosition delegates to preferences`() = runTest {
        val repo = buildRepository()
        repo.savePosition("ex", 3, 14)

        coVerify { preferences.setPosition("ex", 3, 14) }
    }

    @Test
    fun `setFontSize delegates to preferences`() = runTest {
        val repo = buildRepository()
        repo.setFontSize(24f)

        coVerify { preferences.setFontSize(24f) }
    }

    // endregion

    // region clearAll

    @Test
    fun `clearAll clears all caches and resets preferences`() = runTest {
        val repo = buildRepository()
        repo.clearAll()

        coVerify { naaCache.clear() }
        coVerify { araCache.clear() }
        coVerify { preferences.resetAll() }
    }

    @Test
    fun `clearAll empties booksFlow and cachedTranslationsFlow`() = runTest {
        onDisk(BibleTranslation.NAA, BibleTranslation.ARA)
        coEvery { naaCache.load() } returns listOf(genesisDto)

        val repo = buildRepository()
        collectBooks(repo)
        advanceUntilIdle()
        assertTrue(repo.booksFlow.value!!.isNotEmpty())

        repo.clearAll()
        advanceUntilIdle()

        assertTrue(repo.booksFlow.value!!.isEmpty())
        assertTrue(repo.cachedTranslationsFlow.value.isEmpty())
    }

    // endregion
}
