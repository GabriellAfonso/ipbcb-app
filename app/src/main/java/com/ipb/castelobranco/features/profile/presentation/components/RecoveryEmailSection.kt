package com.ipb.castelobranco.features.profile.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.profile.domain.model.RecoveryEmailStatus
import com.ipb.castelobranco.features.profile.presentation.state.RECOVERY_CODE_LENGTH
import com.ipb.castelobranco.features.profile.presentation.state.RecoveryCodeDialogState
import com.ipb.castelobranco.features.profile.presentation.state.RecoveryEmailFormState
import com.ipb.castelobranco.features.profile.presentation.state.canSaveRecoveryEmail

private const val SECTION_TITLE = "E-mail de recuperação"
private const val PURPOSE_TEXT = "Usado só para recuperar sua senha."
private const val CONFIRMED_LABEL = "Confirmado"
private const val NOT_CONFIRMED_LABEL = "Não confirmado"
private const val ADD_LABEL = "Adicionar e-mail"
private const val CHANGE_TITLE = "Alterar e-mail"
private const val CONFIRM_LABEL = "Confirmar"
private const val CHANGE_LABEL = "Alterar"
private const val RESEND_LABEL = "Reenviar código"

/** The card of `specs/profile/spec.md` §3.2.3; the caller shows it only for an account with a password. */
@Composable
fun RecoveryEmailCard(
    status: RecoveryEmailStatus,
    onAdd: () -> Unit,
    onConfirm: () -> Unit,
    onChange: () -> Unit,
) {
    if (status == RecoveryEmailStatus.Unknown) return
    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(SECTION_TITLE, style = MaterialTheme.typography.titleSmall)
            when (status) {
                RecoveryEmailStatus.Unknown -> Unit
                RecoveryEmailStatus.None -> {
                    Purpose()
                    OutlinedButton(onClick = onAdd) { Text(ADD_LABEL) }
                }
                is RecoveryEmailStatus.Set -> {
                    Text(status.address, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (status.confirmed) CONFIRMED_LABEL else NOT_CONFIRMED_LABEL,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status.confirmed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Purpose()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!status.confirmed) OutlinedButton(onClick = onConfirm) { Text(CONFIRM_LABEL) }
                        TextButton(onClick = onChange) { Text(CHANGE_LABEL) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Purpose() {
    Text(
        text = PURPOSE_TEXT,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Adds or changes the address. The current password is asked for the first address too. */
@Composable
fun RecoveryEmailFormDialog(
    state: RecoveryEmailFormState,
    hasAddress: Boolean,
    onSave: (email: String, currentPassword: String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Endereço e senha ficam só na composição: nunca no estado salvo da tela.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = busyProperties(state.isSaving),
        title = { Text(if (hasAddress) CHANGE_TITLE else ADD_LABEL) },
        text = {
            Column {
                Text(PURPOSE_TEXT, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (hasAddress) "Novo e-mail" else "E-mail") },
                    singleLine = true,
                    enabled = !state.isSaving,
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { { FieldMessage(it) } },
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                PasswordField("Senha atual", password, { password = it }, state.passwordError, !state.isSaving)
                DialogError(state.error)
            }
        },
        confirmButton = {
            BusyButton(
                text = "Salvar",
                isBusy = state.isSaving,
                enabled = canSaveRecoveryEmail(email, password),
                onClick = { onSave(email, password) },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancelar") } },
    )
}

/** Types the code that was sent to [address]. */
@Composable
fun RecoveryCodeDialog(
    state: RecoveryCodeDialogState,
    address: String,
    onConfirm: (code: String) -> Unit,
    onResend: () -> Unit,
    onDismiss: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = busyProperties(state.isConfirming),
        title = { Text("Confirmar e-mail") },
        text = {
            Column {
                Text("Enviamos um código de 6 dígitos para $address.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = code,
                    onValueChange = { typed -> code = typed.filter(Char::isDigit).take(RECOVERY_CODE_LENGTH) },
                    label = { Text("Código") },
                    singleLine = true,
                    enabled = !state.isConfirming,
                    isError = state.codeError != null,
                    supportingText = state.codeError?.let { { FieldMessage(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                TextButton(onClick = onResend, enabled = state.canResend) {
                    Text(
                        if (state.resendSecondsLeft > 0) {
                            "$RESEND_LABEL (${state.resendSecondsLeft}s)"
                        } else {
                            RESEND_LABEL
                        }
                    )
                }
                DialogError(state.error)
            }
        },
        confirmButton = {
            BusyButton(
                text = CONFIRM_LABEL,
                isBusy = state.isConfirming,
                enabled = code.length == RECOVERY_CODE_LENGTH,
                onClick = { onConfirm(code) },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isConfirming) { Text("Cancelar") } },
    )
}
