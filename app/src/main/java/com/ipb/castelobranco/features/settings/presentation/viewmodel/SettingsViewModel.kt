package com.ipb.castelobranco.features.settings.presentation.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import com.ipb.castelobranco.features.bible.domain.usecase.DeleteBibleUseCase
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.usecase.DeleteGalleryUseCase
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

enum class ResetAction { GALLERY, BIBLE }

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val birthdayNotifications: Boolean = true,
    val keepScreenOn: Boolean = true,
    val pendingConfirmation: ResetAction? = null,
    val galleryCleared: Boolean = false,
    val bibleCleared: Boolean = false,
    /** Bytes on disk; `null` while still being measured. */
    val galleryBytes: Long? = null,
    val bibleBytes: Long? = null,
)

private data class ExtraState(
    val pendingConfirmation: ResetAction? = null,
    val galleryCleared: Boolean = false,
    val bibleCleared: Boolean = false,
    val galleryBytes: Long? = null,
    val bibleBytes: Long? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val galleryRepository: GalleryRepository,
    private val deleteGallery: DeleteGalleryUseCase,
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
            galleryCleared = extra.galleryCleared,
            bibleCleared = extra.bibleCleared,
            galleryBytes = extra.galleryBytes,
            bibleBytes = extra.bibleBytes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    private val _events = MutableSharedFlow<Unit>()
    val events = _events.asSharedFlow()

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
                ResetAction.GALLERY -> {
                    deleteGallery()
                    _extra.update { it.copy(galleryCleared = true) }
                }
                ResetAction.BIBLE -> {
                    deleteBible()
                    _extra.update { it.copy(bibleCleared = true) }
                }
            }
            refreshDiskUsage()
        }
    }

    private fun refreshDiskUsage() {
        viewModelScope.launch {
            val gallery = galleryRepository.diskUsageBytes()
            val bible = bibleRepository.diskUsageBytes()
            _extra.update { it.copy(galleryBytes = gallery, bibleBytes = bible) }
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
