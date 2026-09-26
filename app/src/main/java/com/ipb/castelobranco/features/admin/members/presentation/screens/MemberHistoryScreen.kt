package com.ipb.castelobranco.features.admin.members.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.features.admin.members.presentation.state.HistoryLineUi
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberHistoryUiState
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.viewmodel.MemberHistoryViewModel

@Composable
fun MemberHistoryScreen(
    viewModel: MemberHistoryViewModel,
    onBack: () -> Unit,
    onNavigationEvent: (MembersEvent) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.events.collect(onNavigationEvent) }

    BaseScreen(tabName = "Histórico", showBackArrow = true, onBackClick = onBack) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            MemberHistoryContent(state = state, onRetry = viewModel::load)
        }
    }
}

@Composable
fun MemberHistoryContent(state: MemberHistoryUiState, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.error != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Text(state.error, textAlign = TextAlign.Center)
                Button(onClick = onRetry) { Text("Tentar novamente") }
            }
            state.lines.isEmpty() -> Text(
                text = "Nenhuma alteração registrada.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                itemsIndexed(state.lines) { index, line ->
                    TimelineItem(line, isLast = index == state.lines.lastIndex)
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(line: HistoryLineUi, isLast: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(14.dp)) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
                    .background(colors.primary, CircleShape),
            )
            if (!isLast) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(colors.outlineVariant),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 20.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(line.editor) }
                    append(" ")
                    append(line.text)
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = colors.onSurface,
            )
            Text(line.time, fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun MemberHistoryContentPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        MemberHistoryContent(
            state = MemberHistoryUiState(
                isLoading = false,
                lines = listOf(
                    HistoryLineUi("Pr. João", "alterou Situação de Visitante para Ativo", "25/09/2026 14:05"),
                    HistoryLineUi("Pr. João", "trocou a foto", "25/09/2026 14:03"),
                    HistoryLineUi("Usuário removido", "cadastrou o membro", "14/03/2026 10:12"),
                ),
            ),
            onRetry = {},
        )
    }
}
