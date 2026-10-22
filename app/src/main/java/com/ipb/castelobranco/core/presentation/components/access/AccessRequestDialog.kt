package com.ipb.castelobranco.core.presentation.components.access

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ipb.castelobranco.core.domain.access.ACCESS_REQUEST_MESSAGE_MAX_LENGTH
import com.ipb.castelobranco.core.domain.access.AccessRequestType

/** A request dialog being shown; [error] is the server's refusal, shown at the bottom. */
data class AccessRequestDialogState(
    val type: AccessRequestType,
    val isSending: Boolean = false,
    val error: String? = null,
)

/** An access request takes an optional message; a data correction needs one. */
fun canSendAccessRequest(type: AccessRequestType, message: String): Boolean =
    type == AccessRequestType.ACCESS || message.isNotBlank()

private const val ACCESS_TITLE = "Pedir acesso"
private const val ACCESS_LABEL = "Mensagem para a liderança (opcional)"
private const val CORRECTION_TITLE = "Solicitar correção"
private const val CORRECTION_LABEL = "O que precisa ser corrigido?"

@Composable
fun AccessRequestDialog(
    state: AccessRequestDialogState,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var message by rememberSaveable { mutableStateOf("") }
    val isAccess = state.type == AccessRequestType.ACCESS

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = !state.isSending, dismissOnClickOutside = !state.isSending),
        title = { Text(if (isAccess) ACCESS_TITLE else CORRECTION_TITLE) },
        text = {
            Column {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(ACCESS_REQUEST_MESSAGE_MAX_LENGTH) },
                    label = { Text(if (isAccess) ACCESS_LABEL else CORRECTION_LABEL) },
                    minLines = 3,
                    enabled = !state.isSending,
                    supportingText = {
                        Text(
                            "${message.length}/$ACCESS_REQUEST_MESSAGE_MAX_LENGTH",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.error != null) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSend(message) },
                enabled = !state.isSending && canSendAccessRequest(state.type, message),
            ) {
                if (state.isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enviar")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isSending) { Text("Cancelar") } },
    )
}

@Preview
@Composable
private fun AccessRequestDialogPreview() {
    AccessRequestDialog(
        state = AccessRequestDialogState(type = AccessRequestType.ACCESS, error = "Tente novamente amanhã."),
        onSend = {},
        onDismiss = {},
    )
}
