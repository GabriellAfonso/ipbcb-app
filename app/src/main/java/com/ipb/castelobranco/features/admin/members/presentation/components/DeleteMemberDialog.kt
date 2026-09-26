package com.ipb.castelobranco.features.admin.members.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme

/**
 * The only guard before a permanent delete: the server removes record, history and photo at once,
 * with no recycle bin. The button stays off until the leader types the member's name.
 */
@Composable
fun DeleteMemberDialog(
    memberName: String,
    typed: String,
    canConfirm: Boolean,
    isDeleting: Boolean,
    onTypedChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text("Excluir membro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("A ficha, o histórico e a foto de $memberName serão apagados para sempre.")
                Text(
                    text = "Digite o nome do membro para confirmar:",
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = typed,
                    onValueChange = onTypedChange,
                    singleLine = true,
                    placeholder = { Text(memberName) },
                    enabled = !isDeleting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            val colors = MaterialTheme.colorScheme
            TextButton(onClick = onConfirm, enabled = canConfirm && !isDeleting) {
                Text("Excluir", color = if (canConfirm) colors.error else colors.outline)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text("Cancelar") }
        },
    )
}

@Preview
@Composable
private fun DeleteMemberDialogPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        DeleteMemberDialog(
            memberName = "Ana Souza",
            typed = "ana",
            canConfirm = false,
            isDeleting = false,
            onTypedChange = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
