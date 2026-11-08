package com.ipb.castelobranco.features.church.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.components.openLink
import com.ipb.castelobranco.core.presentation.components.openMap
import com.ipb.castelobranco.features.church.presentation.state.ChurchInfo
import kotlinx.coroutines.delay

private const val SCREEN_TITLE = "A Igreja"
private const val ADDRESS_TITLE = "Endereço"
private const val SERVICES_TITLE = "Cultos"
private const val INSTAGRAM_TITLE = "Instagram"
private const val MAP_LABEL = "Mapa"
private const val OPEN_LABEL = "Abrir"
private const val OFFERINGS_TITLE = "Dízimos e ofertas"
private const val PIX_TITLE = "PIX"
private const val QR_LABEL = "QR"
private const val COPY_LABEL = "Copiar"
private const val COPIED_LABEL = "Copiado"
private const val CLOSE_LABEL = "Fechar"
private const val PIX_CLIP_LABEL = "PIX"
private const val COPIED_FEEDBACK_MS = 2_000L
private val QR_SIZE = 220.dp
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
            onCopyPix       = { context.copyPixCode() },
            modifier        = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun ChurchInfoContent(
    onOpenMap: () -> Unit,
    onOpenInstagram: () -> Unit,
    onCopyPix: () -> Unit,
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

        SectionTitle(OFFERINGS_TITLE)
        InfoCard { PixRow(onCopyPix) }

        Spacer(Modifier.height(32.dp))
    }
}

/** "PIX", with "QR" (opens the code in a dialog) and "Copiar" (the copy-and-paste code). */
@Composable
private fun PixRow(onCopy: () -> Unit) {
    var copied by rememberSaveable { mutableStateOf(false) }
    var showQr by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS)
            copied = false
        }
    }
    InfoRow(Icons.Filled.QrCode2, PIX_TITLE) {
        Row {
            RowAction(QR_LABEL) { showQr = true }
            RowAction(if (copied) COPIED_LABEL else COPY_LABEL) {
                onCopy()
                copied = true
            }
        }
    }
    if (showQr) {
        AlertDialog(
            onDismissRequest = { showQr = false },
            confirmButton    = { TextButton(onClick = { showQr = false }) { Text(CLOSE_LABEL) } },
            title            = { Text(PIX_TITLE) },
            // Its own white background: it must scan in dark mode too.
            text             = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter            = painterResource(R.drawable.church_pix_qr),
                        contentDescription = "QR code PIX",
                        modifier           = Modifier.size(QR_SIZE).clip(RoundedCornerShape(12.dp)),
                    )
                }
            },
        )
    }
}

private fun Context.copyPixCode() {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(PIX_CLIP_LABEL, ChurchInfo.PIX_CODE))
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
