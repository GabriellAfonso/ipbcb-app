package com.ipb.castelobranco.features.church.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.components.openLink
import com.ipb.castelobranco.core.presentation.components.openMap
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.core.presentation.theme.onPrimaryLight
import com.ipb.castelobranco.features.church.presentation.state.ChurchInfo
import com.ipb.castelobranco.features.church.presentation.state.ChurchService
import com.ipb.castelobranco.features.church.presentation.state.ServiceBadge
import com.ipb.castelobranco.features.church.presentation.state.ServiceHighlight
import com.ipb.castelobranco.features.church.presentation.state.highlightAt
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val SCREEN_TITLE = "A Igreja"
private const val DIRECTIONS_LABEL = "Como chegar"
private const val INSTAGRAM_LABEL = "Instagram"
private const val OFFERINGS_LABEL = "Ofertas"
private const val SERVICES_TITLE = "Horários"
private const val OFFERINGS_TITLE = "Dízimos e ofertas"
private const val OFFERINGS_HINT = "Aponte a câmera do app do banco ou copie o código."
private const val COPY_LABEL = "Copiar código PIX"
private const val COPIED_LABEL = "Copiado"
private const val LIVE_LABEL = "Agora"
private const val NEXT_LABEL = "Próximo"
private const val PIX_CLIP_LABEL = "PIX"
private const val COPIED_FEEDBACK_MS = 2_000L
private const val CLOCK_TICK_MS = 60_000L
private const val ACTION_ALPHA = 0.13f
private const val ADDRESS_ALPHA = 0.92f
private const val DENOMINATION_ALPHA = 0.78f
private const val DIVIDER_ALPHA = 0.07f
private const val SHADOW_ALPHA = 0.12f
private val CARD_CORNER = 18.dp
private val HERO_CORNER = 28.dp
private val LOGO_SIZE = 64.dp
private val QR_SIZE = 200.dp

/** Same minimum height for every service row: a one-service day is not taller than a two-service day (spec §2). */
private val SERVICE_ROW_MIN_HEIGHT = 44.dp

/**
 * Inter's default digits are proportional ("1" is narrower than "0"), so "19:30" and "09:00" would not line up. Its
 * tabular figures give every digit the same width: the digits stack and the names start at the same x.
 */
private const val TABULAR_FIGURES = "tnum"

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_NAMES = mapOf(
    DayOfWeek.MONDAY to "Segunda",
    DayOfWeek.TUESDAY to "Terça",
    DayOfWeek.WEDNESDAY to "Quarta",
    DayOfWeek.THURSDAY to "Quinta",
    DayOfWeek.FRIDAY to "Sexta",
    DayOfWeek.SATURDAY to "Sábado",
    DayOfWeek.SUNDAY to "Domingo",
)

@Composable
fun ChurchInfoScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            delay(CLOCK_TICK_MS)
            value = LocalDateTime.now()
        }
    }
    val highlight = remember(now) { ChurchInfo.services.highlightAt(now) }

    BaseScreen(
        tabName       = SCREEN_TITLE,
        showBackArrow = true,
        onBackClick   = onBackClick,
    ) { innerPadding ->
        ChurchInfoContent(
            services        = ChurchInfo.services,
            highlight       = highlight,
            onOpenMap       = { context.openMap(ChurchInfo.ADDRESS) },
            onOpenInstagram = { context.openLink(ChurchInfo.INSTAGRAM_URL) },
            onCopyPix       = { context.copyPixCode() },
            modifier        = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun ChurchInfoContent(
    services: List<ChurchService>,
    highlight: ServiceHighlight?,
    onOpenMap: () -> Unit,
    onOpenInstagram: () -> Unit,
    onCopyPix: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOfferings by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ChurchHeader(
            onOpenMap       = onOpenMap,
            onOpenInstagram = onOpenInstagram,
            onOpenOfferings = { showOfferings = true },
        )
        Text(
            text       = SERVICES_TITLE,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        )
        ServicesCard(services, highlight)
        Spacer(Modifier.height(32.dp))
    }

    if (showOfferings) OfferingsSheet(onCopy = onCopyPix, onDismiss = { showOfferings = false })
}

/** The green "business card": logo, name, address and the three quick actions. */
@Composable
private fun ChurchHeader(onOpenMap: () -> Unit, onOpenInstagram: () -> Unit, onOpenOfferings: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = HERO_CORNER, bottomEnd = HERO_CORNER))
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter            = painterResource(R.drawable.ipb_white_logo),
            contentDescription = null,
            modifier           = Modifier.size(LOGO_SIZE),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text       = ChurchInfo.NAME,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = onPrimaryLight,
        )
        Text(
            text  = ChurchInfo.DENOMINATION,
            style = MaterialTheme.typography.bodyMedium,
            color = onPrimaryLight.copy(alpha = DENOMINATION_ALPHA),
        )
        Row(
            modifier          = Modifier.padding(top = 10.dp).widthIn(max = 300.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector        = Icons.Filled.Place,
                contentDescription = null,
                tint               = onPrimaryLight.copy(alpha = ADDRESS_ALPHA),
                modifier           = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text  = ChurchInfo.ADDRESS,
                style = MaterialTheme.typography.bodyMedium,
                color = onPrimaryLight.copy(alpha = ADDRESS_ALPHA),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderAction(Icons.Filled.Directions, DIRECTIONS_LABEL, onOpenMap, Modifier.weight(1f))
            HeaderAction(
                ImageVector.vectorResource(R.drawable.ic_ph_instagram_logo),
                INSTAGRAM_LABEL,
                onOpenInstagram,
                Modifier.weight(1f),
            )
            HeaderAction(Icons.Filled.Pix, OFFERINGS_LABEL, onOpenOfferings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeaderAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(onPrimaryLight.copy(alpha = ACTION_ALPHA))
            .clickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = onPrimaryLight, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = onPrimaryLight)
    }
}

/** Services grouped by day, in the list's order (Monday first, so Sunday last). */
@Composable
private fun ServicesCard(services: List<ChurchService>, highlight: ServiceHighlight?) {
    val shape = RoundedCornerShape(CARD_CORNER)
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(6.dp, shape, spotColor = Color.Black.copy(alpha = SHADOW_ALPHA))
            .clip(shape)
            .background(highlightCardColor()),
    ) {
        services.groupBy { it.day }.entries.forEachIndexed { index, (day, dayServices) ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    text          = DAY_NAMES.getValue(day).uppercase(),
                    style         = MaterialTheme.typography.labelMedium,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 0.7.sp,
                    lineHeight    = 16.sp,
                    color         = MaterialTheme.colorScheme.primary,
                    modifier      = Modifier.padding(top = 6.dp),
                )
                dayServices.forEach { service ->
                    ServiceRow(service, badge = highlight?.takeIf { it.service == service }?.badge)
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(service: ChurchService, badge: ServiceBadge?) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .heightIn(min = SERVICE_ROW_MIN_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text       = service.time.format(TIME_FORMAT),
            style      = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
            softWrap   = false,
        )
        Spacer(Modifier.width(14.dp))
        Text(service.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        badge?.let { ServiceBadgeChip(it) }
    }
}

@Composable
private fun ServiceBadgeChip(badge: ServiceBadge) {
    val live = badge == ServiceBadge.LIVE
    Text(
        text     = if (live) LIVE_LABEL else NEXT_LABEL,
        style    = MaterialTheme.typography.labelSmall,
        color    = if (live) onPrimaryLight else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (live) ipbGreen else MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** QR code plus "Copiar código PIX", which reads "Copiado" for 2 s after a tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferingsSheet(onCopy: () -> Unit, onDismiss: () -> Unit) {
    var copied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS)
            copied = false
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(OFFERINGS_TITLE, style = MaterialTheme.typography.titleLarge)
            Text(
                text      = OFFERINGS_HINT,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            // Its own white background: it must scan in dark mode too.
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter            = painterResource(R.drawable.church_pix_qr),
                    contentDescription = "QR code PIX",
                    modifier           = Modifier.size(QR_SIZE).clip(RoundedCornerShape(12.dp)),
                )
            }
            Button(
                onClick  = {
                    onCopy()
                    copied = true
                },
                colors   = ButtonDefaults.buttonColors(containerColor = ipbGreen, contentColor = onPrimaryLight),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector        = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (copied) COPIED_LABEL else COPY_LABEL)
            }
        }
    }
}

private fun Context.copyPixCode() {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(PIX_CLIP_LABEL, ChurchInfo.PIX_CODE))
}
