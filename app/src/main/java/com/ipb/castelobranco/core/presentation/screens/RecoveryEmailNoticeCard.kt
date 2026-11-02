package com.ipb.castelobranco.core.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme

private const val NOTICE_TITLE = "Adicione um e-mail à sua conta"
private const val NOTICE_TEXT =
    "É o e-mail que permite recuperar sua senha se você a esquecer. " +
        "Ele é usado só para isso, não para entrar no app."
private const val ADD_LABEL = "Adicionar e-mail"
private const val DISMISS_LABEL = "Agora não"

/**
 * The one-time notice (`specs/core/spec.md` §6.3). It takes the carousel's slot, whose height is what the shortcuts
 * leave, so it scrolls inside itself instead of ever moving them.
 */
@Composable
internal fun RecoveryEmailNoticeCard(onAdd: () -> Unit, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(NOTICE_TITLE, style = MaterialTheme.typography.titleMedium)
            Text(NOTICE_TEXT, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd) { Text(ADD_LABEL) }
                TextButton(onClick = onDismiss) { Text(DISMISS_LABEL) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecoveryEmailNoticeCardPreview() {
    IPBCasteloBrancoTheme {
        RecoveryEmailNoticeCard(onAdd = {}, onDismiss = {})
    }
}
