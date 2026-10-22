package com.ipb.castelobranco.features.bible.data.repository

import com.ipb.castelobranco.features.bible.data.local.BibleLocalStore
import com.ipb.castelobranco.features.bible.data.local.BiblePreferences
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BibleBookText
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

    private val store: BibleLocalStore = mockk(relaxed = true)
    private val preferences: BiblePreferences = mockk(relaxed = true)
    private val translation = MutableStateFlow(BibleTranslation.NAA)
    private val position = MutableStateFlow(BibleReadingPosition.default())

    private val genesis = BibleBook(abbrev = "gn", name = "Gênesis", verseCounts = listOf(2, 1))
    private val exodus = BibleBook(abbrev = "ex", name = "Êxodo", verseCounts = listOf(1))
    private val genesisText = BibleBookText("gn", listOf(listOf("No princípio...", "A terra..."), listOf("Assim...")))
    private val exodusText = BibleBookText("ex", listOf(listOf("Estes são...")))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { preferences.translationFlow } returns translation
        every { preferences.positionFlow } returns position
        every { preferences.fontSizeFlow } returns flowOf(BiblePreferences.DEFAULT_FONT_SIZE)
        coEvery { store.loadIndex(any()) } returns listOf(genesis, exodus)
        coEvery { store.loadBook(any(), 0, "gn") } returns genesisText
        coEvery { store.loadBook(any(), 1, "ex") } returns exodusText
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // The repository's scope runs on the test scheduler, so virtual time drives its sharing timeouts.
    private fun buildRepository(): BibleRepositoryImpl =
        BibleRepositoryImpl(store, preferences, testDispatcher)

    private fun onDisk(vararg translations: BibleTranslation) {
        BibleTranslation.entries.forEach { t -> coEvery { store.exists(t) } returns (t in translations) }
    }

    /** Stands in for the Bible graph's ViewModel collecting the current book. */
    private fun TestScope.collectBook(repo: BibleRepositoryImpl) =
        backgroundScope.launch { repo.currentBookFlow.collect {} }

    // region cached translations

    @Test
    fun `cached translations are known without reading any book`() = runTest {
        onDisk(BibleTranslation.NAA, BibleTranslation.ARA)

        val repo = buildRepository()
        advanceUntilIdle()

        assertEquals(setOf(BibleTranslation.NAA, BibleTranslation.ARA), repo.cachedTranslationsFlow.value)
        coVerify(exactly = 0) { store.loadBook(any(), any(), any()) }
    }

    @Test
    fun `checking the disk converts the old single file first`() = runTest {
        onDisk()
        val repo = buildRepository()
        advanceUntilIdle()

        repo.preload()

        coVerify(atLeast = 2) { store.migrateLegacy() }
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

    // region index

    @Test
    fun `the index of the active translation is loaded without collectors`() = runTest {
        onDisk(BibleTranslation.NAA, BibleTranslation.ARA)

        val repo = buildRepository()
        advanceUntilIdle()

        assertEquals(listOf(genesis, exodus), repo.booksFlow.value)
        coVerify(exactly = 1) { store.loadIndex(BibleTranslation.NAA) }
        coVerify(exactly = 0) { store.loadIndex(BibleTranslation.ARA) }
    }

    @Test
    fun `the index is empty when the active translation is not on disk`() = runTest {
        onDisk()

        val repo = buildRepository()
        advanceUntilIdle()

        assertEquals(emptyList<BibleBook>(), repo.booksFlow.value)
        coVerify(exactly = 0) { store.loadIndex(any()) }
    }

    @Test
    fun `a download of the active translation fills the index`() = runTest {
        onDisk()
        val repo = buildRepository()
        advanceUntilIdle()

        onDisk(BibleTranslation.NAA)
        repo.preload()
        advanceUntilIdle()

        assertEquals(listOf(genesis, exodus), repo.booksFlow.value)
    }

    @Test
    fun `a corrupt index counts as missing`() = runTest {
        onDisk(BibleTranslation.NAA)
        // loadIndex() deletes a corrupt translation and returns null.
        coEvery { store.loadIndex(BibleTranslation.NAA) } answers {
            onDisk()
            null
        }

        val repo = buildRepository()
        advanceUntilIdle()

        assertTrue(repo.booksFlow.value!!.isEmpty())
        assertTrue(repo.cachedTranslationsFlow.value.isEmpty())
    }

    // endregion

    // region current book

    @Test
    fun `no book is read while nobody collects`() = runTest {
        onDisk(BibleTranslation.NAA)

        val repo = buildRepository()
        advanceUntilIdle()

        coVerify(exactly = 0) { store.loadBook(any(), any(), any()) }
        assertNull(repo.currentBookFlow.value)
    }

    @Test
    fun `collecting reads only the book of the position`() = runTest {
        onDisk(BibleTranslation.NAA)

        val repo = buildRepository()
        collectBook(repo)
        advanceUntilIdle()

        assertEquals(genesisText, repo.currentBookFlow.value)
        coVerify(exactly = 1) { store.loadBook(BibleTranslation.NAA, 0, "gn") }
        coVerify(exactly = 0) { store.loadBook(any(), 1, any()) }
    }

    @Test
    fun `a chapter change in the same book reads nothing`() = runTest {
        onDisk(BibleTranslation.NAA)
        val repo = buildRepository()
        collectBook(repo)
        advanceUntilIdle()

        position.value = BibleReadingPosition("gn", 2, 1)
        advanceUntilIdle()

        coVerify(exactly = 1) { store.loadBook(any(), any(), any()) }
    }

    @Test
    fun `a book change reads the new book by its place in the index`() = runTest {
        onDisk(BibleTranslation.NAA)
        val repo = buildRepository()
        collectBook(repo)
        advanceUntilIdle()

        position.value = BibleReadingPosition("EX", 1, 1)
        advanceUntilIdle()

        assertEquals(exodusText, repo.currentBookFlow.value)
    }

    @Test
    fun `an unknown book falls back to the first of the index`() = runTest {
        onDisk(BibleTranslation.NAA)
        position.value = BibleReadingPosition("xyz", 1, 1)

        val repo = buildRepository()
        collectBook(repo)
        advanceUntilIdle()

        assertEquals(genesisText, repo.currentBookFlow.value)
    }

    @Test
    fun `changing translation reads the book from the new one`() = runTest {
        onDisk(BibleTranslation.NAA, BibleTranslation.ARA)
        val repo = buildRepository()
        collectBook(repo)
        advanceUntilIdle()

        translation.value = BibleTranslation.ARA
        advanceUntilIdle()

        coVerify(exactly = 1) { store.loadIndex(BibleTranslation.ARA) }
        coVerify(exactly = 1) { store.loadBook(BibleTranslation.ARA, 0, "gn") }
    }

    @Test
    fun `the book is released after the last collector leaves`() = runTest {
        onDisk(BibleTranslation.NAA)
        val repo = buildRepository()
        val collector = collectBook(repo)
        advanceUntilIdle()
        assertEquals(genesisText, repo.currentBookFlow.value)

        collector.cancel()
        advanceTimeBy(6_000)
        runCurrent()

        assertNull(repo.currentBookFlow.value)
        // The index stays: names are needed before any text.
        assertEquals(listOf(genesis, exodus), repo.booksFlow.value)
    }

    @Test
    fun `the book survives a short gap between collectors`() = runTest {
        onDisk(BibleTranslation.NAA)
        val repo = buildRepository()
        collectBook(repo).also { advanceUntilIdle() }.cancel()
        advanceTimeBy(1_000)
        collectBook(repo)
        advanceUntilIdle()

        coVerify(exactly = 1) { store.loadBook(any(), any(), any()) }
    }

    @Test
    fun `a corrupt book counts its translation as missing`() = runTest {
        onDisk(BibleTranslation.NAA)
        // loadBook() deletes a corrupt translation and returns null.
        coEvery { store.loadBook(BibleTranslation.NAA, 0, "gn") } answers {
            onDisk()
            null
        }

        val repo = buildRepository()
        collectBook(repo)
        advanceUntilIdle()

        assertNull(repo.currentBookFlow.value)
        assertTrue(repo.cachedTranslationsFlow.value.isEmpty())
        assertTrue(repo.booksFlow.value!!.isEmpty())
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
    fun `clearAll clears both translations and resets preferences`() = runTest {
        val repo = buildRepository()
        repo.clearAll()

        coVerify { store.clear(BibleTranslation.NAA) }
        coVerify { store.clear(BibleTranslation.ARA) }
        coVerify { preferences.resetAll() }
    }

    @Test
    fun `clearAll empties the index, the book and cachedTranslationsFlow`() = runTest {
        onDisk(BibleTranslation.NAA, BibleTranslation.ARA)
        val repo = buildRepository()
        collectBook(repo)
        advanceUntilIdle()
        assertEquals(genesisText, repo.currentBookFlow.value)

        repo.clearAll()
        advanceUntilIdle()

        assertTrue(repo.booksFlow.value!!.isEmpty())
        assertNull(repo.currentBookFlow.value)
        assertTrue(repo.cachedTranslationsFlow.value.isEmpty())
    }

    // endregion
}
