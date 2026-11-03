package com.ipb.castelobranco.features.events.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.features.events.presentation.state.EventListItemUi
import com.ipb.castelobranco.features.events.presentation.state.EventsListUiState
import com.ipb.castelobranco.features.events.presentation.viewmodel.EventsListViewModel

private const val SCREEN_TITLE = "Eventos"
private const val EMPTY_MESSAGE = "Nenhum evento programado."
private const val ERROR_MESSAGE = "Não foi possível carregar os eventos."
private const val RETRY_LABEL = "Tentar novamente"
private const val MEMBERS_ONLY_LABEL = "Só membros"
private const val IMAGE_ASPECT_RATIO = 3f / 2f
private val THUMBNAIL_WIDTH = 108.dp
private val CARD_CORNER = 18.dp
private val THUMBNAIL_CORNER = 12.dp

@Composable
fun EventsListScreen(
    onBackClick: () -> Unit,
    onEventClick: (Int) -> Unit,
    viewModel: EventsListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BaseScreen(
        tabName       = SCREEN_TITLE,
        showBackArrow = true,
        onBackClick   = onBackClick,
    ) { innerPadding ->
        EventsListContent(
            state        = state,
            imageLoader  = viewModel.imageLoader,
            onRefresh    = viewModel::refresh,
            onEventClick = onEventClick,
            modifier     = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun EventsListContent(
    state: EventsListUiState,
    imageLoader: ImageLoader,
    onRefresh: () -> Unit,
    onEventClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    ElasticPullToRefresh(
        isRefreshing = state.isRefreshing,
        onRefresh    = onRefresh,
        modifier     = modifier.fillMaxSize(),
    ) {
        // Always a list, even with one message: pulling to refresh needs something that scrolls.
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.loadFailed -> item { ErrorMessage(onRetry = onRefresh) }
                state.events.isEmpty() -> item { CenteredMessage(EMPTY_MESSAGE) }
                else -> items(state.events, key = { it.id }) { event ->
                    EventListItem(event, imageLoader, onClick = { onEventClick(event.id) })
                }
            }
        }
    }
}

@Composable
private fun EventListItem(event: EventListItemUi, imageLoader: ImageLoader, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CARD_CORNER))
            .background(highlightCardColor())
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model              = event.imageUrl,
            imageLoader        = imageLoader,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .width(THUMBNAIL_WIDTH)
                .aspectRatio(IMAGE_ASPECT_RATIO)
                .clip(RoundedCornerShape(THUMBNAIL_CORNER))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text       = event.title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(event.dateLabel, style = MaterialTheme.typography.bodyMedium)
            val details = listOfNotNull(event.location, MEMBERS_ONLY_LABEL.takeIf { event.isMembersOnly })
            if (details.isNotEmpty()) {
                Text(
                    text     = details.joinToString(" · "),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Text(
        text      = text,
        style     = MaterialTheme.typography.bodyLarge,
        color     = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier  = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
    )
}

@Composable
private fun ErrorMessage(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CenteredMessage(ERROR_MESSAGE)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(RETRY_LABEL) }
    }
}
