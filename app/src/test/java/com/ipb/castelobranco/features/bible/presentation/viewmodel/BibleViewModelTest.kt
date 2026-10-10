package com.ipb.castelobranco.features.bible.presentation.viewmodel

import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BibleReadingPosition
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import com.ipb.castelobranco.features.bible.domain.model.VerseRef
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import com.ipb.castelobranco.features.bible.domain.usecase.BibleAutoDownloadUseCase
import androidx.work.WorkManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BibleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val repository: BibleRepository = mockk(relaxed = true)
    private val autoDownload: BibleAutoDownloadUseCase = mockk(relaxed = true)
    private val workManager: WorkManager = mockk(relaxed = true)
    private val connectivityObserver: NetworkConnectivityObserver = mockk(relaxed = true)

    private val genesis = BibleBook(
        abbrev = "gn",
        name = "Gênesis",
        chapters = listOf(
            listOf("No princípio criou Deus os céus e a terra.", "A terra era sem forma e vazia."),
            listOf("Assim foram acabados os céus e a terra.", "E no sétimo dia Deus descansou."),
        ),
    )

    private val exodus = BibleBook(
        abbrev = "ex",
        name = "Êxodo",
        chapters = listOf(
            listOf("Estes são os nomes dos filhos de Israel."),
        ),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.booksFlow } returns MutableStateFlow(listOf(genesis, exodus))
        every { repository.activeTranslationFlow } returns MutableStateFlow(BibleTranslation.NAA)
        every { repository.cachedTranslationsFlow } returns MutableStateFlow(setOf(BibleTranslation.NAA))
        every { repository.positionFlow } returns MutableStateFlow(BibleReadingPosition("gn", 1, 1))
        every { repository.fontSizeFlow } returns MutableStateFlow(18f)
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(emptyList())
        every { connectivityObserver.isOnWifi } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): BibleViewModel =
        BibleViewModel(repository, autoDownload, workManager, connectivityObserver)

    /** Subscribe to uiState so WhileSubscribed upstream activates. */
    private fun TestScope.subscribeAndAdvance(vm: BibleViewModel): kotlinx.coroutines.Job {
        val job = launch { vm.uiState.collect { } }
        advanceUntilIdle()
        return job
    }

    // region uiState

    @Test
    fun `uiState combines repository flows`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        val state = vm.uiState.value
        assertEquals(listOf(genesis, exodus), state.books)
        assertEquals(BibleTranslation.NAA, state.activeTranslation)
        assertTrue(state.isBibleReady)
        job.cancel()
    }

    @Test
    fun `uiState isBibleReady false when books empty`() = runTest {
        every { repository.booksFlow } returns MutableStateFlow(emptyList())
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        assertFalse(vm.uiState.value.isBibleReady)
        assertFalse(vm.uiState.value.isLoadingBooks)
        job.cancel()
    }

    @Test
    fun `uiState isLoadingBooks while the repository decodes`() = runTest {
        every { repository.booksFlow } returns MutableStateFlow(null)
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        assertTrue(vm.uiState.value.isLoadingBooks)
        assertFalse(vm.uiState.value.isBibleReady)
        job.cancel()
    }

    @Test
    fun `currentBook returns book matching position`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        assertEquals(genesis, vm.uiState.value.currentBook)
        job.cancel()
    }

    @Test
    fun `currentChapterVerses returns verses for current position`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        val verses = vm.uiState.value.currentChapterVerses
        assertEquals(2, verses.size)
        assertEquals("No princípio criou Deus os céus e a terra.", verses[0])
        job.cancel()
    }

    // endregion

    // region navigation

    @Test
    fun `setActiveTranslation delegates to repository`() = runTest {
        val vm = buildViewModel()
        vm.setActiveTranslation(BibleTranslation.ARA)
        advanceUntilIdle()

        coVerify { repository.setActiveTranslation(BibleTranslation.ARA) }
    }

    @Test
    fun `navigateTo saves position and clears selection`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        vm.toggleVerseSelection(VerseRef("gn", 1, 1))
        advanceUntilIdle()

        vm.navigateTo("ex", 1, 1)
        advanceUntilIdle()

        coVerify { repository.savePosition("ex", 1, 1) }
        job.cancel()
    }

    @Test
    fun `nextChapter advances within same book`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        vm.nextChapter()
        advanceUntilIdle()

        coVerify { repository.savePosition("gn", 2, verse = 1) }
        job.cancel()
    }

    @Test
    fun `nextChapter goes to next book at last chapter`() = runTest {
        every { repository.positionFlow } returns MutableStateFlow(BibleReadingPosition("gn", 2, 1))
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        vm.nextChapter()
        advanceUntilIdle()

        coVerify { repository.savePosition("ex", 1, 1) }
        job.cancel()
    }

    @Test
    fun `previousChapter goes back within same book`() = runTest {
        every { repository.positionFlow } returns MutableStateFlow(BibleReadingPosition("gn", 2, 1))
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        vm.previousChapter()
        advanceUntilIdle()

        coVerify { repository.savePosition("gn", 1, verse = 1) }
        job.cancel()
    }

    @Test
    fun `previousChapter goes to previous book at chapter 1`() = runTest {
        every { repository.positionFlow } returns MutableStateFlow(BibleReadingPosition("ex", 1, 1))
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        vm.previousChapter()
        advanceUntilIdle()

        coVerify { repository.savePosition("gn", 2, 1) }
        job.cancel()
    }

    // endregion

    // region font size

    @Test
    fun `setFontSize delegates to repository`() = runTest {
        val vm = buildViewModel()
        vm.setFontSize(24f)
        advanceUntilIdle()

        coVerify { repository.setFontSize(24f) }
    }

    // endregion

    // region verse selection

    @Test
    fun `toggleVerseSelection adds verse`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        vm.toggleVerseSelection(VerseRef("gn", 1, 1))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.selectedVerses.contains(VerseRef("gn", 1, 1)))
        job.cancel()
    }

    @Test
    fun `toggleVerseSelection removes already selected verse`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        vm.toggleVerseSelection(VerseRef("gn", 1, 1))
        vm.toggleVerseSelection(VerseRef("gn", 1, 1))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.selectedVerses.contains(VerseRef("gn", 1, 1)))
        job.cancel()
    }

    @Test
    fun `clearSelection empties selected verses`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        vm.toggleVerseSelection(VerseRef("gn", 1, 1))
        vm.toggleVerseSelection(VerseRef("gn", 1, 2))
        advanceUntilIdle()

        vm.clearSelection()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.selectedVerses.isEmpty())
        job.cancel()
    }

    // endregion

    // region retry download

    @Test
    fun `retryDownloadOnMobileData calls enqueueAnyNetwork`() = runTest {
        val vm = buildViewModel()
        vm.retryDownloadOnMobileData()

        verify { autoDownload.enqueueAnyNetwork() }
    }

    @Test
    fun `retryDownloadWifi calls enqueueWifiOnly with REPLACE policy`() = runTest {
        val vm = buildViewModel()
        vm.retryDownloadWifi()

        verify { autoDownload.enqueueWifiOnly(replaceExisting = true) }
    }

    // endregion
}
