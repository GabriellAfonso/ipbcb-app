package com.ipb.castelobranco.features.admin.members.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.model.MemberRecord
import com.ipb.castelobranco.features.admin.members.domain.model.changesFrom
import com.ipb.castelobranco.features.admin.members.domain.model.toDraft
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberOptionsUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.SaveMemberUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase
import com.ipb.castelobranco.features.admin.members.presentation.navigation.MembersRoutes
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberFormUiState
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.util.toMembersEvent
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
 * One form for a new member and for an edit. Local rules run first so an impossible member never
 * becomes a request; the server's `field_errors` land on the same fields. An edit sends only what
 * the leader changed.
 */
@HiltViewModel
class MemberFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMember: GetMemberUseCase,
    private val getOptions: GetMemberOptionsUseCase,
    private val validate: ValidateMemberDraftUseCase,
    private val saveMember: SaveMemberUseCase,
) : ViewModel() {

    /** The nav argument arrives as a String when the optional query parameter is present. */
    private val memberId: Int? = savedStateHandle.get<Any>(MembersRoutes.ARG_MEMBER_ID)
        ?.toString()?.toIntOrNull()

    private val _uiState = MutableStateFlow(MemberFormUiState(isEditing = memberId != null))
    val uiState: StateFlow<MemberFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MembersEvent>()
    val events: SharedFlow<MembersEvent> = _events.asSharedFlow()

    private var original: MemberRecord? = null
    private var originalDraft: MemberDraft? = null

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val record = memberId?.let { id ->
                getMember(id).getOrElse { throwable ->
                    failLoading(throwable)
                    return@launch
                }
            }
            val options = getOptions().getOrElse { throwable ->
                failLoading(throwable)
                return@launch
            }
            original = record
            originalDraft = record?.toDraft()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    options = options,
                    draft = originalDraft ?: MemberDraft(),
                    hasUnsavedChanges = false,
                )
            }
        }
    }

    fun onDraftChanged(draft: MemberDraft) {
        _uiState.update {
            it.copy(
                draft = draft,
                hasUnsavedChanges = draft.changesFrom(originalDraft ?: MemberDraft()).isEmpty.not(),
                fieldErrors = it.fieldErrors - changedFields(it.draft, draft),
                generalError = null,
            )
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return
        val localErrors = validate(state.draft)
        if (localErrors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = localErrors) }
            return
        }

        _uiState.update { it.copy(isSaving = true, fieldErrors = emptyMap(), generalError = null) }
        viewModelScope.launch {
            saveMember(state.draft, original, originalDraft)
                .onSuccess { saved ->
                    _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                    _events.emit(MembersEvent.Saved(saved.id))
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isSaving = false) }
                    handleSaveFailure(throwable)
                }
        }
    }

    private suspend fun handleSaveFailure(throwable: Throwable) {
        val error = throwable.toAppError()
        val serverFields = (error as? AppError.Server)?.fieldErrors.orEmpty()
        val onFields = serverFields.mapNotNull { (key, messages) ->
            MemberField.fromApiKey(key)?.let { field -> field to messages.joinToString(" ") }
        }.toMap()
        val unplaced = serverFields.filterKeys { MemberField.fromApiKey(it) == null }.values.flatten()

        val event = throwable.toMembersEvent()
        if (event !is MembersEvent.ShowMessage) {
            _events.emit(event)
            return
        }
        val general = when {
            unplaced.isNotEmpty() -> unplaced.joinToString(" ")
            onFields.isEmpty() -> event.message
            else -> null
        }
        _uiState.update { it.copy(fieldErrors = onFields, generalError = general) }
        // A refusal that names no field may be a status, role or ministry removed meanwhile.
        if (error is AppError.Server && onFields.isEmpty()) reloadOptions()
    }

    private fun reloadOptions() {
        viewModelScope.launch {
            getOptions().onSuccess { options -> _uiState.update { it.copy(options = options) } }
        }
    }

    private suspend fun failLoading(throwable: Throwable) {
        val event = throwable.toMembersEvent()
        if (event is MembersEvent.ShowMessage) {
            _uiState.update { it.copy(isLoading = false, loadError = event.message) }
        } else {
            _events.emit(event)
        }
    }

    private fun changedFields(before: MemberDraft, after: MemberDraft): Set<MemberField> =
        after.changesFrom(before).values.keys
}
