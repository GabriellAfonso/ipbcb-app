package com.ipb.castelobranco.features.admin.schedule.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import com.ipb.castelobranco.features.admin.schedule.domain.model.ScheduleItem
import com.ipb.castelobranco.features.admin.schedule.domain.repository.AdminScheduleRepository
import com.ipb.castelobranco.features.admin.schedule.presentation.state.AdminScheduleEvent
import com.ipb.castelobranco.features.admin.schedule.presentation.state.SaveResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class AdminScheduleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: AdminScheduleRepository
    private lateinit var viewModel: AdminScheduleViewModel

    private val member1 = Member(id = 1, name = "João")
    private val member2 = Member(id = 2, name = "Maria")

    private val scheduleItem = ScheduleItem(
        date = "2026-04-01",
        day = 1,
        scheduleTypeName = "Terça de Oração",
        scheduleTypeId = 1,
        selectedMember = member1
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = AdminScheduleViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region init

    @Test
    fun `init populates skeleton items for default month`() {
        assertTrue(viewModel.uiState.value.items.isNotEmpty())
        assertTrue(viewModel.uiState.value.items.all { it.selectedMember == null })
    }

    @Test
    fun `init does not mark unsaved changes`() {
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    // endregion

    // region generateSchedule

    @Test
    fun `generateSchedule success maps ScheduleItem to EditableScheduleUiState in UI state`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertEquals(1, items.size)
        with(items[0]) {
            assertEquals("2026-04-01", date)
            assertEquals(1, day)
            assertEquals("Terça de Oração", scheduleTypeName)
            assertEquals(1, scheduleTypeId)
            assertEquals(member1, selectedMember)
        }
    }

    @Test
    fun `generateSchedule success clears isGenerating flag`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(emptyList())

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isGenerating)
    }

    @Test
    fun `generateSchedule success marks unsaved changes`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `generateSchedule failure sets snackbar message and clears isGenerating`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns
            Result.failure(Exception("server error"))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()

        assertEquals("Falha ao gerar escala.", viewModel.uiState.value.snackbarMessage)
        assertFalse(viewModel.uiState.value.isGenerating)
    }

    @Test
    fun `generateSchedule uses current year and month from UI state`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(emptyList())
        viewModel.onEvent(AdminScheduleEvent.MonthChanged(2027, 6))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()

        coVerify { repository.generateSchedule(year = 2027, month = 6) }
    }

    @Test
    fun `generateSchedule maps multiple items preserving order`() = runTest {
        val item2 = ScheduleItem("2026-04-08", 8, "Quinta de Oração", 2, member2)
        coEvery { repository.generateSchedule(any(), any()) } returns
            Result.success(listOf(scheduleItem, item2))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertEquals(2, items.size)
        assertEquals("2026-04-01", items[0].date)
        assertEquals("2026-04-08", items[1].date)
    }

    // endregion

    // region save confirmation

    @Test
    fun `SaveRequested opens the confirmation without saving`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.SaveRequested)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSaveConfirmation)
        coVerify(exactly = 0) { repository.saveSchedule(any(), any(), any()) }
    }

    @Test
    fun `SaveRequested does not open the confirmation when the schedule is incomplete`() = runTest {
        viewModel.onEvent(AdminScheduleEvent.SaveRequested)

        assertFalse(viewModel.uiState.value.showSaveConfirmation)
    }

    @Test
    fun `SaveConfirmationDismissed closes the confirmation without saving`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.SaveRequested)
        viewModel.onEvent(AdminScheduleEvent.SaveConfirmationDismissed)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showSaveConfirmation)
        coVerify(exactly = 0) { repository.saveSchedule(any(), any(), any()) }
    }

    @Test
    fun `SaveSchedule after confirmation closes the dialog and saves`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))
        coEvery { repository.saveSchedule(any(), any(), any()) } returns Result.success(Unit)

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.SaveRequested)
        viewModel.onEvent(AdminScheduleEvent.SaveSchedule)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showSaveConfirmation)
        assertEquals(SaveResult.Success, viewModel.uiState.value.saveResult)
    }

    // endregion

    // region saveSchedule

    @Test
    fun `saveSchedule calls repository with ScheduleItem converted from UI state items`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))
        coEvery { repository.saveSchedule(any(), any(), any()) } returns Result.success(Unit)

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.SaveSchedule)
        advanceUntilIdle()

        coVerify {
            repository.saveSchedule(
                year = any(),
                month = any(),
                items = match { items ->
                    items.size == 1 &&
                        items[0].date == "2026-04-01" &&
                        items[0].scheduleTypeId == 1 &&
                        items[0].selectedMember == member1
                }
            )
        }
    }

    @Test
    fun `saveSchedule success sets Success saveResult and clears unsaved changes`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))
        coEvery { repository.saveSchedule(any(), any(), any()) } returns Result.success(Unit)

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.SaveSchedule)
        advanceUntilIdle()

        assertEquals(SaveResult.Success, viewModel.uiState.value.saveResult)
        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `saveSchedule failure sets Error saveResult with the authored userMessage`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))
        coEvery { repository.saveSchedule(any(), any(), any()) } returns
            Result.failure(AppError.Server(code = 400, message = "raw body", userMessage = "Escala já existe"))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.SaveSchedule)
        advanceUntilIdle()

        assertEquals(SaveResult.Error("Escala já existe"), viewModel.uiState.value.saveResult)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `saveSchedule failure without userMessage falls back to the generic message`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))
        coEvery { repository.saveSchedule(any(), any(), any()) } returns
            Result.failure(Exception())

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.SaveSchedule)
        advanceUntilIdle()

        assertEquals(SaveResult.Error("Algo deu errado. Tente novamente."), viewModel.uiState.value.saveResult)
    }

    @Test
    fun `saveSchedule failure does not clear unsaved changes`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))
        coEvery { repository.saveSchedule(any(), any(), any()) } returns
            Result.failure(Exception("boom"))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.SaveSchedule)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `saveSchedule does not call repository when any item has null selectedMember`() = runTest {
        val itemWithoutMember = scheduleItem.copy(selectedMember = null)
        coEvery { repository.generateSchedule(any(), any()) } returns
            Result.success(listOf(itemWithoutMember))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.SaveSchedule)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.saveSchedule(any(), any(), any()) }
    }

    // endregion

    // region SaveResultDismissed

    @Test
    fun `SaveResultDismissed clears saveResult`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))
        coEvery { repository.saveSchedule(any(), any(), any()) } returns Result.success(Unit)
        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.SaveSchedule)
        advanceUntilIdle()

        viewModel.onEvent(AdminScheduleEvent.SaveResultDismissed)

        assertNull(viewModel.uiState.value.saveResult)
    }

    // endregion

    // region selectMember

    @Test
    fun `selectMember updates selectedMember on the target item only`() = runTest {
        val item2 = scheduleItem.copy(date = "2026-04-08", day = 8, selectedMember = null)
        coEvery { repository.generateSchedule(any(), any()) } returns
            Result.success(listOf(scheduleItem, item2))

        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        viewModel.onEvent(AdminScheduleEvent.MemberSelected(itemIndex = 1, member = member2))

        val items = viewModel.uiState.value.items
        assertEquals(member1, items[0].selectedMember) // unchanged
        assertEquals(member2, items[1].selectedMember) // updated
    }

    @Test
    fun `selectMember marks unsaved changes`() = runTest {
        coEvery { repository.saveSchedule(any(), any(), any()) } returns Result.success(Unit)
        // Saving once to clear hasUnsavedChanges (skeleton is populated by init).
        // Fill all skeleton items so canSave becomes true.
        viewModel.uiState.value.items.forEachIndexed { idx, _ ->
            viewModel.onEvent(AdminScheduleEvent.MemberSelected(idx, member1))
        }
        viewModel.onEvent(AdminScheduleEvent.SaveSchedule)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onEvent(AdminScheduleEvent.MemberSelected(0, member2))

        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    // endregion

    // region changeMonth

    @Test
    fun `changeMonth updates year and month in UI state`() = runTest {
        viewModel.onEvent(AdminScheduleEvent.MonthChanged(2027, 3))

        assertEquals(2027, viewModel.uiState.value.year)
        assertEquals(3, viewModel.uiState.value.month)
    }

    @Test
    fun `changeMonth repopulates skeleton with empty members`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))
        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()

        viewModel.onEvent(AdminScheduleEvent.MonthChanged(2027, 3))

        val items = viewModel.uiState.value.items
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.selectedMember == null })
    }

    @Test
    fun `changeMonth resets unsaved changes`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns Result.success(listOf(scheduleItem))
        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onEvent(AdminScheduleEvent.MonthChanged(2027, 3))

        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    // endregion

    // region loadMembers

    @Test
    fun `loadMembers success updates members and clears isLoadingMembers`() = runTest {
        coEvery { repository.getMembers() } returns Result.success(listOf(member1, member2))

        viewModel.onEvent(AdminScheduleEvent.LoadMembers)
        advanceUntilIdle()

        assertEquals(listOf(member1, member2), viewModel.uiState.value.members)
        assertFalse(viewModel.uiState.value.isLoadingMembers)
    }

    @Test
    fun `loadMembers failure sets snackbar message and clears isLoadingMembers`() = runTest {
        coEvery { repository.getMembers() } returns Result.failure(Exception("error"))

        viewModel.onEvent(AdminScheduleEvent.LoadMembers)
        advanceUntilIdle()

        assertEquals("Falha ao carregar membros.", viewModel.uiState.value.snackbarMessage)
        assertFalse(viewModel.uiState.value.isLoadingMembers)
    }

    @Test
    fun `loadMembers does not call repository when members already loaded`() = runTest {
        coEvery { repository.getMembers() } returns Result.success(listOf(member1))
        viewModel.onEvent(AdminScheduleEvent.LoadMembers)
        advanceUntilIdle()

        viewModel.onEvent(AdminScheduleEvent.LoadMembers)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.getMembers() }
    }

    // endregion

    // region snackbarShown

    @Test
    fun `SnackbarShown clears snackbar message`() = runTest {
        coEvery { repository.generateSchedule(any(), any()) } returns
            Result.failure(Exception("error"))
        viewModel.onEvent(AdminScheduleEvent.GenerateSchedule)
        advanceUntilIdle()

        viewModel.onEvent(AdminScheduleEvent.SnackbarShown)

        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    // endregion
}
