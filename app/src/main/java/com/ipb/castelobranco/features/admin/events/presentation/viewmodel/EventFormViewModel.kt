package com.ipb.castelobranco.features.admin.events.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.di.EventImageLoader
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.features.admin.events.domain.model.EventDraft
import com.ipb.castelobranco.features.admin.events.domain.model.EventField
import com.ipb.castelobranco.features.admin.events.domain.model.EventRecord
import com.ipb.castelobranco.features.admin.events.domain.model.toDraft
import com.ipb.castelobranco.features.admin.events.domain.usecase.DeleteEventUseCase
import com.ipb.castelobranco.features.admin.events.domain.usecase.GetEventRecordUseCase
import com.ipb.castelobranco.features.admin.events.domain.usecase.SaveEventUseCase
import com.ipb.castelobranco.features.admin.events.domain.usecase.ValidateEventDraftUseCase
import com.ipb.castelobranco.features.admin.events.domain.usecase.ValidateEventImageUseCase
import com.ipb.castelobranco.features.admin.events.presentation.navigation.EventsAdminRoutes
import com.ipb.castelobranco.features.admin.events.presentation.state.EventFormUiState
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminEvent
import com.ipb.castelobranco.features.admin.events.presentation.util.FailureKind
import com.ipb.castelobranco.features.admin.events.presentation.util.toEventsAdminEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One form for a new event, an edit and — below `manage` — a read-only look. Local rules run first so
 * an impossible event never becomes a request; the server's `field_errors` land on the same fields. An
 * edit sends only what changed, and the flyer only when a new one was picked.
 */
@HiltViewModel
class EventFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEvent: GetEventRecordUseCase,
    private val validate: ValidateEventDraftUseCase,
    private val validateImage: ValidateEventImageUseCase,
    private val saveEvent: SaveEventUseCase,
    private val deleteEvent: DeleteEventUseCase,
    observeAccess: ObserveAccessUseCase,
    @param:EventImageLoader val imageLoader: ImageLoader,
) : ViewModel() {

    /** The nav argument arrives as a String when the optional query parameter is present. */
    private val eventId: Int? = savedStateHandle.get<Any>(EventsAdminRoutes.ARG_EVENT_ID)
        ?.toString()?.toIntOrNull()

    private val _uiState = MutableStateFlow(EventFormUiState(isEditing = eventId != null, isLoading = eventId != null))
    val uiState: StateFlow<EventFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EventsAdminEvent>()
    val events: SharedFlow<EventsAdminEvent> = _events.asSharedFlow()

    private var original: EventRecord? = null
    private var originalDraft: EventDraft? = null

    init {
        viewModelScope.launch {
            observeAccess().collect { access ->
                _uiState.update {
                    it.copy(
                        isReadOnly = !access.allows(Scope.EVENTS, AccessLevel.MANAGE),
                        canDelete = eventId != null && access.allows(Scope.EVENTS, AccessLevel.OWNER),
                    )
                }
            }
        }
        if (eventId != null) load()
    }

    fun load() {
        val id = eventId ?: return
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            getEvent(id)
                .onSuccess { record ->
                    original = record
                    originalDraft = record.toDraft()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            draft = record.toDraft(),
                            imageUrl = record.imageUrl,
                            pickedImage = null,
                            hasUnsavedChanges = false,
                        )
                    }
                }
                .onFailure { throwable ->
                    when (val event = throwable.toEventsAdminEvent(FailureKind.READ)) {
                        is EventsAdminEvent.ShowMessage ->
                            _uiState.update { it.copy(isLoading = false, loadError = event.message) }
                        else -> _events.emit(event)
                    }
                }
        }
    }

    fun onDraftChanged(draft: EventDraft) {
        if (_uiState.value.isReadOnly) return
        _uiState.update {
            it.copy(
                draft = draft,
                hasUnsavedChanges = draft != (originalDraft ?: EventDraft()) || it.pickedImage != null,
                fieldErrors = it.fieldErrors - changedFields(it.draft, draft),
                generalError = null,
            )
        }
    }

    fun onImagePicked(bytes: ByteArray) {
        if (_uiState.value.isReadOnly) return
        val problem = validateImage(bytes)
        _uiState.update {
            if (problem != null) {
                it.copy(fieldErrors = it.fieldErrors + (EventField.IMAGE to problem))
            } else {
                it.copy(pickedImage = bytes, hasUnsavedChanges = true, fieldErrors = it.fieldErrors - EventField.IMAGE)
            }
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isReadOnly) return
        val hasImage = state.pickedImage != null || state.imageUrl != null
        val localErrors = validate(state.draft, hasImage)
        if (localErrors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = localErrors) }
            return
        }

        _uiState.update { it.copy(isSaving = true, fieldErrors = emptyMap(), generalError = null) }
        viewModelScope.launch {
            saveEvent(state.draft, state.pickedImage, original, originalDraft)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                    _events.emit(EventsAdminEvent.Saved)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isSaving = false) }
                    handleSaveFailure(throwable)
                }
        }
    }

    fun onDelete() {
        val id = eventId ?: return
        val state = _uiState.value
        if (state.isDeleting || !state.canDelete) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            deleteEvent(id)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, hasUnsavedChanges = false) }
                    _events.emit(EventsAdminEvent.Deleted)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.emit(throwable.toEventsAdminEvent(FailureKind.WRITE))
                }
        }
    }

    private suspend fun handleSaveFailure(throwable: Throwable) {
        val event = throwable.toEventsAdminEvent(FailureKind.WRITE)
        if (event !is EventsAdminEvent.ShowMessage) {
            _events.emit(event)
            return
        }
        val serverFields = (throwable.toAppError() as? AppError.Server)?.fieldErrors.orEmpty()
        val onFields = serverFields.mapNotNull { (key, messages) ->
            EventField.fromApiKey(key)?.let { field -> field to messages.joinToString(" ") }
        }.toMap()
        val unplaced = serverFields.filterKeys { EventField.fromApiKey(it) == null }.values.flatten()
        val general = when {
            unplaced.isNotEmpty() -> unplaced.joinToString(" ")
            onFields.isEmpty() -> event.message
            else -> null
        }
        _uiState.update { it.copy(fieldErrors = onFields, generalError = general) }
    }

    private fun changedFields(before: EventDraft, after: EventDraft): Set<EventField> {
        val old = before.toWire()
        return after.toWire().filter { (field, value) -> old[field] != value }.keys
    }
}
