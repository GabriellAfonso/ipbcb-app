package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.Highlight
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.HighlightDelta
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.HymnRanking
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportPeriod
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportReading
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportSlice
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components.BulletinCard
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components.CalendarGrid
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components.EmptyReading
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components.HorizontalBarChart
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components.PeriodSelector
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components.ReadingSelector
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components.SliceSelector
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components.VerticalBarChart
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.state.HymnalReportEvent
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.state.HymnalReportUiState
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util.toFullDate
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.viewmodel.HymnalReportViewModel
import java.time.LocalDate

/**
 * One period, seven readings. The screen renders what the domain produced and computes nothing:
 * no proportion, no delta, no temporal sentence is assembled here.
 */
@Composable
fun HymnalReportScreen(
    viewModel: HymnalReportViewModel,
    onBack: () -> Unit,
    onOpenServiceWindows: () -> Unit,
    onOpenCollectionSettings: () -> Unit,
    onOpenHymnCard: (String) -> Unit,
    onLeaveArea: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is HymnalReportEvent.LeaveArea) onLeaveArea(event.message)
        }
    }

    BaseScreen(
        tabName = "Histórico do hinário",
        logoRes = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick = onBack,
        extraActions = {
            ReportOverflowMenu(
                onOpenServiceWindows = onOpenServiceWindows,
                // Configuring collection is `owner` on the report; below that the entry does not exist.
                onOpenCollectionSettings = onOpenCollectionSettings.takeIf { state.canOpenSettings },
            )
        },
    ) { innerPadding ->
        HymnalReportContent(
            state = state,
            innerPadding = innerPadding,
            onPeriodSelected = viewModel::onPeriodSelected,
            onSliceSelected = viewModel::onSliceSelected,
            onReadingSelected = viewModel::onReadingSelected,
            onRankingExpandToggled = viewModel::onRankingExpandToggled,
            onDaySelected = viewModel::onDaySelected,
            onRetry = viewModel::onRetry,
            onHymnClick = { number ->
                viewModel.onHymnSelected(number)
                onOpenHymnCard(number)
            },
        )
    }
}

@Composable
private fun ReportOverflowMenu(
    onOpenServiceWindows: () -> Unit,
    onOpenCollectionSettings: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Administração da coleta",
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Janelas de culto") },
            onClick = {
                expanded = false
                onOpenServiceWindows()
            },
        )
        if (onOpenCollectionSettings != null) {
            DropdownMenuItem(
                text = { Text("Parâmetros de coleta") },
                onClick = {
                    expanded = false
                    onOpenCollectionSettings()
                },
            )
        }
    }
}

@Composable
fun HymnalReportContent(
    state: HymnalReportUiState,
    innerPadding: PaddingValues,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onSliceSelected: (ReportSlice) -> Unit,
    onReadingSelected: (ReportReading) -> Unit,
    onRankingExpandToggled: () -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onRetry: () -> Unit,
    onHymnClick: (String) -> Unit,
) {
    // Coverage covers all recorded history, so the period and slice selectors do not apply to it
    // and are hidden rather than left on screen doing nothing.
    val periodApplies = state.reading != ReportReading.COVERAGE

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            ReadingSelector(selected = state.reading, onReadingSelected = onReadingSelected)
        }

        if (periodApplies) {
            item {
                PeriodSelector(
                    selected = state.period,
                    rangeLabel = state.rangeLabel,
                    rangeError = state.rangeError,
                    onPeriodSelected = onPeriodSelected,
                )
            }
            item {
                SliceSelector(
                    selected = state.slice,
                    services = state.availableServices,
                    onSliceSelected = onSliceSelected,
                )
            }
        }

        when {
            periodApplies && state.isLoading -> item { LoadingBlock() }
            periodApplies && state.error != null -> item {
                ErrorBlock(message = state.error, onRetry = onRetry)
            }

            else -> readingBody(
                state = state,
                onRankingExpandToggled = onRankingExpandToggled,
                onDaySelected = onDaySelected,
                onHymnClick = onHymnClick,
            )
        }
    }
}

private fun LazyListScope.readingBody(
    state: HymnalReportUiState,
    onRankingExpandToggled: () -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onHymnClick: (String) -> Unit,
) {
    val emptyReason = state.emptyReason
    if (state.reading != ReportReading.COVERAGE && emptyReason != null) {
        item { EmptyReading(reason = emptyReason, modifier = Modifier.padding(16.dp)) }
        return
    }

    when (state.reading) {
        ReportReading.HIGHLIGHTS -> items(state.highlights) { highlight ->
            HighlightCard(highlight = highlight, modifier = Modifier.padding(horizontal = 16.dp))
        }

        ReportReading.RANKING -> {
            val ranking = state.ranking ?: return
            item {
                HorizontalBarChart(
                    bars = ranking.bars,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onBarClick = { bar -> bar.hymnNumber?.let(onHymnClick) },
                )
            }
            if (ranking.isCapped || state.rankingExpanded) {
                item {
                    TextButton(
                        onClick = onRankingExpandToggled,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Text(
                            if (state.rankingExpanded) "Mostrar menos"
                            else "Ver todos (${ranking.totalHymns})"
                        )
                    }
                }
            }
        }

        ReportReading.EVOLUTION -> item {
            state.evolution?.let {
                VerticalBarChart(bars = it.bars, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        ReportReading.SERVICES -> {
            val focusedDay = state.focusedDay
            val bulletins = focusedDay
                ?.let { day -> state.bulletins.filter { it.date == day } }
                ?: state.bulletins

            if (focusedDay != null) {
                item {
                    Text(
                        text = "Boletim de ${focusedDay.toFullDate()}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(bulletins) { bulletin ->
                BulletinCard(
                    bulletin = bulletin,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onHymnClick = { hymn -> onHymnClick(hymn.number) },
                )
            }
        }

        ReportReading.CALENDAR -> items(state.calendar) { month ->
            CalendarGrid(
                month = month,
                modifier = Modifier.padding(horizontal = 16.dp),
                onDayClick = onDaySelected,
            )
        }

        ReportReading.IN_VS_OUTSIDE -> {
            val comparison = state.inVsOutside ?: return
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SideHeading("No culto", comparison.onlyInService.size)
                    RankingOrEmpty(
                        ranking = comparison.inService,
                        emptyMessage = "Nenhum hino aberto em culto neste período.",
                        onHymnClick = onHymnClick,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    SideHeading("Fora do culto", comparison.onlyOutsideService.size)
                    RankingOrEmpty(
                        ranking = comparison.outsideService,
                        emptyMessage = "Nenhum hino aberto fora de culto neste período.",
                        onHymnClick = onHymnClick,
                    )
                }
            }
        }

        ReportReading.COVERAGE -> coverageBody(state, onHymnClick)
    }
}

private fun LazyListScope.coverageBody(
    state: HymnalReportUiState,
    onHymnClick: (String) -> Unit,
) {
    val coverageError = state.coverageError
    val coverageEmptyReason = state.coverageEmptyReason
    val coverage = state.coverage

    when {
        state.isCoverageLoading -> item { LoadingBlock() }
        coverageError != null -> item { ErrorBlock(message = coverageError, onRetry = null) }

        coverageEmptyReason != null -> item {
            EmptyReading(reason = coverageEmptyReason, modifier = Modifier.padding(16.dp))
        }

        coverage != null -> {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Todo o histórico",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandColors.Green,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = coverage.proportionText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            item { SectionHeading("Nunca cantados (${coverage.neverSung.size})") }
            items(coverage.neverSung) { hymn ->
                SimpleHymnRow(
                    title = "${hymn.number} · ${hymn.title}",
                    subtitle = null,
                    onClick = { onHymnClick(hymn.number) },
                )
            }

            item { SectionHeading("Hinos esquecidos") }
            items(coverage.forgotten) { hymn ->
                SimpleHymnRow(
                    title = "${hymn.number} · ${hymn.title}",
                    subtitle = hymn.text,
                    onClick = { onHymnClick(hymn.number) },
                )
            }
        }
    }
}

@Composable
private fun SideHeading(title: String, exclusiveCount: Int) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = when (exclusiveCount) {
                0 -> "Nenhum hino exclusivo deste lado"
                1 -> "1 hino só deste lado"
                else -> "$exclusiveCount hinos só deste lado"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun RankingOrEmpty(
    ranking: HymnRanking,
    emptyMessage: String,
    onHymnClick: (String) -> Unit,
) {
    if (ranking.bars.isEmpty()) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    } else {
        HorizontalBarChart(
            bars = ranking.bars,
            modifier = Modifier.padding(horizontal = 16.dp),
            onBarClick = { bar -> bar.hymnNumber?.let(onHymnClick) },
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun SimpleHymnRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun HighlightCard(highlight: Highlight, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = highlight.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = highlight.value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        highlight.delta?.let { delta ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = delta.text,
                style = MaterialTheme.typography.labelSmall,
                color = delta.direction.color(),
            )
        }
    }
}

@Composable
private fun HighlightDelta.Direction.color(): Color = when (this) {
    HighlightDelta.Direction.UP -> BrandColors.Green
    HighlightDelta.Direction.DOWN -> MaterialTheme.colorScheme.error
    HighlightDelta.Direction.FLAT -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HighlightCardPreview() {
    Row(modifier = Modifier.padding(16.dp)) {
        HighlightCard(
            highlight = Highlight(
                label = "Hino mais cantado",
                value = "50 · Grandioso És Tu",
                delta = HighlightDelta(
                    direction = HighlightDelta.Direction.UP,
                    text = "+2 vezes em relação ao período anterior",
                ),
            )
        )
    }
}
