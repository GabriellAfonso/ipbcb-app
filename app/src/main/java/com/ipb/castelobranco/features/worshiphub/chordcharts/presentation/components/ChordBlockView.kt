package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordLine
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.LineToken
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SectionTag
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.chordColor
import kotlin.math.roundToInt

// A chord chart block as the chord chart screen and the new chord chart's preview draw it.

private const val INTRO_LABEL = "Intro"

/** One block: the intro's chords in a row, or a section's tag and its chord-over-lyric lines. */
@Composable
internal fun ChordBlockView(block: ChordBlock) {
    if (block.isIntro) IntroBlock(block) else SectionBlock(block)
}

@Composable
private fun IntroBlock(block: ChordBlock) {
    val chordColor = chordColor()
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionTag(INTRO_LABEL)
        Spacer(modifier = Modifier.width(8.dp))
        block.lines.forEach { line ->
            line.tokens.filterIsInstance<LineToken.Chord>().forEach { chord ->
                Text(
                    text       = chord.value,
                    color      = chordColor,
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun SectionBlock(block: ChordBlock) {
    if (block.title != null) SectionTag(block.title)
    Spacer(modifier = Modifier.height(8.dp))
    block.lines.forEach { line ->
        ChordLineRow(line = line)
        Spacer(modifier = Modifier.height(2.dp))
    }
}

private data class ChordLyricGroup(val chord: String?, val charOffset: Int = 0, val lyrics: String)

private fun groupTokens(tokens: List<LineToken>): List<ChordLyricGroup> {
    val groups = mutableListOf<ChordLyricGroup>()
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        when (token) {
            is LineToken.Chord -> {
                val lyrics = (tokens.getOrNull(i + 1) as? LineToken.Lyrics)?.value ?: ""
                groups += ChordLyricGroup(token.value, token.charOffset, lyrics)
                i += 2
            }
            is LineToken.Lyrics -> {
                groups += ChordLyricGroup(null, 0, token.value)
                i += 1
            }
        }
    }
    return groups
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChordLineRow(line: ChordLine) {
    val lyricColor = MaterialTheme.colorScheme.onSurface
    val chordColor = chordColor()
    val groups = groupTokens(line.tokens)
    FlowRow {
        groups.forEach { group ->
            ChordLyricGroupItem(group = group, lyricColor = lyricColor, chordColor = chordColor)
        }
    }
}

@Composable
private fun ChordLyricGroupItem(group: ChordLyricGroup, lyricColor: Color, chordColor: Color) {
    var chordOffsetXPx by remember(group.chord, group.charOffset, group.lyrics) {
        mutableFloatStateOf(0f)
    }

    val chordContent: @Composable () -> Unit = {
        Text(
            text       = group.chord ?: "",
            color      = chordColor,
            fontWeight = FontWeight.Bold,
            style      = MaterialTheme.typography.labelMedium,
            softWrap   = false,
            maxLines   = 1,
        )
    }

    val lyricsContent: @Composable () -> Unit = {
        Text(
            text         = group.lyrics,
            color        = lyricColor,
            style        = MaterialTheme.typography.bodyLarge,
            softWrap     = false,
            maxLines     = 1,
            onTextLayout = { result ->
                if (group.chord != null && group.lyrics.isNotEmpty()) {
                    val idx = group.charOffset.coerceIn(0, group.lyrics.length - 1)
                    chordOffsetXPx = result.getCursorRect(idx).left
                }
            },
        )
    }

    Layout(contents = listOf(chordContent, lyricsContent)) { measurables, _ ->
        val chordPlaceable  = measurables[0].firstOrNull()?.measure(Constraints())
        val lyricsPlaceable = measurables[1].firstOrNull()?.measure(Constraints())
            ?: return@Layout layout(0, 0) {}

        val chordH    = chordPlaceable?.height ?: 0
        val chordLeft = if (group.chord != null) chordOffsetXPx.roundToInt() else 0
        val chordEnd  = chordLeft + (chordPlaceable?.width ?: 0)

        layout(
            width  = maxOf(lyricsPlaceable.width, chordEnd),
            height = chordH + lyricsPlaceable.height,
        ) {
            chordPlaceable?.placeRelative(chordLeft, 0)
            lyricsPlaceable.placeRelative(0, chordH)
        }
    }
}
