package com.ipb.castelobranco.features.studies.presentation.screens

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.core.presentation.theme.ipbGreen

private const val TITLE = "Estudos"
private const val BAND_TITLE = "Aprenda e aprofunde sua fé"
private const val BAND_TEXT = "Material da igreja: doutrina, aulas da EBD e mais."
private const val SOON_LABEL = "EM BREVE"

private const val MUTED_ALPHA = 0.75f
private const val TEXT_ALPHA = 0.85f
private const val SOON_ALPHA = 0.6f
private const val SHADOW_AMBIENT_ALPHA = 0.08f
private const val SHADOW_SPOT_ALPHA = 0.12f

private val BandCorner = 28.dp
private val TileOverlap = 24.dp
private val TileHeight = 104.dp
private val TileCorner = 18.dp
private val TileGap = 12.dp
private val TileElevation = 6.dp
private val TileCircle = 44.dp
private val TileIcon = 24.dp
private val SidePadding = 16.dp

/** One card of the grid; [onClick] null = not built yet ("EM BREVE"). */
private data class StudiesTile(val icon: ImageVector, val label: String, val onClick: (() -> Unit)?)

@Composable
fun StudiesScreen(
    onBackClick: () -> Unit,
    onOpenDoctrine: () -> Unit,
    onOpenSundaySchool: () -> Unit,
) {
    BaseScreen(
        tabName = TITLE,
        showBackArrow = true,
        onBackClick = onBackClick,
    ) { innerPadding ->
        StudiesContent(
            onOpenDoctrine = onOpenDoctrine,
            onOpenSundaySchool = onOpenSundaySchool,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/** Green band and a 2x2 grid over it, like the worship hub (`specs/studies/spec.md` §1.1). */
@Composable
fun StudiesContent(
    onOpenDoctrine: () -> Unit,
    onOpenSundaySchool: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = listOf(
        StudiesTile(Icons.AutoMirrored.Filled.MenuBook, "Doutrina", onOpenDoctrine),
        StudiesTile(Icons.Filled.School, "EBD", onOpenSundaySchool),
        StudiesTile(Icons.Filled.Person, "Personagens Bíblicos", null),
        StudiesTile(Icons.Filled.History, "História do Cristianismo", null),
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        StudiesBand()
        Column(
            modifier = Modifier
                .offset(y = -TileOverlap)
                .fillMaxWidth()
                .padding(horizontal = SidePadding),
            verticalArrangement = Arrangement.spacedBy(TileGap),
        ) {
            tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                    row.forEach { tile -> StudiesTileCard(tile, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun StudiesBand() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = TileOverlap + 18.dp),
    ) {
        Text(TITLE, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = MUTED_ALPHA))
        Text(
            text = BAND_TITLE,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(BAND_TEXT, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = TEXT_ALPHA))
    }
}

@Composable
private fun StudiesTileCard(tile: StudiesTile, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val shape = RoundedCornerShape(TileCorner)
    val onClick = tile.onClick
    val isSoon = onClick == null
    Box(
        modifier = modifier
            .height(TileHeight)
            .shadow(
                elevation = TileElevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = SHADOW_AMBIENT_ALPHA),
                spotColor = Color.Black.copy(alpha = SHADOW_SPOT_ALPHA),
            )
            .clip(shape)
            .background(highlightCardColor())
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        onClick()
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isSoon) SOON_ALPHA else 1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(TileCircle)
                    .clip(CircleShape)
                    .background(
                        if (isSoon) MaterialTheme.colorScheme.outlineVariant
                        else MaterialTheme.colorScheme.primaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tile.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(TileIcon))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        if (isSoon) {
            Text(
                text = SOON_LABEL,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudiesContentPreview() {
    IPBCasteloBrancoTheme { StudiesContent(onOpenDoctrine = {}, onOpenSundaySchool = {}) }
}
