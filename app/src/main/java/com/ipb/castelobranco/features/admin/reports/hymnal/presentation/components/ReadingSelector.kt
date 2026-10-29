package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.core.presentation.theme.onPrimaryContainerLight
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportReading

private const val ACTIVE_TAB_ALPHA = 0.16f
private const val INACTIVE_TAB_ALPHA = 0.8f

/**
 * Which reading of the slice is on screen, as tabs on the green bar (pass it as `BaseScreen.topBarExtension`).
 * Seven readings do not fit side by side, so the row scrolls. Changing it never fetches.
 */
@Composable
fun ReadingTabs(
    selected: ReportReading,
    onReadingSelected: (ReportReading) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ipbGreen)
            .horizontalScroll(rememberScrollState())
            .padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ReportReading.entries.forEach { reading ->
            val isSelected = reading == selected
            Text(
                text = reading.label(),
                color = Color.White.copy(alpha = if (isSelected) 1f else INACTIVE_TAB_ALPHA),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color.White.copy(alpha = ACTIVE_TAB_ALPHA) else Color.Transparent)
                    .drawBehind {
                        if (isSelected) {
                            val stroke = 2.dp.toPx()
                            drawRect(
                                color = onPrimaryContainerLight,
                                topLeft = Offset(0f, size.height - stroke),
                                size = Size(size.width, stroke),
                            )
                        }
                    }
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onReadingSelected(reading) })
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
    }
}

fun ReportReading.label(): String = when (this) {
    ReportReading.HIGHLIGHTS -> "Destaques"
    ReportReading.RANKING -> "Ranking"
    ReportReading.EVOLUTION -> "Evolução"
    ReportReading.SERVICES -> "Cultos"
    ReportReading.CALENDAR -> "Calendário"
    ReportReading.IN_VS_OUTSIDE -> "Culto × fora"
    ReportReading.COVERAGE -> "Cobertura"
}

@Preview(showBackground = true)
@Composable
private fun ReadingTabsPreview() {
    ReadingTabs(selected = ReportReading.HIGHLIGHTS, onReadingSelected = {})
}
