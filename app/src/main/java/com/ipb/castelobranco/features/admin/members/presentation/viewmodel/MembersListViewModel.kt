package com.ipb.castelobranco.features.admin.members.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.features.admin.members.di.MemberPhotoLoader
import com.ipb.castelobranco.features.admin.members.domain.model.MemberSummary
import com.ipb.castelobranco.features.admin.members.domain.usecase.ObserveMembersUseCase
import com.ipb.castelobranco.features.admin.members.domain.usecase.RefreshMembersUseCase
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberCardUi
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersListUiState
import com.ipb.castelobranco.features.admin.members.presentation.util.FailureKind
import com.ipb.castelobranco.features.admin.members.presentation.util.initialsOf
import com.ipb.castelobranco.features.admin.members.presentation.util.normalizeForSearch
import com.ipb.castelobranco.features.admin.members.presentation.util.statusLabel
import com.ipb.castelobranco.features.admin.members.presentation.util.toMembersEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The roll as a grid. The list itself lives in the repository, in memory, so a change made on
 * the profile or the form shows here without a reload; this ViewModel only asks the server to
 * revalidate and filters by name on the device.
 */
@HiltViewModel
class MembersListViewModel @Inject constructor(
    observeMembers: ObserveMembersUseCase,
    private val refreshMembers: RefreshMembersUseCase,
    observeAccess: ObserveAccessUseCase,
    @MemberPhotoLoader val imageLoader: ImageLoader,
) : ViewModel() {

    private val canAdd = observeAccess().map { it.allows(Scope.MEMBERS, AccessLevel.MANAGE) }

    private val query = MutableStateFlow("")
    private val isRefreshing = MutableStateFlow(true)
    private val loadError = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<MembersEvent>()
    val events: SharedFlow<MembersEvent> = _events.asSharedFlow()

    val uiState: StateFlow<MembersListUiState> =
        combine(observeMembers(), query, isRefreshing, loadError, canAdd) { members, query, refreshing, error, add ->
            buildState(members, query, refreshing, error).copy(canAdd = add)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MembersListUiState())

    init {
        refresh()
    }

    fun refresh() {
        isRefreshing.value = true
        loadError.value = null
        viewModelScope.launch {
            refreshMembers().onFailure { throwable ->
                val event = throwable.toMembersEvent(FailureKind.READ)
                if (event is MembersEvent.ShowMessage) loadError.value = event.message
                _events.emit(event)
            }
            isRefreshing.value = false
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    private fun buildState(
        members: List<MemberSummary>?,
        query: String,
        refreshing: Boolean,
        error: String?,
    ): MembersListUiState {
        val all = members.orEmpty()
        val needle = normalizeForSearch(query)
        val visible = if (needle.isEmpty()) all else all.filter { needle in normalizeForSearch(it.name) }
        return MembersListUiState(
            // A list already in memory stays on screen while it revalidates.
            isLoading = members == null && refreshing,
            error = error.takeIf { members == null },
            query = query,
            members = visible.map { it.toCard() },
            totalCount = all.size,
        )
    }

    private fun MemberSummary.toCard() = MemberCardUi(
        id = id,
        name = name,
        initials = initialsOf(name),
        photoUrl = photoUrl,
        statusLabel = statusLabel(status),
        isValid = isValid,
    )
}
