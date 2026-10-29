package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.HymnProfile
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.HymnRecurrence
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ServiceShare
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util.toFullDate
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.viewmodel.HymnalReportViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Where every list in this feature leads.
 *
 * Each fact is labelled with the history it covers: the total comes from all recorded history,
 * the dates and the recurrence from the last year — the furthest back the app can see.
 */
@Composable
fun HymnCardScreen(
    viewModel: HymnalReportViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BaseScreen(
        tabName = "Ficha do hino",
        logoRes = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick = onBack,
    ) { innerPadding ->
        when {
            state.isHymnProfileLoading -> LoadingScreen(innerPadding)
            state.hymnProfileError != null -> ErrorScreen(state.hymnProfileError, innerPadding)
            state.hymnProfile != null -> HymnCardContent(
                profile = requireNotNull(state.hymnProfile),
                innerPadding = innerPadding,
            )
        }
    }
}

/** Band with the number, three figures over it, where it shows up and its pattern (§6.4). */
@Composable
fun HymnCardContent(
    profile: HymnProfile,
    innerPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        HymnBand(profile)
        if (profile.neverRecorded) {
            CardBox(Modifier.padding(16.dp)) {
                Text(
                    text = "Este hino nunca foi aberto por ninguém desde o início da coleta.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            return@Column
        }
        Row(
            modifier = Modifier
                .offset(y = -STATS_OVERLAP.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                value = profile.allTimeCount.toString(),
                label = if (profile.allTimeCount == 1) "vez em todo o histórico" else "vezes em todo o histórico",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = profile.lastSeen?.format(DAY_MONTH) ?: NO_VALUE,
                label = "última vez",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = profile.typicalReach.toString(),
                label = "aparelhos de alcance típico",
                modifier = Modifier.weight(1f),
            )
        }
        Column(
            modifier = Modifier.offset(y = -STATS_OVERLAP.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (profile.services.isNotEmpty()) {
                SectionTitle("Onde costuma aparecer")
                CardBox(Modifier.padding(horizontal = 16.dp)) {
                    profile.services.forEachIndexed { index, share ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ServiceShareRow(share)
                    }
                }
            }
            profile.recurrence?.let {
                SectionTitle("Padrão")
                RecurrenceCard(it)
            }
            profile.firstSeen?.let {
                Text(
                    text = "Primeira vez em ${it.toFullDate()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun HymnBand(profile: HymnProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BAND_CORNER.dp, bottomEnd = BAND_CORNER.dp))
            .background(ipbGreen)
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = if (profile.neverRecorded) 20.dp else (STATS_OVERLAP + 16).dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = profile.number,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Hino", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = MUTED_ALPHA))
            Text(
                text = profile.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    CardBox(modifier, padding = 12.dp) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** A white card. */
@Composable
private fun CardBox(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(CARD_CORNER.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(CARD_ELEVATION.dp, shape, spotColor = Color.Black.copy(alpha = SHADOW_ALPHA))
            .clip(shape)
            .background(highlightCardColor())
            .padding(padding),
        content = content,
    )
}

/** The recurrence in a mint card: the sentence and one mark per instance. */
@Composable
private fun RecurrenceCard(recurrence: HymnRecurrence) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(CARD_CORNER.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp),
    ) {
        Text(
            text = recurrence.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            recurrence.marks.forEach { sung ->
                Box(
                    modifier = Modifier
                        .size(MARK_SIZE_DP.dp)
                        .clip(CircleShape)
                        .background(if (sung) ipbGreen else MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }
        }
    }
}

@Composable
private fun ServiceShareRow(share: ServiceShare) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = share.serviceName ?: "Fora de culto",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "${share.occurrenceCount}×",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LoadingScreen(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(message: String?, innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
    }
}

private const val MARK_SIZE_DP = 12
private const val BAND_CORNER = 24
private const val STATS_OVERLAP = 28
private const val CARD_CORNER = 16
private const val CARD_ELEVATION = 6
private const val SHADOW_ALPHA = 0.12f
private const val MUTED_ALPHA = 0.75f
private const val NO_VALUE = "—"
private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

@Preview(showBackground = true)
@Composable
private fun HymnCardContentPreview() {
    HymnCardContent(
        profile = HymnProfile(
            number = "50",
            title = "Grandioso És Tu",
            allTimeCount = 42,
            firstSeen = LocalDate.of(2025, 9, 7),
            lastSeen = LocalDate.of(2026, 8, 16),
            services = listOf(ServiceShare("Culto de Domingo à Noite", 12)),
            typicalReach = 24,
            recurrence = HymnRecurrence(
                serviceName = "Culto de Domingo à Noite",
                text = "Cantado em 6 dos últimos 8 registros de \"Culto de Domingo à Noite\"",
                marks = listOf(true, true, false, true, true, false, true, true),
            ),
            neverRecorded = false,
        ),
        innerPadding = PaddingValues(0.dp),
    )
}
