package com.ipb.castelobranco.features.admin.profiles.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ipb.castelobranco.features.admin.profiles.domain.model.DENIAL_REASON_MAX_LENGTH
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ConfirmAction
import com.ipb.castelobranco.features.admin.profiles.presentation.state.MemberPickerState

private fun busy(isBusy: Boolean) = DialogProperties(dismissOnBackPress = !isBusy, dismissOnClickOutside = !isBusy)

@Composable
fun DenyRequestDialog(isBusy: Boolean, onDeny: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = busy(isBusy),
        title = { Text("Negar pedido") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it.take(DENIAL_REASON_MAX_LENGTH) },
                label = { Text("Motivo (opcional)") },
                minLines = 2,
                enabled = !isBusy,
                supportingText = { Text("${reason.length}/$DENIAL_REASON_MAX_LENGTH") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onDeny(reason) }, enabled = !isBusy) {
                Text("Negar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Cancelar") } },
    )
}

@Composable
fun ConfirmActionDialog(
    action: ConfirmAction,
    accountName: String,
    accessViaRole: Boolean,
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, text, button) = when (action) {
        ConfirmAction.REVOKE -> Triple(
            "Revogar acesso",
            "Revogar o acesso de $accountName?" + if (accessViaRole) " O papel continua liberando o acesso." else "",
            "Revogar",
        )
        ConfirmAction.UNLINK -> Triple(
            "Remover vínculo",
            "Remover o vínculo de $accountName com o rol?",
            "Remover",
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = busy(isBusy),
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isBusy) { Text(button, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Cancelar") } },
    )
}

/** Picks the roll member an account is linked to, searching by name on the device. */
@Composable
fun MemberPickerDialog(
    state: MemberPickerState,
    isBusy: Boolean,
    onQueryChange: (String) -> Unit,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = busy(isBusy),
        title = { Text("Vincular ao rol") },
        text = {
            Column {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    placeholder = { Text("Buscar por nome") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 320.dp)) {
                    when {
                        state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        state.error != null -> Text(
                            state.error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        state.members.isEmpty() -> Text(
                            "Nenhum membro encontrado.",
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        else -> LazyColumn {
                            items(state.members, key = { it.id }) { member ->
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isBusy) { onPick(member.id) }
                                        .padding(vertical = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Cancelar") } },
    )
}
