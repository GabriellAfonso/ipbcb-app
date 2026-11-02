package com.ipb.castelobranco.features.studies.presentation.components.sundayschool

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.ipb.castelobranco.core.presentation.components.nextRowColors

/**
 * The roles colors play in the EBD list and reader (`specs/020-sunday-school/research.md` R-07). Every value comes
 * from the app's theme, so the EBD looks like Tabelas and Escala in light and dark.
 */
@Immutable
data class SundaySchoolColors(
    val sheet: Color,
    val page: Color,
    val card: Color,
    val text: Color,
    val muted: Color,
    val overline: Color,
    val label: Color,
    val accent: Color,
    val rule: Color,
    val highlight: Color,
    val highlightLabel: Color,
    val tableHeader: Color,
    val iconBadge: Color,
)

val sundaySchoolColors: SundaySchoolColors
    @Composable
    @ReadOnlyComposable
    get() {
        val scheme = MaterialTheme.colorScheme
        val next = nextRowColors()
        return SundaySchoolColors(
            sheet = scheme.surfaceDim,
            page = scheme.surface,
            card = scheme.surfaceContainer,
            text = scheme.onSurface,
            muted = scheme.onSurfaceVariant,
            overline = scheme.outline,
            label = scheme.primary,
            accent = next.stripe,
            rule = scheme.outlineVariant,
            highlight = next.background,
            highlightLabel = next.text,
            tableHeader = scheme.surfaceContainerHigh,
            iconBadge = scheme.secondaryContainer,
        )
    }
