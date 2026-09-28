package com.ipb.castelobranco.features.admin.members.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.features.admin.members.di.MemberPhotoLoader
import com.ipb.castelobranco.features.admin.members.domain.model.DeleteConfirmation
import com.ipb.castelobranco.features.admin.members.domain.model.MemberRecord
import com.ipb.castelobranco.features.admin.members.domain.usecase.ComputeMemberAgeUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.DeleteMemberUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberHistoryUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RemoveMemberPhotoUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.SetMemberValidityUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.UploadMemberPhotoUseCase
import com.ipb.castelobranco.features.admin.members.presentation.navigation.MembersRoutes
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberProfileUi
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberProfileUiState
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.util.AGE_UNKNOWN
import com.ipb.castelobranco.features.admin.members.presentation.util.NOT_INFORMED
import com.ipb.castelobranco.features.admin.members.presentation.util.NO_ROLE
import com.ipb.castelobranco.features.admin.members.presentation.util.formatDate
import com.ipb.castelobranco.features.admin.members.presentation.util.initialsOf
import com.ipb.castelobranco.features.admin.members.presentation.util.statusLabel
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
import java.time.ZoneId
import javax.inject.Inject

/**
 * One member's profile. Validity saves the moment the switch moves and goes back if the server
 * refuses; photo and delete are driven from here too. Every write lands in the repository's
 * in-memory list, so the grid is already up to date when the leader goes back.
 */
@HiltViewModel
class MemberProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMember: GetMemberUseCase,
    private val getHistory: GetMemberHistoryUseCase,
    private val setValidity: SetMemberValidityUseCase,
    private val uploadPhoto: UploadMemberPhotoUseCase,
    private val removePhoto: RemoveMemberPhotoUseCase,
    private val deleteMember: DeleteMemberUseCase,
    private val computeAge: ComputeMemberAgeUseCase,
    @MemberPhotoLoader val imageLoader: ImageLoader,
) : ViewModel() {

    private val memberId: Int = checkNotNull(savedStateHandle[MembersRoutes.ARG_MEMBER_ID])

    private val _uiState = MutableStateFlow(MemberProfileUiState())
    val uiState: StateFlow<MemberProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MembersEvent>()
    val events: SharedFlow<MembersEvent> = _events.asSharedFlow()

    private var record: MemberRecord? = null

    /** Called whenever the screen comes back to the front, so an edit shows at once. */
    fun load() {
        _uiState.update { it.copy(isLoading = it.profile == null, error = null) }
        viewModelScope.launch {
            getMember(memberId)
                .onSuccess { show(it) }
                .onFailure { throwable -> fail(throwable, inPlace = _uiState.value.profile == null) }
            _uiState.update { it.copy(isLoading = false) }
            loadLastChange()
        }
    }

    // region validity

    fun onValidityChanged(isValid: Boolean) {
        val current = record ?: return
        if (_uiState.value.isSavingValidity || current.isValid == isValid) return

        render(current.copy(isValid = isValid))
        _uiState.update { it.copy(isSavingValidity = true) }
        viewModelScope.launch {
            setValidity(memberId, isValid)
                .onSuccess { updated ->
                    show(updated)
                    loadLastChange()
                }
                .onFailure { throwable ->
                    render(current)
                    fail(throwable)
                }
            _uiState.update { it.copy(isSavingValidity = false) }
        }
    }

    // endregion

    // region photo

    fun onPhotoPicked(bytes: ByteArray) {
        val current = record ?: return
        _uiState.update { it.copy(isPhotoBusy = true) }
        viewModelScope.launch {
            uploadPhoto(memberId, bytes)
                .onSuccess { url ->
                    show(current.copy(photoUrl = url))
                    loadLastChange()
                }
                .onFailure { fail(it) }
            _uiState.update { it.copy(isPhotoBusy = false) }
        }
    }

    fun onRemovePhotoRequested() {
        _uiState.update { it.copy(showRemovePhotoDialog = true) }
    }

    fun onRemovePhotoDismissed() {
        _uiState.update { it.copy(showRemovePhotoDialog = false) }
    }

    fun onRemovePhotoConfirmed() {
        val current = record ?: return
        _uiState.update { it.copy(showRemovePhotoDialog = false, isPhotoBusy = true) }
        viewModelScope.launch {
            removePhoto(memberId)
                .onSuccess {
                    show(current.copy(photoUrl = null))
                    loadLastChange()
                }
                .onFailure { fail(it) }
            _uiState.update { it.copy(isPhotoBusy = false) }
        }
    }

    // endregion

    // region delete

    fun onDeleteRequested() {
        _uiState.update { it.copy(showDeleteDialog = true, deleteTyped = "", canConfirmDelete = false) }
    }

    fun onDeleteTypedChange(value: String) {
        val name = record?.name.orEmpty()
        _uiState.update {
            it.copy(deleteTyped = value, canConfirmDelete = DeleteConfirmation.matches(value, name))
        }
    }

    fun onDeleteDismissed() {
        _uiState.update { it.copy(showDeleteDialog = false, deleteTyped = "") }
    }

    fun onDeleteConfirmed() {
        if (!_uiState.value.canConfirmDelete || _uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            deleteMember(memberId)
                .onSuccess { _events.emit(MembersEvent.Deleted) }
                .onFailure { throwable ->
                    _uiState.update { it.copy(showDeleteDialog = false, deleteTyped = "") }
                    fail(throwable)
                }
            _uiState.update { it.copy(isDeleting = false) }
        }
    }

    // endregion

    private suspend fun loadLastChange() {
        // The card is a convenience: when the history cannot load, the profile simply goes without it.
        getHistory(memberId).onSuccess { lines ->
            _uiState.update { it.copy(lastChange = lines.firstOrNull()) }
        }
    }

    private fun show(member: MemberRecord) {
        record = member
        render(member)
    }

    private fun render(member: MemberRecord) {
        _uiState.update { it.copy(profile = member.toUi(), error = null) }
    }

    /**
     * @param inPlace nothing is on screen yet, so a plain failure becomes the screen's error state
     *   instead of a passing message.
     */
    private suspend fun fail(throwable: Throwable, inPlace: Boolean = false) {
        val event = throwable.toMembersEvent()
        if (inPlace && event is MembersEvent.ShowMessage) {
            _uiState.update { it.copy(error = event.message) }
        } else {
            _events.emit(event)
        }
    }

    private fun MemberRecord.toUi(): MemberProfileUi {
        val age = computeAge.ageLabel(birthDate)
        val since = computeAge.sinceLabel(baptismDate)
        return MemberProfileUi(
            id = id,
            name = name,
            initials = initialsOf(name),
            photoUrl = photoUrl,
            headline = listOfNotNull(age, gender?.label).joinToString(HEADLINE_SEPARATOR),
            statusLabel = statusLabel(status),
            roleLabel = role?.name,
            firstName = firstName.ifBlank { NOT_INFORMED },
            lastName = lastName.ifBlank { NOT_INFORMED },
            birthDateLabel = birthDate?.let(::formatDate) ?: NOT_INFORMED,
            ageLabel = age ?: if (birthDate == null) NOT_INFORMED else AGE_UNKNOWN,
            genderLabel = gender?.label ?: NOT_INFORMED,
            roleText = role?.name ?: NO_ROLE,
            baptismLabel = baptismDate
                ?.let { date -> listOfNotNull(formatDate(date), since).joinToString(HEADLINE_SEPARATOR) }
                ?: NOT_INFORMED,
            ministries = ministries.map { it.name },
            createdAtLabel = formatDate(createdAt.atZone(ZoneId.systemDefault()).toLocalDate()),
            isValid = isValid,
        )
    }

    private companion object {
        const val HEADLINE_SEPARATOR = " · "
    }
}
