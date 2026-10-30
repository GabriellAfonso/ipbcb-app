package com.ipb.castelobranco.features.admin.members.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.members.presentation.state.HistoryDayUi
import com.ipb.castelobranco.features.admin.members.presentation.state.HistoryLineUi
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberHistoryUiState
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.viewmodel.MemberHistoryViewModel

private val SheetRadius = 18.dp
private const val BAND_SECONDARY_ALPHA = 0.82f
private const val DIVIDER_ALPHA = 0.6f

@Composable
fun MemberHistoryScreen(
    viewModel: MemberHistoryViewModel,
    onBack: () -> Unit,
    onNavigationEvent: (MembersEvent) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.events.collect(onNavigationEvent) }

    BaseScreen(
        tabName = "Histórico",
        showBackArrow = true,
        onBackClick = onBack,
        // The green behind the sheet's rounded corners continues the band.
        containerColor = ipbGreen,
        topBarExtension = { HistoryBand(state) },
    ) { innerPadding ->
        Box(
            Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            MemberHistoryContent(state = state, onRetry = viewModel::load)
        }
    }
}

/** The member's name (when known) and "N alterações desde …", inside the green top-bar block. */
@Composable
private fun HistoryBand(state: MemberHistoryUiState) {
    val summary = state.summary
    if (state.memberName == null && summary == null) return
    Column(
        Modifier
            .fillMaxWidth()
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 16.dp),
    ) {
        state.memberName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
        summary?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = BAND_SECONDARY_ALPHA),
            )
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
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                state.days.forEachIndexed { index, day ->
                    item(key = "day-$index") { DayCard(day) }
                }
            }
        }
    }
}

@Composable
private fun DayCard(day: HistoryDayUi) {
    Column {
        Text(
            text = day.day,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            day.lines.forEachIndexed { index, line ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
                }
                HistoryEntry(line)
            }
        }
    }
}

/** Who (initials), what, at what time, and for a field change its old → new value. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryEntry(line: HistoryLineUi) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (line.isEditorUnknown) colors.outlineVariant else colors.tertiary),
        ) {
            Text(
                text = line.initials,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (line.isEditorUnknown) colors.onSurface else colors.onTertiary,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(line.editor) }
                    append(" ")
                    append(line.action)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
            Text(line.hour, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            line.newValue?.let { newValue ->
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    line.oldValue?.let {
                        ValueTag(it, colors.surfaceContainerHighest, colors.onSurfaceVariant, struck = true)
                        Text("→", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    ValueTag(newValue, colors.secondaryContainer, colors.onSecondaryContainer, struck = false)
                }
            }
        }
    }
}

@Composable
private fun ValueTag(text: String, background: Color, content: Color, struck: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = content,
        textDecoration = if (struck) TextDecoration.LineThrough else null,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun MemberHistoryContentPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        MemberHistoryContent(
            state = MemberHistoryUiState(
                isLoading = false,
                lines = listOf(
                    HistoryLineUi(
                        editor = "Pr. João",
                        text = "alterou Situação de Visitante para Ativo",
                        time = "25/09/2026 14:05",
                        initials = "PJ",
                        action = "alterou Situação",
                        oldValue = "Visitante",
                        newValue = "Ativo",
                    ),
                    HistoryLineUi("Pr. João", "trocou a foto", "25/09/2026 14:03", initials = "PJ"),
                    HistoryLineUi(
                        "Usuário removido", "cadastrou o membro", "14/03/2026 10:12",
                        initials = "?", isEditorUnknown = true,
                    ),
                ),
            ),
            onRetry = {},
        )
    }
}
