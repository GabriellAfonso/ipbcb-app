package com.ipb.castelobranco.features.events.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.features.events.presentation.state.EventDetailUi
import com.ipb.castelobranco.features.events.presentation.state.EventDetailUiState
import com.ipb.castelobranco.features.events.presentation.viewmodel.EventDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SCREEN_TITLE = "Evento"
private const val GONE_MESSAGE = "Este evento não está mais disponível."
private const val MEMBERS_ONLY_LABEL = "Só membros"
private const val IMAGE_ASPECT_RATIO = 3f / 2f
private const val SHARE_LABEL = "Compartilhar"
private const val MAP_LABEL = "Mapa"
private const val ABOUT_TITLE = "Sobre"
private const val CHURCH_NAME = "IPB Castelo Branco"
private const val TEXT_MIME = "text/plain"
private const val IMAGE_MIME = "image/jpeg"
private const val SHARE_DIR = "shared"
private const val SHARE_FILE_NAME = "evento.jpg"
private const val JPEG_QUALITY = 90
private const val DIVIDER_ALPHA = 0.07f
private const val SHADOW_ALPHA = 0.12f
private val CARD_CORNER = 18.dp

@Composable
fun EventDetailScreen(
    onBackClick: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val event = state.event

    BaseScreen(
        tabName       = SCREEN_TITLE,
        showBackArrow = true,
        onBackClick   = onBackClick,
        extraActions  = {
            if (event != null) {
                IconButton(onClick = { scope.launch { context.shareEvent(event, viewModel.imageLoader) } }) {
                    Icon(Icons.Filled.Share, contentDescription = SHARE_LABEL, tint = Color.White)
                }
            }
        },
    ) { innerPadding ->
        EventDetailContent(
            state       = state,
            imageLoader = viewModel.imageLoader,
            modifier    = Modifier.padding(innerPadding),
            onOpenMap   = { place -> context.openMap(place) },
        )
    }
}

@Composable
fun EventDetailContent(
    state: EventDetailUiState,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    onOpenMap: (String) -> Unit = {},
) {
    val event = state.event
    when {
        state.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        event == null -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text      = GONE_MESSAGE,
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        else -> EventDetailBody(event = event, imageLoader = imageLoader, onOpenMap = onOpenMap, modifier = modifier)
    }
}

/** "Cartaz em tela cheia" (`specs/events/spec.md` §3). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventDetailBody(
    event: EventDetailUi,
    imageLoader: ImageLoader,
    onOpenMap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AsyncImage(
            model              = event.imageUrl,
            imageLoader        = imageLoader,
            contentDescription = event.title,
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT_RATIO)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )

        Text(
            text       = event.title,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
        )
        FlowRow(
            modifier              = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement   = Arrangement.spacedBy(6.dp),
        ) {
            InfoTag(Icons.Filled.CalendarMonth, event.shortDateLabel)
            event.timeLabel?.let { InfoTag(Icons.Filled.AccessTime, it) }
            if (event.isMembersOnly) InfoTag(Icons.Filled.Lock, MEMBERS_ONLY_LABEL)
        }

        InfoCard {
            InfoRow(icon = Icons.Filled.CalendarMonth, title = event.dateLabel, subtitle = event.durationLabel)
            event.location?.let { place ->
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
                InfoRow(icon = Icons.Filled.Place, title = place, subtitle = CHURCH_NAME) {
                    TextButton(onClick = { onOpenMap(place) }) { Text(MAP_LABEL, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        event.description?.let {
            SectionTitle(ABOUT_TITLE)
            Text(
                text     = it,
                style    = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun InfoTag(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier           = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text       = text,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
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
    subtitle: String?,
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
private fun SectionTitle(text: String) {
    Row(
        modifier          = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** The poster with [EventDetailUi.shareText] as its caption; just the text when the poster can't be loaded. */
private suspend fun Context.shareEvent(event: EventDetailUi, imageLoader: ImageLoader) {
    val poster = savePosterForSharing(event.imageUrl, imageLoader)
    val send = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, event.shareText)
        if (poster == null) {
            type = TEXT_MIME
        } else {
            type = IMAGE_MIME
            putExtra(Intent.EXTRA_STREAM, poster)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    startActivity(Intent.createChooser(send, null))
}

/** Reads the poster through the event loader (usually from its cache) into a shareable JPEG. */
private suspend fun Context.savePosterForSharing(url: String, imageLoader: ImageLoader): Uri? {
    if (url.isBlank()) return null
    val request = ImageRequest.Builder(this).data(url).allowHardware(false).build()
    val drawable = (imageLoader.execute(request) as? SuccessResult)?.drawable ?: return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val file = cacheDir.resolve(SHARE_DIR).apply { mkdirs() }.resolve(SHARE_FILE_NAME)
            file.outputStream().use { drawable.toBitmap().compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            FileProvider.getUriForFile(this@savePosterForSharing, "$packageName.provider", file)
        }.getOrNull()
    }
}

/** The maps app, searching for [place]; nothing happens without one. */
private fun Context.openMap(place: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(place)))
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No maps app on the phone.
    }
}

@Preview(showBackground = true)
@Composable
private fun EventDetailContentPreview() {
    EventDetailContent(
        state = EventDetailUiState(
            isLoading = false,
            event = EventDetailUi(
                title         = "Congresso de Jovens",
                dateLabel     = "14 a 16 de novembro",
                timeLabel     = "19:30",
                location      = "Templo principal",
                description   = "Três noites de louvor e palavra. Traga um amigo!",
                imageUrl      = "",
                isMembersOnly = false,
                shortDateLabel = "14 a 16 nov",
                durationLabel = "3 dias",
            ),
        ),
        imageLoader = ImageLoader(LocalContext.current),
    )
}

@Preview(showBackground = true)
@Composable
private fun EventDetailGonePreview() {
    EventDetailContent(
        state       = EventDetailUiState(isLoading = false, isGone = true),
        imageLoader = ImageLoader(LocalContext.current),
    )
}
