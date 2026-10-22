package com.ipb.castelobranco.features.profile.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.domain.access.AccessPrompt
import com.ipb.castelobranco.core.domain.access.AccessStatus
import com.ipb.castelobranco.core.domain.access.MemberAccess
import com.ipb.castelobranco.core.domain.access.accessPromptFor
import com.ipb.castelobranco.core.presentation.components.access.ACCESS_BLOCKED_MESSAGE
import com.ipb.castelobranco.features.profile.presentation.state.OwnMemberSection

/** "Acesso: Liberado / Aguardando / Não liberado"; hidden while the status is unknown. */
@Composable
fun AccessCard(access: MemberAccess, onRequestAccess: () -> Unit) {
    val label = when (access.status) {
        AccessStatus.GRANTED -> "Liberado"
        AccessStatus.PENDING -> "Aguardando"
        AccessStatus.NONE -> "Não liberado"
        AccessStatus.UNKNOWN -> return
    }
    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Acesso", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            when (accessPromptFor(access)) {
                AccessPrompt.CAN_REQUEST -> OutlinedButton(onClick = onRequestAccess) { Text("Pedir acesso") }
                AccessPrompt.BLOCKED -> Note(ACCESS_BLOCKED_MESSAGE)
                AccessPrompt.PENDING, AccessPrompt.NONE -> Unit
            }
        }
    }
}

/** "Meus dados na igreja": the linked roll record, read-only, with a way to ask for a correction. */
@Composable
fun OwnMemberCard(
    section: OwnMemberSection,
    access: MemberAccess,
    onRequestCorrection: () -> Unit,
    onRetry: () -> Unit,
) {
    if (section == OwnMemberSection.Hidden) return
    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meus dados na igreja", style = MaterialTheme.typography.titleSmall)
            when (section) {
                OwnMemberSection.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                is OwnMemberSection.Error -> {
                    Text(section.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Tentar novamente") }
                }
                is OwnMemberSection.Loaded -> {
                    section.lines.forEach { (label, value) ->
                        Column {
                            Note(label)
                            Text(value, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    when {
                        access.requestsBlocked -> Unit
                        access.correctionPending -> OutlinedButton(onClick = {}, enabled = false) {
                            Text("Correção solicitada")
                        }
                        else -> OutlinedButton(onClick = onRequestCorrection) { Text("Solicitar correção") }
                    }
                }
                OwnMemberSection.Hidden -> Unit
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
