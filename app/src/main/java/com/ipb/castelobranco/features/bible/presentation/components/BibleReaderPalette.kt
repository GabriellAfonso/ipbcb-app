package com.ipb.castelobranco.features.bible.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Near-black bar in both themes: it must read apart from the page, which a theme container color does not. */
internal val BibleToolbarBackground = Color(0xFF141414)
internal val BibleToolbarPillBackground = Color(0xFF2B2B2B)

/** Dark-theme page: a step lighter than the bar, so bar and text never blend. */
private val DarkReaderPage = Color(0xFF1E1E20)
private const val DARK_SURFACE_LUMINANCE = 0.5f

/**
 * Background of the reader's page. The light theme keeps its own surface.
 *
 * Example: `Scaffold(containerColor = bibleReaderPageColor()) { ... }`
 */
@Composable
fun bibleReaderPageColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    return if (surface.luminance() < DARK_SURFACE_LUMINANCE) DarkReaderPage else surface
}
