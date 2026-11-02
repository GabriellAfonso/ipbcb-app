package com.ipb.castelobranco.core.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ipb.castelobranco.core.presentation.theme.BrandColors

/** The green that marks what stands out in a list; it must read on `surfaceContainer` in both themes. */
@Immutable
data class NextRowColors(val background: Color, val stripe: Color, val text: Color)

private val NextRowLight = NextRowColors(Color(0xFFC4DFD3), BrandColors.Green, Color(0xFF0B5A4C))
private val NextRowDark = NextRowColors(Color(0xFF1D3A32), Color(0xFF5FB59C), Color(0xFF8ED1BA))
private const val DARK_SURFACE_LUMINANCE = 0.5f

@Composable
@ReadOnlyComposable
fun nextRowColors(): NextRowColors =
    if (MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE) NextRowDark else NextRowLight
