package com.ipb.castelobranco.features.admin.events.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.admin.events.domain.model.EventStatusFilter
import com.ipb.castelobranco.features.admin.events.presentation.components.EventAdminCard
import com.ipb.castelobranco.features.admin.events.presentation.state.EventAdminItemUi
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminEvent
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminListUiState
import com.ipb.castelobranco.features.admin.events.presentation.viewmodel.EventsAdminListViewModel

@Composable
fun EventsAdminListScreen(
    viewModel: EventsAdminListViewModel,
    onBack: () -> Unit,
    onOpenEvent: (Int) -> Unit,
    onNewEvent: () -> Unit,
    onNavigationEvent: (EventsAdminEvent) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Back from the form the list shows what was saved or deleted.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EventsAdminEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> onNavigationEvent(event)
            }
        }
    }

    BaseScreen(
        tabName = "Eventos",
        showBackArrow = true,
        onBackClick = onBack,
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            EventsAdminListContent(
                state = state,
                imageLoader = viewModel.imageLoader,
                onFilterSelected = viewModel::onFilterSelected,
                onOpenEvent = onOpenEvent,
                onRetry = viewModel::load,
            )
            if (state.canCreate) {
                ExtendedFloatingActionButton(
                    onClick = onNewEvent,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Novo evento") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                )
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun EventsAdminListContent(
    state: EventsAdminListUiState,
    imageLoader: ImageLoader,
    onFilterSelected: (EventStatusFilter) -> Unit,
    onOpenEvent: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            listOf(EventStatusFilter.ACTIVE, EventStatusFilter.PAST, EventStatusFilter.ALL).forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) },
                )
            }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.items.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null && state.items.isEmpty() -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                ) {
                    Text(state.error, textAlign = TextAlign.Center)
                    Button(onClick = onRetry) { Text("Tentar novamente") }
                }
                state.items.isEmpty() -> Text(
                    text = "Nenhum evento.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        EventAdminCard(item = item, imageLoader = imageLoader, onClick = { onOpenEvent(item.id) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventsAdminListContentPreview() {
    EventsAdminListContent(
        state = EventsAdminListUiState(
            isLoading = false,
            items = listOf(
                EventAdminItemUi(1, "Congresso de Jovens", "14 a 16 nov · 19:30", "", false, false),
                EventAdminItemUi(2, "Assembleia", "30 nov", "", true, false),
                EventAdminItemUi(3, "Almoço de Páscoa", "5 abr", "", false, true),
            ),
            canCreate = true,
        ),
        imageLoader = ImageLoader(LocalContext.current),
        onFilterSelected = {},
        onOpenEvent = {},
        onRetry = {},
    )
}
