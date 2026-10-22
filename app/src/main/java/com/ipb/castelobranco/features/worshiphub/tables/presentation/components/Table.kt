package com.ipb.castelobranco.features.worshiphub.tables.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


enum class ColumnAlignment { Start, Center, End }

data class TableColumn(
    val title: String,
    val weight: Float,
    val alignment: ColumnAlignment = ColumnAlignment.Start
)

@Composable
fun Header(columns: List<TableColumn>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceDim)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        columns.forEach { column ->
            Box(
                modifier = Modifier.weight(column.weight),
                contentAlignment = when (column.alignment) {
                    ColumnAlignment.Start -> Alignment.CenterStart
                    ColumnAlignment.Center -> Alignment.Center
                    ColumnAlignment.End -> Alignment.CenterEnd
                }
            ) {
                Text(
                    text = column.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}