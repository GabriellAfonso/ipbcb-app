package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.viewmodel

import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.state.ServiceWindowsEvent
import app.cash.turbine.test
import com.ipb.castelobranco.features.admin.reports.hymnal.FakeHymnalHistoryAdminRepository
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.DeleteServiceWindowUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.GetServiceWindowsUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.SaveServiceWindowUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.ServiceWindowFields
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.ValidateServiceWindowUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.serviceWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceWindowsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeHymnalHistoryAdminRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeHymnalHistoryAdminRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val access = FakeAccessRepository(
        accessOf(Role.ADMIN, Scope.HYMNAL_HISTORY_REPORT to AccessLevel.OWNER)
    )

    private fun viewModel() = ServiceWindowsViewModel(
        getServiceWindows = GetServiceWindowsUseCase(repository),
        saveServiceWindow = SaveServiceWindowUseCase(repository),
        deleteServiceWindow = DeleteServiceWindowUseCase(repository),
        validate = ValidateServiceWindowUseCase(),
        observeAccess = ObserveAccessUseCase(access),
    )

    // region access (spec 006)

    @Test
    fun `below owner the list is read-only`() = runTest {
        access.state.value = accessOf(Role.LEADER, Scope.HYMNAL_HISTORY_REPORT to AccessLevel.VIEW)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCreateRequested()
        viewModel.onDeleteRequested(viewModel.uiState.value.windows.single())

        assertEquals(false, viewModel.uiState.value.canManage)
        assertNull(viewModel.uiState.value.editing)
        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun `owner manages the windows`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canManage)
    }

    @Test
    fun `a refused load leaves the reports area`() = runTest {
        repository.windowsResult = Result.failure(AppError.Auth(code = 403, userMessage = "Sem permissão"))

        val viewModel = viewModel()

        viewModel.events.test {
            assertEquals(ServiceWindowsEvent.LeaveArea("Sem permissão"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    @Test
    fun `loading lists the services`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.windows.size)
        assertEquals(DayOfWeek.SUNDAY, viewModel.uiState.value.windows.single().weekday)
    }

    @Test
    fun `a failed load surfaces a message`() = runTest {
        repository.windowsResult = Result.failure(AppError.Network())

        val viewModel = viewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `an end before the start is refused locally and issues no request`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCreateRequested()
        viewModel.onDraftChanged(
            requireNotNull(viewModel.uiState.value.editing).copy(
                name = "Culto de Oração",
                startTime = LocalTime.of(21, 0),
                endTime = LocalTime.of(19, 0),
            )
        )
        viewModel.onSave()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.fieldErrors[ServiceWindowFields.END_TIME])
        assertNull(repository.savedDraft)
    }

    @Test
    fun `an empty name is refused locally`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCreateRequested()
        viewModel.onSave()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.fieldErrors[ServiceWindowFields.NAME])
        assertNull(repository.savedDraft)
    }

    @Test
    fun `a valid draft is saved and the form closes`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCreateRequested()
        viewModel.onDraftChanged(
            requireNotNull(viewModel.uiState.value.editing).copy(
                name = "Culto de Oração",
                weekday = DayOfWeek.WEDNESDAY,
            )
        )
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals("Culto de Oração", repository.savedDraft?.name)
        assertNull(viewModel.uiState.value.editing)
    }

    @Test
    fun `a server field error lands on the field the service named`() = runTest {
        repository.saveResult = Result.failure(
            AppError.Server(
                code = 400,
                errorCode = "VALIDATION_ERROR",
                fieldErrors = mapOf("name" to listOf("Já existe um culto com este nome.")),
            )
        )

        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCreateRequested()
        viewModel.onDraftChanged(
            requireNotNull(viewModel.uiState.value.editing).copy(name = "Culto de Oração")
        )
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(
            "Já existe um culto com este nome.",
            viewModel.uiState.value.fieldErrors[ServiceWindowFields.NAME],
        )
    }

    @Test
    fun `toggling active patches the window without touching anything else`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onActiveToggled(serviceWindow(active = true))
        advanceUntilIdle()

        val draft = requireNotNull(repository.savedDraft)
        assertEquals(false, draft.active)
        assertEquals(serviceWindow().name, draft.name)
    }

    @Test
    fun `deleting asks first and only then removes`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDeleteRequested(serviceWindow())
        assertNotNull(viewModel.uiState.value.pendingDelete)
        assertNull(repository.deletedId)

        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(serviceWindow().id, repository.deletedId)
        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun `dismissing the confirmation deletes nothing`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDeleteRequested(serviceWindow())
        viewModel.onDeleteDismissed()
        advanceUntilIdle()

        assertNull(repository.deletedId)
        assertTrue(viewModel.uiState.value.windows.isNotEmpty())
    }
}
