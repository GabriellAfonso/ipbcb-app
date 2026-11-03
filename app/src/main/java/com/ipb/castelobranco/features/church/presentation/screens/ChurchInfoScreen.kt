package com.ipb.castelobranco.features.church.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.components.openLink
import com.ipb.castelobranco.core.presentation.components.openMap
import com.ipb.castelobranco.features.church.presentation.state.ChurchInfo

private const val SCREEN_TITLE = "A Igreja"
private const val ADDRESS_TITLE = "Endereço"
private const val SERVICES_TITLE = "Cultos"
private const val INSTAGRAM_TITLE = "Instagram"
private const val MAP_LABEL = "Mapa"
private const val OPEN_LABEL = "Abrir"
private const val DIVIDER_ALPHA = 0.07f
private const val SHADOW_ALPHA = 0.12f
private val CARD_CORNER = 18.dp

@Composable
fun ChurchInfoScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current

    BaseScreen(
        tabName       = SCREEN_TITLE,
        showBackArrow = true,
        onBackClick   = onBackClick,
    ) { innerPadding ->
        ChurchInfoContent(
            onOpenMap       = { context.openMap(ChurchInfo.ADDRESS) },
            onOpenInstagram = { context.openLink(ChurchInfo.INSTAGRAM_URL) },
            modifier        = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun ChurchInfoContent(
    onOpenMap: () -> Unit,
    onOpenInstagram: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionTitle(ADDRESS_TITLE)
        InfoCard {
            InfoRow(Icons.Filled.Place, ChurchInfo.ADDRESS, ChurchInfo.NAME) { RowAction(MAP_LABEL, onOpenMap) }
        }

        SectionTitle(SERVICES_TITLE)
        InfoCard {
            ChurchInfo.services.forEachIndexed { index, service ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
                InfoRow(Icons.Filled.AccessTime, service.name, "${service.day}, ${service.time}")
            }
        }

        SectionTitle(INSTAGRAM_TITLE)
        InfoCard {
            InfoRow(ImageVector.vectorResource(R.drawable.ic_ph_instagram_logo), ChurchInfo.INSTAGRAM_HANDLE) {
                RowAction(OPEN_LABEL, onOpenInstagram)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(CARD_CORNER)
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(6.dp, shape, spotColor = Color.Black.copy(alpha = SHADOW_ALPHA))
            .clip(shape)
            .background(highlightCardColor()),
        content = content,
    )
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier          = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier           = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action?.invoke()
    }
}

@Composable
private fun RowAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(label, fontWeight = FontWeight.SemiBold) }
}
