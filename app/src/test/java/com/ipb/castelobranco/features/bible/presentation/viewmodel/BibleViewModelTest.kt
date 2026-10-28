package com.ipb.castelobranco.features.bible.presentation.viewmodel

import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BibleBookText
import com.ipb.castelobranco.features.bible.domain.model.BibleHeading
import com.ipb.castelobranco.features.bible.domain.model.BibleProgress
import com.ipb.castelobranco.features.bible.domain.model.ProgressTarget
import com.ipb.castelobranco.features.bible.data.work.BibleDownloadWorker
import com.ipb.castelobranco.features.bible.domain.model.BibleReadingPosition
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import com.ipb.castelobranco.features.bible.domain.model.VerseRef
import com.ipb.castelobranco.features.bible.domain.repository.BibleProgressRepository
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import com.ipb.castelobranco.features.bible.domain.usecase.BibleAutoDownloadUseCase
import androidx.work.WorkInfo
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
import org.junit.Assert.assertNull
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
    private val progressRepository: BibleProgressRepository = mockk(relaxed = true)
    private val progress = MutableStateFlow(BibleProgress.EMPTY)

    private val genesis = BibleBook(abbrev = "gn", name = "Gênesis", verseCounts = listOf(2, 2))

    private val exodus = BibleBook(abbrev = "ex", name = "Êxodo", verseCounts = listOf(1))

    private val genesisText = BibleBookText(
        abbrev = "gn",
        chapters = listOf(
            listOf("No princípio criou Deus os céus e a terra.", "A terra era sem forma e vazia."),
            listOf("Assim foram acabados os céus e a terra.", "E no sétimo dia Deus descansou."),
        ),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.booksFlow } returns MutableStateFlow(listOf(genesis, exodus))
        every { repository.currentBookFlow } returns MutableStateFlow(genesisText)
        every { repository.activeTranslationFlow } returns MutableStateFlow(BibleTranslation.NAA)
        every { repository.cachedTranslationsFlow } returns MutableStateFlow(setOf(BibleTranslation.NAA))
        every { repository.positionFlow } returns MutableStateFlow(BibleReadingPosition("gn", 1, 1))
        every { repository.fontSizeFlow } returns MutableStateFlow(18f)
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(emptyList())
        every { connectivityObserver.isOnWifi } returns flowOf(false)
        every { progressRepository.progress } returns progress
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): BibleViewModel =
        BibleViewModel(repository, progressRepository, autoDownload, workManager, connectivityObserver)

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

        val verses = vm.uiState.value.currentChapterVerses!!
        assertEquals(2, verses.size)
        assertEquals("No princípio criou Deus os céus e a terra.", verses[0])
        assertTrue(vm.uiState.value.isChapterReady)
        job.cancel()
    }

    @Test
    fun `chapter is not ready while the book's text is on its way`() = runTest {
        every { repository.currentBookFlow } returns MutableStateFlow(null)
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        assertEquals(genesis, vm.uiState.value.currentBook)
        assertEquals(null, vm.uiState.value.currentChapterVerses)
        assertFalse(vm.uiState.value.isChapterReady)
        job.cancel()
    }

    @Test
    fun `the previous book's text does not stand in for the new book`() = runTest {
        every { repository.positionFlow } returns MutableStateFlow(BibleReadingPosition("ex", 1, 1))
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        assertEquals(exodus, vm.uiState.value.currentBook)
        assertFalse(vm.uiState.value.isChapterReady)
        job.cancel()
    }

    @Test
    fun `a pending verse is showing only once its chapter's text is there`() {
        val onGenesis = BibleUiState(
            books = listOf(genesis, exodus),
            bookText = genesisText,
            position = BibleReadingPosition("gn", 2, 1),
        )

        assertTrue(onGenesis.isShowing(VerseRef("GN", 2, 2)))
        assertFalse(onGenesis.isShowing(VerseRef("gn", 1, 1)))
        assertFalse(onGenesis.isShowing(VerseRef("ex", 1, 1)))
        // Position already moved to Exodus, its text still on its way.
        assertFalse(onGenesis.copy(position = BibleReadingPosition("ex", 1, 1)).isShowing(VerseRef("ex", 1, 1)))
    }

    @Test
    fun `chapterTitle names the target chapter from the index`() {
        val state = BibleUiState(books = listOf(genesis, exodus))

        assertEquals("Êxodo 1", state.chapterTitle(VerseRef("ex", 1, 3)))
        assertNull(BibleUiState().chapterTitle(VerseRef("ex", 1, 3)))
    }

    @Test
    fun `currentChapterHeadings follows the chapter of the position`() = runTest {
        val heading = BibleHeading(verse = 1, level = 1, title = "A criação do Universo")
        every { repository.currentBookFlow } returns MutableStateFlow(
            genesisText.copy(headings = listOf(listOf(heading), emptyList())),
        )
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        assertEquals(listOf(heading), vm.uiState.value.currentChapterHeadings)
        job.cancel()
    }

    @Test
    fun `currentChapterHeadings is empty for a book read without headings`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        assertTrue(vm.uiState.value.currentChapterHeadings.isEmpty())
        job.cancel()
    }

    // endregion

    // region download status

    private fun workInfo(state: WorkInfo.State, tags: Set<String> = emptySet()): WorkInfo = mockk(relaxed = true) {
        every { this@mockk.state } returns state
        every { this@mockk.tags } returns tags
    }

    @Test
    fun `a queued download shows as pending`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns
            flowOf(listOf(workInfo(WorkInfo.State.ENQUEUED)))
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        assertTrue(vm.uiState.value.download.isPending)
        job.cancel()
    }

    @Test
    fun `the silent format upgrade shows nothing while queued or running`() = runTest {
        val tags = setOf(BibleDownloadWorker.TAG_FORMAT_UPGRADE)
        for (state in listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING)) {
            every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(listOf(workInfo(state, tags)))
            val vm = buildViewModel()
            val job = subscribeAndAdvance(vm)

            val download = vm.uiState.value.download
            assertFalse(download.isPending)
            assertFalse(download.isDownloading)
            job.cancel()
        }
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
        assertEquals(VerseRef("ex", 1, 1), vm.pendingScroll.value)
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
        advanceUntilIdle()

        coVerify { autoDownload.enqueueAnyNetwork() }
    }

    @Test
    fun `retryDownloadWifi calls enqueueWifiOnly with REPLACE policy`() = runTest {
        val vm = buildViewModel()
        vm.retryDownloadWifi()
        advanceUntilIdle()

        coVerify { autoDownload.enqueueWifiOnly(replaceExisting = true) }
    }

    // endregion

    // region openPassage

    @Test
    fun `openPassage saves the position and keeps the verse pending until consumed`() = runTest {
        val vm = buildViewModel()

        vm.openPassage("ex", 1, 1)
        advanceUntilIdle()

        coVerify { repository.savePosition("ex", 1, 1) }
        assertEquals(VerseRef("ex", 1, 1), vm.pendingScroll.value)

        vm.consumePendingScroll()
        assertEquals(null, vm.pendingScroll.value)
    }

    @Test
    fun `openPassage highlights the cited verses without selecting them`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        vm.openPassage("ex", 1, 3, highlighted = setOf(3, 4, 7))
        advanceUntilIdle()

        val expected = setOf(VerseRef("ex", 1, 3), VerseRef("ex", 1, 4), VerseRef("ex", 1, 7))
        assertEquals(expected, vm.uiState.value.highlightedVerses)
        assertEquals(emptySet<VerseRef>(), vm.uiState.value.selectedVerses)
        job.cancel()
    }

    @Test
    fun `moving to another chapter clears the highlight`() = runTest {
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        vm.openPassage("ex", 1, 3, highlighted = setOf(3))
        advanceUntilIdle()
        vm.navigateTo("ex", 2, 1)
        advanceUntilIdle()

        assertEquals(emptySet<VerseRef>(), vm.uiState.value.highlightedVerses)
        job.cancel()
    }

    // endregion

    @Test
    fun `signed out has no progress, so no marking controls`() = runTest(testDispatcher) {
        progress.value = BibleProgress(mapOf("gn" to setOf(1)), emptySet())
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)

        assertNull(vm.uiState.value.progress)
        assertNull(vm.uiState.value.isCurrentChapterRead)
        job.cancel()
    }

    @Test
    fun `toggling the current chapter queues the opposite state`() = runTest(testDispatcher) {
        progress.value = BibleProgress(mapOf("gn" to setOf(1)), emptySet())
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)
        vm.setSignedIn(true)
        advanceUntilIdle()

        assertEquals(true, vm.uiState.value.isCurrentChapterRead)
        vm.toggleCurrentChapterRead()
        advanceUntilIdle()

        coVerify { progressRepository.mark(listOf(ProgressTarget.chapter("gn", 1)), done = false) }
        job.cancel()
    }

    @Test
    fun `marking a book queues only the chapters whose state changes`() = runTest(testDispatcher) {
        progress.value = BibleProgress(mapOf("gn" to setOf(1)), emptySet())
        val vm = buildViewModel()
        val job = subscribeAndAdvance(vm)
        vm.setSignedIn(true)
        advanceUntilIdle()

        vm.setBookRead(genesis, done = true)
        advanceUntilIdle()

        coVerify { progressRepository.mark(listOf(ProgressTarget.chapter("gn", 2)), done = true) }
        job.cancel()
    }
}
