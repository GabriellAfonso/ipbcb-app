package com.ipb.castelobranco.core.presentation.components.access

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.domain.access.AccessPrompt

const val ACCESS_PENDING_MESSAGE = "Pedido enviado. Aguardando a liderança."
const val ACCESS_BLOCKED_MESSAGE = "Pedidos de acesso indisponíveis. Fale com a liderança."

/**
 * What a signed-in person without member access sees on a members-only screen (a 403): the screen's [message]
 * and, from the profile, a way to ask for access, the pending text or the blocked text.
 */
@Composable
fun MembersOnlyPlaceholder(
    message: String,
    modifier: Modifier = Modifier,
    viewModel: MembersOnlyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    MembersOnlyContent(
        message = message,
        prompt = state.prompt,
        modifier = modifier,
        onRequestAccess = viewModel::openDialog,
    )
    state.dialog?.let { dialog ->
        AccessRequestDialog(state = dialog, onSend = viewModel::send, onDismiss = viewModel::dismissDialog)
    }
}

@Composable
fun MembersOnlyContent(
    message: String,
    prompt: AccessPrompt,
    modifier: Modifier = Modifier,
    onRequestAccess: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        when (prompt) {
            AccessPrompt.CAN_REQUEST -> {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = onRequestAccess) { Text("Pedir acesso") }
            }
            AccessPrompt.PENDING -> PromptNote(ACCESS_PENDING_MESSAGE)
            AccessPrompt.BLOCKED -> PromptNote(ACCESS_BLOCKED_MESSAGE)
            AccessPrompt.NONE -> Unit
        }
    }
}

@Composable
private fun PromptNote(text: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Composable
private fun MembersOnlyCanRequestPreview() {
    MembersOnlyContent(message = "Disponível apenas para membros.", prompt = AccessPrompt.CAN_REQUEST)
}

@Preview(showBackground = true)
@Composable
private fun MembersOnlyPendingPreview() {
    MembersOnlyContent(message = "Disponível apenas para membros.", prompt = AccessPrompt.PENDING)
}
