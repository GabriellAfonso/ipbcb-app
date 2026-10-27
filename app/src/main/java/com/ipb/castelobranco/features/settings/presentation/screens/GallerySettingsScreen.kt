package com.ipb.castelobranco.features.settings.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.domain.gallery.GalleryUsage
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.GalleryStorageDialogs
import com.ipb.castelobranco.core.presentation.components.MediaTypesDialog
import com.ipb.castelobranco.core.presentation.components.MobileDataDialog
import com.ipb.castelobranco.features.settings.presentation.viewmodel.GalleryStorageDialog
import com.ipb.castelobranco.features.settings.presentation.viewmodel.GallerySettingsTexts
import com.ipb.castelobranco.features.settings.presentation.viewmodel.GallerySettingsUiState
import com.ipb.castelobranco.features.settings.presentation.viewmodel.GallerySettingsViewModel

/** The events of Configurações › Galeria. */
data class GallerySettingsActions(
    val onBack: () -> Unit = {},
    val onAutoDownloadChange: (Boolean) -> Unit = {},
    val onOpenDownload: () -> Unit = {},
    val onOpenDelete: () -> Unit = {},
    val onToggle: (Int) -> Unit = {},
    /** `true` = "Baixar agora", `false` = "Esperar WiFi". */
    val onAnswerMobileData: (downloadNow: Boolean) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)

@Composable
fun GallerySettingsScreen(
    viewModel: GallerySettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        GallerySettingsContent(
            state = state,
            actions = GallerySettingsActions(
                onBack = onBackClick,
                onAutoDownloadChange = viewModel::setAutoDownloadVideos,
                onOpenDownload = viewModel::openDownload,
                onOpenDelete = viewModel::openDelete,
                onToggle = viewModel::toggle,
                onAnswerMobileData = viewModel::answerMobileData,
                onConfirm = viewModel::confirm,
                onDismiss = viewModel::dismissDialog,
            ),
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
fun GallerySettingsContent(state: GallerySettingsUiState, actions: GallerySettingsActions) {
    val dialog = state.dialog
    when {
        dialog == null -> Unit
        (dialog as? GalleryStorageDialog.Download)?.askingMobileData == true -> MobileDataDialog(
            question = GalleryStorageDialogs.MOBILE_DATA_QUESTION,
            onDownloadNow = { actions.onAnswerMobileData(true) },
            onWaitForWifi = { actions.onAnswerMobileData(false) },
            onDismiss = actions.onDismiss,
        )
        else -> MediaTypesDialog(
            state = dialog.state,
            onToggle = actions.onToggle,
            onConfirm = actions.onConfirm,
            onDismiss = actions.onDismiss,
        )
    }
    BaseScreen(
        tabName = GallerySettingsTexts.TITLE,
        logoRes = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick = actions.onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSectionHeader(GallerySettingsTexts.VIDEOS_SECTION)
            SettingsSwitchRow(
                icon = Icons.Filled.Movie,
                title = GallerySettingsTexts.AUTO_DOWNLOAD,
                description = state.videosDescription,
                checked = state.autoDownloadVideos,
                onCheckedChange = actions.onAutoDownloadChange,
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSectionHeader(GallerySettingsTexts.STORAGE_SECTION)
            SettingsActionRow(
                icon = Icons.Filled.Download,
                title = GallerySettingsTexts.DOWNLOAD,
                description = GallerySettingsTexts.DOWNLOAD_DESCRIPTION,
                onClick = actions.onOpenDownload,
            )
            SettingsActionRow(
                icon = Icons.Filled.Delete,
                title = GallerySettingsTexts.DELETE,
                description = state.usage?.let(::galleryUsageText) ?: GallerySettingsTexts.DELETE_DESCRIPTION,
                tint = MaterialTheme.colorScheme.error,
                onClick = actions.onOpenDelete,
            )
        }
    }
}

@Preview
@Composable
private fun GallerySettingsPreview() {
    GallerySettingsContent(
        state = GallerySettingsUiState(
            autoDownloadVideos = false,
            videosDescription = "Somente no WiFi · 12 vídeos (480 MB)",
            usage = GalleryUsage(photoBytes = 320L * 1024 * 1024, videoBytes = 0),
        ),
        actions = GallerySettingsActions(),
    )
}
