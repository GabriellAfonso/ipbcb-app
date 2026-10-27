package com.ipb.castelobranco.features.hymnal.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.KeepScreenOn
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.core.presentation.theme.onPrimaryContainerLight
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyric
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyricType
import com.ipb.castelobranco.features.hymnal.presentation.mapper.stanzaLabels
import com.ipb.castelobranco.features.hymnal.presentation.viewmodel.HymnViewTrackingViewModel
import com.ipb.castelobranco.features.hymnal.presentation.viewmodel.HymnalViewModel
import kotlin.math.roundToInt

private val SheetRadius = 18.dp
private const val DARK_SURFACE_LUMINANCE = 0.5f
private const val TRACK_ALPHA = 0.25f
private const val DIM_ICON_ALPHA = 0.6f
private const val LINE_HEIGHT_FACTOR = 1.5f

/** Chorus card colors; the chorus must read as different from a verse in both themes. */
private data class ChorusColors(val background: Color, val text: Color)

private val ChorusLight = ChorusColors(Color(0xFFDCEBE4), Color(0xFF0B5A4C))
private val ChorusDark = ChorusColors(Color(0xFF1D3A32), Color(0xFF8ED1BA))

@Composable
private fun chorusColors(): ChorusColors =
    if (MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE) ChorusDark else ChorusLight

@Composable
fun HymnDetailScreen(
    hymnId: String,
    viewModel: HymnalViewModel,
    trackingViewModel: HymnViewTrackingViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fontSizeSp by viewModel.hymnalFontSize.collectAsStateWithLifecycle()
    KeepScreenOn()

    val hymn = remember(hymnId, state.hymns) {
        state.hymns.firstOrNull { it.number == hymnId }
    }

    // Counts only while the hymn is genuinely in front of the member. RESUMED is left when the
    // app is backgrounded, when the screen turns off, and when another surface covers it —
    // STARTED would keep counting behind the lock screen on some devices.
    LifecycleResumeEffect(hymn?.id) {
        trackingViewModel.onHymnVisible(hymn?.id)
        onPauseOrDispose { trackingViewModel.onHymnHidden() }
    }

    HymnDetailContent(
        hymn = hymn,
        fontSizeSp = fontSizeSp,
        onFontSizeChange = viewModel::setHymnalFontSize,
        onBack = onBack
    )
}

@Composable
fun HymnDetailContent(
    hymn: Hymn?,
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    var showFontBar by remember { mutableStateOf(false) }
    val labels = remember(hymn) { hymn?.let { stanzaLabels(it.lyrics) }.orEmpty() }

    BaseScreen(
        tabName = "Hinário",
        showBackArrow = true,
        onBackClick = onBack,
        // The green behind the sheet's rounded corners continues the top bar.
        containerColor = ipbGreen,
        extraActions = {
            IconButton(onClick = { showFontBar = !showFontBar }) {
                Icon(
                    imageVector = Icons.Filled.FormatSize,
                    contentDescription = "Tamanho da fonte",
                    tint = if (showFontBar) Color.White.copy(alpha = DIM_ICON_ALPHA) else Color.White,
                )
            }
        },
        topBarExtension = {
            Column(modifier = Modifier.fillMaxWidth().background(ipbGreen)) {
                AnimatedVisibility(visible = showFontBar) {
                    FontSizeBar(fontSizeSp = fontSizeSp, onFontSizeChange = onFontSizeChange)
                }
                if (hymn != null) HymnHeader(hymn)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            if (hymn == null) {
                Text(
                    text = "Hino não encontrado",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium
                )
                return@Box
            }

            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    hymn.lyrics.forEachIndexed { index, lyric ->
                        StanzaCard(lyric = lyric, label = labels.getOrNull(index), fontSizeSp = fontSizeSp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HymnHeader(hymn: Hymn) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Hino ${hymn.number}",
            color = onPrimaryContainerLight,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
        Text(
            text = hymn.title,
            color = Color.White,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FontSizeBar(fontSizeSp: Float, onFontSizeChange: (Float) -> Unit) {
    val minFont = 16f
    val maxFont = 32f
    val step = 1f
    val thumbColor = onPrimaryContainerLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onFontSizeChange((fontSizeSp - step).coerceIn(minFont, maxFont)) }) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Diminuir fonte", tint = Color.White)
        }
        Box(
            modifier = Modifier.weight(1f).height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Slider(
                value = fontSizeSp,
                onValueChange = { onFontSizeChange(it) },
                valueRange = minFont..maxFont,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = thumbColor,
                    inactiveTrackColor = Color.White.copy(alpha = TRACK_ALPHA),
                ),
            )
            Canvas(modifier = Modifier.matchParentSize().padding(horizontal = 12.dp)) {
                val fraction = (fontSizeSp - minFont) / (maxFont - minFont)
                drawCircle(
                    color = thumbColor,
                    radius = 14.dp.toPx(),
                    center = Offset(size.width * fraction, size.height / 2),
                )
            }
        }
        IconButton(onClick = { onFontSizeChange((fontSizeSp + step).coerceIn(minFont, maxFont)) }) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Aumentar fonte", tint = Color.White)
        }
        Text(
            text = "${fontSizeSp.roundToInt()}sp",
            modifier = Modifier.padding(end = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun StanzaCard(
    lyric: HymnLyric,
    label: String?,
    fontSizeSp: Float
) {
    val isChorus = lyric.type == HymnLyricType.CHORUS
    val chorus = chorusColors()
    val background = if (isChorus) chorus.background else MaterialTheme.colorScheme.surfaceContainer
    val textColor = if (isChorus) chorus.text else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.9.sp,
                color = if (isChorus) chorus.text else MaterialTheme.colorScheme.outline,
            )
        }
        Text(
            text = lyric.text.trim(),
            color = textColor,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * LINE_HEIGHT_FACTOR).sp,
            fontStyle = if (isChorus) FontStyle.Italic else FontStyle.Normal,
        )
    }
}
