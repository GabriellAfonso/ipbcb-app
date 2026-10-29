package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportSlice
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util.WeekdayLabels

private const val SHEET_TITLE = "O que ver"
private const val ALL_LABEL = "Todos os cultos"
private const val ALL_HINT = "Dentro e fora do culto"
private const val SERVICES_GROUP = "Cultos"
private const val OUTSIDE_GROUP = "Fora do culto"
private const val OUTSIDE_LABEL = "Fora do culto"
private const val OUTSIDE_HINT = "Hinos abertos fora dos horários de culto"
private const val WEEKDAY_GROUP = "Dia da semana"

/** The slice's name on the filter button. */
fun ReportSlice.label(): String = when (this) {
    ReportSlice.All -> ALL_LABEL
    ReportSlice.OutsideService -> OUTSIDE_LABEL
    is ReportSlice.Service -> name
    is ReportSlice.Weekday -> WeekdayLabels.full(day)
}

/**
 * Which occurrences of the loaded period are counted (§6.2). Changing it never fetches.
 *
 * Only **active** services are offered: a deactivated one would produce an empty slice that looks
 * like the congregation stopped singing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SliceSheet(
    selected: ReportSlice,
    services: List<ReportSlice.Service>,
    onSliceSelected: (ReportSlice) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = SHEET_TITLE,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            SliceOption(ALL_LABEL, ALL_HINT, selected == ReportSlice.All) { onSliceSelected(ReportSlice.All) }
            if (services.isNotEmpty()) {
                GroupTitle(SERVICES_GROUP)
                services.forEach { service ->
                    SliceOption(service.name, null, selected == service) { onSliceSelected(service) }
                }
            }
            GroupTitle(OUTSIDE_GROUP)
            SliceOption(OUTSIDE_LABEL, OUTSIDE_HINT, selected == ReportSlice.OutsideService) {
                onSliceSelected(ReportSlice.OutsideService)
            }
            GroupTitle(WEEKDAY_GROUP)
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                WeekdayLabels.CALENDAR_ORDER.forEach { day ->
                    val slice = ReportSlice.Weekday(day)
                    val isSelected = selected == slice
                    Text(
                        text = WeekdayLabels.short(day),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ipbGreen else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) ipbGreen else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { onSliceSelected(slice) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun SliceOption(label: String, hint: String?, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        hint?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
