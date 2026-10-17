package com.ipb.castelobranco.features.hymnal.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.features.hymnal.domain.model.Hymn
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyric
import com.ipb.castelobranco.features.hymnal.domain.model.HymnLyricType
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.hymnal.presentation.viewmodel.HymnViewTrackingViewModel
import com.ipb.castelobranco.features.hymnal.presentation.viewmodel.HymnalViewModel
import kotlin.math.roundToInt
import com.ipb.castelobranco.core.presentation.components.KeepScreenOn

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
    val headerGreen = BrandColors.Green

    val minFont = 16f
    val maxFont = 32f
    val step = 1f

    var showFontBar by remember { mutableStateOf(false) }

    BaseScreen(
        tabName = "Hinário",
        showBackArrow = true,
        onBackClick = onBack,
        containerColor = MaterialTheme.colorScheme.surfaceDim,
        extraActions = {
            IconButton(onClick = { showFontBar = !showFontBar }) {
                Icon(
                    imageVector = Icons.Filled.FormatSize,
                    contentDescription = "Tamanho da fonte",
                    tint = if (showFontBar) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                           else androidx.compose.ui.graphics.Color.White,
                )
            }
        },
        topBarExtension = {
            AnimatedVisibility(visible = showFontBar) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { onFontSizeChange((fontSizeSp - step).coerceIn(minFont, maxFont)) }
                        ) {
                            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Diminuir fonte")
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
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            )
                            val thumbColor = MaterialTheme.colorScheme.primary
                            Canvas(modifier = Modifier.matchParentSize().padding(horizontal = 12.dp)) {
                                val fraction = (fontSizeSp - minFont) / (maxFont - minFont)
                                drawCircle(
                                    color = thumbColor,
                                    radius = 14.dp.toPx(),
                                    center = Offset(size.width * fraction, size.height / 2),
                                )
                            }
                        }
                        IconButton(
                            onClick = { onFontSizeChange((fontSizeSp + step).coerceIn(minFont, maxFont)) }
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Aumentar fonte")
                        }
                        Text(
                            text = "${fontSizeSp.roundToInt()}sp",
                            modifier = Modifier.padding(end = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (hymn == null) {
            Text(
                text = "Hino não encontrado",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                color = headerGreen,
                style = MaterialTheme.typography.titleMedium
            )
            return@BaseScreen
        }

        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "${hymn.number} \u2022 ${hymn.title}",
                    color = headerGreen,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.headlineSmall
                )

                hymn.lyrics.forEach { lyric ->
                    LyricCard(
                        lyric = lyric,
                        fontSizeSp = fontSizeSp
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricCard(
    lyric: HymnLyric,
    fontSizeSp: Float
) {
    val container = MaterialTheme.colorScheme.surfaceBright
    val stripe = when (lyric.type) {
        HymnLyricType.VERSE -> BrandColors.Orange
        HymnLyricType.CHORUS -> BrandColors.Green
        HymnLyricType.OTHER -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Spacer(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(stripe, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = lyric.text.trim(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp + 10f).sp,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}