package com.ipb.castelobranco.features.studies.presentation.components.sundayschool

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.ipb.castelobranco.core.presentation.theme.provider
import androidx.compose.ui.text.font.Font as DeviceFont

/**
 * The aged-parchment look of the EBD reader (`specs/020-sunday-school/research.md` R-07): plain cream page behind
 * the text, parchment only on the lesson header and key verses, very dark brown instead of black in dark mode.
 */
@Immutable
data class SundaySchoolColors(
    val page: Color,
    val text: Color,
    val muted: Color,
    val gold: Color,
    val parchment: Color,
    val parchmentEdge: Color,
    val rule: Color,
    val highlight: Color,
)

private val LightColors = SundaySchoolColors(
    page = Color(0xFFFBF6EA),
    text = Color(0xFF2B2118),
    muted = Color(0xFF6B5A45),
    gold = Color(0xFFA8781E),
    parchment = Color(0xFFF1E4C6),
    parchmentEdge = Color(0xFFE3CFA3),
    rule = Color(0xFFE2D3B3),
    highlight = Color(0xFFF6EBD0),
)

private val DarkColors = SundaySchoolColors(
    page = Color(0xFF1C1611),
    text = Color(0xFFEDE3D0),
    muted = Color(0xFFB8A88E),
    gold = Color(0xFFD9AE55),
    parchment = Color(0xFF2E241A),
    parchmentEdge = Color(0xFF241C14),
    rule = Color(0xFF3D3124),
    highlight = Color(0xFF33281C),
)

/** Follows the app's own light/dark choice, which is what the Material color scheme reflects. */
val sundaySchoolColors: SundaySchoolColors
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE) DarkColors else LightColors

private const val DARK_LUMINANCE = 0.5f

private val serifFallback = DeviceFont(DeviceFontFamilyName("serif"))

/** Titles: Cormorant Garamond, downloaded once by Play services; the system serif until then. */
val SundaySchoolTitleFont = FontFamily(
    Font(googleFont = GoogleFont("Cormorant Garamond"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Cormorant Garamond"), fontProvider = provider, weight = FontWeight.Bold),
    serifFallback,
)

/** Running text: Literata. */
val SundaySchoolTextFont = FontFamily(
    Font(googleFont = GoogleFont("Literata"), fontProvider = provider, weight = FontWeight.Normal),
    Font(
        googleFont = GoogleFont("Literata"),
        fontProvider = provider,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
    ),
    Font(googleFont = GoogleFont("Literata"), fontProvider = provider, weight = FontWeight.SemiBold),
    serifFallback,
)

/**
 * Parchment: a warm wash with a soft darker vignette and a few faint fibres, drawn behind the lesson header and key
 * verses only — never behind running text.
 */
fun Modifier.parchment(colors: SundaySchoolColors): Modifier = drawWithCache {
    val vignette = Brush.radialGradient(
        colors = listOf(Color.Transparent, colors.parchmentEdge),
        center = Offset(size.width / 2, size.height / 2),
        radius = size.maxDimension * VIGNETTE_RADIUS,
    )
    val fibre = colors.parchmentEdge.copy(alpha = FIBRE_ALPHA)
    onDrawBehind {
        drawRect(colors.parchment)
        drawRect(vignette)
        FIBRES.forEach { (y, x0, x1) ->
            drawLine(
                color = fibre,
                start = Offset(size.width * x0, size.height * y),
                end = Offset(size.width * x1, size.height * y + FIBRE_SLANT),
                strokeWidth = FIBRE_WIDTH,
            )
        }
    }
}

private const val VIGNETTE_RADIUS = 0.75f
private const val FIBRE_ALPHA = 0.35f
private const val FIBRE_WIDTH = 1.2f
private const val FIBRE_SLANT = 3f

/** Fixed, so the texture does not change between frames: (y, x start, x end) as fractions of the size. */
private val FIBRES = listOf(
    Triple(0.18f, 0.05f, 0.42f),
    Triple(0.37f, 0.55f, 0.93f),
    Triple(0.61f, 0.12f, 0.33f),
    Triple(0.79f, 0.48f, 0.86f),
)
