package com.ipb.castelobranco.features.admin.events.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import coil.ImageLoader
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.admin.events.domain.model.EventField
import com.ipb.castelobranco.features.admin.events.domain.repository.EventsAdminRepository
import com.ipb.castelobranco.features.admin.events.domain.repository.ImageBounds
import com.ipb.castelobranco.features.admin.events.domain.usecase.DeleteEventUseCase
import com.ipb.castelobranco.features.admin.events.domain.usecase.GetEventRecordUseCase
import com.ipb.castelobranco.features.admin.events.domain.usecase.SaveEventUseCase
import com.ipb.castelobranco.features.admin.events.domain.usecase.ValidateEventDraftUseCase
import com.ipb.castelobranco.features.admin.events.domain.usecase.ValidateEventImageUseCase
import com.ipb.castelobranco.features.admin.events.presentation.navigation.EventsAdminRoutes
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminEvent
import com.ipb.castelobranco.features.admin.events.testing.FakeEventsAdminRepository
import com.ipb.castelobranco.features.admin.events.testing.record
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class EventFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeEventsAdminRepository()
    private val access = FakeAccessRepository(accessOf(Role.ADMIN, Scope.EVENTS to AccessLevel.OWNER))
    private var bounds: ImageBounds? = ImageBounds(1920, 1080)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(eventId: Int? = null) = EventFormViewModel(
        savedStateHandle = SavedStateHandle(
            eventId?.let { mapOf(EventsAdminRoutes.ARG_EVENT_ID to it.toString()) } ?: emptyMap()
        ),
        getEvent = GetEventRecordUseCase(repository),
        validate = ValidateEventDraftUseCase(),
        validateImage = ValidateEventImageUseCase { bounds },
        saveEvent = SaveEventUseCase(repository),
        deleteEvent = DeleteEventUseCase(repository),
        observeAccess = ObserveAccessUseCase(access),
        imageLoader = mockk<ImageLoader>(),
    )

    @Test
    fun `local errors block the save without calling the server`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onSave()
        advanceUntilIdle()

        assertEquals(
            setOf(EventField.TITLE, EventField.START_DATE, EventField.IMAGE),
            vm.uiState.value.fieldErrors.keys,
        )
        assertTrue(repository.creates.isEmpty())
    }

    @Test
    fun `a new event is created and reports Saved`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onDraftChanged(vm.uiState.value.draft.copy(title = "Almoço", startDate = LocalDate.of(2026, 12, 6)))
        vm.onImagePicked(byteArrayOf(1))

        vm.events.test {
            vm.onSave()
            vm.onSave() // a double tap while saving is ignored
            advanceUntilIdle()
            assertEquals(EventsAdminEvent.Saved, awaitItem())
        }
        assertEquals(1, repository.creates.size)
    }

    @Test
    fun `a small image is refused under the image field`() = runTest(dispatcher) {
        bounds = ImageBounds(600, 300)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onImagePicked(byteArrayOf(1))

        assertEquals(ValidateEventImageUseCase.TOO_SMALL, vm.uiState.value.fieldErrors[EventField.IMAGE])
        assertNull(vm.uiState.value.pickedImage)
    }

    @Test
    fun `server field errors land on their fields, unknown keys become a general error`() = runTest(dispatcher) {
        repository.writeResult = Result.failure(
            AppError.Server(
                code = 400,
                errorCode = "VALIDATION_ERROR",
                fieldErrors = mapOf("image" to listOf("Imagem fora de 16:9."), "other" to listOf("Algo.")),
            )
        )
        val vm = viewModel(eventId = 1)
        advanceUntilIdle()
        vm.onDraftChanged(vm.uiState.value.draft.copy(title = "Novo título"))

        vm.onSave()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Imagem fora de 16:9.", state.fieldErrors[EventField.IMAGE])
        assertEquals("Algo.", state.generalError)
        assertEquals("Novo título", state.draft.title)
    }

    @Test
    fun `a vanished event goes back to the list`() = runTest(dispatcher) {
        repository.getResult = Result.failure(
            AppError.Server(code = 404, userMessage = EventsAdminRepository.EVENT_GONE)
        )
        val vm = viewModel(eventId = 1)

        vm.events.test {
            advanceUntilIdle()
            assertEquals(EventsAdminEvent.EventGone(EventsAdminRepository.EVENT_GONE), awaitItem())
        }
    }

    @Test
    fun `below manage the form is read-only and cannot save`() = runTest(dispatcher) {
        access.state.value = accessOf(Role.MEDIA, Scope.EVENTS to AccessLevel.VIEW)
        repository.getResult = Result.success(record(id = 1))
        val vm = viewModel(eventId = 1)
        advanceUntilIdle()

        vm.onDraftChanged(vm.uiState.value.draft.copy(title = "Outro"))
        vm.onSave()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isReadOnly)
        assertFalse(vm.uiState.value.canDelete)
        assertEquals("Congresso", vm.uiState.value.draft.title)
        assertTrue(repository.updates.isEmpty())
    }

    @Test
    fun `an owner deletes and reports Deleted, manage cannot delete`() = runTest(dispatcher) {
        val vm = viewModel(eventId = 1)
        advanceUntilIdle()

        vm.events.test {
            vm.onDelete()
            advanceUntilIdle()
            assertEquals(EventsAdminEvent.Deleted, awaitItem())
        }
        assertEquals(listOf(1), repository.deletes)

        access.state.value = accessOf(Role.LEADER, Scope.EVENTS to AccessLevel.MANAGE)
        advanceUntilIdle()
        vm.onDelete()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canDelete)
        assertEquals(listOf(1), repository.deletes)
    }
}
