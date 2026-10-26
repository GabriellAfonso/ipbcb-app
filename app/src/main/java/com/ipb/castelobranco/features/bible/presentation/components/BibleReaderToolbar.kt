package com.ipb.castelobranco.features.bible.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation

@Composable
fun BibleReaderToolbar(
    bookName: String,
    chapter: Int,
    activeTranslation: BibleTranslation,
    cachedTranslations: Set<BibleTranslation>,
    onBack: () -> Unit,
    onBookClick: () -> Unit,
    onChapterClick: () -> Unit,
    onTranslationSelected: (BibleTranslation) -> Unit,
    onFontSizeClick: () -> Unit,
) {
    // Fixed near-black in both themes so the bar reads apart from the text (a theme container color turns lighter
    // than the page in dark mode); dark gray pills with white bold text on top.
    Surface(
        color = BibleToolbarBackground,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            ToolbarPill(text = bookName, onClick = onBookClick)
            ToolbarPill(text = chapter.toString(), onClick = onChapterClick)
            TranslationPill(
                active = activeTranslation,
                cached = cachedTranslations,
                onSelected = onTranslationSelected,
            )
            Box(modifier = Modifier.weight(1f))
            IconButton(onClick = onFontSizeClick) {
                Icon(Icons.Filled.FormatSize, contentDescription = "Tamanho da fonte")
            }
        }
    }
}

@Composable
private fun ToolbarPill(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        color = BibleToolbarPillBackground,
        shape = RoundedCornerShape(14.dp),
        onClick = onClick,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
@Composable
private fun TranslationPill(
    active: BibleTranslation,
    cached: Set<BibleTranslation>,
    onSelected: (BibleTranslation) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val available = remember(cached) {
        BibleTranslation.entries.filter { it in cached }
            .ifEmpty { listOf(active) }
    }

    Box {
        ToolbarPill(text = active.displayName, onClick = { if (available.size > 1) expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            available.forEach { translation ->
                DropdownMenuItem(
                    text = { Text(translation.displayName) },
                    onClick = {
                        expanded = false
                        if (translation != active) onSelected(translation)
                    },
                )
            }
        }
    }
}
