package com.ipb.castelobranco.features.schedule.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Immutable
data class ScheduleRowUi(
    val day: Int,
    val member: String
)

@Immutable
data class ScheduleSectionUi(
    val title: String,
    val time: String,
    val rows: List<ScheduleRowUi>
)

/** Section shown on the home highlight; [nextDay] is the upcoming date, null when every date has passed. */
@Immutable
data class NextScheduleUi(
    val section: ScheduleSectionUi,
    val nextDay: Int?
)

@Composable
fun MonthScheduleTable(
    sections: List<ScheduleSectionUi>?,
    modifier: Modifier = Modifier
) {
    // Se for null, estamos no primeiro carregamento do cache.
    // Retornamos vazio para não mostrar o texto de "Sem escala" antes da hora.
    if (sections == null) return

    if (sections.isEmpty()) {
        EmptyScheduleState(modifier = modifier.fillMaxWidth())
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp) // reduzido de 12.dp
    ) {
        sections.forEach { section ->
            SectionCard(section = section)
        }
    }
}

@Composable
fun SectionCard(section: ScheduleSectionUi) {
    val container = MaterialTheme.colorScheme.surfaceContainerHighest
    val headerBg = MaterialTheme.colorScheme.surfaceDim
    val shape = RoundedCornerShape(8.dp) // reduzido de 12.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, shape)
            .padding(8.dp) // reduzido de 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (section.time.isNotBlank()) {
                Text(
                    text = section.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp)) // reduzido de 10.dp

        // Header da “tabela”
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg, RoundedCornerShape(8.dp)) // reduzido de 10.dp
                .padding(horizontal = 8.dp, vertical = 6.dp), // reduzido de 12/10
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dia",
                modifier = Modifier.weight(0.25f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Responsável",
                modifier = Modifier.weight(0.75f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp)) // reduzido de 6.dp

        section.rows
            .sortedBy { it.day }
            .forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp), // reduzido de 12/10
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "%02d", row.day),
                        modifier = Modifier.weight(0.25f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = row.member,
                        modifier = Modifier.weight(0.75f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (index != section.rows.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                }
            }
    }
}

@Composable
private fun EmptyScheduleState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp), // reduzido de 16.dp
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Sem escala disponível.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

    }
}