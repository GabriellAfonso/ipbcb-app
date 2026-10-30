package com.ipb.castelobranco.features.admin.profiles.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.features.admin.members.presentation.util.initialsOf
import com.ipb.castelobranco.features.admin.profiles.di.ProfilePhotoLoader
import com.ipb.castelobranco.features.admin.profiles.domain.model.ManagedAccount
import com.ipb.castelobranco.features.admin.profiles.domain.model.ProfilesTab
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.ObserveManagedAccountsUseCase
import com.ipb.castelobranco.features.admin.profiles.domain.usecase.RefreshManagedAccountsUseCase
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfileRowUi
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfilesEvent
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfilesListUiState
import com.ipb.castelobranco.features.admin.profiles.presentation.util.formatDateTime
import com.ipb.castelobranco.features.admin.profiles.presentation.util.isAccessLost
import com.ipb.castelobranco.features.admin.profiles.presentation.util.message
import com.ipb.castelobranco.features.admin.profiles.presentation.util.requestTypeLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pendentes / Todos. The lists live in the repository, so a decision on the detail shows here at once. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfilesListViewModel @Inject constructor(
    private val observeAccounts: ObserveManagedAccountsUseCase,
    private val refreshAccounts: RefreshManagedAccountsUseCase,
    @ProfilePhotoLoader val imageLoader: ImageLoader,
) : ViewModel() {

    private val tab = MutableStateFlow(ProfilesTab.PENDING)
    private val isRefreshing = MutableStateFlow(false)
    private val loadError = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<ProfilesEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProfilesEvent> = _events.asSharedFlow()

    /** Pending / all sizes for the tab labels; each stays `null` until its list loads. */
    private val counts = combine(observeAccounts(ProfilesTab.PENDING), observeAccounts(ProfilesTab.ALL)) { p, a ->
        p?.size to a?.size
    }

    val uiState: StateFlow<ProfilesListUiState> = combine(
        tab,
        tab.flatMapLatest { observeAccounts(it) },
        isRefreshing,
        loadError,
        counts,
    ) { tab, accounts, refreshing, error, (pendingCount, allCount) ->
        ProfilesListUiState(
            tab = tab,
            isLoading = accounts == null && error == null,
            isRefreshing = refreshing && accounts != null,
            error = error.takeIf { accounts == null },
            rows = accounts.orEmpty().map { it.toRow() },
            pendingCount = pendingCount,
            allCount = allCount,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfilesListUiState())

    init {
        refresh()
    }

    /** "Pendentes" opens first: a notification about a new request lands there. */
    fun selectTab(value: ProfilesTab) {
        if (tab.value == value) return
        tab.value = value
        refresh()
    }

    fun refresh() {
        val current = tab.value
        isRefreshing.value = true
        loadError.value = null
        viewModelScope.launch {
            refreshAccounts(current).onFailure { error ->
                if (error.isAccessLost()) {
                    _events.tryEmit(ProfilesEvent.LeaveArea(error.message()))
                } else {
                    loadError.value = error.message()
                    _events.tryEmit(ProfilesEvent.ShowMessage(error.message()))
                }
            }
            isRefreshing.value = false
        }
    }

    private fun ManagedAccount.toRow(): ProfileRowUi {
        val oldest = pendingRequests.firstOrNull()
        return ProfileRowUi(
            id = id,
            name = name,
            initials = initialsOf(name),
            photoUrl = photoUrl,
            pendingLabel = oldest?.let { requestTypeLabel(it.type) },
            isBlocked = requestsBlocked,
            username = username,
            hasAccess = memberAccess,
            isCorrection = oldest?.type == AccessRequestType.DATA_CORRECTION,
            pendingMessage = oldest?.message,
            pendingAt = oldest?.createdAt?.let(::formatDateTime),
        )
    }
}
