package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.components.DateFieldWithPicker
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportPeriod
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util.toFullDate
import java.time.LocalDate

/**
 * The period switch and the slice button on one line, the range under them (§6.2). The period is the one selection
 * that costs a request; the slice only recomputes in memory, in its own sheet.
 */
@Composable
fun ReportFilters(
    selected: ReportPeriod,
    rangeLabel: String,
    rangeError: String?,
    sliceLabel: String,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onOpenSlices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(3.dp),
            ) {
                PRESETS.forEach { (period, label) ->
                    PeriodOption(label, selected == period) { onPeriodSelected(period) }
                }
                PeriodOption(CUSTOM_LABEL, selected is ReportPeriod.Custom) { showCustomDialog = true }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(percent = 50))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 50))
                    .clickable(onClick = onOpenSlices)
                    .padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = sliceLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (rangeLabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (rangeError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rangeError,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showCustomDialog) {
        CustomRangeDialog(
            initial = selected as? ReportPeriod.Custom,
            onDismiss = { showCustomDialog = false },
            onConfirm = { from, to ->
                showCustomDialog = false
                onPeriodSelected(ReportPeriod.Custom(from = from, to = to))
            },
        )
    }
}

@Composable
private fun PeriodOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) ipbGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * Two dates and nothing more. The range rules — start before end, at most a year — are checked in
 * the domain and reported on the selector, so an impossible range never becomes a request.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(
    initial: ReportPeriod.Custom?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    var from by remember { mutableStateOf(initial?.from) }
    var to by remember { mutableStateOf(initial?.to) }
    var picking by remember { mutableStateOf<PickTarget?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Período personalizado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Início", style = MaterialTheme.typography.labelMedium)
                DateFieldWithPicker(
                    dateBr = from?.toFullDate().orEmpty(),
                    onOpenPicker = { picking = PickTarget.FROM },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Fim", style = MaterialTheme.typography.labelMedium)
                DateFieldWithPicker(
                    dateBr = to?.toFullDate().orEmpty(),
                    onOpenPicker = { picking = PickTarget.TO },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = from != null && to != null,
                onClick = {
                    val start = from ?: return@TextButton
                    val end = to ?: return@TextButton
                    onConfirm(start, end)
                },
            ) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    picking?.let { target ->
        ReportDatePickerDialog(
            initialDate = if (target == PickTarget.FROM) from else to,
            onDismiss = { picking = null },
            onConfirm = { date ->
                if (target == PickTarget.FROM) from = date else to = date
                picking = null
            },
        )
    }
}

private enum class PickTarget { FROM, TO }

private val PRESETS: List<Pair<ReportPeriod, String>> = listOf(
    ReportPeriod.ThisWeek to "Semana",
    ReportPeriod.ThisMonth to "Mês",
    ReportPeriod.ThisYear to "Ano",
)

private const val CUSTOM_LABEL = "Outro"

@Preview(showBackground = true)
@Composable
private fun ReportFiltersPreview() {
    ReportFilters(
        selected = ReportPeriod.ThisMonth,
        rangeLabel = "01/08/2026 – 16/08/2026",
        rangeError = null,
        sliceLabel = "Todos os cultos",
        onPeriodSelected = {},
        onOpenSlices = {},
        modifier = Modifier.padding(vertical = 12.dp),
    )
}
