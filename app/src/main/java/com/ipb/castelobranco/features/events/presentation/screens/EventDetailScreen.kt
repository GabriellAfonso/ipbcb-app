package com.ipb.castelobranco.features.events.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.events.presentation.state.EventDetailUi
import com.ipb.castelobranco.features.events.presentation.state.EventDetailUiState
import com.ipb.castelobranco.features.events.presentation.viewmodel.EventDetailViewModel

private const val SCREEN_TITLE = "Evento"
private const val GONE_MESSAGE = "Este evento não está mais disponível."
private const val MEMBERS_ONLY_LABEL = "Só membros"
private const val IMAGE_ASPECT_RATIO = 16f / 9f

@Composable
fun EventDetailScreen(
    onBackClick: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BaseScreen(
        tabName       = SCREEN_TITLE,
        showBackArrow = true,
        onBackClick   = onBackClick,
    ) { innerPadding ->
        EventDetailContent(
            state       = state,
            imageLoader = viewModel.imageLoader,
            modifier    = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun EventDetailContent(
    state: EventDetailUiState,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
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
        else -> EventDetailBody(event = event, imageLoader = imageLoader, modifier = modifier)
    }
}

@Composable
private fun EventDetailBody(
    event: EventDetailUi,
    imageLoader: ImageLoader,
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

        Column(
            modifier            = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text       = event.title,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            DetailRow(icon = Icons.Filled.CalendarMonth, text = event.dateLabel)
            event.timeLabel?.let { DetailRow(icon = Icons.Filled.AccessTime, text = it) }
            event.location?.let { DetailRow(icon = Icons.Filled.Place, text = it) }
            if (event.isMembersOnly) DetailRow(icon = Icons.Filled.Lock, text = MEMBERS_ONLY_LABEL)
            event.description?.let {
                Text(
                    text     = it,
                    style    = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp),
        )
        Text(
            text     = text,
            style    = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
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
