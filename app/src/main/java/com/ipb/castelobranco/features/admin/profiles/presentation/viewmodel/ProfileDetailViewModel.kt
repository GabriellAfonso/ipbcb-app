package com.ipb.castelobranco.features.admin.profiles.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.features.admin.members.domain.model.MemberSummary
import com.ipb.castelobranco.features.admin.members.domain.usecase.ObserveMembersUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RefreshMembersUseCase
import com.ipb.castelobranco.features.admin.members.presentation.util.normalizeForSearch
import com.ipb.castelobranco.features.admin.profiles.di.ProfilePhotoLoader
import com.ipb.castelobranco.features.admin.profiles.domain.model.ManagedAccount
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.ApproveAccessRequestUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.DenyAccessRequestUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.GetManagedAccountUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.LinkAccountToMemberUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.RevokeMemberAccessUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.SetAccessRequestsBlockedUseCase
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ConfirmAction
import com.ipb.castelobranco.features.admin.profiles.presentation.state.DenyDialogState
import com.ipb.castelobranco.features.admin.profiles.presentation.state.MemberOptionUi
import com.ipb.castelobranco.features.admin.profiles.presentation.state.MemberPickerState
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfileDetailUiState
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfilesEvent
import com.ipb.castelobranco.features.admin.profiles.presentation.util.isAccessLost
import com.ipb.castelobranco.features.admin.profiles.presentation.util.isConflict
import com.ipb.castelobranco.features.admin.profiles.presentation.util.message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One account in "Perfis": its requests, history and, with `manage`, every action on it. */
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAccount: GetManagedAccountUseCase,
    private val approveRequest: ApproveAccessRequestUseCase,
    private val denyRequest: DenyAccessRequestUseCase,
    private val revokeAccess: RevokeMemberAccessUseCase,
    private val setRequestsBlocked: SetAccessRequestsBlockedUseCase,
    private val linkToMember: LinkAccountToMemberUseCase,
    private val observeMembers: ObserveMembersUseCase,
    private val refreshMembers: RefreshMembersUseCase,
    observeAccess: ObserveAccessUseCase,
    @ProfilePhotoLoader val imageLoader: ImageLoader,
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle[ARG_USER_ID])

    private val _uiState = MutableStateFlow(ProfileDetailUiState())
    val uiState: StateFlow<ProfileDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfilesEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProfilesEvent> = _events.asSharedFlow()

    /** The roll for the picker; named apart from [MemberPickerState.members], which the filter builds. */
    private var roll: List<MemberSummary> = emptyList()
    private var pickerJob: Job? = null

    init {
        viewModelScope.launch {
            observeAccess().collect { access ->
                _uiState.update { it.copy(canManage = access.allows(Scope.PROFILES, AccessLevel.MANAGE)) }
            }
        }
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = it.account == null, error = null) }
        viewModelScope.launch {
            getAccount(userId)
                .onSuccess { account -> _uiState.update { it.copy(isLoading = false, account = account) } }
                .onFailure { error ->
                    val shown = _uiState.value.account != null
                    when {
                        error.isAccessLost() -> _events.tryEmit(ProfilesEvent.LeaveArea(error.message()))
                        // An account already on screen stays; the failure is only a notice.
                        shown -> _events.tryEmit(ProfilesEvent.ShowMessage(error.message()))
                        else -> _uiState.update { it.copy(isLoading = false, error = error.message()) }
                    }
                }
        }
    }

    fun approve(requestId: Long) = act(APPROVED_MESSAGE) { approveRequest(requestId) }

    fun openDeny(requestId: Long) {
        _uiState.update { it.copy(denyDialog = DenyDialogState(requestId)) }
    }

    fun dismissDeny() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(denyDialog = null) }
    }

    fun deny(reason: String) {
        val dialog = _uiState.value.denyDialog ?: return
        act(DENIED_MESSAGE) { denyRequest(dialog.requestId, reason) }
    }

    fun askRevoke() {
        _uiState.update { it.copy(confirm = ConfirmAction.REVOKE) }
    }

    fun askUnlink() {
        _uiState.update { it.copy(confirm = ConfirmAction.UNLINK) }
    }

    fun dismissConfirm() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(confirm = null) }
    }

    fun confirm() {
        when (_uiState.value.confirm) {
            ConfirmAction.REVOKE -> act(REVOKED_MESSAGE) { revokeAccess(userId) }
            ConfirmAction.UNLINK -> act(UNLINKED_MESSAGE) { linkToMember(userId, null) }
            null -> Unit
        }
    }

    fun setBlocked(blocked: Boolean) =
        act(if (blocked) BLOCKED_MESSAGE else UNBLOCKED_MESSAGE) { setRequestsBlocked(userId, blocked) }

    fun openPicker() {
        _uiState.update { it.copy(picker = MemberPickerState()) }
        pickerJob?.cancel()
        pickerJob = viewModelScope.launch {
            launch {
                observeMembers().collect { list ->
                    if (list != null) {
                        roll = list
                        _uiState.update { state ->
                            state.copy(picker = state.picker?.let { it.copy(isLoading = false).filtered() })
                        }
                    }
                }
            }
            refreshMembers().onFailure { error ->
                _uiState.update { state ->
                    state.copy(picker = state.picker?.copy(isLoading = false, error = error.message()))
                }
            }
        }
    }

    fun onPickerQuery(query: String) {
        _uiState.update { state -> state.copy(picker = state.picker?.copy(query = query)?.filtered()) }
    }

    fun dismissPicker() {
        if (_uiState.value.isBusy) return
        pickerJob?.cancel()
        _uiState.update { it.copy(picker = null) }
    }

    fun pick(memberId: Int) = act(LINKED_MESSAGE) { linkToMember(userId, memberId) }

    private fun MemberPickerState.filtered(): MemberPickerState {
        val needle = normalizeForSearch(query)
        val visible = if (needle.isEmpty()) roll else roll.filter { needle in normalizeForSearch(it.name) }
        return copy(members = visible.map { MemberOptionUi(it.id, it.name) })
    }

    /**
     * Runs one action: the answer replaces the account and closes the open dialog. A conflict (someone else
     * acted first) says why and reloads; a lost scope leaves the area.
     */
    private fun act(success: String, call: suspend () -> Result<ManagedAccount>) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            call()
                .onSuccess { account ->
                    pickerJob?.cancel()
                    _uiState.update {
                        it.copy(isBusy = false, account = account, denyDialog = null, confirm = null, picker = null)
                    }
                    _events.tryEmit(ProfilesEvent.ShowMessage(success))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isBusy = false) }
                    when {
                        error.isAccessLost() -> _events.tryEmit(ProfilesEvent.LeaveArea(error.message()))
                        error.isConflict() -> {
                            _uiState.update { it.copy(denyDialog = null, confirm = null, picker = null) }
                            _events.tryEmit(ProfilesEvent.ShowMessage(error.message()))
                            load()
                        }
                        else -> _events.tryEmit(ProfilesEvent.ShowMessage(error.message()))
                    }
                }
        }
    }

    companion object {
        const val ARG_USER_ID = "userId"

        const val APPROVED_MESSAGE = "Pedido aprovado"
        const val DENIED_MESSAGE = "Pedido negado"
        const val REVOKED_MESSAGE = "Acesso revogado"
        const val BLOCKED_MESSAGE = "Pedidos bloqueados"
        const val UNBLOCKED_MESSAGE = "Pedidos desbloqueados"
        const val LINKED_MESSAGE = "Conta vinculada ao rol"
        const val UNLINKED_MESSAGE = "Vínculo removido"
    }
}
