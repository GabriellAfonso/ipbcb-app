package com.ipb.castelobranco.features.admin.events.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Deleting is permanent — the event and its flyer are gone, there is no trash (`specs/events/spec.md` §4.3). */
@Composable
fun DeleteEventDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir evento?") },
        text = { Text("A exclusão é permanente e não pode ser desfeita.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
