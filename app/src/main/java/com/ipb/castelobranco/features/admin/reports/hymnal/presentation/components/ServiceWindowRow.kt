package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ServiceWindow
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util.WeekdayLabels
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util.toHourMinute
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * One weekly service.
 *
 * The weekday shown here is already a `DayOfWeek`; the service's `0 = Monday` convention is
 * converted once, in the mapper, so nothing on screen can rotate the week.
 *
 * @param editable false below `owner` on `reports.hymnal_history`: the row is read-only, with no
 *   switch, edit or delete.
 */
@Composable
fun ServiceWindowRow(
    window: ServiceWindow,
    editable: Boolean,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (window.active) 1f else 0.5f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = window.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            Text(
                text = "${WeekdayLabels.full(window.weekday)} · " +
                    "${window.startTime.toHourMinute()} às ${window.endTime.toHourMinute()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * contentAlpha),
            )
            if (!window.active) {
                Text(
                    text = "Inativo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }

        if (editable) {
            Switch(checked = window.active, onCheckedChange = { onToggleActive() })

            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Editar culto")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Apagar culto")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ServiceWindowRowPreview() {
    ServiceWindowRow(
        window = ServiceWindow(
            id = 3,
            name = "Culto de Domingo à Noite",
            weekday = DayOfWeek.SUNDAY,
            startTime = LocalTime.of(19, 0),
            endTime = LocalTime.of(21, 0),
            active = true,
        ),
        editable = true,
        onEdit = {},
        onToggleActive = {},
        onDelete = {},
        modifier = Modifier.padding(16.dp),
    )
}
