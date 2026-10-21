package com.ipb.castelobranco.features.profile.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ipb.castelobranco.features.profile.domain.model.DELETION_CONFIRMATION_WORD
import com.ipb.castelobranco.features.profile.domain.model.PROFILE_NAME_MAX_LENGTH
import com.ipb.castelobranco.features.profile.presentation.state.DeleteDialogState
import com.ipb.castelobranco.features.profile.presentation.state.NameDialogState
import com.ipb.castelobranco.features.profile.presentation.state.PasswordDialogState
import com.ipb.castelobranco.features.profile.presentation.state.canChangePassword
import com.ipb.castelobranco.features.profile.presentation.state.canDeleteAccount
import com.ipb.castelobranco.features.profile.presentation.state.canSaveName

private const val DELETE_EXPLANATION =
    "Sua conta, sua foto e seus dados de acesso serão apagados agora e não podem ser recuperados. " +
        "O que você publicou na igreja continua, sem autor."

@Composable
fun RemovePhotoDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remover foto") },
        text = { Text("Remover sua foto de perfil?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Remover") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun EditNameDialog(
    state: NameDialogState,
    currentName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = busyProperties(state.isSaving),
        title = { Text("Editar nome") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(PROFILE_NAME_MAX_LENGTH) },
                    label = { Text("Nome") },
                    singleLine = true,
                    enabled = !state.isSaving,
                    isError = state.nameError != null,
                    supportingText = { FieldMessage(state.nameError ?: "${name.length}/$PROFILE_NAME_MAX_LENGTH") },
                    modifier = Modifier.fillMaxWidth(),
                )
                DialogError(state.error)
            }
        },
        confirmButton = {
            BusyButton(
                text = "Salvar",
                isBusy = state.isSaving,
                enabled = canSaveName(name, currentName),
                onClick = { onSave(name) },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancelar") } },
    )
}

@Composable
fun ChangePasswordDialog(
    state: PasswordDialogState,
    onConfirm: (current: String, new: String, confirm: String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Senhas ficam só na composição: nunca no estado salvo da tela.
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = busyProperties(state.isSaving),
        title = { Text("Trocar senha") },
        text = {
            Column {
                PasswordField("Senha atual", current, { current = it }, state.currentError, !state.isSaving)
                PasswordField("Nova senha", new, { new = it }, state.newError, !state.isSaving)
                PasswordField("Confirmar nova senha", confirm, { confirm = it }, state.confirmError, !state.isSaving)
                DialogError(state.error)
            }
        },
        confirmButton = {
            BusyButton(
                text = "Trocar",
                isBusy = state.isSaving,
                enabled = canChangePassword(current, new, confirm),
                onClick = { onConfirm(current, new, confirm) },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancelar") } },
    )
}

@Composable
fun DeleteAccountDialog(
    state: DeleteDialogState,
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = busyProperties(state.isDeleting),
        title = { Text("Excluir conta") },
        text = {
            Column {
                Text(DELETE_EXPLANATION, style = MaterialTheme.typography.bodyMedium)
                if (state.requiresPassword) {
                    PasswordField("Senha", password, { password = it }, state.passwordError, !state.isDeleting)
                }
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text("Digite $DELETION_CONFIRMATION_WORD para confirmar") },
                    singleLine = true,
                    enabled = !state.isDeleting,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                DialogError(state.error)
            }
        },
        confirmButton = {
            BusyButton(
                text = if (state.requiresPassword) "Excluir" else "Confirmar com o Google",
                isBusy = state.isDeleting,
                enabled = canDeleteAccount(state.requiresPassword, password, confirmation),
                color = MaterialTheme.colorScheme.error,
                onClick = { onConfirm(password) },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isDeleting) { Text("Cancelar") } },
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    enabled: Boolean,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { { FieldMessage(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar senha" else "Mostrar senha",
                )
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

@Composable
private fun BusyButton(
    text: String,
    isBusy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    color: Color = Color.Unspecified,
) {
    TextButton(onClick = onClick, enabled = enabled && !isBusy) {
        if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text(text, color = if (enabled) color else Color.Unspecified)
        }
    }
}

@Composable
private fun FieldMessage(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun DialogError(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** While a request runs the dialog cannot be dismissed by tapping outside or going back. */
private fun busyProperties(isBusy: Boolean) =
    DialogProperties(dismissOnBackPress = !isBusy, dismissOnClickOutside = !isBusy)
