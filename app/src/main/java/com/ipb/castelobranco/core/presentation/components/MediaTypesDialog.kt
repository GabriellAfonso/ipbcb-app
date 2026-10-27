package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** One checkbox of a [MediaTypesDialog]: "Fotos 320 MB", or disabled with "Já baixadas", on the same line. */
data class MediaTypeOption(
    val label: String,
    val detail: String? = null,
    val checked: Boolean,
    val enabled: Boolean = true,
)

/**
 * A confirmation with one checkbox per media type — "Apagar galeria" and "Baixar galeria". [note] is a
 * line under the boxes. The confirm button is disabled while no type is checked.
 */
data class MediaTypesDialogState(
    val title: String,
    val message: String,
    val options: List<MediaTypeOption>,
    val confirmLabel: String,
    val note: String? = null,
) {
    val canConfirm: Boolean get() = options.any { it.checked && it.enabled }
}

@Composable
fun MediaTypesDialog(
    state: MediaTypesDialogState,
    onToggle: (index: Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                state.options.forEachIndexed { index, option -> OptionRow(option) { onToggle(index) } }
                state.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = state.canConfirm) { Text(state.confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(CANCEL_LABEL) } },
    )
}

@Composable
private fun OptionRow(option: MediaTypeOption, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = option.checked,
                enabled = option.enabled,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            ),
    ) {
        Checkbox(checked = option.checked, onCheckedChange = null, enabled = option.enabled)
        val detailColor = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = buildAnnotatedString {
                append(option.label)
                option.detail?.let { detail ->
                    append(DETAIL_GAP)
                    withStyle(SpanStyle(color = detailColor)) { append(detail) }
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Asked when a download would start on mobile data: now, or wait for WiFi. */
@Composable
fun MobileDataDialog(
    question: String,
    onDownloadNow: () -> Unit,
    onWaitForWifi: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(question) },
        confirmButton = { TextButton(onClick = onDownloadNow) { Text(DOWNLOAD_NOW_LABEL) } },
        dismissButton = { TextButton(onClick = onWaitForWifi) { Text(WAIT_WIFI_LABEL) } },
    )
}

private const val CANCEL_LABEL = "Cancelar"
private const val DOWNLOAD_NOW_LABEL = "Baixar agora"
private const val WAIT_WIFI_LABEL = "Esperar WiFi"
private const val DETAIL_GAP = "  "

@Preview
@Composable
private fun MediaTypesDialogPreview() {
    MediaTypesDialog(
        state = MediaTypesDialogState(
            title = "Apagar galeria",
            message = "Escolha o que apagar deste aparelho.",
            options = listOf(
                MediaTypeOption("Fotos", "320 MB", checked = true),
                MediaTypeOption("Vídeos", "1,2 GB", checked = true),
            ),
            confirmLabel = "Apagar",
            note = "Apagar os vídeos desliga o download automático de vídeos.",
        ),
        onToggle = {},
        onConfirm = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun MobileDataDialogPreview() {
    MobileDataDialog(
        question = "Você está nos dados móveis. Baixar as fotos agora?",
        onDownloadNow = {},
        onWaitForWifi = {},
        onDismiss = {},
    )
}
