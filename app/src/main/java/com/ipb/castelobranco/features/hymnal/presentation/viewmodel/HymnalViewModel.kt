package com.ipb.castelobranco.features.hymnal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.data.local.ThemePreferences
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.hymnal.domain.usecase.ObserveHymnsUseCase
import com.ipb.castelobranco.features.hymnal.domain.usecase.SearchHymnsUseCase
import com.ipb.castelobranco.features.hymnal.presentation.mapper.groupHymnsByHundred
import com.ipb.castelobranco.features.hymnal.presentation.screens.HymnalUiState
import com.ipb.castelobranco.features.settings.domain.repository.SettingsRepository
import com.ipb.castelobranco.core.di.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HymnalViewModel @Inject constructor(
    private val observeHymnsUseCase: ObserveHymnsUseCase,
    private val searchHymnsUseCase: SearchHymnsUseCase,
    private val settingsRepository: SettingsRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh(minDurationMs: Long = 600L) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val start = System.currentTimeMillis()
            runCatching { observeHymnsUseCase.refresh() }
                .onFailure { Timber.w(it, "Failed to refresh hymns") }
            val elapsed = System.currentTimeMillis() - start
            if (elapsed < minDurationMs) delay(minDurationMs - elapsed)
            _isRefreshing.value = false
        }
    }

    val hymnalFontSize: StateFlow<Float> = settingsRepository.hymnalFontSizeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemePreferences.DEFAULT_HYMNAL_FONT_SIZE
        )

    fun setHymnalFontSize(size: Float) {
        viewModelScope.launch { settingsRepository.setHymnalFontSize(size) }
    }

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<HymnalUiState> = combine(
        observeHymnsUseCase(),
        // Debounce apenas na digitacao: a busca varre a letra inteira de todos os hinos, mas a
        // query vazia (abertura da tela e limpeza do campo) tem que passar direto, senao a lista
        // so aparece 200 ms depois de entrar no hinario.
        _query.debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
    ) { state, query ->
        when (state) {
            is SnapshotState.Loading -> HymnalUiState(isLoading = true, error = null)
            is SnapshotState.Data -> {
                val filtered = searchHymnsUseCase(state.value, query)
                HymnalUiState(
                    hymns = state.value,
                    filteredHymns = filtered,
                    groups = groupHymnsByHundred(filtered),
                    isLoading = false,
                    error = null
                )
            }
            is SnapshotState.Error -> HymnalUiState(
                isLoading = false,
                error = state.error.toUserMessage()
            )
        }
    }.flowOn(defaultDispatcher)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HymnalUiState(isLoading = true)
    )

    fun onQueryChange(query: String) {
        _query.value = query
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 200L
    }
}
