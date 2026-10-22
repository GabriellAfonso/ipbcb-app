package com.ipb.castelobranco.features.settings.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ipb.castelobranco.BuildConfig
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.base.findActivity
import com.ipb.castelobranco.features.settings.domain.model.ThemeMode
import com.ipb.castelobranco.features.settings.presentation.components.rememberNotificationAccessRequest
import com.ipb.castelobranco.features.settings.presentation.components.withDiskUsage
import com.ipb.castelobranco.features.settings.presentation.viewmodel.ResetAction
import com.ipb.castelobranco.features.settings.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToLogViewer: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val requestNotificationAccess = rememberNotificationAccessRequest()

    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            context.findActivity()?.recreate()
        }
    }

    SettingsContent(
        onBackClick                  = onBackClick,
        themeMode                    = uiState.themeMode,
        onSelectThemeMode            = viewModel::selectThemeMode,
        keepScreenOn                 = uiState.keepScreenOn,
        onToggleKeepScreenOn         = { viewModel.toggleKeepScreenOn() },
        birthdayNotifications        = uiState.birthdayNotifications,
        onToggleBirthdayNotifications = {
            // Turning it on is the user's way to get a second chance at a permission denied by accident.
            if (!uiState.birthdayNotifications) requestNotificationAccess()
            viewModel.toggleBirthdayNotifications()
        },
        onRequestResetGallery        = { viewModel.requestReset(ResetAction.GALLERY) },
        onRequestResetBible          = { viewModel.requestReset(ResetAction.BIBLE) },
        pendingConfirmation          = uiState.pendingConfirmation,
        onConfirmReset               = { viewModel.confirmReset() },
        onDismissConfirmation        = { viewModel.dismissConfirmation() },
        galleryCleared               = uiState.galleryCleared,
        bibleCleared                 = uiState.bibleCleared,
        galleryBytes                 = uiState.galleryBytes,
        bibleBytes                   = uiState.bibleBytes,
        onNavigateToLogViewer        = onNavigateToLogViewer,
    )
}

@Composable
fun SettingsContent(
    onBackClick: () -> Unit,
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    keepScreenOn: Boolean = true,
    onToggleKeepScreenOn: () -> Unit = {},
    birthdayNotifications: Boolean = true,
    onToggleBirthdayNotifications: () -> Unit = {},
    onRequestResetGallery: () -> Unit,
    onRequestResetBible: () -> Unit,
    pendingConfirmation: ResetAction?,
    onConfirmReset: () -> Unit,
    onDismissConfirmation: () -> Unit,
    galleryCleared: Boolean,
    bibleCleared: Boolean,
    galleryBytes: Long? = null,
    bibleBytes: Long? = null,
    onNavigateToLogViewer: () -> Unit = {},
) {
    if (pendingConfirmation != null) {
        val (title, message) = when (pendingConfirmation) {
            ResetAction.GALLERY -> "Resetar galeria" to "Apaga todas as fotos em cache. Deseja continuar?"
            ResetAction.BIBLE   -> "Resetar Bíblia"  to "Apaga e baixa novamente a Bíblia. Deseja continuar?"
        }
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title            = { Text(title) },
            text             = { Text(message) },
            confirmButton    = {
                TextButton(onClick = onConfirmReset) { Text("Confirmar") }
            },
            dismissButton    = {
                TextButton(onClick = onDismissConfirmation) { Text("Cancelar") }
            },
        )
    }

    BaseScreen(
        tabName           = "Configurações",
        logoRes           = R.drawable.ic_sarca_ipb,
        showBackArrow     = true,
        onBackClick       = onBackClick,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSectionHeader("Aparência")
            ThemeModeRow(selected = themeMode, onSelect = onSelectThemeMode)

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSectionHeader("Leitura")
            SettingsSwitchRow(
                icon            = Icons.Filled.LightMode,
                title           = "Manter tela acesa",
                description     = "Em hinos, Bíblia, letras e cifras",
                checked         = keepScreenOn,
                onCheckedChange = { onToggleKeepScreenOn() },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSectionHeader("Notificações")
            SettingsSwitchRow(
                icon            = Icons.Filled.Cake,
                title           = "Aniversários",
                description     = if (birthdayNotifications) "Ativado" else "Desativado",
                checked         = birthdayNotifications,
                onCheckedChange = { onToggleBirthdayNotifications() },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSectionHeader("Dados")
            SettingsActionRow(
                icon        = Icons.Filled.Image,
                title       = "Resetar galeria",
                description = if (galleryCleared) {
                    "Cache apagado"
                } else {
                    withDiskUsage("Apaga as fotos em cache", galleryBytes)
                },
                tint        = MaterialTheme.colorScheme.error,
                done        = galleryCleared,
                onClick     = onRequestResetGallery,
            )
            SettingsActionRow(
                icon        = Icons.Filled.AutoStories,
                title       = "Resetar Bíblia",
                description = if (bibleCleared) {
                    "Baixando novamente…"
                } else {
                    withDiskUsage("Apaga e baixa novamente", bibleBytes)
                },
                tint        = MaterialTheme.colorScheme.error,
                done        = bibleCleared,
                onClick     = onRequestResetBible,
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSectionHeader("Diagnóstico")
            SettingsActionRow(
                icon        = Icons.Filled.BugReport,
                title       = "Logs do app",
                description = "Visualizar logs em tempo real",
                onClick     = onNavigateToLogViewer,
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSectionHeader("Sobre")
            SettingsInfoRow(
                icon  = Icons.Filled.Info,
                title = "Versão",
                value = BuildConfig.VERSION_NAME,
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text       = title,
        style      = MaterialTheme.typography.labelMedium,
        color      = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

private val themeModeOptions = listOf(
    ThemeMode.FOLLOW_SYSTEM to "Sistema",
    ThemeMode.LIGHT         to "Claro",
    ThemeMode.DARK          to "Escuro",
)

@Composable
private fun ThemeModeRow(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    ListItem(
        headlineContent   = { Text("Tema") },
        supportingContent = {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                themeModeOptions.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = mode == selected,
                        onClick  = { onSelect(mode) },
                        shape    = SegmentedButtonDefaults.itemShape(index, themeModeOptions.size),
                        label    = { Text(label) },
                    )
                }
            }
        },
        leadingContent    = {
            Icon(
                imageVector        = Icons.Filled.Brightness6,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent   = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent    = {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    done: Boolean = false,
    onClick: () -> Unit,
) {
    val rowAlpha = if (done) 0.38f else 1f
    ListItem(
        modifier          = Modifier
            .alpha(rowAlpha)
            .then(if (!done) Modifier.clickable(onClick = onClick) else Modifier),
        headlineContent   = { Text(title, color = tint) },
        supportingContent = { Text(description) },
        leadingContent    = {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = tint,
            )
        },
        trailingContent = if (done) ({
            Icon(
                imageVector        = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.outline,
            )
        }) else null,
    )
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent  = {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Text(
                text  = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
