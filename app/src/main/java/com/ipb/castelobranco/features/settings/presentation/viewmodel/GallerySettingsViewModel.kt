package com.ipb.castelobranco.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.gallery.GalleryMissing
import com.ipb.castelobranco.core.domain.gallery.GalleryStoragePort
import com.ipb.castelobranco.core.domain.gallery.GalleryUsage
import com.ipb.castelobranco.core.domain.gallery.GalleryVideoSummary
import com.ipb.castelobranco.core.presentation.components.GalleryStorageChoice
import com.ipb.castelobranco.core.presentation.components.GalleryStorageDialogs
import com.ipb.castelobranco.core.presentation.components.MediaTypesDialogState
import com.ipb.castelobranco.core.presentation.components.formatDiskUsage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The open "Apagar galeria" / "Baixar galeria" dialog with what is checked. */
sealed interface GalleryStorageDialog {
    val state: MediaTypesDialogState

    data class Delete(val usage: GalleryUsage?, val choice: GalleryStorageChoice) : GalleryStorageDialog {
        override val state get() = GalleryStorageDialogs.deleteDialog(usage, choice)
    }

    data class Download(
        val missing: GalleryMissing,
        val autoDownloadVideos: Boolean,
        val choice: GalleryStorageChoice,
    ) : GalleryStorageDialog {
        override val state get() = GalleryStorageDialogs.downloadDialog(missing, autoDownloadVideos, choice)
    }
}

data class GallerySettingsUiState(
    val autoDownloadVideos: Boolean = false,
    /** Under the switch: what is available, or the progress. */
    val videosDescription: String = GallerySettingsTexts.WIFI_ONLY,
    val usage: GalleryUsage? = null,
    val dialog: GalleryStorageDialog? = null,
)

/** Configurações › Galeria. Talks to the gallery only through [GalleryStoragePort] (core). */
@HiltViewModel
class GallerySettingsViewModel @Inject constructor(
    private val storage: GalleryStoragePort,
) : ViewModel() {

    private val usage = MutableStateFlow<GalleryUsage?>(null)
    private val dialog = MutableStateFlow<GalleryStorageDialog?>(null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** One-shot texts: "Vídeos apagados", ... */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val uiState: StateFlow<GallerySettingsUiState> = combine(
        storage.autoDownloadVideos,
        storage.videos,
        usage,
        dialog,
    ) { auto, videos, usage, dialog ->
        GallerySettingsUiState(
            autoDownloadVideos = auto,
            videosDescription = GallerySettingsTexts.videos(auto, videos),
            usage = usage,
            dialog = dialog,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GallerySettingsUiState())

    init {
        refreshUsage()
    }

    fun setAutoDownloadVideos(enabled: Boolean) {
        viewModelScope.launch { storage.setAutoDownloadVideos(enabled) }
    }

    fun openDelete() {
        dialog.value = GalleryStorageDialog.Delete(usage.value, GalleryStorageDialogs.initialDelete())
    }

    fun openDownload() {
        viewModelScope.launch {
            val missing = storage.missing()
            dialog.value = GalleryStorageDialog.Download(
                missing = missing,
                autoDownloadVideos = uiState.value.autoDownloadVideos,
                choice = GalleryStorageDialogs.initialDownload(missing),
            )
        }
    }

    /** [index] 0 = Fotos, 1 = Vídeos. */
    fun toggle(index: Int) = updateChoice { choice ->
        if (index == 0) choice.copy(photos = !choice.photos) else choice.copy(videos = !choice.videos)
    }

    fun toggleMobileData() = updateChoice { it.copy(useMobileData = !it.useMobileData) }

    fun dismissDialog() {
        dialog.value = null
    }

    fun confirm() {
        val open = dialog.value ?: return
        val state = open.state
        if (!state.canConfirm) return
        dialog.value = null
        val photos = state.options[0].let { it.checked && it.enabled }
        val videos = state.options[1].let { it.checked && it.enabled }
        viewModelScope.launch {
            when (open) {
                is GalleryStorageDialog.Delete -> {
                    storage.delete(photos, videos)
                    _messages.tryEmit(GalleryStorageDialogs.deletedMessage(open.choice))
                }
                is GalleryStorageDialog.Download ->
                    storage.download(photos, videos, useMobileData = state.extra?.checked == true)
            }
            refreshUsage()
        }
    }

    fun refreshUsage() {
        viewModelScope.launch { usage.value = storage.usage() }
    }

    private fun updateChoice(transform: (GalleryStorageChoice) -> GalleryStorageChoice) {
        dialog.update { current ->
            when (current) {
                is GalleryStorageDialog.Delete -> current.copy(choice = transform(current.choice))
                is GalleryStorageDialog.Download -> current.copy(choice = transform(current.choice))
                null -> null
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Texts of Configurações › Galeria. Pure, tested as is. */
object GallerySettingsTexts {
    const val TITLE = "Galeria"
    const val VIDEOS_SECTION = "Vídeos"
    const val STORAGE_SECTION = "Armazenamento"
    const val AUTO_DOWNLOAD = "Baixar vídeos automaticamente"
    const val DOWNLOAD = "Baixar galeria"
    const val DOWNLOAD_DESCRIPTION = "Escolha fotos e vídeos para baixar"
    const val DELETE = "Apagar galeria"
    const val DELETE_DESCRIPTION = "Escolha fotos e vídeos para apagar deste aparelho"
    const val WIFI_ONLY = "Somente no WiFi"

    /**
     * Off: "Somente no WiFi · 12 vídeos (480 MB)". On and downloading or missing some: "8 de 12 baixados".
     * On and complete: "Somente no WiFi · 12 de 12 baixados".
     */
    fun videos(autoDownload: Boolean, summary: GalleryVideoSummary): String {
        if (summary.total == 0) return WIFI_ONLY
        val progress = "${summary.downloaded} de ${summary.total} baixados"
        return when {
            !autoDownload -> {
                val count = if (summary.total == 1) "1 vídeo" else "${summary.total} vídeos"
                val size = summary.totalBytes.takeIf { it > 0 }?.let { " (${formatDiskUsage(it)})" }.orEmpty()
                "$WIFI_ONLY · $count$size"
            }
            summary.downloaded < summary.total -> progress
            else -> "$WIFI_ONLY · $progress"
        }
    }
}
