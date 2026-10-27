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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** One checkbox of a [MediaTypesDialog]: "Fotos 320 MB", or disabled with "Já baixadas". */
data class MediaTypeOption(
    val label: String,
    val detail: String? = null,
    val checked: Boolean,
    val enabled: Boolean = true,
)

/**
 * A confirmation with one checkbox per media type — "Apagar galeria" and "Baixar galeria". [note] is a
 * line under the boxes; [extra] an optional last checkbox (e.g. mobile data for photos). The confirm
 * button is disabled while no type is checked.
 */
data class MediaTypesDialogState(
    val title: String,
    val message: String,
    val options: List<MediaTypeOption>,
    val confirmLabel: String,
    val note: String? = null,
    val extra: MediaTypeOption? = null,
) {
    val canConfirm: Boolean get() = options.any { it.checked && it.enabled }
}

@Composable
fun MediaTypesDialog(
    state: MediaTypesDialogState,
    onToggle: (index: Int) -> Unit,
    onToggleExtra: () -> Unit,
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
                state.extra?.let { OptionRow(it, onToggleExtra) }
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
        Column(modifier = Modifier.weight(1f)) {
            Text(option.label, style = MaterialTheme.typography.bodyLarge)
            option.detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private const val CANCEL_LABEL = "Cancelar"

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
        onToggleExtra = {},
        onConfirm = {},
        onDismiss = {},
    )
}
