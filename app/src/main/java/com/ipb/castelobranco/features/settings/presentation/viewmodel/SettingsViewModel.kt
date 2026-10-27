package com.ipb.castelobranco.features.settings.presentation.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.gallery.GalleryStoragePort
import com.ipb.castelobranco.core.domain.gallery.GalleryUsage
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import com.ipb.castelobranco.features.bible.domain.usecase.DeleteBibleUseCase
import com.ipb.castelobranco.features.settings.domain.model.ThemeMode
import com.ipb.castelobranco.features.settings.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What "Dados" can delete from here; the gallery's own options live in Configurações › Galeria. */
enum class ResetAction { BIBLE }

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val birthdayNotifications: Boolean = true,
    val keepScreenOn: Boolean = true,
    val pendingConfirmation: ResetAction? = null,
    val bibleCleared: Boolean = false,
    /** Bytes on disk per type; `null` while still being measured. */
    val galleryUsage: GalleryUsage? = null,
    val bibleBytes: Long? = null,
    /** "Logs do app" stays hidden until "Versão" is tapped [DIAGNOSTICS_UNLOCK_TAPS] times. */
    val showDiagnostics: Boolean = false,
)

const val DIAGNOSTICS_UNLOCK_TAPS = 5

private data class ExtraState(
    val pendingConfirmation: ResetAction? = null,
    val bibleCleared: Boolean = false,
    val galleryUsage: GalleryUsage? = null,
    val bibleBytes: Long? = null,
    val versionTaps: Int = 0,
)

/**
 * The main settings screen. The gallery is reached only through [GalleryStoragePort] (core): features
 * never import each other.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val galleryStorage: GalleryStoragePort,
    private val deleteBible: DeleteBibleUseCase,
    private val bibleRepository: BibleRepository,
) : ViewModel() {

    private val _extra = MutableStateFlow(ExtraState())

    init {
        refreshDiskUsage()
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.themeModeFlow,
        repository.birthdayNotificationsFlow,
        repository.keepScreenOnFlow,
        _extra,
    ) { mode, birthdayNotif, keepScreenOn, extra ->
        SettingsUiState(
            themeMode = mode,
            birthdayNotifications = birthdayNotif,
            keepScreenOn = keepScreenOn,
            pendingConfirmation = extra.pendingConfirmation,
            bibleCleared = extra.bibleCleared,
            galleryUsage = extra.galleryUsage,
            bibleBytes = extra.bibleBytes,
            showDiagnostics = extra.versionTaps >= DIAGNOSTICS_UNLOCK_TAPS,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    private val _events = MutableSharedFlow<Unit>()
    val events = _events.asSharedFlow()

    fun onVersionTap() {
        _extra.update { it.copy(versionTaps = (it.versionTaps + 1).coerceAtMost(DIAGNOSTICS_UNLOCK_TAPS)) }
    }

    fun requestReset(action: ResetAction) {
        _extra.update { it.copy(pendingConfirmation = action) }
    }

    fun dismissConfirmation() {
        _extra.update { it.copy(pendingConfirmation = null) }
    }

    fun confirmReset() {
        val action = _extra.value.pendingConfirmation ?: return
        _extra.update { it.copy(pendingConfirmation = null) }
        viewModelScope.launch {
            when (action) {
                ResetAction.BIBLE -> {
                    deleteBible()
                    _extra.update { it.copy(bibleCleared = true) }
                }
            }
            refreshDiskUsage()
        }
    }

    /** Also on every return to the screen: the gallery screen may have deleted or downloaded media. */
    fun refreshDiskUsage() {
        viewModelScope.launch {
            val gallery = galleryStorage.usage()
            val bible = bibleRepository.diskUsageBytes()
            _extra.update { it.copy(galleryUsage = gallery, bibleBytes = bible) }
        }
    }

    fun toggleBirthdayNotifications() {
        viewModelScope.launch {
            repository.setBirthdayNotifications(!uiState.value.birthdayNotifications)
        }
    }

    fun toggleKeepScreenOn() {
        viewModelScope.launch {
            repository.setKeepScreenOn(!uiState.value.keepScreenOn)
        }
    }

    fun selectThemeMode(mode: ThemeMode) {
        if (mode == uiState.value.themeMode) return
        viewModelScope.launch {
            repository.setThemeMode(mode)
            AppCompatDelegate.setDefaultNightMode(
                when (mode) {
                    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    ThemeMode.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
            _events.emit(Unit)
        }
    }
}
