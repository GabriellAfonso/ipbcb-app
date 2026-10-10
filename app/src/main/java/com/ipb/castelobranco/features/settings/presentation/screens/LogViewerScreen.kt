package com.ipb.castelobranco.features.settings.presentation.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.data.logging.LogBufferTree
import com.ipb.castelobranco.core.data.logging.LogEntry
import com.ipb.castelobranco.core.presentation.base.BaseScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    onBackClick: () -> Unit = {},
) {
    var entries by remember { mutableStateOf(LogBufferTree.snapshot()) }
    var minPriority by remember { mutableIntStateOf(Log.DEBUG) }
    val filtered = remember(entries, minPriority) {
        entries.filter { it.priority >= minPriority }
    }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(filtered.size) {
        if (filtered.isNotEmpty()) listState.animateScrollToItem(filtered.lastIndex)
    }

    BaseScreen(
        tabName = "Logs",
        logoRes = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick = onBackClick,
        extraActions = {
            IconButton(onClick = {
                val text = entries.joinToString("\n") { it.formatted() }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Compartilhar logs"))
            }) {
                Icon(Icons.Filled.Share, contentDescription = "Compartilhar")
            }
            IconButton(onClick = {
                LogBufferTree.clear()
                entries = emptyList()
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Limpar")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                LevelChip("D", Log.DEBUG, minPriority) { minPriority = it }
                Spacer(Modifier.width(6.dp))
                LevelChip("I", Log.INFO, minPriority) { minPriority = it }
                Spacer(Modifier.width(6.dp))
                LevelChip("W", Log.WARN, minPriority) { minPriority = it }
                Spacer(Modifier.width(6.dp))
                LevelChip("E", Log.ERROR, minPriority) { minPriority = it }
            }

            HorizontalDivider()

            if (filtered.isEmpty()) {
                Text(
                    text = "Nenhum log",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        LogRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelChip(
    label: String,
    level: Int,
    currentMin: Int,
    onSelect: (Int) -> Unit,
) {
    FilterChip(
        selected = currentMin == level,
        onClick = { onSelect(level) },
        label = { Text(label) },
    )
}

@Composable
private fun LogRow(entry: LogEntry) {
    val color = when (entry.priority) {
        Log.WARN -> Color(0xFFFFA000)
        Log.ERROR, Log.ASSERT -> MaterialTheme.colorScheme.error
        Log.INFO -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.onSurface
    }

    // One container per row: a selection spanning lazy items breaks once they scroll out of composition.
    SelectionContainer {
        Text(
            text = entry.formatted(),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
